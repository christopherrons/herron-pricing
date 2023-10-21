package com.herron.exchange.pricingengine.server.marketdata.external;

import com.herron.exchange.common.api.common.api.marketdata.MarketDataPrice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ExternalMarketDataHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalMarketDataHandler.class);
    private final EurexPreviousDaySettlementHandler eurexPreviousDaySettlementHandler;

    public ExternalMarketDataHandler(EurexPreviousDaySettlementHandler eurexPreviousDaySettlementHandler) {
        this.eurexPreviousDaySettlementHandler = eurexPreviousDaySettlementHandler;
    }

    public List<MarketDataPrice> getPreviousDaySettlementPrices() {
        return eurexPreviousDaySettlementHandler.getPreviousDaySettlementPrices();
    }
}
