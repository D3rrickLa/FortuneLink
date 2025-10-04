package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import java.util.Objects;

public record MarketSymbol(String symbol) implements AssetIdentifier {

    public MarketSymbol {
        Objects.nonNull(symbol);
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be blank");
        }
        symbol = symbol.trim().toUpperCase();
    }

    public static MarketSymbol of(String sybmol) {
        return new MarketSymbol(sybmol);
    }

    @Override
    public String displayName() {
        return this.symbol;
    }
    
}
