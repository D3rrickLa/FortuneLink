package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;

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
    private Money totalAdjustedCostBasisPortfolioCurrency;
    private Money totalAdjustedCostBasisNativeCurrency;
    private final Instant createdAt;
    private Instant updatedAt;
    
    private AssetHolding(
        AssetHoldingId assetHoldingId, 
        PortfolioId portfolioId, 
        AssetIdentifier assetIdentifier,
        BigDecimal quantity, 
        Money totalAdjustedCostBasisPortfolioCurrency,
        Money totalAdjustedCostBasisNativeCurrency, 
        Instant createdAt, 
        Instant updatedAt
    ) {
        this.assetHoldingId = assetHoldingId;
        this.portfolioId = portfolioId;
        this.assetIdentifier = assetIdentifier;
        this.quantity = quantity;
        this.totalAdjustedCostBasisPortfolioCurrency = totalAdjustedCostBasisPortfolioCurrency;
        this.totalAdjustedCostBasisNativeCurrency = totalAdjustedCostBasisNativeCurrency;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public AssetHolding(
        AssetHoldingId assetHoldingId, 
        PortfolioId portfolioId, 
        AssetIdentifier assetIdentifier,
        BigDecimal quantity, 
        Money totalAdjustedCostBasisPortfolioCurrency,
        Money totalAdjustedCostBasisNativeCurrency, 
        Instant createdAt 
    ) {
        this(
            assetHoldingId, 
            portfolioId, 
            assetIdentifier,
            quantity, 
            totalAdjustedCostBasisPortfolioCurrency,
            totalAdjustedCostBasisNativeCurrency, 
            createdAt,
            createdAt
        );

    }


    public Money calculateCapitalGain(BigDecimal soldQuantity, Money salePrice) {
        return null;
    }

    public void addToPosition(BigDecimal quantity, Money costBasis) {
        
    }

    public void removeFromPosition(BigDecimal quantity) {

    }

    public Money getCurrentValue(MarketPrice currentPrice) {
        return null;
    }

    public Money getAverageACBPerUnit() {
        return null;
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

    public Money getTotalAdjustedCostBasisPortfolioCurrency() {
        return totalAdjustedCostBasisPortfolioCurrency;
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
