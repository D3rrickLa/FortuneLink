package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

public class AssetTransferDetails extends TransactionDetails{
    private final UUID sourceAccountId;
    private final UUID destinationAccountId;
    private final AssetIdentifier assetIdentifier;
    private final BigDecimal quantity;
    private final Money costBasisPerUnit; // allowed to be null
    
    public AssetTransferDetails(UUID sourceAccountId, UUID destinationAccountId, AssetIdentifier assetIdentifier, BigDecimal quantity, Money costBasisPerUnit) {

        Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null for AssetTransferDetails.");
        Objects.requireNonNull(quantity, "Quantity cannot be null for AssetTransferDetails.");
        
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive for AssetTransferDetails.");
        }

        if (sourceAccountId == null && destinationAccountId == null) {
            throw new IllegalArgumentException("Either sourceAccountId or destinationAccountId (or both) must be specified for AssetTransferDetails.");
        }

        if (costBasisPerUnit != null) {
            // This assumes AssetIdentifier.currency() returns the primary currency of the asset
            // If your assetIdentifier doesn't have a currency, you might check against the portfolio's base currency later.
            // if (!costBasisPerUnit.currency().equals(assetIdentifier.currency())) {
            //     throw new IllegalArgumentException("Cost basis currency must match asset currency for AssetTransferDetails.");
            // }
            // Or if Money.currency() can be null for Money.ZERO etc., you might need more checks
        }


        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.assetIdentifier = assetIdentifier;
        this.quantity = quantity;
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
        return quantity;
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

        AssetTransferDetails that = (AssetTransferDetails) o;
        return Objects.equals(this.sourceAccountId, that.sourceAccountId)
            && Objects.equals(this.destinationAccountId, that.destinationAccountId)
            && Objects.equals(this.assetIdentifier, that.assetIdentifier)
            && Objects.equals(this.quantity, that.quantity)
            && Objects.equals(this.costBasisPerUnit, that.costBasisPerUnit);
    }
    
    @Override
    public int hashCode() {
            return Objects.hash(this.sourceAccountId, this.destinationAccountId);
    }
    
    
}
