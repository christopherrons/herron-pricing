package com.herron.exchange.pricingengine.server.curves;


import com.herron.exchange.common.api.common.math.api.CartesianPoint2d;
import com.herron.exchange.common.api.common.math.interpolation.CubicSplineInterpolation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class YieldCurve {

    public final YieldRefData yieldRefData;
    private final CubicSplineInterpolation splineInterpolation;
    private final List<CartesianPoint2d> yieldPoints = new ArrayList<>();

    public YieldCurve(YieldRefData yieldRefData) {
        this.yieldRefData = yieldRefData;
        createYieldPoints(yieldRefData.getMaturities(), yieldRefData.getYields());
        this.splineInterpolation = new CubicSplineInterpolation(yieldPoints);
    }

    private void createYieldPoints(double[] maturities, double[] yields) {
        for (int i = 0; i < maturities.length; i++) {
            yieldPoints.add(new YieldPoint(maturities[i], yields[i]));
        }
        yieldPoints.sort(Comparator.comparing(CartesianPoint2d::x));
    }

    public double getYield(final double maturity) {
        return splineInterpolation.getFunctionValue(maturity);
    }

    public double[] getMaturities() {
        return yieldRefData.getMaturities();
    }

    public double[] getYields() {
        return yieldRefData.getYields();
    }

    public List<CartesianPoint2d> getYieldPoints() {
        return yieldPoints;
    }

    public double getStartBoundaryMaturity() {
        return splineInterpolation.getStartBoundaryX();
    }

    public double getStartBoundaryYield() {
        return splineInterpolation.getStartBoundaryY();
    }

    public double getEndBoundaryMaturity() {
        return splineInterpolation.getEndBoundaryX();
    }

    public double getEndBoundaryYield() {
        return splineInterpolation.getEndBoundaryY();
    }
}
