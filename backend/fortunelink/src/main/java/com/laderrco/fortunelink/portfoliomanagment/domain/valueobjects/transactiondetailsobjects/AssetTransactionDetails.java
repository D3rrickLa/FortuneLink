package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;

public final class AssetTransactionDetails extends TransactionDetails {
    private final AssetIdentifier assetIdentifier;
    private final BigDecimal quantity;
    private final Money pricePerUnit;
    private final Money assetValueInAssetCurrency; // quant * ppu
    private final Money assetValueInPortfolioCurrency;
    private final Money costBasisInPortfolioCurrency; // quant * ppu + fees
    private final Money costBasisInAssetCurrency;
    private final Money totalFeesInPortfolioCurrency; // SIMPLIFIED: Combined all fees
    private final Money totalFeesInAssetCurrency;
    private final UUID assetHoldingId;

    public AssetTransactionDetails(
        AssetIdentifier assetIdentifier, 
        BigDecimal quantity, 
        Money pricePerUnit,
        Money assetValueInAssetCurrency, 
        Money assetValueInPortfolioCurrency, 
        Money costBasisInPortfolioCurrency,
        Money costBasisInAssetCurrency,
        Money totalFeesInPortfolioCurrency,
        Money totalFeesInAssetCurrency,
        UUID assetHoldingId
    ) {
        Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");
        Objects.requireNonNull(quantity, "Quantity cannot be null.");
        Objects.requireNonNull(pricePerUnit, "Price per unit cannot be null.");
        Objects.requireNonNull(assetValueInAssetCurrency, "Asset value in asset currency cannot be null.");
        Objects.requireNonNull(assetValueInPortfolioCurrency, "Asset value in portfolio currency cannot be null.");
        Objects.requireNonNull(costBasisInPortfolioCurrency, "Cost basis in portfolio currency cannot be null.");
        Objects.requireNonNull(costBasisInAssetCurrency, "Cost basis in asset currency cannot be null.");
        Objects.requireNonNull(totalFeesInPortfolioCurrency, "Total fees in portfolio currency cannot be null.");
        Objects.requireNonNull(totalFeesInAssetCurrency, "Total fees in asset currency cannot be null.");
        
        this.assetIdentifier = assetIdentifier;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.assetValueInAssetCurrency = assetValueInAssetCurrency;
        this.assetValueInPortfolioCurrency = assetValueInPortfolioCurrency;
        this.costBasisInPortfolioCurrency = costBasisInPortfolioCurrency;
        this.costBasisInAssetCurrency = costBasisInAssetCurrency;
        this.totalFeesInPortfolioCurrency = totalFeesInPortfolioCurrency;
        this.totalFeesInAssetCurrency = totalFeesInAssetCurrency;
        this.assetHoldingId = assetHoldingId;
    }

    /**
     * Returns a new AssetTransactionDetails instance with the specified assetHoldingId.
     * This is essential for populating the ID after it's generated.
     */
    public AssetTransactionDetails withAssetHoldingId(UUID newAssetHoldingId) {
        return new AssetTransactionDetails(
            this.assetIdentifier,
            this.quantity,
            this.pricePerUnit,
            this.assetValueInAssetCurrency,
            this.assetValueInPortfolioCurrency,
            this.costBasisInPortfolioCurrency,
            this.costBasisInAssetCurrency,
            this.totalFeesInPortfolioCurrency,
            this.totalFeesInAssetCurrency,
            newAssetHoldingId
            
        );
    }

    public AssetTransactionDetails(
        AssetIdentifier assetIdentifier, 
        BigDecimal quantity, 
        Money pricePerUnit,
        Money assetValueInAssetCurrency, 
        Money assetValueInPortfolioCurrency, 
        Money costBasisInPortfolioCurrency,
        Money costBasisInAssetCurrency,
        Money totalFeesInPortfolioCurrency,
        Money totalFeesInAssetCurrency
    ) {
        super();
        this.assetIdentifier = assetIdentifier;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.assetValueInAssetCurrency = assetValueInAssetCurrency;
        this.assetValueInPortfolioCurrency = assetValueInPortfolioCurrency;
        this.costBasisInPortfolioCurrency = costBasisInPortfolioCurrency;
        this.costBasisInAssetCurrency = costBasisInAssetCurrency;
        this.totalFeesInPortfolioCurrency = totalFeesInPortfolioCurrency;
        this.totalFeesInAssetCurrency = totalFeesInAssetCurrency;
        this.assetHoldingId = null;
    }
    
    
    public UUID getAssetHoldingId() {return assetHoldingId;}
    public AssetIdentifier getAssetIdentifier() {return assetIdentifier;}
    public BigDecimal getQuantity() {return quantity;}
    public Money getPricePerUnit() {return pricePerUnit;}
    public Money getAssetValueInAssetCurrency() {return assetValueInAssetCurrency;}
    public Money getAssetValueInPortfolioCurrency() {return assetValueInPortfolioCurrency;}
    public Money getCostBasisInPortfolioCurrency() {return costBasisInPortfolioCurrency;}
    public Money getCostBasisInAssetCurrency() {return costBasisInAssetCurrency;}
    public Money getTotalFeesInPortfolioCurrency() {return totalFeesInPortfolioCurrency;}
    public Money getTotalFeesInAssetCurrency() {return totalFeesInAssetCurrency;}

    
    
}   

/*
 * NOTE
 * so for fees, we want to sum only those that aren't in portfolio prefernce
 */