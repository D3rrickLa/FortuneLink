package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import java.util.Objects;

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
    
}
