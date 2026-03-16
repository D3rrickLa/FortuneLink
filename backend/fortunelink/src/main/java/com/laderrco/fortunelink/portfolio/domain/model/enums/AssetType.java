package com.laderrco.fortunelink.portfolio.domain.model.enums;

import com.laderrco.fortunelink.shared.enums.Precision;

public enum AssetType {
  BOND(Precision.BOND, AssetCategory.FIXED_INCOME, true),
  CRYPTO(Precision.CRYPTO, AssetCategory.CRYPTOCURRENCY, true),
  ETF(Precision.STOCK, AssetCategory.EQUITY, true),
  STOCK(Precision.STOCK, AssetCategory.EQUITY, true),

  CASH(Precision.CASH, AssetCategory.CASH, false),
  COMMODITY(Precision.COMMODITY, AssetCategory.COMMODITY, false),
  FOREX_PAIR(Precision.FOREX, AssetCategory.CASH_EQUIVALENT, true),
  REAL_ESTATE(Precision.CASH, AssetCategory.REAL_ASSET, false),
  OTHER(Precision.CASH, AssetCategory.OTHER, false);

  private final Precision precision;
  private final AssetCategory category;
  private final boolean marketTraded;

  AssetType(Precision precision, AssetCategory category, boolean marketTraded) {
    this.precision = precision;
    this.category = category;
    this.marketTraded = marketTraded;
  }

  public Precision precision() {
    return precision;
  }

  public AssetCategory category() {
    return category;
  }

  public boolean isMarketTraded() {
    return marketTraded;
  }

  public boolean isCrypto() {
    return category == AssetCategory.CRYPTOCURRENCY;
  }
}