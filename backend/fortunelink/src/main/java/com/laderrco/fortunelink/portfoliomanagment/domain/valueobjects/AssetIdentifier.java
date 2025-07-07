package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;

public record AssetIdentifier(
    AssetType assetType,
    String isin,
    String assetCommonName,
    String assetExchangeInformation,
    String description
    
) {
    public AssetIdentifier{
        Objects.requireNonNull(assetType, "Asset type cannot be null.");
        Objects.requireNonNull(isin, "ISIN cannot be null.");
        Objects.requireNonNull(assetCommonName, "Asset common name cannot be null.");
        Objects.requireNonNull(assetExchangeInformation, "Asset exchange information cannot be null.");
        
        assetCommonName = assetCommonName.trim();
        if (assetCommonName.isBlank()) {
            throw new IllegalArgumentException("Asset name cannot be blank.");            
        }

        isin = isin.trim().toUpperCase();
        if (!isValidISIN(isin)) {
            throw new IllegalArgumentException("Invalid ISIN format.");
        }
    }

    private static boolean isValidISIN(String isin) {
        return isin != null && isin.matches("[A-Z]{2}[A-Z0-9]{9}[0-9]");
    }

    public boolean isCrypto() {
        return this.assetType == AssetType.CRYPTO;
    }

    public boolean isStockOrETF() {
        return this.assetType == AssetType.STOCK || this.assetType == AssetType.ETF;
    }
}
