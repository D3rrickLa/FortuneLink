package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums;

public enum DecimalPrecision {
    STOCK(4),
    BOND(6),
    CRYPTO(8),
    FOREX(5),
    COMMODITY(3),
    CASH(2); // this is wrong, cash/money handled by currency.getprecisiondefault()

    private final int decimalPlaces;

    DecimalPrecision(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }
}
