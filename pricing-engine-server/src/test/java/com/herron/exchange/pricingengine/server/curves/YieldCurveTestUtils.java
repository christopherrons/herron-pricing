package com.herron.exchange.pricingengine.server.curves;

import com.herron.exchange.common.api.common.api.math.CartesianPoint2d;
import com.herron.exchange.common.api.common.math.model.CubicPolynomial;
import com.herron.exchange.common.api.common.math.model.Point2d;

import java.util.ArrayList;
import java.util.List;

public class YieldCurveTestUtils {

    public static List<CartesianPoint2d> get3YieldPoints() {
        List<CartesianPoint2d> yieldPoints = new ArrayList<>();
        yieldPoints.add(new Point2d(1, 1));
        yieldPoints.add(new Point2d(2, 5));
        yieldPoints.add(new Point2d(3, 4));
        return yieldPoints;
    }

    public static List<CartesianPoint2d> get5YieldPoints() {
        List<CartesianPoint2d> yieldPoints = new ArrayList<>();
        yieldPoints.add(new Point2d(1, 1));
        yieldPoints.add(new Point2d(2, 5));
        yieldPoints.add(new Point2d(3, 4));
        yieldPoints.add(new Point2d(4, 3));
        yieldPoints.add(new Point2d(5, 2));
        return yieldPoints;
    }

    public static double getFunctionValue3Points(final double x) {
        if (x < 2) {
            return getCubicFunctionValue(x, -1.25, 3.75, 1.5, -3);
        }
        return getCubicFunctionValue(x, 1.25, -11.25, 31.5, -23);
    }


    public static double getFunctionValue5Points(final double x) {
        if (x < 2) {
            return getCubicFunctionValue(x, -1.3392857142857153, 4.017857142857148, 1.3214285714285618, -3.0000000000000004);
        }
        if (x < 3) {
            return getCubicFunctionValue(x, 1.6964285714285718, -14.196428571428575, 37.75000000000001, -27.28571428571429);
        }
        if (x < 4) {
            return getCubicFunctionValue(x, -0.44642857142857206, 5.089285714285722, -20.10714285714289, 30.571428571428612);
        }
        return getCubicFunctionValue(x, 0.08928571428571408, -1.3392857142857113, 5.607142857142844, -3.7142857142856966);
    }

    public static double getCubicFunctionValue(final double x, final double firstCoeff,
                                               final double secondCoeff, final double thirdCoeff, final double fourthCoeff) {
        CubicPolynomial polynomial = new CubicPolynomial(firstCoeff, secondCoeff, thirdCoeff, fourthCoeff);
        return polynomial.getFunctionValue(x);
    }


}
