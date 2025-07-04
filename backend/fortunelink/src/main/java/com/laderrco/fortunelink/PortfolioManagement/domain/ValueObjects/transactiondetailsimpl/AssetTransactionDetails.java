package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsimpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Set;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.interfaces.TransactionDetails;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

// asset need to deal with both sale and buying of an asset.
/*
 * Why do we have assetValueInX and costBasisOfSoldQuantInX when we have quantity and pricePerUnit?
 * 
 * pricePerUnit * quantity is the intended or quoted value while the assetValueInX is the definitive total monetary amount
 * that was exchanged for the asset, part of the transaction, as a historical fact. That is why we have the assetValueInX
 * 
 * costBasisOfSoldQuantityInPortfolioCurrency -> to capture accurate captial gains/losses while maintaining
 * our average cost basis when dealing with asset sales and their reversals
 * If we don't record this donwn somewhere, we cannot determine the gian or loss on that specific sale.
 * This represents what your originally paid for those specific shares you just sold
 * 
 * Example: we bought 10 shares of APPL for $10. total cost basis: 10 * 10 = 100. avg cost per share: 100 / 10 -> $10/share
 * we then buy another 10 shares at $12. total quantity = 10 + 10 = 20 shares. total cost basis = $100 + (10 * 12). avg cost per share: 220 / 20 -> $11/share 
 * now we want to sell 5 sharesat $15. total price we sold was: 5 * 15 = 75. (money we received.)
 * but there is more. What was the cost of those 5 specific shares you just sold?. Since our AssetHolding uses the average cost method, the cost of any share at the time of sale is its 
 * current average cost.
 * OUR AVERAGE COST FOR APPL WAS $11/SHARE
 * so for those 5 shares, it was 5 * 11/share = $55 (this is what your originally paid for these specific  shares, on average.)
 * 
 * we need this $55 to calculate our protif/loss on this sale.
 * profit = sale procees - cost basis of shares shold
 * profit = 75 -55 = 20 (captial gain)
 * 
 * if we didn't store this $55 with the sale, we would never know how to calculate the $20 profit/loss, and we wouldn't know the correct amount ($55) to subtract from the asset holding's total cost basis
 * 
 * pricePerUnit & quantity: Tell you the raw terms of the transaction.
 * assetValueInPortfolioCurrency (for a Sale): Tells you the total cash you received for the asset.
 * costBasisOfSoldQuantityInPortfolioCurrency (for a Sale): Tells you the total cash you paid for those specific shares at the time you acquired them (or their average cost).
 */
public final class AssetTransactionDetails extends TransactionDetails {
    private final AssetIdentifier assetIdentifier;
    private final BigDecimal quantity;
    private final Money pricePerUnit;

    private final TransactionType transactionType; // Only allow BUY OR SELL
    private final Money assetValueInAssetCurrency;
    private final Money assetValueInPortfolioCurrency;

    // This represents the cost basis of the shares sold, ONLY relevant for SELL transactions.
    // N shares * cost Basis NOT N shares * current PricePerShare
    private final Money costBasisOfSoldQuantityInPortfolioCurrency;

    private final Money totalForexConversionFeesInPortfolioCurrency;
    private final Money totalOtherFeesInPortfolioCurrency;

    public AssetTransactionDetails(
            AssetIdentifier assetIdentifier, BigDecimal quantity, Money pricePerUnit,
            TransactionType transactionType, Money assetValueInAssetCurrency, Money assetValueInPortfolioCurrency,
            Money costBasisOfSoldQuantityInPortfolioCurrency, Money totalForexConversionFeesInPortfolioCurrency,
            Money totalOtherFeesInPortfolioCurrency
    ){
        Objects.requireNonNull(assetIdentifier, "Asset Identifier cannot be null.");
        Objects.requireNonNull(quantity, "Quantity cannot be null.");
        Objects.requireNonNull(pricePerUnit, "Price Per Unit cannot be null.");
        Objects.requireNonNull(transactionType, "Transaction type cannot be null.");
        Objects.requireNonNull(assetValueInAssetCurrency, "Asset value in asset currency cannot be null.");
        Objects.requireNonNull(assetValueInPortfolioCurrency, "Asset value in portfolio currency cannot be null.");
        Objects.requireNonNull(totalForexConversionFeesInPortfolioCurrency, "Total FOREX conversion fees cannot be null.");
        Objects.requireNonNull(totalOtherFeesInPortfolioCurrency, "Total other fees cannot be null.");
        
        if (pricePerUnit.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price of each asset unit must be greater than zero.");
        }
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity cannot be negative or zero.");
        }
        if (assetValueInAssetCurrency.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Asset value in asset currency cannot be negative.");
        }
        if (assetValueInPortfolioCurrency.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Asset value in portfolio currency cannot be negative.");
        }
        if (totalForexConversionFeesInPortfolioCurrency.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total FOREX conversion fees cannot be negative.");
        }
        if (totalOtherFeesInPortfolioCurrency.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total other fees cannot be negative.");
        }

