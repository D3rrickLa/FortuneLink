package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.AssetType;
// for stocks
public record MarketIdentifier(String symbol) implements AssetIdentifier {
    public MarketIdentifier {
        Objects.requireNonNull(symbol);
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be blank");
        }
    }

    @Override
    public String displayName() {
        return this.symbol;
    }

    @Override
    public AssetType getAssetType() {
        return AssetType.STOCK;
    }
    
}
