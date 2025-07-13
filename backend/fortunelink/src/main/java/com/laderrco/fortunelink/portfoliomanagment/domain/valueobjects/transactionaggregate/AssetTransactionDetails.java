package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate;

import java.math.BigDecimal;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public final class AssetTransactionDetails extends TransactionDetails {
    private final AssetIdentifier assetIdentifier;
    private final BigDecimal quantity;
    private final Money pricePerUnit;
    private final Money assetValueInAssetCurrency;
    private final Money assetValueInPortfolioCurrency;
    private final Money costBasisInPortfolioCurrency;
    private final Money totalFeesInPortfolioCurrency; // SIMPLIFIED: Combined all fees
    
    public AssetTransactionDetails(
        AssetIdentifier assetIdentifier, 
        BigDecimal quantity, 
        Money pricePerUnit,
        Money assetValueInAssetCurrency, 
        Money assetValueInPortfolioCurrency, 
        Money costBasisInPortfolioCurrency,
        Money totalFeesInPortfolioCurrency
    ) {
        this.assetIdentifier = assetIdentifier;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.assetValueInAssetCurrency = assetValueInAssetCurrency;
        this.assetValueInPortfolioCurrency = assetValueInPortfolioCurrency;
        this.costBasisInPortfolioCurrency = costBasisInPortfolioCurrency;
        this.totalFeesInPortfolioCurrency = totalFeesInPortfolioCurrency;
    }

    public AssetIdentifier getAssetIdentifier() {return assetIdentifier;}
    public BigDecimal getQuantity() {return quantity;}
    public Money getPricePerUnit() {return pricePerUnit;}
    public Money getAssetValueInAssetCurrency() {return assetValueInAssetCurrency;}
    public Money getAssetValueInPortfolioCurrency() {return assetValueInPortfolioCurrency;}
    public Money getCostBasisInPortfolioCurrency() {return costBasisInPortfolioCurrency;}
    public Money getTotalFeesInPortfolioCurrency() {return totalFeesInPortfolioCurrency;}

    
}   
