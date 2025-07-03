package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsimpl;

import java.math.BigDecimal;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.interfaces.TransactionDetails;

public final class CorporateActionTransactionDetails extends TransactionDetails {
    private final AssetIdentifier assetIdentifier;
    private final BigDecimal splitRatio;
    
    public CorporateActionTransactionDetails(AssetIdentifier assetIdentifier, BigDecimal splitRatio) {
        Objects.requireNonNull(assetIdentifier, "Asset Identifier cannot be null");

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