package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import java.math.BigDecimal;

import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;

public record Ratio(int numerator, int denominator) {
    // Example: 3-for-1 is (3, 1)
    // Example: 1-for-10 reverse is (1, 10)

    public BigDecimal multiplier() {
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), Precision.DIVISION.getDecimalPlaces(),
                        Rounding.DIVISION.getMode());
    }
}