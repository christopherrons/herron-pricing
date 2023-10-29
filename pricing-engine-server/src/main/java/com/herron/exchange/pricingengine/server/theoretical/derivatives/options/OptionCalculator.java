package com.herron.exchange.pricingengine.server.theoretical.derivatives.options;

import com.herron.exchange.common.api.common.api.pricing.PriceModelResult;
import com.herron.exchange.common.api.common.api.referencedata.instruments.OptionInstrument;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.pricing.FailedPriceModelResult;
import com.herron.exchange.pricingengine.server.marketdata.MarketDataService;

public class OptionCalculator {
    private final MarketDataService marketDataService;

    public OptionCalculator(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public PriceModelResult calculate(OptionInstrument instrument, Timestamp valuationTime) {
        return switch (instrument.priceModel()) {
            case BLACK_SCHOLES -> FailedPriceModelResult.createFailedResult("");
            default -> FailedPriceModelResult.createFailedResult(String.format("Option price model %s not supported.", instrument));
        };
    }
}
