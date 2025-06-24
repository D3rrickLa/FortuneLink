package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.util.Objects;

// NOTE: you would really put this as an interface, because there are many assets besides crypto and stocks/etfs
// think about holding actual metals or private companies... but that is out of scope for now
public record AssetIdentifier(String tickerSymbol, String exchange, String cryptoSymbol, String assetCommonName) {
    public AssetIdentifier {
        Objects.requireNonNull(exchange, "Exchange platform cannot be null.");
        Objects.requireNonNull(assetCommonName, "Asset name cannot be null.");
        String normalizedExchange = exchange.trim().toUpperCase();
        String normalizedAssetName = assetCommonName.trim();
        String normalizedTicker = tickerSymbol != null ? tickerSymbol.trim().toUpperCase() : null;
        String normalizedCrypto = (cryptoSymbol != null && !cryptoSymbol.trim().isEmpty())
                ? cryptoSymbol.trim().toUpperCase()
                : null;

        boolean hasTraditionalTicker = normalizedTicker != null && !normalizedTicker.isEmpty();
        boolean isCrypto = normalizedCrypto != null;

        if (hasTraditionalTicker && isCrypto) {
            throw new IllegalArgumentException(
                    "AssetIdentifier cannot represent both a traditional asset and a cryptocurrency.");
        }
        if (!hasTraditionalTicker && !isCrypto) {
            throw new IllegalArgumentException(
                    "AssetIdentifier must represent either a traditional asset (ticker/exchange) or a cryptocurrency (crypto symbol).");
        }

        // need to test two cases, when it's traditional stocks and when it's
        // alternative (crypto)
        if (tickerSymbol != null) {
            tickerSymbol = tickerSymbol.trim().toUpperCase();
        } else {
            cryptoSymbol = cryptoSymbol.trim().toUpperCase();
        }

        tickerSymbol = normalizedTicker;
        cryptoSymbol = normalizedCrypto;
        exchange = normalizedExchange;
        assetCommonName = normalizedAssetName;

    }

    // What can the Asset Identifier do? - verbs
    // find if item is a stock or crypto

    public boolean isStockOrETF() {
        return this.tickerSymbol == null ? false : true;
    }

    public boolean isCrypto() {
        return this.cryptoSymbol == null ? false : true;
    }

    // for All Value Objects we compare everything but memory address
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AssetIdentifier that = (AssetIdentifier) o;
        return Objects.equals(this.tickerSymbol, that.tickerSymbol())
                && Objects.equals(this.exchange, that.exchange())
                && Objects.equals(this.cryptoSymbol, that.cryptoSymbol())
                && Objects.equals(this.assetCommonName, that.assetCommonName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.tickerSymbol, this.exchange, this.cryptoSymbol, this.assetCommonName);
    }
}
