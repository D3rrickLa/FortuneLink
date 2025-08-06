package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
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
    
    public AssetHolding(
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
        
}