        // Specific validation based on transactionType
        if (!Set.of(TransactionType.BUY, TransactionType.SELL).contains(transactionType)) {
            throw new IllegalArgumentException("Invalid transaction type for AssetTransactionDetails: " + transactionType);
        }

        // Cost basis of sold quantity is ONLY relevant for ASSET_SALE
        if (transactionType == TransactionType.BUY && costBasisOfSoldQuantityInPortfolioCurrency != null) {
            throw new IllegalArgumentException("Cost basis of sold quantity must be null for BUY transaction type.");
        }
        if (transactionType == TransactionType.SELL) {
            Objects.requireNonNull(costBasisOfSoldQuantityInPortfolioCurrency, "Cost basis of sold quantity cannot be null for SELL transaction type.");
            if (costBasisOfSoldQuantityInPortfolioCurrency.amount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Cost basis of sold quantity cannot be negative for SELL.");
            }
        }

        this.assetIdentifier = assetIdentifier;
        this.quantity = quantity.setScale(AssetType.STOCK.getDefaultQuantityPrecision().getDecimalPlaces(), RoundingMode.HALF_UP);
        this.pricePerUnit = pricePerUnit;
        this.transactionType = transactionType;
        this.assetValueInAssetCurrency = assetValueInAssetCurrency;
        this.assetValueInPortfolioCurrency = assetValueInPortfolioCurrency;
        this.costBasisOfSoldQuantityInPortfolioCurrency = costBasisOfSoldQuantityInPortfolioCurrency;
        this.totalForexConversionFeesInPortfolioCurrency = totalForexConversionFeesInPortfolioCurrency;
        this.totalOtherFeesInPortfolioCurrency = totalOtherFeesInPortfolioCurrency;
    }

    public AssetIdentifier getAssetIdentifier() { return assetIdentifier; }
    public BigDecimal getQuantity() { return quantity; }
    public Money getPricePerUnit() { return pricePerUnit; }
    public TransactionType getTransactionType() { return transactionType; }
    public Money getAssetValueInAssetCurrency() { return assetValueInAssetCurrency; }
    public Money getAssetValueInPortfolioCurrency() { return assetValueInPortfolioCurrency; }
    public Money getCostBasisOfSoldQuantityInPortfolioCurrency() { return costBasisOfSoldQuantityInPortfolioCurrency; } // New getter
    public Money getTotalForexConversionFeesInPortfolioCurrency() { return totalForexConversionFeesInPortfolioCurrency; }
    public Money getTotalOtherFeesInPortfolioCurrency() { return totalOtherFeesInPortfolioCurrency; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssetTransactionDetails that = (AssetTransactionDetails) o;
        return Objects.equals(assetIdentifier, that.assetIdentifier)
            && Objects.equals(quantity, that.quantity)
            && Objects.equals(pricePerUnit, that.pricePerUnit)
            && Objects.equals(this.transactionType,that.transactionType)
            && Objects.equals(assetValueInAssetCurrency, that.assetValueInAssetCurrency)
            && Objects.equals(assetValueInPortfolioCurrency, that.assetValueInPortfolioCurrency)
            && Objects.equals(costBasisOfSoldQuantityInPortfolioCurrency, that.costBasisOfSoldQuantityInPortfolioCurrency)
            && Objects.equals(totalForexConversionFeesInPortfolioCurrency, that.totalForexConversionFeesInPortfolioCurrency)
            && Objects.equals(totalOtherFeesInPortfolioCurrency, that.totalOtherFeesInPortfolioCurrency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetIdentifier, quantity, pricePerUnit, transactionType,
                            assetValueInAssetCurrency, assetValueInPortfolioCurrency,
                            costBasisOfSoldQuantityInPortfolioCurrency,
                            totalForexConversionFeesInPortfolioCurrency, totalOtherFeesInPortfolioCurrency);
    }
}