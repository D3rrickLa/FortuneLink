package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids;

import java.util.Objects;
import java.util.UUID;

public record AssetHoldingId(UUID assetHoldingId) {
    public AssetHoldingId {
        Objects.requireNonNull(assetHoldingId, "Asset holding id cannot be null.");
    }

    public static AssetHoldingId createRandom() {
        return new AssetHoldingId(UUID.randomUUID());
    }    
}
