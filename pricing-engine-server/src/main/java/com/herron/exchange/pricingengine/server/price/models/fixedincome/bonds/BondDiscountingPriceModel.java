package com.herron.exchange.pricingengine.server.price.models.fixedincome.bonds;

import com.herron.exchange.common.api.common.api.referencedata.instruments.BondInstrument;
import com.herron.exchange.common.api.common.enums.CompoundingMethodEnum;
import com.herron.exchange.common.api.common.enums.DayCountConvetionEnum;
import com.herron.exchange.pricingengine.server.curves.YieldCurve;
import com.herron.exchange.pricingengine.server.price.models.fixedincome.bonds.model.BondCalculationResult;
import com.herron.exchange.pricingengine.server.price.models.fixedincome.bonds.model.CouponPeriod;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

public class BondDiscountingPriceModel {
    private static final long DAYS_PER_YEAR = 365;

    public BondCalculationResult calculateBondPrice(BondInstrument instrument,
                                                    double yieldPerYear,
                                                    LocalDate now) {
        return calculateBondPrice(instrument, timeToMaturity -> yieldPerYear, now);
    }

    public BondCalculationResult calculateBondPrice(BondInstrument instrument,
                                                    YieldCurve yieldCurve,
                                                    LocalDate now) {
        return calculateBondPrice(instrument, yieldCurve::getYield, now);
    }

    private BondCalculationResult calculateBondPrice(BondInstrument bondInstrument,
                                                     DoubleUnaryOperator yieldAtMaturityExtractor,
                                                     LocalDate now) {
        if (now.isBefore(bondInstrument.startDate())) {
            now = bondInstrument.startDate();
        }

        List<CouponPeriod> periods = CouponCalculationUtils.generateCouponPeriods(bondInstrument);
        return calculateBondPrice(bondInstrument, yieldAtMaturityExtractor, now, periods);
    }

    private BondCalculationResult calculateBondPrice(BondInstrument bondInstrument,
                                                     DoubleUnaryOperator yieldAtMaturityExtractor,
                                                     LocalDate now,
                                                     List<CouponPeriod> periods) {
        double presentValue = 0;
        double accruedInterest = 0;
        LocalDate maturityDate = bondInstrument.maturityDate();
        for (CouponPeriod period : periods) {
            if (period.endDate().isBefore(now)) {
                continue;
            }

            if (period.isInPeriod(now)) {
                accruedInterest += calculateAccruedInterest(bondInstrument.couponRate(), period.startDate(), now, bondInstrument.dayCountConvention());
            }

            presentValue += period.couponRate() / calculateDiscountFactor(bondInstrument, period.startDate(), maturityDate, yieldAtMaturityExtractor);
        }

        var discountedFaceValue = 1 / calculateDiscountFactor(bondInstrument, now, maturityDate, yieldAtMaturityExtractor);
        presentValue += discountedFaceValue;

        return new BondCalculationResult(presentValue * bondInstrument.nominalValue(), accruedInterest * bondInstrument.nominalValue());
    }

    private double calculateDiscountFactor(BondInstrument bondInstrument, LocalDate now, LocalDate maturityDate, DoubleUnaryOperator yieldAtMaturityExtractor) {
        var timeToMaturity = ChronoUnit.YEARS.between(now, maturityDate);
        double yieldAtTimeToMaturity = yieldAtMaturityExtractor.applyAsDouble(timeToMaturity);
        return calculateDiscountFactor(bondInstrument.compoundingMethod(), yieldAtTimeToMaturity, timeToMaturity, bondInstrument.couponAnnualFrequency());
    }

    private double calculateAccruedInterest(double annualCouponRate, LocalDate startDate, LocalDate now, DayCountConvetionEnum dayCountConvetion) {
        return annualCouponRate * dayCountConvetion.calculateDayCountFraction(startDate, now);
    }

    private double calculateDiscountFactor(CompoundingMethodEnum compoundingMethodEnum,
                                           double yieldAtTime,
                                           double timeToMaturity,
                                           int couponYearlyFrequency) {
        return switch (compoundingMethodEnum) {
            case SIMPLE -> (yieldAtTime / couponYearlyFrequency) * timeToMaturity;
            case COMPOUNDING -> Math.pow(1 + (yieldAtTime / couponYearlyFrequency), timeToMaturity * couponYearlyFrequency);
            case CONTINUOUS -> Math.exp(yieldAtTime * timeToMaturity);
            default -> 1;
        };
    }
}
