package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums;

public enum AssetType {
    STOCK(DecimalPrecision.STOCK),
    ETF(DecimalPrecision.STOCK),
    CRYPTO(DecimalPrecision.CRYPTO),
    BOND(DecimalPrecision.BOND),
    COMMODITY(DecimalPrecision.COMMODITY),
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
