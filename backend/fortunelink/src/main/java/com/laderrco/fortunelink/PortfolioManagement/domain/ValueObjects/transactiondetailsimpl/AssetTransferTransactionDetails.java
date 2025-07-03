package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsimpl;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.interfaces.TransactionDetails;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

public final class AssetTransferTransactionDetails extends TransactionDetails {
    private final UUID sourceAccountId;
    private final UUID destinationAccountId;
    private final AssetIdentifier assetIdentifier;
    private final BigDecimal assetQuantity;
    private final Money costBasisPerUnit;

    public AssetTransferTransactionDetails(UUID sourceAccountId, UUID destinationAccountId, AssetIdentifier assetIdentifier, BigDecimal assetQuantity, Money costBasisPerUnit) {
        Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null for AssetTransferDetails.");
        Objects.requireNonNull(assetQuantity, "Quantity cannot be null for AssetTransferDetails.");
        Objects.requireNonNull(costBasisPerUnit, "Cost Basis Per Unit cannot be null.");
        
        if (assetQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive for AssetTransferDetails.");
        }
        
        if (sourceAccountId == null && destinationAccountId == null) {
            throw new IllegalArgumentException("Either sourceAccountId or destinationAccountId (or both) must be specified for AssetTransferDetails.");
        }


        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.assetIdentifier = assetIdentifier;
        this.assetQuantity = assetQuantity;
        this.costBasisPerUnit = costBasisPerUnit;
    }
    
    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }
    
    public AssetIdentifier getAssetIdentifier() {
        return assetIdentifier;
    }

    public BigDecimal getQuantity() {
        return assetQuantity;
    }

    public Money getCostBasisPerUnit() {
        return costBasisPerUnit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AssetTransferTransactionDetails that = (AssetTransferTransactionDetails) o;
        return Objects.equals(this.sourceAccountId, that.sourceAccountId)
            && Objects.equals(this.destinationAccountId, that.destinationAccountId)
            && Objects.equals(this.assetIdentifier, that.assetIdentifier)
            && Objects.equals(this.assetQuantity, that.assetQuantity)
            && Objects.equals(this.costBasisPerUnit, that.costBasisPerUnit);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(this.sourceAccountId, this.destinationAccountId);
    }
    
    
}