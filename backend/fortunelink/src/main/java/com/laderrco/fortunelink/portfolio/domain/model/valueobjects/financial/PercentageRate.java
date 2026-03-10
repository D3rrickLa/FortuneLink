package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;

// always from o -> 100
public record PercentageRate(BigDecimal rate) implements Comparable<PercentageRate> {
    private static final RoundingMode P_ROUNDING_MODE = Rounding.PERCENTAGE.getMode();
    private static int RATE_PRECISION = Precision.PERCENTAGE.getDecimalPlaces();

    public PercentageRate {
        notNull(rate, "rate");

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

    public PercentageRate annualizedOver(BigDecimal years) {
        if (years.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Years must be positive");
        }
        /*
         * BigDecimal does not support fractional exponentiation.
         * We temporarily convert to double for Math.pow().
         *
         * This introduces minor floating-point rounding error, but the magnitude
         * is extremely small for typical financial return values and acceptable
         * for performance/return display purposes.
         *
         * If higher precision is required in the future, replace with a
         * BigDecimal Newton-method implementation or a library such as
         * BigDecimalMath / Apache Commons Math.
         */
        @SuppressWarnings("FloatingPointLiteralPrecision")
        double annualized = Math.pow(BigDecimal.ONE.add(rate).doubleValue(),
                BigDecimal.ONE.divide(years, MathContext.DECIMAL64).doubleValue())
                - 1.0;

        return new PercentageRate(BigDecimal.valueOf(annualized));
    }

    @Override
    public int compareTo(PercentageRate other) {
        return rate.compareTo(other.rate);
    }

}