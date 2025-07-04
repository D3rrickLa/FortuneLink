package com.laderrco.fortunelink.sharedkernel.ValueObjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.DecimalPrecision;

public record Percentage(BigDecimal percentValue) {
    public Percentage {
        Objects.requireNonNull(percentValue, "Percentage value cannot be null");

        if (percentValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("The percentage cannot be less than zero.");
        }

        if (percentValue.scale() < 6) { // scale is the num of digits to the right of the decimal
            percentValue = percentValue.setScale(DecimalPrecision.PRECENTAGE.getDecimalPlaces(), RoundingMode.HALF_UP);
        }

    }

    /**
     * 
     * @param rawPecentageValue - a BigDecimal representing a precentage, (i.e. 100%, 25%, etc.) as is.
     * @return a new Pecentage instance with the precentValue = x / 100
     */
    public static Percentage fromPercent(BigDecimal rawPecentageValue) {
        BigDecimal decimalValue = rawPecentageValue.divide(BigDecimal.valueOf(100), DecimalPrecision.PRECENTAGE.getDecimalPlaces(), RoundingMode.HALF_UP);
        return new Percentage(decimalValue);
    }
}

