package com.herron.exchange.pricingengine.server.marketdata;

import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataPrice;
import com.herron.exchange.common.api.common.messages.marketdata.requests.MarketDataYieldCurveRequest;
import com.herron.exchange.common.api.common.messages.marketdata.response.MarketDataYieldCurveResponse;
import com.herron.exchange.pricingengine.server.marketdata.external.ExternalMarketDataHandler;

import java.util.List;

public class MarketDataHandler {

    private final ExternalMarketDataHandler externalMarketDataHandler;

    public MarketDataHandler(ExternalMarketDataHandler externalMarketDataHandler) {
        this.externalMarketDataHandler = externalMarketDataHandler;
    }

    public List<MarketDataPrice> getPreviousDaySettlementPrices() {
        return externalMarketDataHandler.getPreviousDaySettlementPrices();
    }

    public MarketDataYieldCurveResponse getEntry(MarketDataYieldCurveRequest request) {
        return null;
    }
}
