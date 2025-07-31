package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums;

public enum DecimalPrecision {
    BOND(6),
    CASH(4),
    COMMODITY(4),
    CRYPTO(8),
    FOREX(5),
    PERCENTAGE(8),
    STOCK(4);

    private final int decimalPlaces;

    DecimalPrecision(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }
}
