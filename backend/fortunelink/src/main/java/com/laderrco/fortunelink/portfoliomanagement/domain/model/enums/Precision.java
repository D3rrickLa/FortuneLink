package com.laderrco.fortunelink.portfoliomanagement.domain.model.enums;

import java.math.MathContext;

public enum Precision {
    BOND(6),
    CASH(6),
    COMMODITY(6),
    CRYPTO(8),
    FOREX(6),
    MONEY(MathContext.DECIMAL128.getPrecision()), // 34 digits
    PERCENTAGE(8),
    STOCK(6);

    private final int decimalPlaces;

    Precision(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public static final int getMoneyDecimalPlaces() {
        return MONEY.getDecimalPlaces();
    }
}
