package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

// Store thet value as the decimal percentage, i.e. 0.25
public record Percentage(BigDecimal percentValue) {
    public Percentage {
        Objects.requireNonNull(percentValue, "Percentage value cannot be null");

        if (percentValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("The percentage cannot be less than zero.");
        }

        if (percentValue.scale() < 6) { // scale is the num of digits to the right of the decimal
            percentValue = percentValue.setScale(6, RoundingMode.HALF_UP);
        }

    }

    public static Percentage fromPercent(BigDecimal rawPecentageValue) {
        BigDecimal decimalValue = rawPecentageValue.multiply(BigDecimal.valueOf(100));
        return new Percentage(decimalValue);
    }

}

