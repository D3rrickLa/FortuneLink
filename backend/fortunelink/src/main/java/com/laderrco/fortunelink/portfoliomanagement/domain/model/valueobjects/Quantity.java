package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import java.math.BigDecimal;
import java.util.Objects;

public record Quantity(BigDecimal amount) {
    public Quantity {
        Objects.requireNonNull(amount);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative: " + amount);
        }
    }

    public static Quantity of(double value) {
        return new Quantity(BigDecimal.valueOf(value));
    }
    
    public static Quantity of(String value) {
        return new Quantity(new BigDecimal(value));
    }
    
    public Quantity add(Quantity other) {
        return new Quantity(amount.add(other.amount()));
    }
    
    public Quantity subtract(Quantity other) {
        return new Quantity(amount.subtract(other.amount()));
    }
}
