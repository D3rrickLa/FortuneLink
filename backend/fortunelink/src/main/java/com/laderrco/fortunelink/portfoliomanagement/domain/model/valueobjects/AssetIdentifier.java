package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

public sealed interface AssetIdentifier permits MarketIdentifier, CustomAssetIdentifier {
    String displayName();  
    
    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();
}