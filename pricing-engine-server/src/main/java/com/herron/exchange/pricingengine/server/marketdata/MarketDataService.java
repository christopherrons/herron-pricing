package com.herron.exchange.pricingengine.server.marketdata;

import com.herron.exchange.common.api.common.api.marketdata.MarketDataEntry;
import com.herron.exchange.common.api.common.api.marketdata.MarketDataRequest;
import com.herron.exchange.common.api.common.api.marketdata.StaticKey;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataPrice;
import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataYieldCurve;
import com.herron.exchange.common.api.common.messages.marketdata.requests.MarketDataPriceRequest;
import com.herron.exchange.common.api.common.messages.marketdata.requests.MarketDataYieldCurveRequest;
import com.herron.exchange.common.api.common.messages.marketdata.response.ImmutableMarketDataPriceResponse;
import com.herron.exchange.common.api.common.messages.marketdata.response.ImmutableMarketDataYieldCurveResponse;
import com.herron.exchange.common.api.common.messages.marketdata.response.MarketDataPriceResponse;
import com.herron.exchange.common.api.common.messages.marketdata.response.MarketDataYieldCurveResponse;
import com.herron.exchange.pricingengine.server.marketdata.external.ExternalMarketDataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.herron.exchange.common.api.common.enums.Status.OK;

public class MarketDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarketDataService.class);

    private final ExternalMarketDataHandler externalMarketDataHandler;
    private final ImpliedVolatilityCalculator impliedVolatilityHandler;
    private final Map<StaticKey, MarketDataRepository> keyToRepository = new ConcurrentHashMap<>();

    public MarketDataService(ExternalMarketDataHandler externalMarketDataHandler) {
        this.externalMarketDataHandler = externalMarketDataHandler;
        this.impliedVolatilityHandler = new ImpliedVolatilityCalculator(this);
    }

    public void init() {
        LOGGER.info("Init Market Data Repository.");
        ExecutorService executor = Executors.newFixedThreadPool(3);
        Runnable task1 = () -> externalMarketDataHandler.getPreviousDaySettlementPrices().forEach(this::addEntry);
        Runnable task2 = () -> externalMarketDataHandler.getYieldCurves(LocalDate.now().minusDays(50), LocalDate.now()).forEach(this::addEntry);
        executor.submit(task1);
        executor.submit(task2);
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
        }

        impliedVolatilityHandler.createSurfaces(Timestamp.now());
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

    private MarketDataEntry getEntry(MarketDataRequest request) {
        if (!keyToRepository.containsKey(request.staticKey())) {
            return null;
        }

        return keyToRepository.get(request.staticKey()).getEntry(request);
    }
}
