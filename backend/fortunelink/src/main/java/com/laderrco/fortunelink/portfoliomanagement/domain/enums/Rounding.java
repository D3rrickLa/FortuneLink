package com.laderrco.fortunelink.portfoliomanagement.domain.enums;

import java.math.RoundingMode;

public enum Rounding {
    MONEY(RoundingMode.HALF_EVEN), 
    PERCENTAGE(RoundingMode.HALF_UP);

    private final RoundingMode mode;

    Rounding(RoundingMode mode) {
        this.mode = mode;
    }

    public RoundingMode getMode() {
        return mode;
    }
}
