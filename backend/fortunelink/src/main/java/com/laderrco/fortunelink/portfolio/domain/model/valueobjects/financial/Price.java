package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

public record Price(Money pricePerUnit) {

    public static Price ZERO(Currency currency) {
        return new Price(Money.ZERO(currency));
    }

    public Price {
        notNull(pricePerUnit, "pricePerUnit cannot be null");
        if (pricePerUnit.isNegative()) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
    }

    public Money calculateValue(Quantity quantity) {
        notNull(quantity, "quantity cannot be null");
        return pricePerUnit.multiply(quantity.amount());
    }

    public Currency currency() {
        return pricePerUnit.currency();
    }
}