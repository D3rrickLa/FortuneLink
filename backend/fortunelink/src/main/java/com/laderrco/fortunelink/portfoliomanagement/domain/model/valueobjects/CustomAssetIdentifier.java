package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record CustomAssetIdentifier(UUID id, String name) implements AssetIdentifier {

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
    
}
