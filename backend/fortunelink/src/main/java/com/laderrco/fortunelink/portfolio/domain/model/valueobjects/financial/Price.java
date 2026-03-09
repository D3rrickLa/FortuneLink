package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import java.math.BigDecimal;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

public record Price(Money pricePerUnit) {

    public Price {
        notNull(pricePerUnit, "pricePerUnit cannot be null");
        if (pricePerUnit.isNegative()) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
    }

    public static Price ZERO(Currency currency) {
        return new Price(Money.ZERO(currency));
    }

    public static Price of(BigDecimal amount, Currency currency) {
        return new Price(new Money(amount, currency));
    }

    public Money calculateValue(Quantity quantity) {
        notNull(quantity, "quantity cannot be null");
        return pricePerUnit.multiply(quantity.amount());
    }

    public Currency currency() {
        return pricePerUnit.currency();
    }

    public BigDecimal amount() {
        return pricePerUnit.amount();
    }
}