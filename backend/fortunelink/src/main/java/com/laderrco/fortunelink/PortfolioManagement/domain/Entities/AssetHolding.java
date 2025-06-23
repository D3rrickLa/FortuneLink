package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;

public class AssetHolding {
    private final UUID assetHoldingId;
    private final UUID porfolioId;
    private final AssetIdentifier assetIdentifier;
    private BigDecimal quantity;
    private ZonedDateTime acqusisitionDate;
    private Money costBasis;

    private Instant createdAt;
    private Instant updatedAt;

    public AssetHolding(final UUID portfolioId, final UUID assetHoldingId, final AssetIdentifier assetIdentifier,
            BigDecimal quantity, ZonedDateTime acqusisitionDate, Money initailCostBasis) {
        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
        Objects.requireNonNull(assetHoldingId, "Asset Holding ID cannot be null.");
        Objects.requireNonNull(assetIdentifier, "Asset Identifier cannot be null.");
        Objects.requireNonNull(quantity, "Asset Quantity cannot be null.");
        Objects.requireNonNull(acqusisitionDate, "Acqusition Date cannot be null.");
        Objects.requireNonNull(initailCostBasis, "Cost Basis cannot be null.");

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity of asset bought cannot be less than or equal to zero.");
        }

        this.porfolioId = portfolioId;
        this.assetHoldingId = assetHoldingId;
        this.assetIdentifier = assetIdentifier;
        this.quantity = quantity;
        this.acqusisitionDate = acqusisitionDate;
        this.costBasis = initailCostBasis;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // What can AssetHolding do? VERBS
    /*
     * NOUN - AssetHolding is a representation of an asset that you own, mainly
     * holding metadata
     *
     * VERBS
     * - create new holdings (constructor)
     * - update the holding based on the IDs
     * - remove a holding (sale)
     */

    public void recordAdditionPurchaseOfAssetHolding(BigDecimal additionalQuantity, Money additionalCostTotal) {
        // we technically don't need to pass the UUID of portfolio and assetIdentifier,
        // we have the constructor already assigning those values
        // acqusition date -> the first instance of when you bought said asset, not needed here
        /*
         * we are passing in the param that addition amount of assets we want (i.e. 2 more APPLE stocks)
         * and the total COST of that purchase (this includes things like fees and other expenses related to a purchase)
         */

        Objects.requireNonNull(additionalQuantity, "Quantity of asset you want to buy must be greater than zero.");
        Objects.requireNonNull(additionalCostTotal, "Price per unit must be greater than zero.");

        if (additionalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity of asset bought cannot be less than or equal to zero.");
            
        }
        else if (additionalCostTotal.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cost total of asset cannot be less than or equal to zero.");

        }

        if (!this.costBasis.currency().equals(additionalCostTotal.currency())) {
            throw new IllegalArgumentException(
                    "Cost total currency must be the same of the AssetHolding currency.");
        }

        BigDecimal currentTotalCost = this.costBasis.amount();
        BigDecimal newTotalQuantity = this.quantity.add(additionalQuantity);
        BigDecimal combinedCost = currentTotalCost.add(additionalCostTotal.amount());

        this.quantity = newTotalQuantity;
        this.costBasis = new Money(combinedCost, this.costBasis.currency());

    }

    public void recordSaleOfAssetHolding() {

    }

    public UUID getAssetHoldingId() {
        return assetHoldingId;
    }

    public UUID getPorfolioId() {
        return porfolioId;
    }

    public AssetIdentifier getAssetIdentifier() {
        return assetIdentifier;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public ZonedDateTime getAcqusisitionDate() {
        return acqusisitionDate;
    }

    public Money getCostBasis() {
        return costBasis;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

}
