package com.herron.exchange.pricingengine.server.pricemodels;

import com.herron.exchange.common.api.common.api.pricing.PriceModelResult;
import com.herron.exchange.common.api.common.api.referencedata.instruments.BondInstrument;
import com.herron.exchange.common.api.common.api.referencedata.instruments.FutureInstrument;
import com.herron.exchange.common.api.common.api.referencedata.instruments.Instrument;
import com.herron.exchange.common.api.common.api.referencedata.instruments.OptionInstrument;
import com.herron.exchange.common.api.common.messages.pricing.ImmutableFailedPriceModelResult;
import com.herron.exchange.pricingengine.server.pricemodels.fixedincome.bonds.BondDiscountingPriceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class TheoreticalPriceCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TheoreticalPriceCalculator.class);
    private final BondDiscountingPriceModel bondDiscountingPriceModel;

    public TheoreticalPriceCalculator(BondDiscountingPriceModel bondDiscountingPriceModel) {
        this.bondDiscountingPriceModel = bondDiscountingPriceModel;
    }

    public PriceModelResult calculatePrice(Instrument instrument) {
        try {
            return calculate(instrument);
        } catch (Exception e) {
            return createFailedResult(String.format("Unhandled exception when calculating result for instrument %s: %s", instrument, e));
        }
    }

    private PriceModelResult calculate(Instrument instrument) {
        return switch (instrument.instrumentType()) {
            case BILL, BOND, PERPETUAL_BOND -> calculate((BondInstrument) instrument);
            case OPTION -> calculate((OptionInstrument) instrument);
            case FUTURE -> calculate((FutureInstrument) instrument);
            default -> createFailedResult(String.format("Instrument type %s not supported.", instrument));
        };
    }

    private PriceModelResult calculate(BondInstrument instrument) {
        return switch (instrument.priceModel()) {
            case BOND_DISCOUNT -> bondDiscountingPriceModel.calculateBondPrice(instrument, LocalDate.now());
            default -> createFailedResult(String.format("Bond price model %s not supported.", instrument));
        };
    }

    private PriceModelResult calculate(OptionInstrument instrument) {
        return switch (instrument.priceModel()) {
            case BLACK_SCHOLES -> createFailedResult("");
            default -> createFailedResult(String.format("Option price model %s not supported.", instrument));
        };
    }

    private PriceModelResult calculate(FutureInstrument instrument) {
        return switch (instrument.priceModel()) {
            case BASIC_FUTURE_MODEL -> createFailedResult("");
            default -> createFailedResult(String.format("Future price model %s not supported.", instrument));
        };
    }

    private PriceModelResult createFailedResult(String reason) {
        LOGGER.error(reason);
        return ImmutableFailedPriceModelResult.builder()
                .failReason(reason)
                .build();
    }
}
