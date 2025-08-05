package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.DecimalPrecision;

public record Percentage(BigDecimal value) {
    private final static int SCALE = DecimalPrecision.PERCENTAGE.getDecimalPlaces();

    public Percentage {
        Objects.requireNonNull(value, "Percentage value cannot be null");
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Percentage value cannot be negative.");
        }

        if (value.scale() != SCALE) {
            value = value.setScale(SCALE, RoundingMode.HALF_UP);
        }
    }

    public static Percentage fromPercentage(BigDecimal percent) {
        Objects.requireNonNull(percent, "Percent cannot be null.");
        return new Percentage(percent.divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP));
    }

    // this should be in a format like like: 0.56, etc.
    public static Percentage fromDecimal(BigDecimal value) {
        Objects.requireNonNull(value, "Value cannot be null.");
        return new Percentage(value.setScale(SCALE, RoundingMode.HALF_UP));
    }

    public BigDecimal toDecimal() {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    // set scale here causes scale to double
    public BigDecimal toPercentage() {
        return value.multiply(BigDecimal.valueOf(100));
    }

    public int compareTo(Percentage other) {
        Objects.requireNonNull(other, "Percentage to compare cannot be null.");
        return this.value.compareTo(other.value);
    }

    public static Percentage of(double value) {
        Objects.requireNonNull(value, "Percent value cannot be null.");
        return new Percentage(BigDecimal.valueOf(value));
    }
    
}
