package com.laderrco.fortunelink.shared.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;

public record Percentage(BigDecimal value) {
    private final static int SCALE = Precision.PERCENTAGE.getDecimalPlaces();
    private final static RoundingMode MODE  = Rounding.PERCENTAGE.getMode();
    
    public Percentage {
        Objects.requireNonNull(value, "Value cannot be null");
        value = value.setScale(SCALE, MODE);
    }

    public static Percentage of(BigDecimal value) {
        return new Percentage(value);
    }

    public static Percentage of(double value) {
        return new Percentage(BigDecimal.valueOf(value));
    }

    public static Percentage fromPercentage(BigDecimal percent) {
        Objects.requireNonNull(percent, "Percent cannot be null");
        return new Percentage(percent.divide(BigDecimal.valueOf(100), SCALE, MODE));
    }

    public static Percentage fromPercentage(double percent) {
        return Percentage.fromPercentage(BigDecimal.valueOf(percent));
    }

    public BigDecimal toPercentage() {
        return this.value.multiply(BigDecimal.valueOf(100));
    }

    public int compareTo(Percentage other) {
        return this.value.compareTo(other.value());
    } 

    public Percentage addPercentage(Percentage other) {
        return new Percentage(this.value.add(other.value()));
    }

    public Percentage multiplyPercentage(Percentage other) {
        return new Percentage(this.value.multiply(other.value()));
    }
}