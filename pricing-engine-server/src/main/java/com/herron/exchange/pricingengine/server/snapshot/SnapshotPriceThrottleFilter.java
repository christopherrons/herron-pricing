package com.herron.exchange.pricingengine.server.snapshot;

import com.herron.exchange.common.api.common.messages.common.Price;

import java.time.Duration;

public class SnapshotPriceThrottleFilter {

    private final Duration minTimeBeforeUpdate;
    private final double minPriceChange;
    private PriceSnapshotCalculator.TimeAndPrice previousTimeAndPrice;

    public SnapshotPriceThrottleFilter(Duration minTimeBeforeUpdate, double minPriceChange) {
        this.minTimeBeforeUpdate = minTimeBeforeUpdate;
        this.minPriceChange = minPriceChange;
    }

    public boolean filter(PriceSnapshotCalculator.TimeAndPrice timeAndPrice) {
        if (accept(timeAndPrice)) {
            previousTimeAndPrice = timeAndPrice;
            return false;
        }
        return true;
    }

    private boolean accept(PriceSnapshotCalculator.TimeAndPrice timeAndPrice) {
        if (timeAndPrice == null || !timeAndPrice.isValid()) {
            return false;
        }

        if (previousTimeAndPrice == null) {
            return true;
        }

        return timeAndPrice.timestamp().timeBetweenMs(previousTimeAndPrice.timestamp()) >= minTimeBeforeUpdate.getSeconds() * 1000
                && !timeAndPrice.price().equals(Price.ZERO)
                && previousTimeAndPrice.price().percentageChange(timeAndPrice.price()) >= minPriceChange;
    }
}
