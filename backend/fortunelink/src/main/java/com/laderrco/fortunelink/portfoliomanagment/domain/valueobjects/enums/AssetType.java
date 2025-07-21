package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums;

public enum AssetType {
    STOCK(DecimalPrecision.STOCK),
    ETF(DecimalPrecision.STOCK),
    CRYPTO(DecimalPrecision.CRYPTO),
    BOND(DecimalPrecision.BOND),
    COMMODITY(DecimalPrecision.COMMODITY),
    REAL_ESTATE(DecimalPrecision.CASH),
    FOREX_PAIR(DecimalPrecision.FOREX),
    OTHER(DecimalPrecision.CASH);


    private final DecimalPrecision defaultQuantityPrecision;

    private AssetType(DecimalPrecision defaultQuantityPrecision) {
        this.defaultQuantityPrecision = defaultQuantityPrecision;
    }

    public DecimalPrecision getDefaultQuantityPrecision() {
        return defaultQuantityPrecision;
    }

}
