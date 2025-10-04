package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids;

import java.util.Objects;
import java.util.UUID;

public record AssetHoldingId(UUID assetId) {
    public AssetHoldingId {
        Objects.nonNull(assetId);
    }

    public static AssetHoldingId randomId() {
        return new AssetHoldingId(UUID.randomUUID());
    }
}
