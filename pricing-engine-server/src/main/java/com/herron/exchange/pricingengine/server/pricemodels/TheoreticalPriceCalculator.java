package com.herron.exchange.pricingengine.server.pricemodels;

import com.herron.exchange.common.api.common.api.referencedata.instruments.Instrument;
import com.herron.exchange.common.api.common.messages.common.Price;

public class TheoreticalPriceCalculator {

    public static Price calculatePrice(Instrument instrument) {
        return switch (instrument.priceModel()) {
            case BASIC_FUTURE_MODEL -> Price.EMPTY;
            case BLACK_SCHOLES -> Price.EMPTY;
            case BOND_DISCOUNT -> Price.EMPTY;
            case INTANGIBLE -> Price.EMPTY;
        };
    }
}
