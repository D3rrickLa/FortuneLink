package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.AssetType;

public record CustomAssetIdentifier(UUID id, String name, AssetType assetType) implements AssetIdentifier {

    public CustomAssetIdentifier {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        if (name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank for custom asset identifier");
        }
    }

    @Override
    public String displayName() {
        return name;
    }

    @Override
    public AssetType getAssetType() {
        return assetType;
    }
    
}
