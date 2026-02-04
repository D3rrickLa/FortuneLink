package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;
import com.laderrco.fortunelink.portfolio_management.shared.enums.Precision;
import com.laderrco.fortunelink.portfolio_management.shared.enums.Rounding;

// our return rate, can be like -20% YTD or +50% MOM
public record PercentageChange(BigDecimal change) implements ClassValidation, Comparable<PercentageChange> {
    private static final int SCALE = Precision.PERCENTAGE.getDecimalPlaces();
    private static final RoundingMode MODE = Rounding.PERCENTAGE.getMode();

    public PercentageChange {
        ClassValidation.validateParameter(change, "change");
        change = change.setScale(SCALE, MODE);
    }

    // -0.20
    public static PercentageChange loss(double percent) {
        return new PercentageChange(BigDecimal.valueOf(percent).negate().divide(BigDecimal.valueOf(100), SCALE, MODE));
    }

    // +0.10
    public static PercentageChange gain(double percent) {
        return new PercentageChange(BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100), SCALE, MODE));
    }

    public BigDecimal toPercent() {
        return change.multiply(BigDecimal.valueOf(100));
    }

    public boolean isLoss() {
        return change.signum() < 0;
    }

    public boolean isGain() {
        return change.signum() > 0;
    }

    @Override
    public int compareTo(PercentageChange other) {
        return change.compareTo(other.change);
    }
}