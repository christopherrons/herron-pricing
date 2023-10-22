package com.herron.exchange.pricingengine.server.snapshot;

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

        return timeAndPrice.timeOfPriceMs() - previousTimeAndPrice.timeOfPriceMs() >= minTimeBeforeUpdate.getSeconds() * 1000 &&
                previousTimeAndPrice.price().percentageChange(timeAndPrice.price()) >= minPriceChange;
    }
}
