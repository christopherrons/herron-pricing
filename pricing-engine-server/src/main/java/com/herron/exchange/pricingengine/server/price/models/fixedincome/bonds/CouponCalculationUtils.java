package com.herron.exchange.pricingengine.server.price.models.fixedincome.bonds;

import com.herron.exchange.common.api.common.api.referencedata.instruments.BondInstrument;
import com.herron.exchange.pricingengine.server.price.models.fixedincome.bonds.model.CouponPeriod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CouponCalculationUtils {
    private static final long DAYS_PER_YEAR = 365;

    public static List<CouponPeriod> generateCouponPeriods(BondInstrument instrument) {
        if (instrument.isZeroCouponBond()) {
            return List.of();
        }

        // This does not handle all corner cases
        var businessCalendar = instrument.product().businessCalendar();
        var startDate = instrument.startDate();
        var maturityDate = instrument.maturityDate();

        long nrOfMonthsPerPeriod = 12 / instrument.couponAnnualFrequency();

        List<CouponPeriod> couponPeriods = new ArrayList<>();
        var couponEndDate = businessCalendar.getFirstDateBeforeHoliday(maturityDate);
        var couponStartDate = businessCalendar.getFirstDateAfterHoliday(couponEndDate.minusMonths(nrOfMonthsPerPeriod));
        while (couponStartDate.isAfter(startDate) || couponStartDate.equals(startDate)) {
            couponPeriods.add(
                    new CouponPeriod(
                            couponStartDate,
                            couponEndDate,
                            instrument.couponRate() / instrument.couponAnnualFrequency()
                    )
            );
            couponEndDate = couponStartDate;
            couponStartDate = businessCalendar.getFirstDateAfterHoliday(couponEndDate.minusMonths(nrOfMonthsPerPeriod));
        }
        couponPeriods.sort(Comparator.comparing(CouponPeriod::startDate));
        return couponPeriods;
    }
}
