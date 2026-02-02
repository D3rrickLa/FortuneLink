package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import com.laderrco.fortunelink.portfolio_management.domain.model.ClassValidation;

public record Price(Money pricePerUnit) implements ClassValidation {

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
}