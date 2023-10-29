package com.herron.exchange.pricingengine.server.theoretical.derivatives.futures;

import com.herron.exchange.common.api.common.api.pricing.PriceModelResult;
import com.herron.exchange.common.api.common.api.referencedata.instruments.FutureInstrument;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.pricing.FailedPriceModelResult;
import com.herron.exchange.pricingengine.server.marketdata.MarketDataService;

public class FuturesCalculator {
    private final MarketDataService marketDataService;

    public FuturesCalculator(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public PriceModelResult calculate(FutureInstrument instrument, Timestamp valuationTime) {
        return switch (instrument.priceModel()) {
            case BASIC_FUTURE_MODEL -> FailedPriceModelResult.createFailedResult("");
            default -> FailedPriceModelResult.createFailedResult(String.format("Future price model %s not supported.", instrument));
        };
    }
}
