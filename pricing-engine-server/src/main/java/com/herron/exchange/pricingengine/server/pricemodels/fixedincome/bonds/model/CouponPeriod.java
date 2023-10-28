package com.herron.exchange.pricingengine.server.pricemodels.fixedincome.bonds.model;

import com.herron.exchange.common.api.common.messages.common.Timestamp;

import java.time.temporal.ChronoUnit;

public record CouponPeriod(Timestamp startDate, Timestamp endDate, double couponRate) {

    public boolean isInPeriod(Timestamp date) {
        return startDate.isBefore(date) && endDate.isAfter(date);
    }

    public long nrOfDaysInPeriod() {
        return ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate());
    }
}
