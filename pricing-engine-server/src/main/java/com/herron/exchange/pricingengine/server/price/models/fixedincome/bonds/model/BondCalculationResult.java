package com.herron.exchange.pricingengine.server.price.models.fixedincome.bonds.model;

public record BondCalculationResult(double presentValue, double accruedInterest) {

    public double cleanPrice() {
        return presentValue;
    }

    public double dirtyPrice() {
        return cleanPrice() + accruedInterest;
    }
}
