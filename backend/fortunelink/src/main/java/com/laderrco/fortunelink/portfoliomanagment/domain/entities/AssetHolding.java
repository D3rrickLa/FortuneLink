package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.MarketPrice;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class AssetHolding {
    private final UUID assetId;
    private final UUID portfolioId;
    private final AssetIdentifier assetIdentifier;
    private BigDecimal totalQuantity;
    private Money totalAdjustedCostBasis; // in asset's native currency, this is a large number btw
    private final Instant createdAt;
    private Instant updatedAt;

    public AssetHolding(
        UUID assetId, 
        UUID portfolioId, 
        AssetIdentifier assetIdentifier, 
        BigDecimal totalQuantity,
        Money totalAdjustedCostBasis, 
        Instant createdAt
    ) {
        Objects.requireNonNull(assetId, "Asset id cannot be null.");
        Objects.requireNonNull(portfolioId, "Portfolio id cannot be null.");
        Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");
        Objects.requireNonNull(totalQuantity, "Total quantity cannot be null.");
        Objects.requireNonNull(totalAdjustedCostBasis, "Total adjusted cost basis cannot be null.");
        Objects.requireNonNull(createdAt, "Created at cannot be null.");
        
        if (totalQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity of asset cannot be less than zero.");
        }

        if (totalAdjustedCostBasis.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total adjusted cost basis must be a positive number.");
        }

        this.assetId = assetId;
        this.portfolioId = portfolioId;
        this.assetIdentifier = assetIdentifier;
        this.totalQuantity = totalQuantity;
        this.totalAdjustedCostBasis = totalAdjustedCostBasis;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public Money getAverageACBPerUnit() {
        // totalAdjustedCostBasis / totalQuantity
        if (totalQuantity.equals(BigDecimal.ZERO)) {
            return Money.ZERO(this.totalAdjustedCostBasis.currency());
        }

        return totalAdjustedCostBasis.divide(totalQuantity);
    }

    /**
     * 
     * @param soldQuantity
     * @param salePrice // total dollar value of what you sold (i.e. 1500), of course after fees
     * @return Money object of what you can expect in terms of returns
     */
    public Money calculateCapitalGain(BigDecimal soldQuantity, Money salePrice) {
        // preview calcualtion
        Objects.requireNonNull(soldQuantity, "Sold quantity cannot be null");
        Objects.requireNonNull(salePrice, "Sale price cannot be null");
        
        if (soldQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Sold quantity must be positive");
        }
        
        if (soldQuantity.compareTo(this.totalQuantity) > 0) {
            throw new IllegalArgumentException("Cannot sell more than owned");
        }
        
        // Calculate cost basis for the sold quantity
        Money averageCostPerUnit = getAverageACBPerUnit();
        Money costBasisForSoldQuantity = averageCostPerUnit.multiply(soldQuantity);
        
        // Capital gain = Sale proceeds - Cost basis
        return salePrice.subtract(costBasisForSoldQuantity);
    }

    /**
     * 
     * @param quantity
     * @param costBasis - total cost basis for the quantity being added, NOT marker price per unit. (i.e. N * ppu = costBasis)
     * 
     * The reason for costBasis instead of PPU is that ACB includes fees. think about buying foreign assets,
     * cost basis reflects the actual amount in portfolio currency after conversion. so in summary, "how muhc total money to added to the
     * cost basis"
     */
    public void addToPosition(BigDecimal quantity, Money costBasis) {
        // called by Portfolio
        Objects.requireNonNull(quantity, "Quantity cannot be null.");
        Objects.requireNonNull(costBasis, "Cost basis cannot be null.");
        
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }

        // Validate currency matches existing holdings
        if (!costBasis.currency().equals(this.totalAdjustedCostBasis.currency())) {
            throw new IllegalArgumentException("Cost basis currency must match existing holdings currency.");
        }

        this.totalQuantity = this.totalQuantity.add(quantity);
        this.totalAdjustedCostBasis = this.totalAdjustedCostBasis.add(costBasis);
        this.updatedAt = Instant.now();

    }

    public void removeFromPosition(BigDecimal quantity) {
        // called by Portfolio
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cannot sell aseet with a negative quantity.");
        }

        if (quantity.compareTo(this.totalQuantity) > 0) {
            throw new IllegalArgumentException("Cannot sell more units than you have.");
        }

        Money averageCostPerUnit = getAverageACBPerUnit();
        Money costBasisOfSoldShares = averageCostPerUnit.multiply(quantity);

        this.totalQuantity = this.totalQuantity.subtract(quantity);
        this.totalAdjustedCostBasis = this.totalAdjustedCostBasis.subtract(costBasisOfSoldShares);
        this.updatedAt = Instant.now();
        
        // When fully sold, ACB should be zero too
        if (this.totalQuantity.equals(BigDecimal.ZERO)) {
            this.totalAdjustedCostBasis = Money.ZERO(this.totalAdjustedCostBasis.currency());
        }
    }

    public Money getCurrentValue(MarketPrice currentPrice) {
        Objects.requireNonNull(currentPrice, "Current price cannot be null.");

        if (!currentPrice.price().currency().equals(this.totalAdjustedCostBasis.currency())) {
            throw new IllegalArgumentException("Market price curreny and liability currency must match.");
        }

        Money unrealizedAmount = new Money(currentPrice.price().amount().multiply(this.totalQuantity), currentPrice.price().currency());
        return unrealizedAmount;
    }

    public UUID getAssetId() {
        return assetId;
    }

    public UUID getPortfolioId() {
        return portfolioId;
    }

    public AssetIdentifier getAssetIdentifier() {
        return assetIdentifier;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public Money getTotalAdjustedCostBasis() {
        return totalAdjustedCostBasis;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    
    
}
