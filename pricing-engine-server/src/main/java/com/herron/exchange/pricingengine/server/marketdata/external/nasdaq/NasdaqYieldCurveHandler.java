package com.herron.exchange.pricingengine.server.marketdata.external.nasdaq;

import com.herron.exchange.common.api.common.curves.YieldCurve;
import com.herron.exchange.common.api.common.curves.YieldCurveModelParameters;
import com.herron.exchange.common.api.common.enums.DayCountConventionEnum;
import com.herron.exchange.common.api.common.enums.InterpolationMethod;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.marketdata.ImmutableDefaultTimeComponentKey;
import com.herron.exchange.common.api.common.messages.marketdata.entries.ImmutableMarketDataYieldCurve;
import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataYieldCurve;
import com.herron.exchange.common.api.common.messages.marketdata.statickeys.ImmutableMarketDataYieldCurveStaticKey;
import com.herron.exchange.integrations.nasdaq.NasdaqYieldCurveClient;

import java.time.LocalDate;
import java.util.List;

public class NasdaqYieldCurveHandler {
    public static final String YIELD_CURVE_ID = "Nasdaq Treasury Yield Curve";
    private final NasdaqYieldCurveClient client;

    public NasdaqYieldCurveHandler(NasdaqYieldCurveClient client) {
        this.client = client;
    }

    public List<MarketDataYieldCurve> getYieldCurves(LocalDate from, LocalDate to) {
        var data = client.requestYieldCurveData(from, to);
        var dataset = data.dataset();
        return dataset.getDataToYieldItems().stream()
                .map(item -> YieldCurveModelParameters.create(
                        DayCountConventionEnum.ACT365,
                        InterpolationMethod.CUBIC_SPLINE,
                        item.date(),
                        item.maturityDates()[item.maturityDates().length - 1],
                        item.maturityDates(),
                        item.yieldValues()
                ))
                .map(parameters -> YieldCurve.create(YIELD_CURVE_ID, parameters))
                .<MarketDataYieldCurve>map(curve ->
                        ImmutableMarketDataYieldCurve.builder()
                                .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(Timestamp.from(curve.getYieldCurveModelParameters().startDate())).build())
                                .staticKey(ImmutableMarketDataYieldCurveStaticKey.builder().curveId(curve.getId()).build())
                                .yieldCurve(curve)
                                .build())
                .toList();
    }
}
