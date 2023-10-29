package com.herron.exchange.pricingengine.server.theoretical.fixedincome.bonds;

import com.herron.exchange.common.api.common.api.pricing.PriceModelResult;
import com.herron.exchange.common.api.common.api.referencedata.instruments.BondInstrument;
import com.herron.exchange.common.api.common.enums.MarketDataRequestTimeFilter;
import com.herron.exchange.common.api.common.enums.Status;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.marketdata.ImmutableDefaultTimeComponentKey;
import com.herron.exchange.common.api.common.messages.marketdata.requests.ImmutableMarketDataYieldCurveRequest;
import com.herron.exchange.common.api.common.messages.marketdata.response.MarketDataYieldCurveResponse;
import com.herron.exchange.common.api.common.messages.marketdata.statickeys.ImmutableMarketDataYieldCurveStaticKey;
import com.herron.exchange.common.api.common.messages.pricing.FailedPriceModelResult;
import com.herron.exchange.pricingengine.server.marketdata.MarketDataService;
import com.herron.exchange.quantlib.pricemodels.fixedincome.bonds.BondDiscountingPriceModel;

public class BondPriceCalculator {

    private final MarketDataService marketDataService;

    public BondPriceCalculator(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public PriceModelResult calculate(BondInstrument instrument, Timestamp valuationTime) {
        return switch (instrument.priceModel()) {
            case BOND_DISCOUNT -> calculateWithDiscountModel(instrument, valuationTime);
            default -> FailedPriceModelResult.createFailedResult(String.format("Bond price model %s not supported.", instrument));
        };
    }

    private PriceModelResult calculateWithDiscountModel(BondInstrument instrument, Timestamp valuationTime) {
        if (instrument.priceModelParameters().calculateWithCurve()) {
            return calculateWithCurve(instrument, valuationTime);
        }
        if (instrument.priceModelParameters().constantYield() != null) {
            return BondDiscountingPriceModel.calculate(instrument, instrument.priceModelParameters().constantYield(), valuationTime);
        }

        return FailedPriceModelResult.createFailedResult("Yield was not set!");
    }

    private PriceModelResult calculateWithCurve(BondInstrument instrument, Timestamp valuationTime) {
        String curveId = instrument.priceModelParameters().yieldCurveId();
        if (curveId == null) {
            return FailedPriceModelResult.createFailedResult("CurveId is null.");
        }

        var request = ImmutableMarketDataYieldCurveRequest.builder()
                .staticKey(ImmutableMarketDataYieldCurveStaticKey.builder().curveId(curveId).build())
                .timeFilter(MarketDataRequestTimeFilter.LATEST)
                .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(Timestamp.now()).build())
                .build();

        MarketDataYieldCurveResponse response = marketDataService.getYieldCurve(request);
        if (response.status() == Status.ERROR || response.yieldCurveEntry() == null) {
            return FailedPriceModelResult.createFailedResult("Market data error: " + response.error());
        }
        var yieldCurve = response.yieldCurveEntry().yieldCurve();
        return BondDiscountingPriceModel.calculate(instrument, yieldCurve, valuationTime);
    }

}
