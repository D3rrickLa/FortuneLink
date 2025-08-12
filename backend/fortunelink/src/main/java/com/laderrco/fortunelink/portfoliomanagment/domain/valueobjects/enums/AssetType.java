package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums;

import java.util.Set;

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

    public boolean requiresISIN() {
        return Set.of(STOCK, ETF, BOND).contains(this);
    }
}
