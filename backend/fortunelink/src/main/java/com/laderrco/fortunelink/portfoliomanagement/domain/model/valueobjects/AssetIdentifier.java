package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.AssetType;

public sealed interface AssetIdentifier permits MarketIdentifier, CustomAssetIdentifier, CashAssetIdentifier {
    String displayName();     
    AssetType getAssetType();
}