package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.DecimalPrecision;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.Rounding;

public record Percentage(BigDecimal value) {
    private final static int SCALE = DecimalPrecision.PERCENTAGE.getDecimalPlaces();
    private final static RoundingMode MODE  = Rounding.PERCENTAGE.getMode();

    /**
     * 
     * @param value - the percentage value, in the form of a decimal, so if you think 45%, we are storing 0.45 
     */
    public Percentage {
        Objects.requireNonNull(value, "Value cannot be null.");
        value = value.setScale(SCALE, MODE);
    }

    public static Percentage of(double value) {
        return new Percentage(BigDecimal.valueOf(value));
    }

    public static Percentage of(BigDecimal value) {
        return new Percentage(value);
    }

    public static Percentage fromPercentage(BigDecimal percent) {
        Objects.requireNonNull(percent, "Percent cannot be null.");
        return new Percentage(percent.divide(BigDecimal.valueOf(100), SCALE, MODE));
    }

    /**
     * 
     * @return value as a Percentage, so like 45%, we are storing the 'percent' as a decimal (i.e. 0.45)
     */
    public BigDecimal toPercentage() {
        return value.multiply(BigDecimal.valueOf(100));
    }
    
    public int compareTo(Percentage other) {
        Objects.requireNonNull(other, "Percentage to compare cannot be null.");
        return this.value.compareTo(other.value);
    }

}
