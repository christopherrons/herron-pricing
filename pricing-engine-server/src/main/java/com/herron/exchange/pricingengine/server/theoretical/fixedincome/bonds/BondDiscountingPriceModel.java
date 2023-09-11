package com.herron.exchange.pricingengine.server.theoretical.fixedincome.bonds;

import com.herron.exchange.common.api.common.api.BondInstrument;
import com.herron.exchange.common.api.common.enums.CompoundingMethodEnum;
import com.herron.exchange.pricingengine.server.curves.YieldCurve;
import com.herron.exchange.pricingengine.server.theoretical.fixedincome.bonds.model.BondCalculationResult;
import com.herron.exchange.pricingengine.server.theoretical.fixedincome.bonds.model.CouponPeriod;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import static com.herron.exchange.pricingengine.server.theoretical.fixedincome.bonds.CouponCalculationUtils.generateCouponPeriods;

public class BondDiscountingPriceModel {
    private static final long DAYS_PER_YEAR = 365;

    public BondCalculationResult calculateBondPrice(BondInstrument instrument,
                                                    double yieldPerYear,
                                                    LocalDate now) {
        if (now.isBefore(instrument.startDate())) {
            now = instrument.startDate();
        }

        List<CouponPeriod> periods = generateCouponPeriods(instrument);
        return calculatePresentValue(instrument, periods, timeToMaturity -> yieldPerYear, now);
    }

    public BondCalculationResult calculateBondPrice(BondInstrument instrument,
                                                    YieldCurve yieldCurve,
                                                    LocalDate now) {
        if (now.isBefore(instrument.startDate())) {
            now = instrument.startDate();
        }

        List<CouponPeriod> periods = generateCouponPeriods(instrument);
        return calculatePresentValue(instrument, periods, yieldCurve::getYield, now);
    }

    private BondCalculationResult calculatePresentValue(BondInstrument bondInstrument,
                                                        List<CouponPeriod> periods,
                                                        DoubleUnaryOperator yieldAtMaturityExtractor,
                                                        LocalDate now) {
        double presentValue = 0;
        double accruedInterest = 0;
        LocalDate maturityDate = bondInstrument.maturityDate();
        for (CouponPeriod period : periods) {
            if (period.endDate().isBefore(now)) {
                continue;
            }

            if (period.isInPeriod(now)) {
                accruedInterest += calculateAccruedInterest(period, now);
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
        return calculateDiscountFactor(bondInstrument.compoundingMethod(), yieldAtTimeToMaturity, timeToMaturity, bondInstrument.couponYearlyFrequency());
    }

    private double calculateAccruedInterest(CouponPeriod period, LocalDate now) {
        double nrOfDaysAccruedInPeriod = ChronoUnit.DAYS.between(period.startDate(), now);
        double ratioOfPeriodPassed = nrOfDaysAccruedInPeriod / period.nrOfDaysInPeriod();
        return period.couponRate() * ratioOfPeriodPassed;
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
