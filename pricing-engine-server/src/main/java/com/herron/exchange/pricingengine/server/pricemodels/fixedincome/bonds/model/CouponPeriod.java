package com.herron.exchange.pricingengine.server.pricemodels.fixedincome.bonds.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record CouponPeriod(LocalDate startDate,LocalDate endDate, double couponRate) {

    public boolean isInPeriod(LocalDate date) {
        return startDate.isBefore(date) && endDate.isAfter(date);
    }

    public long nrOfDaysInPeriod() {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }
}
