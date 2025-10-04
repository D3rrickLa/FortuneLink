package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import java.math.BigDecimal;
import java.util.Objects;

public record Quantity(BigDecimal amount) {
    public Quantity {
        Objects.requireNonNull(amount);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(" Quantity cannot be negative: " + amount);
        }
    }
}
