package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

public record CryptoIdentifier(String primaryId, String name, AssetType assetType, String unitOfTrade, Map<String, String> metadata) implements AssetIdentifier, ClassValidation {
    public CryptoIdentifier {
        primaryId = ClassValidation.validateParameter(primaryId, "Primary Id");
        assetType = ClassValidation.validateParameter(assetType, "Asset Type");
        name = ClassValidation.validateParameter(name, "Name");
        unitOfTrade = ClassValidation.validateParameter(unitOfTrade, "Unit of Trade");

        name = name.trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("Asset name cannot be blank");
        }
    }
    @Override
    public String getPrimaryId() {
        return primaryId;
    }

    @Override
    public String displayName() {
        return name;
    }

    @Override
    public AssetType getAssetType() {
        return AssetType.CRYPTO;
    }
    
}
