package com.laderrco.fortunelink.portfolio_management.shared.enums;

import java.math.MathContext;

public enum Precision {
    BOND(6),
    CASH(6),
    COMMODITY(6),
    CRYPTO(8),
    DIVISION(10),
    FOREX(6),
    MONEY(MathContext.DECIMAL128.getPrecision()),
    PERCENTAGE(8),
    QUANTITY(8),
    STOCK(6);

    private final int precision;

    Precision(int precision) {
        this.precision = precision;
    }

    public int getDecimalPlaces() {
        return precision;
    }

    public static final int getMoneyPrecision() {
        return MONEY.getDecimalPlaces();
    }

    public static Precision fromAssetType(Object object) {
        // we would do a swtich case or hashset
        throw new UnsupportedOperationException("'fromAssetType' not yet implemented");
    }

}