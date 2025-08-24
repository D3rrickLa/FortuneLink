package com.laderrco.fortunelink.portfoliomanagement.domain.enums;

import java.math.MathContext;

public enum DecimalPrecision {
    BOND(6),
    CASH(6),
    COMMODITY(6),
    CRYPTO(8),
    FOREX(6),
    MONEY(MathContext.DECIMAL128.getPrecision()),
    PERCENTAGE(8),
    STOCK(6);

    private final int decimalPlaces;

    DecimalPrecision(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }
}