package com.herron.exchange.pricingengine.server.theoretical.fixedincome.bonds;

import com.herron.exchange.common.api.common.enums.CompoundingMethodEnum;
import com.herron.exchange.common.api.common.enums.InstrumentTypeEnum;
import com.herron.exchange.common.api.common.messages.HerronBondInstrument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

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
                InstrumentTypeEnum.BOND,
                2,
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2021, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.04);

        LocalDate now = LocalDate.of(2021, 6, 30);
        var result = bondPriceModel.calculateWithConstantYield(bond, 0, now);
        assertEquals(19.90, result.accruedInterest(), 0.1);
    }

    @Test
    void test_constant_yield_compounding_interest_with_accrued_interest() {
        var bond = new HerronBondInstrument("instrumentId",
                InstrumentTypeEnum.BOND,
                2,
                LocalDate.of(2031, 1, 1),
                LocalDate.of(2011, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.05);

        LocalDate now = LocalDate.of(2011, 4, 30);
        var result = bondPriceModel.calculateWithConstantYield(bond, 0.04, now);
        assertEquals(16.43, result.accruedInterest(), 0.01);
    }

    @Test
    void test_zero_coupon_pricing() {
        var bond = new HerronBondInstrument("instrumentId",
                InstrumentTypeEnum.BOND,
                1,
                LocalDate.of(2040, 1, 1),
                LocalDate.of(2020, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.00);

        LocalDate now = LocalDate.of(2019, 1, 1);
        var result = bondPriceModel.calculateWithConstantYield(bond, 0.05, now);
        assertEquals(376.89, result.cleanPrice(), 0.1);
        assertEquals(result.cleanPrice(), result.dirtyPrice(), 0);
        assertEquals(0, result.accruedInterest(), 0);
    }

    @Test
    void test_constant_yield_compounding_interest_pricing() {
        var bond = new HerronBondInstrument("instrumentId",
                InstrumentTypeEnum.BOND,
                2,
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2021, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.05);

        LocalDate now = LocalDate.of(2020, 1, 1);
        var result = bondPriceModel.calculateWithConstantYield(bond, 0.03, now);
        assertEquals(1038.54, result.cleanPrice(), 1);
        assertEquals(result.cleanPrice(), result.dirtyPrice(), 0.01);
        assertEquals(0, result.accruedInterest(), 0);
    }

    @Test
    void test_constant_yield_compounding_interest_pricing_2() {
        var bond = new HerronBondInstrument("instrumentId",
                InstrumentTypeEnum.BOND,
                1,
                LocalDate.of(2040, 1, 1),
                LocalDate.of(2020, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.025);

        LocalDate now = LocalDate.of(2020, 1, 1);
        var result = bondPriceModel.calculateWithConstantYield(bond, 0.04, now);
        assertEquals(796.14, result.cleanPrice(), 0.01);
        assertEquals(result.cleanPrice(), result.dirtyPrice(), 0.01);
        assertEquals(0, result.accruedInterest(), 0);
    }

    @Test
    void test_constant_yield_compounding_interest_pricing_3() {
        var bond = new HerronBondInstrument("instrumentId",
                InstrumentTypeEnum.BOND,
                2,
                LocalDate.of(2040, 1, 1),
                LocalDate.of(2020, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.025);

        LocalDate now = LocalDate.of(2020, 1, 1);
        var result = bondPriceModel.calculateWithConstantYield(bond, 0.04, now);
        assertEquals(798.83, result.cleanPrice(), 1);
        assertEquals(result.cleanPrice(), result.dirtyPrice(), 0.01);
        assertEquals(0, result.accruedInterest(), 0);
    }
}