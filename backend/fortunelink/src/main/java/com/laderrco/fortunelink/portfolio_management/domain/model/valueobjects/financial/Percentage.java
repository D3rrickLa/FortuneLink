package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.laderrco.fortunelink.portfolio_management.domain.model.ClassValidation;
import com.laderrco.fortunelink.portfolio_management.shared.enums.Precision;
import com.laderrco.fortunelink.portfolio_management.shared.enums.Rounding;

// like a Rate, 5% on your BOND for the next X years
// or how much must be applied -> taxes, fees, discounts, etc.
// these are grom 0 - 100%
public record Percentage(BigDecimal rate) implements ClassValidation, Comparable<Percentage> {

    private static final int SCALE = Precision.PERCENTAGE.getDecimalPlaces();
    private static final RoundingMode MODE = Rounding.PERCENTAGE.getMode();

    public static final Percentage ZERO = Percentage.fromPercent(BigDecimal.ZERO);

    public Percentage {
        ClassValidation.validateParameter(rate, "rate");

        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Percentage cannot be negative");
        }

        rate = rate.setScale(SCALE, MODE);
    }

    // 0.05 => 5%
    public static Percentage fromRate(BigDecimal rate) {
        return new Percentage(rate);
    }

    // 5% => 0.05
    public static Percentage fromPercent(BigDecimal percent) {
        ClassValidation.validateParameter(percent, "percent");
        return new Percentage(percent.divide(BigDecimal.valueOf(100), SCALE, MODE));
    }

    public BigDecimal toPercent() {
        return rate.multiply(BigDecimal.valueOf(100));
    }

    public Percentage add(Percentage other) {
        return new Percentage(rate.add(other.rate));
    }

    public Percentage compoundWith(Percentage other) {
        return new Percentage(rate.multiply(other.rate));
    }

    public Percentage annualizedOver(double years) {
        if (years <= 0) {
            throw new IllegalArgumentException("Years must be positive");
        }

        double annualized = Math.pow(1 + rate.doubleValue(), 1.0 / years) - 1;

        return new Percentage(BigDecimal.valueOf(annualized));
    }

    @Override
    public int compareTo(Percentage other) {
        return rate.compareTo(other.rate);
    }
}