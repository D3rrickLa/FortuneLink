package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

// unit of trade -> what are we trading in. This is similar to the Asset.java 'currency', but 
// for this var, we are talking about what is acutally being trade (BTC, oz/gold, USD, etc.)
public record MarketIdentifier(String primaryId, Map<String, String> secondaryIds, AssetType assetType, String name, String unitOfTrade, Map<String, String> metadata) implements AssetIdentifier, ClassValidation {
    public MarketIdentifier {
        primaryId = ClassValidation.validateParameter(primaryId, "Primary Id");
        assetType = ClassValidation.validateParameter(assetType, "Asset Type");
        name = ClassValidation.validateParameter(name, "Name");
        unitOfTrade = ClassValidation.validateParameter(unitOfTrade, "Unit of Trade");
        if (!assetType.isMarketTraded()) {
            throw new IllegalArgumentException(
                "MarketIdentifier only supports market-traded assets. " +
                assetType + " (" + assetType.getCategory() + ") is not valid."
            );
        }
        

        name = name.trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("Asset name cannot be blank");
        }
    }

    @Override
    public String getPrimaryId() {
        return this.primaryId;
    }

    @Override
    public String displayName() {
        return this.name;
    }

    @Override
    public AssetType getAssetType() {
        return this.assetType;
    }
    
}
