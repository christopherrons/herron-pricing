package com.herron.exchange.pricingengine.server.curves;

import com.herron.exchange.common.api.common.api.math.CartesianPoint2d;
import com.herron.exchange.common.api.common.enums.DayCountConvetionEnum;
import com.herron.exchange.common.api.common.enums.InterpolationMethod;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;

public record YieldCurveModelParameters(DayCountConvetionEnum dayCountConvetionEnum,
                                        InterpolationMethod interpolationMethod,
                                        LocalDate startDate,
                                        LocalDate endDate,
                                        List<CartesianPoint2d> yieldPoints) {

    public static YieldCurveModelParameters create(DayCountConvetionEnum dayCountConvetionEnum,
                                                   InterpolationMethod interpolationMethod,
                                                   LocalDate startDate,
                                                   LocalDate endDate,
                                                   LocalDate[] maturityDates,
                                                   double[] yields) {

        return new YieldCurveModelParameters(dayCountConvetionEnum, interpolationMethod, startDate, endDate, createYieldPoints(dayCountConvetionEnum, startDate, maturityDates, yields));
    }

    private static List<CartesianPoint2d> createYieldPoints(DayCountConvetionEnum dayCountConvetionEnum,
                                                            LocalDate startDate,
                                                            LocalDate[] maturityDates,
                                                            double[] yields) {
        List<CartesianPoint2d> yieldPoints = new ArrayList<>();
        for (int i = 0; i < yields.length; i++) {
            var maturity = DAYS.between(startDate, maturityDates[i]) / dayCountConvetionEnum.getDaysPerYear();
            yieldPoints.add(new YieldPoint(maturity, yields[i]));
        }
        return yieldPoints;
    }
}
