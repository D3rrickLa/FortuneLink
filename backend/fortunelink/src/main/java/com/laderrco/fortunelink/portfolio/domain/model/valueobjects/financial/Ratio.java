package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;

import java.math.BigDecimal;

public record Ratio(int numerator, int denominator) {
    // Example: 3-for-1 is (3, 1)
    // Example: 1-for-10 reverse is (1, 10)
    public Ratio {
        if (numerator <= 0) {
            throw new IllegalArgumentException("Numerator must be greater than zero");
        }
        if (denominator <= 0) {
            throw new IllegalArgumentException("Denominator must be greater than zero");
        }
    }

    public BigDecimal multiplier() {
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator),
                        Precision.DIVISION.getDecimalPlaces(),
                        Rounding.DIVISION.getMode());
    }
}