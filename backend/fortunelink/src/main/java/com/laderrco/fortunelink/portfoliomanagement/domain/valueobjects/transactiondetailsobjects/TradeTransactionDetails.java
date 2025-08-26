package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.TradeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.MonetaryAmount;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.AssetHoldingId;

public class TradeTransactionDetails extends TransactionDetails {
    private final AssetHoldingId assetHoldingId;
    private final AssetIdentifier assetIdentifier;
    private final BigDecimal quantity;

    private final MonetaryAmount pricePerUnit;
 

    private final MonetaryAmount realizedGainLoss;
    private final MonetaryAmount acbPerUnitAtSale;

    protected TradeTransactionDetails(
        AssetHoldingId assetHoldingId, 
        AssetIdentifier assetIdentifier, 
        BigDecimal quantity,
        MonetaryAmount pricePerUnit,
        MonetaryAmount realizedGainLoss, // Sell specific
        MonetaryAmount acbPerUnitAtSale, // Sell specific
        
        TransactionSource source, 
        String description, 
        List<Fee> fees
    ) {
        super(source, description, fees);

        this.assetHoldingId = Objects.requireNonNull(assetHoldingId, "Asset holding id cannot be null.");
        this.assetIdentifier = Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");
        this.quantity = Objects.requireNonNull(quantity, "Quantity cannot be null.");
        this.pricePerUnit = Objects.requireNonNull(pricePerUnit, "Price per unit cannot be null.");
        this.realizedGainLoss = realizedGainLoss;
        this.acbPerUnitAtSale = acbPerUnitAtSale;
    }

    public static final TradeTransactionDetails buy(
        AssetHoldingId assetHoldingId,
        AssetIdentifier assetIdentifier,
        BigDecimal quantity,
        MonetaryAmount pricePerUnit,
        TransactionSource source, 
        String description, 
        List<Fee> fees
    ) {
        return new TradeTransactionDetails(assetHoldingId, assetIdentifier, quantity, pricePerUnit, null, null, source, description, fees);
    }

    public static final TradeTransactionDetails sell(
        AssetHoldingId assetHoldingId, 
        AssetIdentifier assetIdentifier, 
        BigDecimal quantity,
        MonetaryAmount pricePerUnit,
        MonetaryAmount realizedGainLoss, // Sell specific
        MonetaryAmount acbPerUnitAtSale, // Sell specific
        
        TransactionSource source, 
        String description, 
        List<Fee> fees
    ) {
        realizedGainLoss = Objects.requireNonNull(realizedGainLoss, "Realized gain/loss cannot be null.");
        acbPerUnitAtSale = Objects.requireNonNull(acbPerUnitAtSale, "Adjust cost basis cannot be null.");
        return new TradeTransactionDetails(assetHoldingId, assetIdentifier, quantity, pricePerUnit, realizedGainLoss, acbPerUnitAtSale, source, description, fees);
    }
    
    // Net cost including fees
    public Money calculateNetCost(TradeType tradeType) {
        Money grossValue = getGrossValue().getConversionAmount();
        Money totalFees = super.getTotalFeesInCurrency(grossValue.currency());

        return switch (tradeType) {
            case BUY -> grossValue.add(totalFees);
            case SELL -> grossValue.subtract(totalFees);
            default -> throw new IllegalArgumentException("Unexpected trade type: " + tradeType);
        };
    }

    public boolean isBuy(TradeType tradeType) { return tradeType == TradeType.BUY; }
    public boolean isSell(TradeType tradeType) { return tradeType == TradeType.SELL; }

    // Gross value of the trade (without fees)
    public MonetaryAmount getGrossValue() {return pricePerUnit.multiply(quantity);}

    public AssetHoldingId getAssetHoldingId() {return assetHoldingId;}
    public AssetIdentifier getAssetIdentifier() {return assetIdentifier;}
    public BigDecimal getQuantity() {return quantity;}
    public MonetaryAmount getPricePerUnit() {return pricePerUnit;}
    public MonetaryAmount getRealizedGainLoss() {return realizedGainLoss;}
    public MonetaryAmount getAcbPerUnitAtSale() {return acbPerUnitAtSale;}
    
}
