package com.herron.exchange.pricingengine.server.theoretical.fixedincome.bonds;

import com.herron.exchange.common.api.common.api.BondInstrument;
import com.herron.exchange.pricingengine.server.theoretical.fixedincome.bonds.model.CouponPeriod;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CouponCalculationUtils {
    private static final long DAYS_PER_YEAR = 365;

    public static List<CouponPeriod> generateCouponPeriods(BondInstrument instrument) {
        if (isZeroCoupon(instrument)) {
            return List.of();
        }
        // This does not handle all corner cases
        var startDate = instrument.startDate();
        var maturityDate = instrument.maturityDate();

        double totalYearsToMaturity = ChronoUnit.YEARS.between(startDate, maturityDate);
        long totalNumberOfCouponPeriods = (long) totalYearsToMaturity * instrument.couponYearlyFrequency();
        long nrOfMonthsPerPeriod = 12 / instrument.couponYearlyFrequency();

        List<CouponPeriod> couponPeriods = new ArrayList<>();
        for (int period = 0; period < totalNumberOfCouponPeriods; period++) {
            var couponStartDate = startDate.plusMonths(nrOfMonthsPerPeriod * period);
            var couponEndDate = startDate.plusMonths(nrOfMonthsPerPeriod * (1 + period));
            if (couponEndDate.isAfter(instrument.maturityDate())) {
                couponEndDate = instrument.maturityDate();
            }
            couponPeriods.add(
                    new CouponPeriod(
                            couponStartDate,
                            couponEndDate,
                            instrument.couponRate() / instrument.couponYearlyFrequency()
                    )
            );
        }
        couponPeriods.sort(Comparator.comparing(CouponPeriod::startDate));
        return couponPeriods;
    }

    private static boolean isZeroCoupon(BondInstrument instrument) {
        return instrument.couponRate() == 0;
    }
}
