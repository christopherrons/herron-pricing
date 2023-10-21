package com.herron.exchange.pricingengine.server.pricemodels.fixedincome.bonds;

import com.herron.exchange.common.api.common.api.referencedata.instruments.BondInstrument;
import com.herron.exchange.common.api.common.enums.CompoundingMethodEnum;
import com.herron.exchange.common.api.common.enums.DayCountConvetionEnum;
import com.herron.exchange.pricingengine.server.curves.YieldCurve;
import com.herron.exchange.pricingengine.server.pricemodels.fixedincome.bonds.model.BondCalculationResult;
import com.herron.exchange.pricingengine.server.pricemodels.fixedincome.bonds.model.CouponPeriod;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

public class BondDiscountingPriceModel {

    private BondCalculationResult calculateBondPrice(BondInstrument instrument,
                                                     LocalDate valuationTime) {
        return instrument.priceModelParameters().calculateWithCurve() ?
                calculateBondPrice(instrument) :
                calculateBondPrice(instrument, instrument.priceModelParameters().constantYield(), valuationTime);
    }

    private BondCalculationResult calculateBondPrice(BondInstrument instrument,
                                                     double yieldPerYear,
                                                     LocalDate valuationTime) {
        return calculateBondPrice(instrument, timeToMaturity -> yieldPerYear, valuationTime);
    }

    private BondCalculationResult calculateBondPrice(BondInstrument instrument,
                                                     YieldCurve yieldCurve,
                                                     LocalDate valuationTime) {
        return calculateBondPrice(instrument, yieldCurve::getYield, valuationTime);
    }

    private BondCalculationResult calculateBondPrice(BondInstrument bondInstrument,
                                                     DoubleUnaryOperator yieldAtMaturityExtractor,
                                                     LocalDate valuationTime) {
        if (valuationTime.isBefore(bondInstrument.startDate())) {
            valuationTime = bondInstrument.startDate();
        }

        List<CouponPeriod> periods = CouponCalculationUtils.generateCouponPeriods(bondInstrument);
        return calculateBondPrice(bondInstrument, yieldAtMaturityExtractor, valuationTime, periods);
    }

    private BondCalculationResult calculateBondPrice(BondInstrument bondInstrument,
                                                     DoubleUnaryOperator yieldAtMaturityExtractor,
                                                     LocalDate valuationTime,
                                                     List<CouponPeriod> periods) {
        double presentValue = 0;
        double accruedInterest = 0;
        LocalDate maturityDate = bondInstrument.maturityDate();
        for (CouponPeriod period : periods) {
            if (period.endDate().isBefore(valuationTime)) {
                continue;
            }

            if (period.isInPeriod(valuationTime)) {
                accruedInterest += calculateAccruedInterest(bondInstrument.couponRate(), period.startDate(), valuationTime, bondInstrument.dayCountConvention());
            }

            presentValue += period.couponRate() / calculateDiscountFactor(bondInstrument, period.startDate(), maturityDate, yieldAtMaturityExtractor);
        }

        var discountedFaceValue = 1 / calculateDiscountFactor(bondInstrument, valuationTime, maturityDate, yieldAtMaturityExtractor);
        presentValue += discountedFaceValue;

        return new BondCalculationResult(presentValue * bondInstrument.nominalValue(), accruedInterest * bondInstrument.nominalValue());
    }

    private double calculateDiscountFactor(BondInstrument bondInstrument, LocalDate valuationTime, LocalDate maturityDate, DoubleUnaryOperator yieldAtMaturityExtractor) {
        var timeToMaturity = ChronoUnit.YEARS.between(valuationTime, maturityDate);
        double yieldAtTimeToMaturity = yieldAtMaturityExtractor.applyAsDouble(timeToMaturity);
        return calculateDiscountFactor(bondInstrument.compoundingMethod(), yieldAtTimeToMaturity, timeToMaturity, bondInstrument.couponAnnualFrequency());
    }

    private double calculateAccruedInterest(double annualCouponRate, LocalDate startDate, LocalDate valuationTime, DayCountConvetionEnum dayCountConvetion) {
        return annualCouponRate * dayCountConvetion.calculateDayCountFraction(startDate, valuationTime);
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
