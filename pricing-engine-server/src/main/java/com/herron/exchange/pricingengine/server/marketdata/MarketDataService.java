package com.herron.exchange.pricingengine.server.marketdata;

import com.herron.exchange.common.api.common.api.marketdata.MarketDataEntry;
import com.herron.exchange.common.api.common.api.marketdata.MarketDataRequest;
import com.herron.exchange.common.api.common.api.marketdata.StaticKey;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataForwardPriceCurve;
import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataImpliedVolatilitySurface;
import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataPrice;
import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataYieldCurve;
import com.herron.exchange.common.api.common.messages.marketdata.requests.MarketDataForwardPriceCurveRequest;
import com.herron.exchange.common.api.common.messages.marketdata.requests.MarketDataImpliedVolatilitySurfaceRequest;
import com.herron.exchange.common.api.common.messages.marketdata.requests.MarketDataPriceRequest;
import com.herron.exchange.common.api.common.messages.marketdata.requests.MarketDataYieldCurveRequest;
import com.herron.exchange.common.api.common.messages.marketdata.response.*;
import com.herron.exchange.pricingengine.server.marketdata.external.ExternalMarketDataHandler;
import com.herron.exchange.pricingengine.server.marketdata.internal.ForwardPriceCurveHandler;
import com.herron.exchange.pricingengine.server.marketdata.internal.ImpliedVolatilitySurfaceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.herron.exchange.common.api.common.enums.Status.OK;

public class MarketDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarketDataService.class);

    private final ExternalMarketDataHandler externalMarketDataHandler;
    private final ImpliedVolatilitySurfaceHandler impliedVolatilitySurfaceHandler;
    private final ForwardPriceCurveHandler forwardPriceCurveHandler;
    private final Map<StaticKey, MarketDataRepository> keyToRepository = new ConcurrentHashMap<>();

    public MarketDataService(ExternalMarketDataHandler externalMarketDataHandler) {
        this.externalMarketDataHandler = externalMarketDataHandler;
        this.impliedVolatilitySurfaceHandler = new ImpliedVolatilitySurfaceHandler(this);
        this.forwardPriceCurveHandler = new ForwardPriceCurveHandler(this);
    }

    public void init() {
        LOGGER.info("Init Market Data Repository.");
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Runnable task1 = () ->
                    externalMarketDataHandler.getPreviousDaySettlementPrices().forEach(this::addEntry);

            Runnable task2 = () ->
                    externalMarketDataHandler.getYieldCurves(LocalDate.now().minusDays(50), LocalDate.now()).forEach(this::addEntry);

            Stream.of(task1, task2).forEach(executor::submit);

            executor.shutdown();
            try {
                if (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    System.err.println("Executor did not terminate in the allocated time.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Executor interrupted during termination.");
            }
        }

        forwardPriceCurveHandler.createForwardPriceCurves(Timestamp.now()).forEach(this::addEntry);
        impliedVolatilitySurfaceHandler.createSurfaces(Timestamp.now()).forEach(this::addEntry);
    }

    public void addEntry(MarketDataEntry entry) {
        keyToRepository.computeIfAbsent(entry.staticKey(), MarketDataRepository::new).addEntry(entry);
    }

    public MarketDataPriceResponse getMarketDataPrice(MarketDataPriceRequest request) {
        var entry = getEntry(request);
        if (entry == null) {
            return MarketDataPriceResponse.createErrorResponse(String.format("No matching entry found: %s.", request));
        }
        return ImmutableMarketDataPriceResponse.builder()
                .status(OK)
                .marketDataPrice((MarketDataPrice) entry)
                .build();
    }

    public MarketDataYieldCurveResponse getYieldCurve(MarketDataYieldCurveRequest request) {
        var entry = getEntry(request);
        if (entry == null) {
            return MarketDataYieldCurveResponse.createErrorResponse(String.format("No matching entry found: %s.", request));
        }
        return ImmutableMarketDataYieldCurveResponse.builder()
                .status(OK)
                .yieldCurveEntry((MarketDataYieldCurve) entry)
                .build();
    }

    public MarketDataForwardPriceCurveResponse getForwardPriceCurve(MarketDataForwardPriceCurveRequest request) {
        var entry = getEntry(request);
        if (entry == null) {
            return MarketDataForwardPriceCurveResponse.createErrorResponse(String.format("No matching entry found: %s.", request));
        }
        return ImmutableMarketDataForwardPriceCurveResponse.builder()
                .status(OK)
                .forwardPriceCurveEntry((MarketDataForwardPriceCurve) entry)
                .build();
    }

    public MarketDataImpliedVolatilitySurfaceResponse getImpliedVolatilitySurface(MarketDataImpliedVolatilitySurfaceRequest request) {
        var entry = getEntry(request);
        if (entry == null) {
            return MarketDataImpliedVolatilitySurfaceResponse.createErrorResponse(String.format("No matching entry found: %s.", request));
        }
        return ImmutableMarketDataImpliedVolatilitySurfaceResponse.builder()
                .status(OK)
                .impliedVolatilitySurfaceEntry((MarketDataImpliedVolatilitySurface) entry)
                .build();
    }

    private MarketDataEntry getEntry(MarketDataRequest request) {
        if (!keyToRepository.containsKey(request.staticKey())) {
            return null;
        }

        return keyToRepository.get(request.staticKey()).getEntry(request);
    }
}
