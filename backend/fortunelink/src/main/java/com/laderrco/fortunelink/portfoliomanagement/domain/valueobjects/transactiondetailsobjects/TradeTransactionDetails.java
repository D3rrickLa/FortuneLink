package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.TradeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.TransactionType;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidPriceException;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.MonetaryAmount;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.AssetHoldingId;

// net -> share * price - commission built into price, EXCLUDES FEES
public final class TradeTransactionDetails extends TransactionDetails {
    private final AssetHoldingId assetHoldingId;
    private final AssetIdentifier assetIdentifier;
    private final BigDecimal quantity;

    private final MonetaryAmount pricePerUnit;
 

    private final MonetaryAmount realizedGainLoss; // Only for SELL
    private final MonetaryAmount acbPerUnitAtSale; // Only for SELL

    private final Currency portfolioCurrency; // we have a dedicated field because of the follow issues that could happen
    // if portoflio's base currency changes
    // if you import a trade where you didn't yet have the FX rate - conversion is null?
    // if different amounts carry different conversion contexts
    // all of this can lead to getConvserionAmount -> null pointer exception or giving wrong numbers
    // we add the portfolio currency to enforce that every fee and every monetary amount can be converted to 'this' currency

    protected TradeTransactionDetails(
        AssetHoldingId assetHoldingId, 
        AssetIdentifier assetIdentifier, 
        BigDecimal quantity,
        MonetaryAmount pricePerUnit,
        MonetaryAmount realizedGainLoss, // Sell specific
        MonetaryAmount acbPerUnitAtSale, // Sell specific
        Currency portfolioCurrency,

        TransactionSource source, 
        String description, 
        List<Fee> fees
    ) {
        super(source, description, fees);

        this.assetHoldingId = Objects.requireNonNull(assetHoldingId, "Asset holding id cannot be null.");
        this.assetIdentifier = Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");
        this.quantity = Objects.requireNonNull(quantity, "Quantity cannot be null.");
        this.portfolioCurrency = Objects.requireNonNull(portfolioCurrency, "Portfolio currency cannot be null.");;
        
        this.realizedGainLoss = realizedGainLoss;
        this.acbPerUnitAtSale = acbPerUnitAtSale;
        
        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("Trade quantity must be positive.");
        }
        
        this.pricePerUnit = Objects.requireNonNull(pricePerUnit, "Price per unit cannot be null.");
        if (pricePerUnit.nativeAmount().amount().signum() < 0) {
            throw new InvalidPriceException("Price per unit cannot be negative.");
        }
    }

    public static final TradeTransactionDetails buy(
        AssetHoldingId assetHoldingId,
        AssetIdentifier assetIdentifier,
        BigDecimal quantity,
        MonetaryAmount pricePerUnit,
        Currency portfolioCurrency,
        TransactionSource source, 
        String description, 
        List<Fee> fees
    ) {
        return new TradeTransactionDetails(assetHoldingId, assetIdentifier, quantity, pricePerUnit, null, null, portfolioCurrency, source, description, fees);
    }

    public static final TradeTransactionDetails sell(
        AssetHoldingId assetHoldingId, 
        AssetIdentifier assetIdentifier, 
        BigDecimal quantity,
        MonetaryAmount pricePerUnit,
        MonetaryAmount realizedGainLoss, // Sell specific
        MonetaryAmount acbPerUnitAtSale, // Sell specific
        Currency portfolioCurrency,
        
        TransactionSource source, 
        String description, 
        List<Fee> fees
    ) {
        realizedGainLoss = Objects.requireNonNull(realizedGainLoss, "Realized gain/loss cannot be null.");
        acbPerUnitAtSale = Objects.requireNonNull(acbPerUnitAtSale, "Adjust cost basis cannot be null.");
        return new TradeTransactionDetails(assetHoldingId, assetIdentifier, quantity, pricePerUnit, realizedGainLoss, acbPerUnitAtSale, portfolioCurrency, source, description, fees);
    }


    @Override
    // for active trades only, we pass type from transaction
    public Money calculateNetImpact(TransactionType type) {
        TradeType tradeType = mapToTradeType(type);

        // Gross value in portfolio currency
        Money gross = getGrossValueInPortfolioCurrency();
        // Total fees in portfolio currency
        Money totalFees = super.getTotalFeesInCurrency(this.portfolioCurrency);

        return switch (tradeType) {
            case BUY, COVER_SHORT -> gross.add(totalFees);      // Outflow
            case SELL, SHORT_SELL, OPTIONS_EXERCISED, OPTIONS_ASSIGNED -> gross.subtract(totalFees); // Inflow
            case OPTIONS_EXPIRED -> Money.ZERO(this.portfolioCurrency);
            case CRYPTO_SWAP -> throw new UnsupportedOperationException("Handle swap logic separately");
            // Reversal trades have zero net impact themselves
            case OTHER_TRADE_TYPE_REVERSAL, TRADE_REVERSAL, BUY_REVERSAL, SELL_REVERSAL ->
                Money.ZERO(this.portfolioCurrency);
        };
    }

    public boolean isBuy(TradeType tradeType) {return tradeType == TradeType.BUY;}
    public boolean isSell(TradeType tradeType) {return tradeType == TradeType.SELL;}
    
    public MonetaryAmount getGrossValue() {return pricePerUnit.multiply(quantity);} // Gross value of the trade (without fees)
    public Money getGrossValueInPortfolioCurrency() {return getGrossValue().getPortfolioAmount();}
    
    public AssetHoldingId getAssetHoldingId() {return assetHoldingId;}
    public AssetIdentifier getAssetIdentifier() {return assetIdentifier;}
    public BigDecimal getQuantity() {return quantity;}
    public MonetaryAmount getPricePerUnit() {return pricePerUnit;}
    public MonetaryAmount getRealizedGainLoss() {return realizedGainLoss;}
    public MonetaryAmount getAcbPerUnitAtSale() {return acbPerUnitAtSale;}
    public Currency getPortfolioCurrency() {return portfolioCurrency;}
    
    private TradeType mapToTradeType(TransactionType type) {
        if(!(type instanceof TradeType)) {
            throw new IllegalArgumentException("TransactionType must be a TradeType");
        }
        return (TradeType) type;
    }
}
