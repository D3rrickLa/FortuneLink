package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;

public interface AssetIdentifier {
    String getPrimaryId();
    String displayName();     
    AssetType getAssetType();
    
    default String cacheKey() {
        return String.format("%s:%s", getClass().getSimpleName(), getPrimaryId());
    }
}