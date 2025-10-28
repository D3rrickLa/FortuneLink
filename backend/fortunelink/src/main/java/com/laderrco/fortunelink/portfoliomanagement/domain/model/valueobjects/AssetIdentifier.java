package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

public sealed interface AssetIdentifier permits MarketIdentifier, CustomAssetIdentifier, CashAssetIdentifier {
    String displayName();     
}