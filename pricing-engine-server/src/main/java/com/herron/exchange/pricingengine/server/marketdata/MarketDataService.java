package com.herron.exchange.pricingengine.server.marketdata;

import com.herron.exchange.common.api.common.api.marketdata.MarketDataEntry;
import com.herron.exchange.common.api.common.api.marketdata.MarketDataRequest;
import com.herron.exchange.common.api.common.api.marketdata.StaticKey;
import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataPrice;
import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataYieldCurve;
import com.herron.exchange.common.api.common.messages.marketdata.requests.MarketDataPriceRequest;
import com.herron.exchange.common.api.common.messages.marketdata.requests.MarketDataYieldCurveRequest;
import com.herron.exchange.common.api.common.messages.marketdata.response.ImmutableMarketDataPriceResponse;
import com.herron.exchange.common.api.common.messages.marketdata.response.ImmutableMarketDataYieldCurveResponse;
import com.herron.exchange.common.api.common.messages.marketdata.response.MarketDataPriceResponse;
import com.herron.exchange.common.api.common.messages.marketdata.response.MarketDataYieldCurveResponse;
import com.herron.exchange.pricingengine.server.marketdata.external.ExternalMarketDataHandler;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.herron.exchange.common.api.common.enums.Status.OK;

public class MarketDataService {

    private final ExternalMarketDataHandler externalMarketDataHandler;
    private final Map<StaticKey, MarketDataRepository> keyToRepository = new ConcurrentHashMap<>();

    public MarketDataService(ExternalMarketDataHandler externalMarketDataHandler) {
        this.externalMarketDataHandler = externalMarketDataHandler;
    }

    public void init() {
        externalMarketDataHandler.getPreviousDaySettlementPrices().forEach(this::addEntry);
        externalMarketDataHandler.getYieldCurves(LocalDate.now().minusDays(50), LocalDate.now()).forEach(this::addEntry);
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
