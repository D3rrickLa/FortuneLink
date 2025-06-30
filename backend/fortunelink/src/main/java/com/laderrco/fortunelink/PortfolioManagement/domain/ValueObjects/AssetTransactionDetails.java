package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;


// these are immutable, while asset holding can change and evolve
// this transaction details can't, if we passed the 
// AssetHolding itself, will not be accurate as it can update
// we are pointing to the type of asset, not the specific instance
public final class AssetTransactionDetails extends TransactionDetails {
    private final AssetIdentifier assetIdentifier;
    private final BigDecimal quantity;
    private final Money pricePerUnit;

    public AssetTransactionDetails(AssetIdentifier assetIdentifier, BigDecimal quantity,Money pricePerUnit) {
            Objects.requireNonNull(assetIdentifier, "Asset Identifier cannot be null.");
            Objects.requireNonNull(quantity, "Quantity cannot be null.");
            Objects.requireNonNull(pricePerUnit, "Price Per Unit cannot be null.");

            if (pricePerUnit.amount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Price of each asset unit cannot be less than 0.");
            }
            
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity cannot be negative.");
            }

        this.assetIdentifier = assetIdentifier;
        this.quantity = quantity.setScale(6, RoundingMode.HALF_UP); // should really define a scale to use
        this.pricePerUnit = pricePerUnit;
    }

    public AssetIdentifier getAssetIdentifier() {
        return assetIdentifier;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Money getPricePerUnit() {
        return pricePerUnit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AssetTransactionDetails that = (AssetTransactionDetails) o;
        return Objects.equals(this.assetIdentifier, that.assetIdentifier) && Objects.equals(this.quantity, that.quantity)
                && Objects.equals(this.pricePerUnit, that.pricePerUnit);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.assetIdentifier);
    }
}
