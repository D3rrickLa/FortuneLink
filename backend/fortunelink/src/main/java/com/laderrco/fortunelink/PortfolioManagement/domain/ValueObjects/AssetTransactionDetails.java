package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.math.BigDecimal;

import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

// these are immutable, while asset holding can change and evolve
// this transaction details can't, if we passed the 
// AssetHolding itself, will not be accurate as it can update
// we are pointing to the type of asset, not the specific instance
public final class AssetTransactionDetails extends TransactionDetails {
    private final AssetIdentifier assetIdentifier;
    private final BigDecimal quantity;
    private final Money pricePerUnit;
    
    public AssetTransactionDetails(AssetIdentifier assetIdentifier, BigDecimal quantity,
            Money pricePerUnit) {
        this.assetIdentifier = assetIdentifier;
        this.quantity = quantity;
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
}
