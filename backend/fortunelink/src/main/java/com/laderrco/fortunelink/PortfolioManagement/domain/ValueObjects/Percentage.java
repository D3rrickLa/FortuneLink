package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Percentage(BigDecimal value) {
    public Percentage {
        Objects.requireNonNull(value, "Percentage value cannot be null.");
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("The percentage cannot be less than 0.");
        }
        
        if (value.scale() < 4) {
            value = value.setScale(4, RoundingMode.HALF_UP);
        }
    }
}
