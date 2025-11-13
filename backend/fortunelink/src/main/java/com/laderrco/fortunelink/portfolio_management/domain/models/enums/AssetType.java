package com.laderrco.fortunelink.portfolio_management.domain.models.enums;

import com.laderrco.fortunelink.shared.enums.Precision;

public enum AssetType {
    STOCK(Precision.STOCK),
    ETF(Precision.STOCK),
    CRYPTO(Precision.CRYPTO),
    BOND(Precision.BOND),
    COMMODITY(Precision.COMMODITY),
    CASH(Precision.CASH),
    REAL_ESTATE(Precision.CASH),
    FOREX_PAIR(Precision.FOREX),
    OTHER(Precision.CASH);


    private final Precision defaultQuantityPrecision;

    private AssetType(Precision defaultQuantityPrecision) {
        this.defaultQuantityPrecision = defaultQuantityPrecision;
    }

    public Precision getDefaultQuantityPrecision() {
        return this.defaultQuantityPrecision;
    } 
}
