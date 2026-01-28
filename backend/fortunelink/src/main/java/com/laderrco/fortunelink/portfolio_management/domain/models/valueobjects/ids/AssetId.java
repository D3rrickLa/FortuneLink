package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids;

import java.util.UUID;

import com.laderrco.fortunelink.shared.valueobjects.GenericId;

public record AssetId(UUID assetId) implements GenericId {
    public AssetId {
        assetId = GenericId.validate(assetId);
    }
    
    public static AssetId randomId() {
        return GenericId.random(AssetId::new);
    }
}