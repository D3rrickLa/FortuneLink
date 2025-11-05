package com.laderrco.fortunelink.shared.enums;

import java.math.MathContext;

public enum Precision {
    BOND(6),
    CASH(6),
    COMMODITY(6),
    CRYPTO(8),
    FOREX(6),
    MONEY(MathContext.DECIMAL128.getPrecision()), // 34 digits of precision, not necessarily decimal places
    PERCENTAGE(8),
    STOCK(6);

    private final int precision;

    Precision(int precision) {
        this.precision = precision;
    }

    public int getDecimalPlaces() {
        return precision;
    }

    public static Precision fromAssetType(Object object) { // but in the AssetType 
        // we would do a swtich case or hashset
        throw new UnsupportedOperationException("'fromAssetType' not yet implemented");
    }

    public static final int getMoneyPrecision() {
        return MONEY.getDecimalPlaces();
    }
}