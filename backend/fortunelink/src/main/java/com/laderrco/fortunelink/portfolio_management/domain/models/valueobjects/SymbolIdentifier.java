package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;

// TODO: replace the current imp in the application service with this so we can get ride of the infra mapper
public record SymbolIdentifier(String symbol) implements AssetIdentifier {

    public static SymbolIdentifier of(String symbol) {
        return new SymbolIdentifier(symbol);
    }

    @Override
    public String getPrimaryId() {
        return symbol;
    }

    @Override
    public String displayName() {
        return "UNKNOWN, SYMBOL GIVEN ONLY";
    }

    @Override
    public AssetType getAssetType() {
        return AssetType.OTHER;
    }
    
}
