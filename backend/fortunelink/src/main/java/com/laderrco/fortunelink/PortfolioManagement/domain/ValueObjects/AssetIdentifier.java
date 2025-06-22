package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.util.Objects;

// NOTE: you would really put this as an interface, because there are many assets besides crypto and stocks/etfs
// think about holding actual metals or private companies... but that is out of scope for now
public record AssetIdentifier(String tickerSymbol, String exchange, String cryptoSymbol, String assetCommonName) {
    public AssetIdentifier {
        Objects.requireNonNull(exchange, "Exchange platform cannot be null.");
        Objects.requireNonNull(assetCommonName, "Asset name cannot be null.");

        if (tickerSymbol == null && cryptoSymbol == null) {
            throw new IllegalArgumentException(
                    "AssetIdentifier must represent either a traditional asset (ticker/exchange) or a cryptocurrency (crypto symbol).");
        }
        if (tickerSymbol != null && cryptoSymbol != null) {
            throw new IllegalArgumentException(
                    "AssetIdentifier cannot represent both a traditional asset and a cryptocurrency.");
        }

        // need to test two cases, when it's traditional stocks and when it's
        // alternative (crypto)
        if (tickerSymbol != null) {
            tickerSymbol = tickerSymbol.trim().toUpperCase();
        } else {
            cryptoSymbol = cryptoSymbol.trim().toUpperCase();
        }
        
        exchange = exchange.trim().toUpperCase();
        assetCommonName = assetCommonName.trim();

    }

    // What can the Asset Identifier do? - verbs
    // find if item is a stock or crypto

    public boolean isStockOrETF() {
        return !this.tickerSymbol.isEmpty();
    }

    public boolean isCrypto() {
        return !this.cryptoSymbol.isEmpty();
    }
}
