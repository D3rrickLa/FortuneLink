package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.AssetType;

public record AssetIdentifier(AssetType assetType, String assetCommonName, String primaryIdentifier, String secondaryIdentifier) {
    public AssetIdentifier {
        Objects.requireNonNull(assetType, "assetType cannot be null");
        Objects.requireNonNull(assetCommonName, "assetCommonName cannot be null.");
        Objects.requireNonNull(primaryIdentifier, "primaryIdentifier cannot be null.");
        
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

    public boolean isCrypto() {
        return this.assetType == AssetType.CRYPTO;
    }

    public boolean isStockOrETF() {
        return this.assetType == AssetType.STOCK || this.assetType == AssetType.ETF;
    }
}