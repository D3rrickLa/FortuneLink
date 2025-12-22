package com.laderrco.fortunelink.portfolio_management.domain.models.enums;

import com.laderrco.fortunelink.shared.enums.Precision;

public enum AssetType {    
    STOCK(Precision.STOCK, AssetCategory.EQUITY),
    ETF(Precision.STOCK, AssetCategory.EQUITY),
    CRYPTO(Precision.CRYPTO, AssetCategory.CRYPTOCURRENCY),
    BOND(Precision.BOND, AssetCategory.FIXED_INCOME),
    COMMODITY(Precision.COMMODITY, AssetCategory.COMMODITY),
    CASH(Precision.CASH, AssetCategory.CASH_EQUIVALENT),
    REAL_ESTATE(Precision.CASH, AssetCategory.REAL_ASSET),
    FOREX_PAIR(Precision.FOREX, AssetCategory.OTHER),
    OTHER(Precision.CASH, AssetCategory.OTHER);

    private final Precision defaultQuantityPrecision;
    private final AssetCategory category;

    private AssetType(Precision defaultQuantityPrecision, AssetCategory defaultAssetCategory) {
        this.defaultQuantityPrecision = defaultQuantityPrecision;
        this.category = defaultAssetCategory;
    }

    public Precision getDefaultQuantityPrecision() {
        return this.defaultQuantityPrecision;
    } 

    public AssetCategory getCategory() {
        return this.category;
    }

    public boolean isMarketTraded() {
        return this == STOCK || this == ETF || this == BOND;
    }

    public boolean isCryptoTraded() {
        return this == CRYPTO;
    }
}
