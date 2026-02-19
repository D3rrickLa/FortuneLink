package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

public record Price(Money pricePerUnit) implements ClassValidation {

    public static Price ZERO(Currency currency) {
        return new Price(Money.ZERO(currency));
    }

    public Price {
        ClassValidation.validateParameter(pricePerUnit, "pricePerUnit cannot be null");
        if (pricePerUnit.isNegative()) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
    }

    public Money calculateValue(Quantity quantity) {
        ClassValidation.validateParameter(quantity, "quantity cannot be null");
        return pricePerUnit.multiply(quantity.amount());
    }

    public Currency currency() {
        return pricePerUnit.currency();
    }
}