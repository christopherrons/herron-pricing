package com.herron.exchange.pricingengine.server.theoretical.fixedincome.bonds;

import com.herron.exchange.common.api.common.enums.CompoundingMethodEnum;
import com.herron.exchange.common.api.common.messages.HerronBondInstrument;
import com.herron.exchange.pricingengine.server.curves.YieldCurve;
import com.herron.exchange.pricingengine.server.curves.YieldRefData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BondDiscountingPriceModelTest {

    private BondDiscountingPriceModel bondPriceModel;

    @BeforeEach
    void init() {
        bondPriceModel = new BondDiscountingPriceModel();
    }

    @Test
    void test_zero_yield_compounding_interest_with_accrued_interest() {
        var bond = new HerronBondInstrument("instrumentId",
                2,
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2021, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.04);

        LocalDate now = LocalDate.of(2021, 6, 30);
        var result = bondPriceModel.calculateBondPrice(bond, 0, now);
        assertEquals(19.90, result.accruedInterest(), 0.1);
    }

    @Test
    void test_constant_yield_compounding_interest_with_accrued_interest() {
        var bond = new HerronBondInstrument("instrumentId",
                2,
                LocalDate.of(2031, 1, 1),
                LocalDate.of(2011, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.05);

        LocalDate now = LocalDate.of(2011, 4, 30);
        var result = bondPriceModel.calculateBondPrice(bond, 0.04, now);
        assertEquals(16.43, result.accruedInterest(), 0.01);
    }

    @Test
    void test_zero_coupon_pricing() {
        var bond = new HerronBondInstrument("instrumentId",
                1,
                LocalDate.of(2040, 1, 1),
                LocalDate.of(2020, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.00);

        LocalDate now = LocalDate.of(2019, 1, 1);
        var result = bondPriceModel.calculateBondPrice(bond, 0.05, now);
        assertEquals(376.89, result.cleanPrice(), 0.1);
        assertEquals(result.cleanPrice(), result.dirtyPrice(), 0);
        assertEquals(0, result.accruedInterest(), 0);
    }

    @Test
    void test_constant_yield_compounding_interest_pricing() {
        var bond = new HerronBondInstrument("instrumentId",
                2,
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2021, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.05);

        LocalDate now = LocalDate.of(2020, 1, 1);
        var result = bondPriceModel.calculateBondPrice(bond, 0.03, now);
        assertEquals(1038.54, result.cleanPrice(), 1);
        assertEquals(result.cleanPrice(), result.dirtyPrice(), 0.01);
        assertEquals(0, result.accruedInterest(), 0);
    }

    @Test
    void test_constant_yield_compounding_interest_pricing_2() {
        var bond = new HerronBondInstrument("instrumentId",
                1,
                LocalDate.of(2040, 1, 1),
                LocalDate.of(2020, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.025);

        LocalDate now = LocalDate.of(2020, 1, 1);
        var result = bondPriceModel.calculateBondPrice(bond, 0.04, now);
        assertEquals(796.14, result.cleanPrice(), 0.01);
        assertEquals(result.cleanPrice(), result.dirtyPrice(), 0.01);
        assertEquals(0, result.accruedInterest(), 0);
    }

    @Test
    void test_constant_yield_compounding_interest_pricing_3() {
        var bond = new HerronBondInstrument("instrumentId",
                2,
                LocalDate.of(2040, 1, 1),
                LocalDate.of(2020, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.025);

        LocalDate now = LocalDate.of(2020, 1, 1);
        var result = bondPriceModel.calculateBondPrice(bond, 0.04, now);
        assertEquals(798.83, result.cleanPrice(), 1);
        assertEquals(result.cleanPrice(), result.dirtyPrice(), 0.01);
        assertEquals(0, result.accruedInterest(), 0);
    }

    @Test
    void test_bond_price_with_curve() {
        var bond = new HerronBondInstrument("instrumentId",
                2,
                LocalDate.of(2040, 1, 1),
                LocalDate.of(2020, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.025);

        YieldCurve curve = createTestCurve();
        LocalDate now = LocalDate.of(2020, 1, 1);
        var result = bondPriceModel.calculateBondPrice(bond, curve, now);
        assertEquals(812.32, result.cleanPrice(), 1);
        assertEquals(result.cleanPrice(), result.dirtyPrice(), 0.01);
        assertEquals(0, result.accruedInterest(), 0);
    }

    private YieldCurve createTestCurve() {
        LocalDate startDate = LocalDate.parse("2019-01-01");
        List<LocalDate> maturityDates = new ArrayList<>();
        maturityDates.add(startDate.plusDays((long) YieldRefData.DAYS_PER_YEAR));
        maturityDates.add(startDate.plusDays((long) YieldRefData.DAYS_PER_YEAR * 2));
        maturityDates.add(startDate.plusDays((long) YieldRefData.DAYS_PER_YEAR * 3));
        maturityDates.add(startDate.plusDays((long) YieldRefData.DAYS_PER_YEAR * 4));
        maturityDates.add(startDate.plusDays((long) YieldRefData.DAYS_PER_YEAR * 5));
        maturityDates.add(startDate.plusDays((long) YieldRefData.DAYS_PER_YEAR * 10));
        maturityDates.add(startDate.plusDays((long) YieldRefData.DAYS_PER_YEAR * 20));
        maturityDates.add(startDate.plusDays((long) YieldRefData.DAYS_PER_YEAR * 30));
        maturityDates.add(startDate.plusDays((long) YieldRefData.DAYS_PER_YEAR * 50));
        double[] yields = new double[]{0.01, 0.015, 0.02, 0.03, 0.035, 0.035, 0.04, 0.04, 0.045};
        return new YieldCurve(new YieldRefData(LocalDate.parse("2019-01-01"), maturityDates, yields));
    }
}