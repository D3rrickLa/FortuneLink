package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models;

/**
 * Internal representation of asset metadata from any provider.
 */
public record ProviderAssetInfo(
    String symbol,
    String name,
    String description,
    String assetType,    // "STOCK", "ETF", "CRYPTO"
    String exchange,     // "NYSE", "NASDAQ", "TSX"
    String currency,     // "USD", "CAD"
    String sector,
    String source
) {
    public ProviderAssetInfo {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
    }
}