package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.util.Objects;

public record AssetIdentifier(String tickerSymbol, String exchange, String cryptoSymbol, String assetCommonName) {
    public AssetIdentifier {

        Objects.requireNonNull(tickerSymbol, "Ticket symbol cannot be null.");
        Objects.requireNonNull(exchange, "Financial exchange cannot be null.");
        Objects.requireNonNull(assetCommonName, "Asset name cannot be null.");

        // Handle cryptoSymbol: if it's provided, normalize it, otherwise null it out
        // for consistency
        if (cryptoSymbol != null && !cryptoSymbol.trim().isEmpty()) {
            cryptoSymbol = cryptoSymbol.trim().toUpperCase();
        } else {
            cryptoSymbol = null; // Ensure it's truly null if empty/blank
        }

        tickerSymbol = tickerSymbol.trim().toUpperCase();
        exchange = exchange.trim().toUpperCase();
        assetCommonName = assetCommonName.trim();

        // An AssetIdentifier should be for *either* a sotck/trad asset OR crypto, not
        // both
        // AI coded
        boolean hasTraditionalTicker = !tickerSymbol.isEmpty();
        boolean hasTraditionalExchange = !exchange.isEmpty();
        boolean isCrypto = cryptoSymbol != null;

        // basically there was a problem with the code, our more specific tests need ot
        // come first than general

        // 1. First, enforce the "ticker/exchange must be paired" rule.
        // This is a prerequisite for correctly determining `isTraditionalAsset`.
        if (hasTraditionalTicker && !hasTraditionalExchange) {
            throw new IllegalArgumentException("Exchange cannot be empty if ticker symbol is provided.");
        }
        if (!hasTraditionalTicker && hasTraditionalExchange) {
            throw new IllegalArgumentException("Ticker symbol cannot be empty if exchange is provided.");
        }

        // 2. Now that we know ticker and exchange are either both present or both
        // absent,
        // we can correctly set isTraditionalAsset.
        boolean isTraditionalAsset = hasTraditionalTicker && hasTraditionalExchange;

        // 3. Next, enforce mutual exclusivity: cannot be both traditional AND crypto.
        if (isTraditionalAsset && isCrypto) {
            throw new IllegalArgumentException(
                    "AssetIdentifier cannot represent both a traditional asset and a cryptocurrency.");
        }

        // 4. Finally, enforce that it must be one type or the other (not neither).
        if (!isTraditionalAsset && !isCrypto) {
            throw new IllegalArgumentException(
                    "AssetIdentifier must represent either a traditional asset (ticker/exchange) or a cryptocurrency (crypto symbol).");
        }
        // ----

    }

    // Behaviours realted to its value

    public boolean isStockOrEtf() {
        return !tickerSymbol.isEmpty() && !exchange.isEmpty();
    }

    public boolean isCrypto() {
        return cryptoSymbol != null;
    }

    public String toCanonicalString() {
        if (isStockOrEtf()) {
            return String.format("%s (%s)", tickerSymbol, exchange);
        } else if (isCrypto()) {
            return cryptoSymbol;
        } else { // this can't run 
            // fallback if something happens
            // this should never happen because of our constructor validation, but just in
            // case
            return assetCommonName;
        }
    }

    public String toDisplayName() {
        return assetCommonName;
    }

}