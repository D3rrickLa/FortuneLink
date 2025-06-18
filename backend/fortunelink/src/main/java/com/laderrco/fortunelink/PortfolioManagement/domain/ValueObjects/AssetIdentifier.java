package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.util.Objects;

public record AssetIdentifier(String tickerSymbol, String exchange, String cryptoSymbol, String assetCommonName) {
    public AssetIdentifier {
        if (!cryptoSymbol.trim().isEmpty() || !cryptoSymbol.trim().isBlank()) {
            cryptoSymbol.toUpperCase();
        }

        Objects.requireNonNull(tickerSymbol, "Ticket symbol cannot be null.");
        Objects.requireNonNull(exchange, "Financial exchange cannot be null.");
        Objects.requireNonNull(assetCommonName, "Asset name cannot be null.");

        tickerSymbol.trim().toUpperCase();
        exchange.trim().toUpperCase();
        assetCommonName.trim().toUpperCase();
        
    }
}