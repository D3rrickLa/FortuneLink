package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import com.laderrco.fortunelink.shared.domain.valueobjects.Money;

/*
 * Price is a wrapper for clarity
 */
public record Price(Money pricePerUnit) {
    
    public static Price of (Money other) {
        return new Price(other);
    }
    public Money calculateValue(Quantity quantity) {
        return this.pricePerUnit.multiply(quantity.amount());
    }

}
