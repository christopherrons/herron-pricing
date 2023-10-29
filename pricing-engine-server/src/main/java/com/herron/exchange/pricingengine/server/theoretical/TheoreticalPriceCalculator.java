package com.herron.exchange.pricingengine.server.theoretical;

import com.herron.exchange.common.api.common.api.pricing.PriceModelResult;
import com.herron.exchange.common.api.common.api.referencedata.instruments.BondInstrument;
import com.herron.exchange.common.api.common.api.referencedata.instruments.FutureInstrument;
import com.herron.exchange.common.api.common.api.referencedata.instruments.Instrument;
import com.herron.exchange.common.api.common.api.referencedata.instruments.OptionInstrument;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.pricing.FailedPriceModelResult;
import com.herron.exchange.pricingengine.server.theoretical.derivatives.futures.FuturesCalculator;
import com.herron.exchange.pricingengine.server.theoretical.derivatives.options.OptionCalculator;
import com.herron.exchange.pricingengine.server.theoretical.fixedincome.bonds.BondPriceCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TheoreticalPriceCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TheoreticalPriceCalculator.class);
    private final BondPriceCalculator bondPriceCalculator;
    private final OptionCalculator optionCalculator;
    private final FuturesCalculator futuresCalculator;

    public TheoreticalPriceCalculator(BondPriceCalculator bondPriceCalculator, OptionCalculator optionCalculator, FuturesCalculator futuresCalculator) {
        this.bondPriceCalculator = bondPriceCalculator;
        this.optionCalculator = optionCalculator;
        this.futuresCalculator = futuresCalculator;
    }

    public PriceModelResult calculatePrice(Instrument instrument) {
        try {
            return calculate(instrument, Timestamp.now());
        } catch (Exception e) {
            return createFailedResult(String.format("Unhandled exception when calculating result for instrument %s: %s", instrument, e));
        }
    }

    private PriceModelResult calculate(Instrument instrument, Timestamp valuationTime) {
        return switch (instrument.instrumentType()) {
            case BILL, BOND, PERPETUAL_BOND -> bondPriceCalculator.calculate((BondInstrument) instrument, valuationTime);
            case OPTION -> optionCalculator.calculate((OptionInstrument) instrument, valuationTime);
            case FUTURE -> futuresCalculator.calculate((FutureInstrument) instrument, valuationTime);
            default -> createFailedResult(String.format("Instrument type %s not supported.", instrument));
        };
    }

    private PriceModelResult createFailedResult(String reason) {
        LOGGER.error(reason);
        return FailedPriceModelResult.createFailedResult(reason);
    }
}
