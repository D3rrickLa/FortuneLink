package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.services.CurrencyConversionService;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.MarketPrice;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;

public class AssetHolding {
    private final AssetHoldingId assetHoldingId;
    private final PortfolioId portfolioId;
    private final AssetIdentifier assetIdentifier;
    private BigDecimal quantity;
    private Money totalAdjustedCostBasisNativeCurrency;
    private final Instant createdAt;
    private Instant updatedAt;
    
    private AssetHolding(
        AssetHoldingId assetHoldingId, 
        PortfolioId portfolioId, 
        AssetIdentifier assetIdentifier,
        BigDecimal quantity, 
        Money totalAdjustedCostBasisNativeCurrency, 
        Instant createdAt, 
        Instant updatedAt
    ) {
        validateParameter(assetHoldingId, "Assetholding id");
        validateParameter(portfolioId, "Portfolio id");
        validateParameter(assetIdentifier, "Asset identifier");
        validateParameter(quantity, "Quantity");
        validateParameter(totalAdjustedCostBasisNativeCurrency, "Total adjust cost basis in native asset's currency");
        validateParameter(createdAt, "Creation date");
        validateParameter(updatedAt, "Updated date");

        this.assetHoldingId = assetHoldingId;
        this.portfolioId = portfolioId;
        this.assetIdentifier = assetIdentifier;
        this.quantity = quantity;
        this.totalAdjustedCostBasisNativeCurrency = totalAdjustedCostBasisNativeCurrency;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public AssetHolding(
        AssetHoldingId assetHoldingId, 
        PortfolioId portfolioId, 
        AssetIdentifier assetIdentifier,
        BigDecimal quantity, 
        Money totalAdjustedCostBasisNativeCurrency, 
        Instant createdAt 
    ) {
        this(
            assetHoldingId, 
            portfolioId, 
            assetIdentifier,
            quantity, 
            totalAdjustedCostBasisNativeCurrency, 
            createdAt,
            createdAt
        );

    }

    private void validateParameter(Object other, String parameterName) {
        Objects.requireNonNull(other, String.format("%s cannot be null.", parameterName));
    }


    public Money calculateCapitalGain(BigDecimal soldQuantity, Money salePrice) {
        validateParameter(soldQuantity, "Sold quantity");
        validateParameter(salePrice, "Sale price");

        if (soldQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Sold quantity must be positive");
        }
        
        if (soldQuantity.compareTo(this.quantity) > 0) {
            throw new IllegalArgumentException("Sold quantity greater than your current holdings.");
        }

        if (!salePrice.currency().equals(this.totalAdjustedCostBasisNativeCurrency.currency())) {
            throw new IllegalArgumentException("Sale price currency must match native currency.");
        }

        Money costBasisForSoldQuantity = getAverageACBPerUnit().multiply(soldQuantity);
        Money totalProceeds = salePrice.multiply(soldQuantity);
        
        this.updatedAt = Instant.now();
        
        return totalProceeds.subtract(costBasisForSoldQuantity);
    }

    public void addToPosition(BigDecimal quantity, Money costBasisNative) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        if (!costBasisNative.currency().equals(this.totalAdjustedCostBasisNativeCurrency.currency())) {
            throw new IllegalArgumentException("Cost basis currency must match existing holding currency.");
        }

        this.quantity = this.quantity.add(quantity);
        this.totalAdjustedCostBasisNativeCurrency = this.totalAdjustedCostBasisNativeCurrency.add(costBasisNative);
        this.updatedAt = Instant.now();
        
    }

    public void removeFromPosition(BigDecimal quantity) {
        validateParameter(quantity, "Quantity");

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }

        if (this.quantity.compareTo(quantity) < 0) {
            throw new IllegalArgumentException("Cannot sell more units than you have.");
        }

        Money averageCostPerUnitNative = this.totalAdjustedCostBasisNativeCurrency.divide(this.quantity);
        Money costOfUnitsSoldNative = averageCostPerUnitNative.multiply(quantity);

        this.quantity = this.quantity.subtract(quantity);
        this.totalAdjustedCostBasisNativeCurrency = this.totalAdjustedCostBasisNativeCurrency.subtract(costOfUnitsSoldNative);
            
        if (this.quantity.compareTo(BigDecimal.ZERO) == 0) {
            this.totalAdjustedCostBasisNativeCurrency = Money.ZERO(this.totalAdjustedCostBasisNativeCurrency.currency());
        }
        this.updatedAt = Instant.now();
    }   

    public Money getCurrentValue(MarketPrice currentPrice) {
        validateParameter(currentPrice, "Current market price");
        if (!currentPrice.price().currency().equals(this.totalAdjustedCostBasisNativeCurrency.currency())) {
            throw new IllegalArgumentException("Market price currency must match.");
        }
        return new Money(currentPrice.price().amount().multiply(this.quantity), currentPrice.price().currency());
    }

    public Money getCostBasisInOtherCurrency(CurrencyConversionService conversionService, Currency currency, Instant asOfDate) {
        validateParameter(conversionService, "Conversion service");
        validateParameter(currency, "Currency to convert to");
        validateParameter(asOfDate, "As of date");
        return conversionService.convert(this.totalAdjustedCostBasisNativeCurrency, currency, asOfDate);
    }

    // Corrected to handle division by zero
    public Money getAverageACBPerUnit() {
        if (this.quantity.compareTo(BigDecimal.ZERO) == 0) {
            return Money.ZERO(this.totalAdjustedCostBasisNativeCurrency.currency());
        }
        return this.totalAdjustedCostBasisNativeCurrency.divide(this.quantity);
    }

    public AssetHoldingId getAssetHoldingId() {
        return assetHoldingId;
    }

    public PortfolioId getPortfolioId() {
        return portfolioId;
    }

    public AssetIdentifier getAssetIdentifier() {
        return assetIdentifier;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Money getTotalAdjustedCostBasisNativeCurrency() {
        return totalAdjustedCostBasisNativeCurrency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
            
}
