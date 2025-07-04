package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsimpl;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.interfaces.TransactionDetails;

public final class CorporateActionTransactionDetails extends TransactionDetails {
    private final AssetIdentifier assetIdentifier;
    private final BigDecimal splitRatio;
    
    public CorporateActionTransactionDetails(AssetIdentifier assetIdentifier, BigDecimal splitRatio) {
        Objects.requireNonNull(assetIdentifier, "Asset Identifier cannot be null.");
        Objects.requireNonNull(splitRatio, "Split ratio cannot be null."); // Added null check for splitRatio

        // **NEW VALIDATION: Check AssetType**
        if (!Set.of(AssetType.ETF, AssetType.STOCK).contains(assetIdentifier.assetType())) {
            throw new IllegalArgumentException("Corporate action (like a split) is only applicable to Stock or ETF asset types.");
        }

        // **NEW VALIDATION: Check splitRatio value**
        if (splitRatio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Split ratio must be positive.");
        }

        this.assetIdentifier = assetIdentifier;
        this.splitRatio = splitRatio;
    }

    public AssetIdentifier getAssetIdentifier() {
        return assetIdentifier;
    }

    public BigDecimal getSplitRatio() {
        return splitRatio;
    }  

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CorporateActionTransactionDetails that = (CorporateActionTransactionDetails) o;
        return Objects.equals(this.assetIdentifier, that.assetIdentifier)
                && Objects.equals(this.splitRatio, that.splitRatio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.assetIdentifier, this.splitRatio);
    }
    
}