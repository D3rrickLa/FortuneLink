package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record CustomAssetId(UUID id, String name) implements AssetIdentifier {
    public CustomAssetId {
        Objects.nonNull(id);
        Objects.nonNull(name);
        if (name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
    }

    public static CustomAssetId create(String name) {
        return new CustomAssetId(UUID.randomUUID(), name);
    }

    @Override
    public String displayName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'displayName'");
    }
    
}
