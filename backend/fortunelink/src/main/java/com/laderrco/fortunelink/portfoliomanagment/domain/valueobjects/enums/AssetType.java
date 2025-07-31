package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums;

public enum AssetType {
    BOND(DecimalPrecision.BOND),
    COMMODITY(DecimalPrecision.COMMODITY),
    CRYPTO(DecimalPrecision.CRYPTO),
    ETF(DecimalPrecision.STOCK),
    FOREX_PAIR(DecimalPrecision.FOREX),
    REAL_ESTATE(DecimalPrecision.CASH),
    STOCK(DecimalPrecision.STOCK),
    
    OTHER(DecimalPrecision.CASH);


    private final DecimalPrecision defaultQuantityPrecision;

    private AssetType(DecimalPrecision defaultQuantityPrecision) {
        this.defaultQuantityPrecision = defaultQuantityPrecision;
    }

    public DecimalPrecision getDefaultQuantityPrecision() {
        return defaultQuantityPrecision;
    }
}
