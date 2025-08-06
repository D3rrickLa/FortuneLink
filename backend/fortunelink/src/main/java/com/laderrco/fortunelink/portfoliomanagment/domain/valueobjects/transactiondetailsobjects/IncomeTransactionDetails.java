package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.AssetHoldingId;

public final class IncomeTransactionDetails extends TransactionDetails {
    /*
     * for handling passive income events like dividends or interest payments from an asset
     */

    private final AssetHoldingId assetHoldingId;
    private final AssetIdentifier assetIdentifier; // the asset that generated the income
    private final Money amount; // amount of money received in native currency
    // should have an incomeType Enum, but most of the values are in the TransactionType Enum

    protected IncomeTransactionDetails(
        AssetHoldingId assetHoldingId,
        AssetIdentifier assetIdentifier,
        Money amount,
        
        TransactionSource source, 
        String description, 
        List<Fee> fees) {
        super(source, description, fees);
        
        validateParameter(assetHoldingId, "Asset holding id");
        validateParameter(assetIdentifier, "Asset identifier");
        validateParameter(amount, "Amount");

        this.assetHoldingId = assetHoldingId;
        this.assetIdentifier = assetIdentifier;
        this.amount = amount;
    }

    private void validateParameter(Object other, String parameterName) {
        Objects.requireNonNull(other, String.format("%s cannot be null.", parameterName));
    }

    public AssetHoldingId getAssetHoldingId() {
        return assetHoldingId;
    }

    public AssetIdentifier getAssetIdentifier() {
        return assetIdentifier;
    }

    public Money getAmount() {
        return amount;
    }
    
}
