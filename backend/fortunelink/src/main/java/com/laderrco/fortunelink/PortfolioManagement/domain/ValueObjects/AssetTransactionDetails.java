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
    private final Money grossAssetCostInAssetCurrency;
    private final Money grossAssetCostInPortfolio;
    private final Money totalFOREXConversionFeesInPortfolioCurrency;
    private final Money totalOtherFeesInPortfolioCurrency;

    public AssetTransactionDetails(AssetIdentifier assetIdentifier, BigDecimal quantity,Money pricePerUnit, Money grossAssetCostInAssetCurrency, Money grossAssestCostInPortfolioCurrency, Money totalFOREXConversionFeesInPortfolioCurrency, Money totalOtherFeesInPortfolioCurrency) {
        Objects.requireNonNull(assetIdentifier, "Asset Identifier cannot be null.");
        Objects.requireNonNull(quantity, "Quantity cannot be null.");
        Objects.requireNonNull(pricePerUnit, "Price Per Unit cannot be null.");
        Objects.requireNonNull(grossAssetCostInAssetCurrency, "Gross asset cost in asset currency cannot be null.");
        Objects.requireNonNull(grossAssestCostInPortfolioCurrency, "Gross asset cost in portfolio currency cannot be null.");
        Objects.requireNonNull(totalFOREXConversionFeesInPortfolioCurrency, "Total FOREX conversion fees cannot be null.");
        Objects.requireNonNull(totalOtherFeesInPortfolioCurrency, "Total other fees cannot be null.");


        if (pricePerUnit.amount().compareTo(BigDecimal.ZERO) <= 0) { // Changed to <= 0
            throw new IllegalArgumentException("Price of each asset unit must be greater than zero.");
        }
        
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }

        if (grossAssetCostInAssetCurrency.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Gross asset cost in asset currency must be strictly positive for a purchase."); // Clarified message
        }
        if (grossAssestCostInPortfolioCurrency.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Gross asset cost in portfolio currency must be strictly positive for a purchase."); // Clarified message
        }
        if (totalFOREXConversionFeesInPortfolioCurrency.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total FOREX conversion fees cannot be negative.");
        }
        if (totalOtherFeesInPortfolioCurrency.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total other fees cannot be negative.");
        }

        // we need a scaler here, mainly in the Money class
        this.assetIdentifier = assetIdentifier;
        this.quantity = quantity.setScale(6, RoundingMode.HALF_UP); // should really define a scale to use
        this.pricePerUnit = pricePerUnit;
        this.grossAssetCostInAssetCurrency = grossAssetCostInAssetCurrency;
        this.grossAssetCostInPortfolio = grossAssestCostInPortfolioCurrency;
        this.totalFOREXConversionFeesInPortfolioCurrency = totalFOREXConversionFeesInPortfolioCurrency;
        this.totalOtherFeesInPortfolioCurrency = totalOtherFeesInPortfolioCurrency;
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

    public Money getGrossAssetCostInAssetCurrency() {
        return grossAssetCostInAssetCurrency;
    }

    public Money getGrossAssetCostInPortfolio() {
        return grossAssetCostInPortfolio;
    }

    public Money getTotalFOREXConversionFeesInPortfolioCurrency() {
        return totalFOREXConversionFeesInPortfolioCurrency;
    }

    public Money getTotalOtherFeesInPortfolioCurrency() {
        return totalOtherFeesInPortfolioCurrency;
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
