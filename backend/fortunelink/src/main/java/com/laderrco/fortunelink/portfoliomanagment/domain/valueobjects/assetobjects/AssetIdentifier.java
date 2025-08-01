package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects;

import java.util.Objects;
import java.util.Set;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;

public record AssetIdentifier(
    AssetType type,
    String symbol,
    String isin,
    String assetName,
    String assetExchangeName
) {
    public AssetIdentifier {
        Objects.requireNonNull(type, "Asset Type cannot be null.");
        Objects.requireNonNull(symbol, "Asset Symbol cannot be null.");
        Objects.requireNonNull(isin, "ISIN cannot be null.");
        Objects.requireNonNull(assetName, "Assest Name cannot be null.");
        Objects.requireNonNull(assetExchangeName, "Asset Exchange cannot be null.");

        assetName = assetName.trim();
        assetExchangeName = assetExchangeName.trim().toUpperCase();
        isin = isin.trim().toUpperCase();
        if (assetName.isEmpty()) {
            throw new IllegalArgumentException("Asset Name cannot be blank.");
        }
        if (assetExchangeName.isEmpty()) {
            throw new IllegalArgumentException("Asset Name cannot be blank.");
        }
        if (isValidISIN(isin) == false) {
            throw new IllegalArgumentException("Invalid ISIN format.");
        }
        
    }

    private boolean isValidISIN(String isin) {
        return isin.matches("[A-Z]{2}[A-Z0-9]{9}[0-9]");
    }

    public boolean isCrypto() {
        return this.type == AssetType.CRYPTO;
    }

    public boolean isStockOrEtf() {
        return Set.of(AssetType.STOCK, AssetType.ETF).contains(this.type);
    }
}
