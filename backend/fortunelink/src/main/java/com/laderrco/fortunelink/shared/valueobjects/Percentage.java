package com.laderrco.fortunelink.shared.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;

public record Percentage(BigDecimal value) implements ClassValidation {
    private final static int SCALE = Precision.PERCENTAGE.getDecimalPlaces();
    private final static RoundingMode MODE  = Rounding.PERCENTAGE.getMode();
    
    public Percentage {
        ClassValidation.validateParameter(value, "Value cannot be null");
        value = value.setScale(SCALE, MODE);
    }

    public static Percentage of(BigDecimal value) {
        return new Percentage(value);
    }

    public static Percentage of(double value) {
        return new Percentage(BigDecimal.valueOf(value));
    }

    public static Percentage fromPercentage(BigDecimal percent) {
        ClassValidation.validateParameter(percent, "Percent cannot be null");
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

    public Percentage annualize(double years) {
        if (years  <= 0) {
            throw new IllegalArgumentException("Years cannot be negative or 0.");
        }
        return new Percentage(this.value.divide(BigDecimal.valueOf(years)));
    }
}