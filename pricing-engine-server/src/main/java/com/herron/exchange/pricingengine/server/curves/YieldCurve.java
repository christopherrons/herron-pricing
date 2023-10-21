package com.herron.exchange.pricingengine.server.curves;


import com.herron.exchange.common.api.common.api.math.Function2d;
import com.herron.exchange.common.api.common.math.interpolation.CubicSplineInterpolation;

public class YieldCurve {

    private final String id;
    public final YieldCurveModelParameters yieldCurveModelParameters;
    private final Function2d yieldFunction;

    private YieldCurve(String id, YieldCurveModelParameters yieldCurveModelParameters) {
        this.id = id;
        this.yieldCurveModelParameters = yieldCurveModelParameters;
        this.yieldFunction = createYieldFunction();
    }

    public static YieldCurve create(String id, YieldCurveModelParameters yieldCurveModelParameters) {
        return new YieldCurve(id, yieldCurveModelParameters);
    }

    private Function2d createYieldFunction() {
        return switch (yieldCurveModelParameters.interpolationMethod()) {
            case CUBIC_SPLINE -> CubicSplineInterpolation.create(yieldCurveModelParameters.yieldPoints());
            default -> throw new IllegalArgumentException("Other methods are not supported");
        };
    }

    public double getYield(final double maturity) {
        return yieldFunction.getFunctionValue(maturity);
    }

    public double getStartBoundaryMaturity() {
        return yieldFunction.getStartBoundaryX();
    }

    public double getStartBoundaryYield() {
        return yieldFunction.getStartBoundaryY();
    }

    public double getEndBoundaryMaturity() {
        return yieldFunction.getEndBoundaryX();
    }

    public double getEndBoundaryYield() {
        return yieldFunction.getEndBoundaryY();
    }
}
