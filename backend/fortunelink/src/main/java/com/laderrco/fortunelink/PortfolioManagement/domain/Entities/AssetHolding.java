package com.laderrco.fortunelink.portfoliomanagement.domain.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

public class AssetHolding {
    private final UUID assetHoldingId;
    private final UUID porfolioId;
    private final AssetIdentifier assetIdentifier;
    private BigDecimal quantity;
    private ZonedDateTime acqusisitionDate;
    private Money costBasis; // per share cost basis

    private Instant createdAt;
    private Instant updatedAt;

    public AssetHolding(final UUID portfolioId, final UUID assetHoldingId, final AssetIdentifier assetIdentifier,
            BigDecimal quantity, ZonedDateTime acqusisitionDate, Money totalSpentCost) {
        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
        Objects.requireNonNull(assetHoldingId, "Asset Holding ID cannot be null.");
        Objects.requireNonNull(assetIdentifier, "Asset Identifier cannot be null.");
        Objects.requireNonNull(quantity, "Asset Quantity cannot be null.");
        Objects.requireNonNull(acqusisitionDate, "Acqusition Date cannot be null.");
        Objects.requireNonNull(totalSpentCost, "Spent cost cannot be null.");

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity of asset bought cannot be less than or equal to zero.");
        }

        this.porfolioId = portfolioId;
        this.assetHoldingId = assetHoldingId;
        this.assetIdentifier = assetIdentifier;
        this.quantity = quantity;
        this.acqusisitionDate = acqusisitionDate;
        this.costBasis = new Money(totalSpentCost.amount().divide(quantity, RoundingMode.HALF_UP),
                totalSpentCost.currency());
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

        Objects.requireNonNull(additionalQuantity, "Quantity of asset you want to buy must be greater than zero.");
        Objects.requireNonNull(additionalCostTotal, "Price per unit must be greater than zero.");

        if (additionalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity of asset bought cannot be less than or equal to zero.");

        } else if (additionalCostTotal.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cost total of asset cannot be less than or equal to zero.");

        }

        if (!this.costBasis.currency().equals(additionalCostTotal.currency())) {
            throw new IllegalArgumentException(
                    "Cost total currency must be the same of the AssetHolding currency.");
        }

        // formula for costbasis: total spent on asset / total quantity of 'shares'
        // cost basis - the average price you paid for a share
        BigDecimal currentTotalCost = this.costBasis.amount().multiply(this.quantity);
        BigDecimal combinedCost = currentTotalCost.add(additionalCostTotal.amount());
        BigDecimal newTotalQuantity = this.quantity.add(additionalQuantity);

        this.quantity = newTotalQuantity;
        this.costBasis = new Money(combinedCost.divide(newTotalQuantity), this.costBasis.currency()); // Store total

        this.updatedAt = Instant.now();

    }

    public void recordSaleOfAssetHolding(BigDecimal quantityToSell, Money totalProceeds) {
        Objects.requireNonNull(quantityToSell, "Quantity value cannot be null.");
        Objects.requireNonNull(totalProceeds, "Total Proceeds cannot be null");

        if (quantityToSell.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity to sell cannot be less than or equal to zero.");

        } else if (totalProceeds.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total Proceeds cannot be less than or equal to zero.");

        }

        if (!this.costBasis.currency().equals(totalProceeds.currency())) {
            throw new IllegalArgumentException(
                    "Currency of Total Proceeds must be the same of the AssetHolding currency.");
        }

        if (quantityToSell.compareTo(this.quantity) > 0) {
            throw new IllegalArgumentException("Amount enter to sell is larger than what you have for this asset.");
        }

        // when you sell your asset, your cost basis per share doesn't change
        this.quantity = this.quantity.subtract(quantityToSell);

        this.updatedAt = Instant.now();
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
