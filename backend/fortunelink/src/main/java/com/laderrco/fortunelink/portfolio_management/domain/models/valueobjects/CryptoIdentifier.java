package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

public record CryptoIdentifier(String primaryId, String name, AssetType assetType, String unitOfTrade, Map<String, String> metadata) implements AssetIdentifier, ClassValidation {

    @Override
    public String getPrimaryId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPrimaryId'");
    }

    @Override
    public String displayName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'displayName'");
    }

    @Override
    public AssetType getAssetType() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAssetType'");
    }
    
}
