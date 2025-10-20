package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids;

import java.util.UUID;

public record AssetId(UUID assetId) implements GenericId {
    public AssetId {
        assetId = GenericId.validate(assetId);
    }
    public static AssetId randomId() {
        return GenericId.random(AssetId::new);
    }
}
