package com.herron.exchange.pricingengine.server.pricemodels.fixedincome.bonds;

import com.herron.exchange.common.api.common.api.pricing.PriceModelResult;
import com.herron.exchange.common.api.common.api.referencedata.instruments.BondInstrument;
import com.herron.exchange.common.api.common.enums.CompoundingMethodEnum;
import com.herron.exchange.common.api.common.enums.DayCountConventionEnum;
import com.herron.exchange.common.api.common.enums.MarketDataRequestTimeFilter;
import com.herron.exchange.common.api.common.enums.Status;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.marketdata.ImmutableDefaultTimeComponentKey;
import com.herron.exchange.common.api.common.messages.marketdata.requests.ImmutableMarketDataYieldCurveRequest;
import com.herron.exchange.common.api.common.messages.marketdata.response.MarketDataYieldCurveResponse;
import com.herron.exchange.common.api.common.messages.marketdata.statickeys.ImmutableMarketDataYieldCurveStaticKey;
import com.herron.exchange.common.api.common.messages.pricing.ImmutableBondDiscountPriceModelResult;
import com.herron.exchange.common.api.common.messages.pricing.ImmutableFailedPriceModelResult;
import com.herron.exchange.pricingengine.server.marketdata.MarketDataService;
import com.herron.exchange.pricingengine.server.pricemodels.fixedincome.bonds.model.CouponPeriod;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import static com.herron.exchange.common.api.common.enums.EventType.SYSTEM;
import static com.herron.exchange.common.api.common.enums.Status.OK;
import static java.time.temporal.ChronoUnit.YEARS;

public class BondDiscountingPriceModel {

    private final MarketDataService marketDataService;

    public BondDiscountingPriceModel(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public PriceModelResult calculateBondPrice(BondInstrument instrument, LocalDate valuationTime) {
        if (instrument.priceModelParameters().calculateWithCurve()) {
            return calculateWithCurve(instrument, valuationTime);
        }
        return calculateWithConstantYield(instrument, valuationTime);
    }

    private PriceModelResult calculateWithCurve(BondInstrument instrument, LocalDate valuationTime) {
        String curveId = instrument.priceModelParameters().yieldCurveId();
        if (curveId == null) {
            return ImmutableFailedPriceModelResult.builder().failReason("CurveId is null.").build();
        }

        var request = ImmutableMarketDataYieldCurveRequest.builder()
                .staticKey(ImmutableMarketDataYieldCurveStaticKey.builder().curveId(curveId).build())
                .timeFilter(MarketDataRequestTimeFilter.LATEST)
                .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(LocalDateTime.now()).build())
                .build();

        MarketDataYieldCurveResponse response = marketDataService.getEntry(request);
        if (response.status() == Status.ERROR || response.yieldCurveEntry() == null) {
            return ImmutableFailedPriceModelResult.builder().failReason("Market data error: " + response.error()).build();
        }
        var yieldCurve = response.yieldCurveEntry().yieldCurve();
        return calculateBondPrice(instrument, yieldCurve::getYield, valuationTime);
    }

    private PriceModelResult calculateWithConstantYield(BondInstrument instrument, LocalDate valuationTime) {
        var yieldPerYear = instrument.priceModelParameters().constantYield();
        if (yieldPerYear == null) {
            return ImmutableFailedPriceModelResult.builder().failReason("Yield per year is null.").build();
        }
        return calculateBondPrice(instrument, timeToMaturity -> yieldPerYear, valuationTime);
    }

    private PriceModelResult calculateBondPrice(BondInstrument bondInstrument,
                                                DoubleUnaryOperator yieldAtMaturityExtractor,
                                                LocalDate valuationTime) {
        if (valuationTime.isBefore(bondInstrument.startDate())) {
            valuationTime = bondInstrument.startDate();
        }

        List<CouponPeriod> periods = CouponCalculationUtils.generateCouponPeriods(bondInstrument);
        return calculateBondPrice(bondInstrument, yieldAtMaturityExtractor, valuationTime, periods);
    }

    private PriceModelResult calculateBondPrice(BondInstrument bondInstrument,
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
                accruedInterest += calculateAccruedInterest(bondInstrument.couponRate(), period.startDate(), valuationTime, bondInstrument.priceModelParameters().dayCountConvention());
            }

            presentValue += period.couponRate() / calculateDiscountFactor(bondInstrument, period.startDate(), maturityDate, yieldAtMaturityExtractor);
        }

        var discountedFaceValue = 1 / calculateDiscountFactor(bondInstrument, valuationTime, maturityDate, yieldAtMaturityExtractor);
        presentValue += discountedFaceValue;

        var accruedInterestAmount = accruedInterest * bondInstrument.nominalValue().getRealValue();
        var presentValueAmount = presentValue * bondInstrument.nominalValue().getRealValue();
        return ImmutableBondDiscountPriceModelResult.builder()
                .accruedInterest(accruedInterestAmount)
                .cleanPrice(Price.create(presentValueAmount))
                .price(Price.create(presentValueAmount + accruedInterestAmount))
                .eventType(SYSTEM)
                .timeOfEventMs(Instant.now().toEpochMilli())
                .status(OK)
                .build();
    }

    private double calculateDiscountFactor(BondInstrument bondInstrument, LocalDate start, LocalDate end, DoubleUnaryOperator yieldAtMaturityExtractor) {
        var timeToMaturity = YEARS.between(start, end);
        double yieldAtTimeToMaturity = yieldAtMaturityExtractor.applyAsDouble(timeToMaturity);
        return bondInstrument.priceModelParameters().compoundingMethod().calculateValue(yieldAtTimeToMaturity, timeToMaturity, bondInstrument.couponAnnualFrequency());
    }

    private double calculateAccruedInterest(double annualCouponRate, LocalDate startDate, LocalDate valuationTime, DayCountConventionEnum dayCountConvention) {
        return annualCouponRate * dayCountConvention.calculateDayCountFraction(startDate, valuationTime);
    }

    private double calculateDiscountFactor(CompoundingMethodEnum compoundingMethodEnum,
                                           double yieldAtTime,
                                           double timeToMaturity,
                                           int couponYearlyFrequency) {
        return switch (compoundingMethodEnum) {
            case SIMPLE -> (yieldAtTime / couponYearlyFrequency) * timeToMaturity;
            case COMPOUNDING -> Math.pow(1 + (yieldAtTime / couponYearlyFrequency), timeToMaturity * couponYearlyFrequency);
            case CONTINUOUS -> Math.exp(yieldAtTime * timeToMaturity);
        };
    }
}
