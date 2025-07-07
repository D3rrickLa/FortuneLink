package com.laderrco.fortunelink.shared.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Percentage(BigDecimal percentageValue) {
    public Percentage {
        Objects.requireNonNull(percentageValue, "Percentage value cannot be null");
        if (percentageValue.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Percentage value cannot be negative"); {
        }

        if (percentageValue.scale() < 6) {
            percentageValue = percentageValue.setScale(6, RoundingMode.HALF_UP);   
        }
    }

    public static Percentage fromPercentage(BigDecimal percent) {
        return new Percentage(percent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
    }
    public static Percentage fromDecimal(BigDecimal decimal) {
        return new Percentage(decimal.setScale(6, RoundingMode.HALF_UP));
    }
    
    public BigDecimal toDecimal() {
        return percentageValue.setScale(6, RoundingMode.HALF_UP);
    }
    
    public BigDecimal toPercentage() {
        return percentageValue.multiply(BigDecimal.valueOf(100)).setScale(6, RoundingMode.HALF_UP);
    }    
}
