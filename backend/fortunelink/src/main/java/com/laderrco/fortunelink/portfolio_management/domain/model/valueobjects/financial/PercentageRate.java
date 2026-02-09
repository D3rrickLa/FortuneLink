package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;
import com.laderrco.fortunelink.portfolio_management.shared.enums.Precision;
import com.laderrco.fortunelink.portfolio_management.shared.enums.Rounding;

// always from o -> 100
public record PercentageRate(BigDecimal rate) implements ClassValidation, Comparable<PercentageRate> {
    private static final RoundingMode P_ROUNDING_MODE = Rounding.PERCENTAGE.getMode();
    private static int RATE_PRECISION = Precision.PERCENTAGE.getDecimalPlaces();

    public PercentageRate {
        ClassValidation.validateParameter(rate, "rate");

        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("PercentageRate rate cannot be negative");
        }

        rate = rate.setScale(RATE_PRECISION, P_ROUNDING_MODE);
    }

    // 0.05 => 5%
    public static PercentageRate fromRate(BigDecimal rate) {
        return new PercentageRate(rate);
    }

    // 5% => 0.05
    public static PercentageRate fromPercent(BigDecimal PercentageRate) {
        return new PercentageRate(PercentageRate.divide(BigDecimal.valueOf(100), RATE_PRECISION, P_ROUNDING_MODE));
    }

    public BigDecimal toPercent() {
        return rate.multiply(BigDecimal.valueOf(100));
    }

    public PercentageRate add(PercentageRate other) {
        return new PercentageRate(rate.add(other.rate));
    }

    public PercentageRate compoundWith(PercentageRate other) {
        return new PercentageRate(rate.multiply(other.rate));
    }

    public PercentageRate annualizedOver(double years) {
        if (years <= 0) {
            throw new IllegalArgumentException("Years must be positive");
        }

        double annualized = Math.pow(1 + rate.doubleValue(), 1.0 / years) - 1;

        return new PercentageRate(BigDecimal.valueOf(annualized));
    }

    @Override
    public int compareTo(PercentageRate other) {
        return rate.compareTo(other.rate);
    }

}