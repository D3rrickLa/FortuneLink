package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.AssetHoldingId;

public final class TradeExecutionTransactionDetails extends TransactionDetails {
    private final AssetHoldingId assetHoldingId;
    private final AssetIdentifier assetIdentifier;
    private final BigDecimal quantity;
    private final Money pricePerUnit; // in native's currency
    private final Money assetValueInNativeCurrency; // quantity * ppu
    private final Money assetValueInPortfolioCurrency; // at time of purchase
    private final Money totalFeesInPortfolioCurrency;
    private final Money realizedGainLossAssetCurrency;
    private final Money realizedGainLossPortfolioCurrency;
    private final Money acbPerUnitAtSale; // Store ACB per unit at time of sale for reversals



    private TradeExecutionTransactionDetails(
        AssetHoldingId assetHoldingId,
        AssetIdentifier assetIdentifier,
        BigDecimal quantity,
        Money pricePerUnit,
        Money assetValueInNativeCurrency,
        Money assetValueInPortfolioCurrency,
        Money totalFeesInPortfolioCurrency,
        Money realizedGainLossAssetCurrency,
        Money realizedGainLossPortfolioCurrency,
        Money acbPerUnitAtSale,

        TransactionSource source,
        String description, 
        List<Fee> fees
    ) {
        super(source, description, fees);

        this.assetHoldingId = Objects.requireNonNull(assetHoldingId, "Asset holding id cannot be null.");
        this.assetIdentifier = Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");
        this.quantity = Objects.requireNonNull(quantity, "Quantity cannot be null.");
        this.pricePerUnit = Objects.requireNonNull(pricePerUnit, "Price per unit cannot be null.");
        this.assetValueInNativeCurrency = Objects.requireNonNull(assetValueInNativeCurrency, "Asset value in native currency cannot be null.");
        this.assetValueInPortfolioCurrency = Objects.requireNonNull(assetValueInPortfolioCurrency, "Asset value in portfolio currency cannot be null.");
        this.totalFeesInPortfolioCurrency = Objects.requireNonNull(totalFeesInPortfolioCurrency, "Total fees in portfolio currency cannot be null.");

        // For sell transaction, these must not be null
        this.realizedGainLossAssetCurrency = realizedGainLossAssetCurrency;
        this.realizedGainLossPortfolioCurrency = realizedGainLossPortfolioCurrency;
        this.acbPerUnitAtSale = acbPerUnitAtSale;
    }

    public static TradeExecutionTransactionDetails createBuyDetails(
        AssetHoldingId assetHoldingId,
        AssetIdentifier assetIdentifier,
        BigDecimal quantity,
        Money pricePerUnit, 
        Money assetValueInPortfolioCurrency, 
        Money totalFeesInPortfolioCurrency,

        TransactionSource source,
        String description,
        List<Fee> nativeFees

    ) {
        return new TradeExecutionTransactionDetails(
            assetHoldingId,
            assetIdentifier,
            quantity, 
            pricePerUnit,
            pricePerUnit.multiply(quantity), // assetValueInNativeCurrency   
            assetValueInPortfolioCurrency, // doesn't include fees
            totalFeesInPortfolioCurrency,
            null,
            null,
            null,
            source,
            description,
            nativeFees
        );
    }

    public static TradeExecutionTransactionDetails createSellDetails(
        AssetHoldingId assetHoldingId,
        AssetIdentifier assetIdentifier,
        BigDecimal quantity,
        Money pricePerUnit, 
        Money assetProceedsInPortfolioCurrency, 
        Money totalFeesInPortfolioCurrency,
        Money realizedGainLossAssetCurrency,
        Money realizedGainLossPortfolioCurrency,
        Money acbPerUnitAtSale,

        TransactionSource source,
        String description,
        List<Fee> nativeFees
    ) {
        Objects.requireNonNull(realizedGainLossAssetCurrency, "Realized gain/loss for asset currency cannot be null.");
        Objects.requireNonNull(realizedGainLossPortfolioCurrency, "Realized gain/loss for portfolio currency cannot be null.");
        Objects.requireNonNull(acbPerUnitAtSale, "ACB cannot be null.");

        return new TradeExecutionTransactionDetails(
            assetHoldingId, 
            assetIdentifier, 
            quantity, 
            pricePerUnit, 
            pricePerUnit.multiply(quantity), 
            assetProceedsInPortfolioCurrency, 
            totalFeesInPortfolioCurrency, 
            realizedGainLossAssetCurrency, 
            realizedGainLossPortfolioCurrency, 
            acbPerUnitAtSale, 
            source, 
            description, 
            nativeFees
        );
    }

    public AssetIdentifier getAssetIdentifier() { return assetIdentifier; }
    public BigDecimal getQuantity() { return quantity; }
    public Money getPricePerUnit() { return pricePerUnit; }
    public Money getAssetValueInNativeCurrency() { return assetValueInNativeCurrency; }
    public Money getAssetValueInPortfolioCurrency() { return assetValueInPortfolioCurrency; }
    public Money getTotalFeesInPortfolioCurrency() { return totalFeesInPortfolioCurrency; }
    public AssetHoldingId getAssetHoldingId() { return assetHoldingId; }
    public Money getRealizedGainLossAssetCurrency() { return realizedGainLossAssetCurrency; }
    public Money getRealizedGainLossPortfolioCurrency() { return realizedGainLossPortfolioCurrency; }
    public Money getAcbPerUnitAtSale() { return acbPerUnitAtSale; }
    
}
