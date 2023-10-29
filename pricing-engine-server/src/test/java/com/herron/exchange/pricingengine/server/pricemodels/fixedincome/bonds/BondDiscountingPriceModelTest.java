package com.herron.exchange.pricingengine.server.pricemodels.fixedincome.bonds;

import com.herron.exchange.common.api.common.api.referencedata.instruments.BondInstrument;
import com.herron.exchange.common.api.common.curves.YieldCurve;
import com.herron.exchange.common.api.common.curves.YieldCurveModelParameters;
import com.herron.exchange.common.api.common.enums.CompoundingMethodEnum;
import com.herron.exchange.common.api.common.enums.DayCountConventionEnum;
import com.herron.exchange.common.api.common.enums.InterpolationMethod;
import com.herron.exchange.common.api.common.messages.common.BusinessCalendar;
import com.herron.exchange.common.api.common.messages.common.MonetaryAmount;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.marketdata.ImmutableDefaultTimeComponentKey;
import com.herron.exchange.common.api.common.messages.marketdata.entries.ImmutableMarketDataYieldCurve;
import com.herron.exchange.common.api.common.messages.marketdata.statickeys.ImmutableMarketDataYieldCurveStaticKey;
import com.herron.exchange.common.api.common.messages.pricing.BondDiscountPriceModelResult;
import com.herron.exchange.common.api.common.messages.pricing.ImmutableBondDiscountPriceModelParameters;
import com.herron.exchange.common.api.common.messages.refdata.*;
import com.herron.exchange.pricingengine.server.marketdata.MarketDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.herron.exchange.common.api.common.enums.DayCountConventionEnum.ACT365;
import static com.herron.exchange.common.api.common.enums.DayCountConventionEnum.BOND_BASIS_30360;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BondDiscountingPriceModelTest {

    private final MarketDataService marketDataService = new MarketDataService(null);
    private BondDiscountingPriceModel bondPriceModel;


    @BeforeEach
    void init() {
        bondPriceModel = new BondDiscountingPriceModel(marketDataService);
    }

    @Test
    void test_zero_yield_compounding_interest_with_accrued_interest() {
        var bond = buildInstrument(
                false,
                0,
                2,
                Timestamp.from(LocalDate.of(2023, 1, 1)),
                Timestamp.from(LocalDate.of(2021, 1, 1)),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.04,
                ACT365,
                buildProduct(BusinessCalendar.noHolidayCalendar())
        );

        var now = Timestamp.from(LocalDate.of(2021, 6, 30));
        var result = (BondDiscountPriceModelResult) bondPriceModel.calculateBondPrice(bond, now);
        assertEquals(19.90, result.accruedInterest(), 0.1);
    }

    @Test
    void test_constant_yield_compounding_interest_with_accrued_interest() {
        var bond = buildInstrument(
                false,
                0.04,
                2,
                Timestamp.from(LocalDate.of(2031, 1, 1)),
                Timestamp.from(LocalDate.of(2011, 1, 1)),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.05,
                ACT365,
                buildProduct(BusinessCalendar.noHolidayCalendar())
        );

        var now = Timestamp.from(LocalDate.of(2011, 4, 30));
        var result = (BondDiscountPriceModelResult) bondPriceModel.calculateBondPrice(bond, now);
        assertEquals(16.43, result.accruedInterest(), 0.01);
    }

    @Test
    void test_zero_coupon_pricing() {
        var bond = buildInstrument(
                false,
                0.05,
                1,
                Timestamp.from(LocalDate.of(2040, 1, 1)),
                Timestamp.from(LocalDate.of(2020, 1, 1)),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.00,
                ACT365,
                buildProduct(BusinessCalendar.noHolidayCalendar())
        );

        var now = Timestamp.from(LocalDate.of(2019, 1, 1));
        var result = (BondDiscountPriceModelResult) bondPriceModel.calculateBondPrice(bond, now);
        assertEquals(376.89, result.dirtyPrice().getRealValue(), 0.1);
        assertEquals(result.dirtyPrice().getRealValue(), result.dirtyPrice().getRealValue(), 0);
        assertEquals(0, result.accruedInterest(), 0);
    }

    @Test
    void test_constant_yield_compounding_interest_pricing() {
        var bond = buildInstrument(
                false,
                0.03,
                2,
                Timestamp.from(LocalDate.of(2023, 1, 1)),
                Timestamp.from(LocalDate.of(2021, 1, 1)),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.05,
                ACT365,
                buildProduct(BusinessCalendar.noHolidayCalendar()));

        var now = Timestamp.from(LocalDate.of(2020, 1, 1));
        var result = (BondDiscountPriceModelResult) bondPriceModel.calculateBondPrice(bond, now);
        assertEquals(1038.54, result.dirtyPrice().getRealValue(), 1);
        assertEquals(result.dirtyPrice().getRealValue(), result.dirtyPrice().getRealValue(), 0.01);
        assertEquals(0, result.accruedInterest(), 0);
    }

    @Test
    void test_constant_yield_compounding_interest_pricing_2() {
        var bond = buildInstrument(
                false,
                0.04,
                1,
                Timestamp.from(LocalDate.of(2040, 1, 1)),
                Timestamp.from(LocalDate.of(2020, 1, 1)),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.025,
                ACT365,
                buildProduct(BusinessCalendar.noHolidayCalendar())
        );

        var now = Timestamp.from(LocalDate.of(2020, 1, 1));
        var result = (BondDiscountPriceModelResult) bondPriceModel.calculateBondPrice(bond, now);
        assertEquals(796.14, result.dirtyPrice().getRealValue(), 0.01);
        assertEquals(result.dirtyPrice().getRealValue(), result.dirtyPrice().getRealValue(), 0.01);
        assertEquals(0, result.accruedInterest(), 0);
    }

    @Test
    void test_constant_yield_compounding_interest_pricing_3() {
        var bond = buildInstrument(
                false,
                0.04,
                2,
                Timestamp.from(LocalDate.of(2040, 1, 1)),
                Timestamp.from(LocalDate.of(2020, 1, 1)),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.025,
                ACT365,
                buildProduct(BusinessCalendar.noHolidayCalendar())
        );

        var now = Timestamp.from(LocalDate.of(2020, 1, 1));
        var result = (BondDiscountPriceModelResult) bondPriceModel.calculateBondPrice(bond, now);
        assertEquals(798.83, result.dirtyPrice().getRealValue(), 1);
        assertEquals(result.dirtyPrice().getRealValue(), result.dirtyPrice().getRealValue(), 0.01);
        assertEquals(0, result.accruedInterest(), 0);
    }

    @Test
    void test_constant_yield_compounding_interest__30360_pricing_4() {
        var bond = buildInstrument(
                false,
                0.1,
                2,
                Timestamp.from(LocalDate.of(2028, 10, 1)),
                Timestamp.from(LocalDate.of(2023, 1, 1)),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.015,
                BOND_BASIS_30360,
                buildProduct(BusinessCalendar.noHolidayCalendar())
        );

        var now = Timestamp.from(LocalDate.of(2020, 1, 1));
        var result = (BondDiscountPriceModelResult) bondPriceModel.calculateBondPrice(bond, now);
        assertEquals(677.91, result.dirtyPrice().getRealValue(), 0.01);
        assertEquals(result.dirtyPrice().getRealValue(), result.dirtyPrice().getRealValue(), 0.01);
        assertEquals(0.00, result.accruedInterest(), 0.001);
    }

    @Test
    void test_bond_price_with_curve() {
        var bond = buildInstrument(
                true,
                1,
                2,
                Timestamp.from(LocalDate.of(2040, 1, 1)),
                Timestamp.from(LocalDate.of(2020, 1, 1)),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.025,
                ACT365,
                buildProduct(BusinessCalendar.noHolidayCalendar())
        );

        YieldCurve curve = createTestCurve();
        marketDataService.addEntry(
                ImmutableMarketDataYieldCurve.builder()
                        .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(Timestamp.now()).build())
                        .staticKey(ImmutableMarketDataYieldCurveStaticKey.builder().curveId("id").build())
                        .yieldCurve(curve)
                        .build()
        );
        var now = Timestamp.from(LocalDate.of(2020, 1, 1));
        var result = (BondDiscountPriceModelResult) bondPriceModel.calculateBondPrice(bond, now);
        assertEquals(812.32, result.dirtyPrice().getRealValue(), 1);
        assertEquals(result.dirtyPrice().getRealValue(), result.dirtyPrice().getRealValue(), 0.01);
        assertEquals(0, result.accruedInterest(), 0);
    }

    private Product buildProduct(BusinessCalendar businessCalendar) {
        return ImmutableProduct.builder()
                .productId("product")
                .businessCalendar(businessCalendar)
                .market(buildMarket(businessCalendar))
                .currency("eur")
                .build();
    }

    private Market buildMarket(BusinessCalendar businessCalendar) {
        return ImmutableMarket.builder()
                .marketId("market")
                .businessCalendar(businessCalendar)
                .build();
    }

    private BondInstrument buildInstrument(boolean useCurve,
                                           double yieldPerYear,
                                           int frequency,
                                           Timestamp maturityData,
                                           Timestamp startDate,
                                           double nominalValue,
                                           CompoundingMethodEnum compoundingMethodEnum,
                                           double couponRate,
                                           DayCountConventionEnum dayCountConvetionEnum,
                                           Product product) {
        return ImmutableDefaultBondInstrument.builder()
                .instrumentId("instrumentId")
                .couponAnnualFrequency(frequency)
                .maturityDate(maturityData)
                .startDate(startDate)
                .nominalValue(MonetaryAmount.create(nominalValue, "eur"))
                .couponRate(couponRate)
                .priceModelParameters(ImmutableBondDiscountPriceModelParameters.builder().dayCountConvention(dayCountConvetionEnum)
                        .compoundingMethod(compoundingMethodEnum)
                        .calculateWithCurve(useCurve)
                        .constantYield(yieldPerYear)
                        .yieldCurveId("id")
                        .build()
                )
                .product(product)
                .firstTradingDate(Timestamp.from(LocalDate.MIN))
                .lastTradingDate(Timestamp.from(LocalDate.MAX))
                .build();
    }

    private YieldCurve createTestCurve() {
        LocalDate startDate = LocalDate.parse("2019-01-01");
        var dayCountConvention = ACT365;
        List<LocalDate> maturityDates = new ArrayList<>();
        maturityDates.add(startDate.plusDays((long) dayCountConvention.getDaysPerYear()));
        maturityDates.add(startDate.plusDays((long) dayCountConvention.getDaysPerYear() * 2));
        maturityDates.add(startDate.plusDays((long) dayCountConvention.getDaysPerYear() * 3));
        maturityDates.add(startDate.plusDays((long) dayCountConvention.getDaysPerYear() * 4));
        maturityDates.add(startDate.plusDays((long) dayCountConvention.getDaysPerYear() * 5));
        maturityDates.add(startDate.plusDays((long) dayCountConvention.getDaysPerYear() * 10));
        maturityDates.add(startDate.plusDays((long) dayCountConvention.getDaysPerYear() * 20));
        maturityDates.add(startDate.plusDays((long) dayCountConvention.getDaysPerYear() * 30));
        maturityDates.add(startDate.plusDays((long) dayCountConvention.getDaysPerYear() * 50));
        double[] yields = new double[]{0.01, 0.015, 0.02, 0.03, 0.035, 0.035, 0.04, 0.04, 0.045};
        var parameters = YieldCurveModelParameters.create(dayCountConvention,
                InterpolationMethod.CUBIC_SPLINE,
                LocalDate.parse("2019-01-01"),
                maturityDates.get(0),
                maturityDates.toArray(new LocalDate[0]),
                yields
        );
        return YieldCurve.create("id", parameters);
    }
}