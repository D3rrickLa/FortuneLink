package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums;

public enum DecimalPrecision {
    BOND(6),
    COMMODITY(3),
    CRYPTO(8),
    FOREX(5),
    PERCENTAGE(6),
    STOCK(4),
    CASH(2);

    private final int decimalPlaces;

    DecimalPrecision(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }
}
