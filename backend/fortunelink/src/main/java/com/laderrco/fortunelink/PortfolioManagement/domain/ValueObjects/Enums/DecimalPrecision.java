package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums;

// For BigDecimal operations
// used in Money class and Quantities variables
// NOTE: for displaying precisoin, we use a different thing (separate concern to handle Locale), this stuff doesn't belong here
public enum DecimalPrecision {
    STOCK(4),
    BOND(6),
    CRYPTO(8),
    FOREX(5),
    COMMODITY(3),
    CASH(2);

    private final int decimalPlaces;

    DecimalPrecision(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }
    public int getDecimalPlaces() { return decimalPlaces; }
}
