package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.AssetType;

// NOTE: you would really put this as an interface, because there are many assets besides crypto and stocks/etfs
// think about holding actual metals or private companies... but that is out of scope for now
public record AssetIdentifier(AssetType assetType, String primaryIdentifier, String assetCommonName, String secondaryIdentifier) {
    public AssetIdentifier {
        Objects.requireNonNull(assetType, "Asset type cannot null.");
        Objects.requireNonNull(primaryIdentifier, "Primary identifier cannot be null.");
        Objects.requireNonNull(assetCommonName, "Asset name cannot be null.");

        primaryIdentifier = primaryIdentifier.trim().toUpperCase();
        assetCommonName = assetCommonName.trim();
        secondaryIdentifier = secondaryIdentifier != null ? secondaryIdentifier.trim().toUpperCase() : null;

        if (assetCommonName.isBlank()) {
            throw new IllegalArgumentException("Asset name cannot be blank.");
        }

        switch (assetType) {
            case STOCK, ETF:
                if (primaryIdentifier.isEmpty()) {
                    throw new IllegalArgumentException("Stock/ETF must have a ticker symbol as primary identifier.");
                }
                if (secondaryIdentifier == null || secondaryIdentifier.isEmpty()) {
                    throw new IllegalArgumentException("Stock/ETF must have an exchange as secondary identifier.");
                }
                break;
            case BOND:
                if (primaryIdentifier.isEmpty()) {
                    throw new IllegalArgumentException("Bond must have a CUSIP/ISIN or unique ID as primary identifier.");
                }
                break;
            case CRYPTO:
                if (primaryIdentifier.isEmpty()) {
                    throw new IllegalArgumentException("Cryptocurrency must have a symbol as primary identifier.");
                }
                break;
            case COMMODITY:
                if (primaryIdentifier.isEmpty()) {
                    throw new IllegalArgumentException("Commodity must have a symbol/code as primary identifier.");
                }
                break;
            case FOREX_PAIR:
                if (primaryIdentifier.isEmpty() || !primaryIdentifier.matches("[A-Z]{1,6}")) { // e.g., 6-letter pair. MUST BE 6 LEN LONG...
                    throw new IllegalArgumentException("Forex pair must have a valid 6-letter symbol.");
                }
                break;

            default:
                throw new IllegalArgumentException("ERROR, unknown Asset Type given.");
        }
    }

    // What can the Asset Identifier do? - verbs
    // find if item is a stock or crypto

    public boolean isCrypto() {
        return this.assetType == AssetType.CRYPTO;
    }

    public boolean isStockOrETF() {
        return this.assetType == AssetType.STOCK || this.assetType == AssetType.ETF;
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
        return Objects.equals(this.assetType, that.assetType())
                && Objects.equals(this.primaryIdentifier, that.primaryIdentifier())
                && Objects.equals(this.secondaryIdentifier, that.secondaryIdentifier())
                && Objects.equals(this.assetCommonName, that.assetCommonName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.assetType, this.primaryIdentifier, this.secondaryIdentifier, this.assetCommonName);
    }
}
