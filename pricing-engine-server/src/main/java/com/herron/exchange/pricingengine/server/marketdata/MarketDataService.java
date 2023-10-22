package com.herron.exchange.pricingengine.server.marketdata;

import com.herron.exchange.common.api.common.api.marketdata.MarketDataEntry;
import com.herron.exchange.common.api.common.api.marketdata.StaticKey;
import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataPrice;
import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataYieldCurve;
import com.herron.exchange.common.api.common.messages.marketdata.requests.MarketDataYieldCurveRequest;
import com.herron.exchange.common.api.common.messages.marketdata.response.ImmutableMarketDataYieldCurveResponse;
import com.herron.exchange.common.api.common.messages.marketdata.response.MarketDataYieldCurveResponse;
import com.herron.exchange.pricingengine.server.marketdata.external.ExternalMarketDataHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.herron.exchange.common.api.common.enums.Status.OK;

public class MarketDataService {

    private final ExternalMarketDataHandler externalMarketDataHandler;
    private final Map<StaticKey, MarketDataRepository> keyToRepository = new ConcurrentHashMap<>();

    public MarketDataService(ExternalMarketDataHandler externalMarketDataHandler) {
        this.externalMarketDataHandler = externalMarketDataHandler;
    }

    public List<MarketDataPrice> getPreviousDaySettlementPrices() {
        return externalMarketDataHandler.getPreviousDaySettlementPrices();
    }

    public void addEntry(MarketDataEntry entry) {
        keyToRepository.computeIfAbsent(entry.staticKey(), MarketDataRepository::new).addEntry(entry);
    }

    public MarketDataYieldCurveResponse getEntry(MarketDataYieldCurveRequest request) {
        if (!keyToRepository.containsKey(request.staticKey())) {
            return MarketDataYieldCurveResponse.createErrorResponse(String.format("No matching key found: %s.", request));
        }
        var entry = keyToRepository.get(request.staticKey()).getEntry(request);
        if (entry == null) {
            return MarketDataYieldCurveResponse.createErrorResponse(String.format("No matching entries found: %s.", request));
        }

        return ImmutableMarketDataYieldCurveResponse.builder()
                .status(OK)
                .yieldCurveEntry((MarketDataYieldCurve) entry)
                .build();
    }
}
