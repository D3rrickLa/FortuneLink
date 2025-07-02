package com.laderrco.fortunelink.portfoliomanagement.domain.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

public class AssetHolding {
    private final UUID assetHoldingId;
    private final UUID porfolioId;
    private final AssetIdentifier assetIdentifier;
    private BigDecimal quantity;
    private Instant acqusisitionDate;
    private Money costBasis; // per share cost basis
    private Money totalCostBasisInPortfolioCurrency; // This is the total cost for all units held
    private Money averageCostPerUnitInPortfolioCurrency; // Derived

    private Instant createdAt;
    private Instant updatedAt;

    public AssetHolding(final UUID portfolioId, final UUID assetHoldingId, final AssetIdentifier assetIdentifier,
            BigDecimal quantity, Instant acqusisitionDate, Money totalSpentCost) {
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
        BigDecimal newquantity = this.quantity.add(additionalQuantity);

        this.quantity = newquantity;
        this.costBasis = new Money(combinedCost.divide(newquantity, RoundingMode.HALF_EVEN), this.costBasis.currency()); // Store total

        this.updatedAt = Instant.now();

    }

    public Money recordSaleOfAssetHolding(BigDecimal quantityToSell, Money totalProceeds) {
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
        Money costBasisOfSoldQuantity = this.costBasis.multiply(quantityToSell);
        costBasisOfSoldQuantity = costBasisOfSoldQuantity.setScale(this.costBasis.currency().getDefaultScale(), RoundingMode.HALF_EVEN);
        // when you sell your asset, your cost basis per share doesn't change
        this.quantity = this.quantity.subtract(quantityToSell);

        this.updatedAt = Instant.now();

        return totalProceeds.subtract(costBasisOfSoldQuantity);
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

    public Instant getAcqusisitionDate() {
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

    public void reverseSale(BigDecimal quantityToRevert, Money costBasisOfRevertedQuantity) {
        Objects.requireNonNull(quantityToRevert, "Quantity to revert cannot be null.");
        Objects.requireNonNull(costBasisOfRevertedQuantity, "Cost basis of reverted quantity cannot be null.");

        if (quantityToRevert.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity to revert must be greater than zero.");
        }
        if (costBasisOfRevertedQuantity.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cost basis of reverted quantity cannot be negative.");
        }
        // Ensure currency matches portfolio currency
        if (!this.totalCostBasisInPortfolioCurrency.currency().equals(costBasisOfRevertedQuantity.currency())) {
            throw new IllegalArgumentException("Currency of cost basis to revert must match portfolio currency.");
        }

        // 1. Add the quantity back
        this.quantity = this.quantity.add(quantityToRevert).setScale(6, RoundingMode.HALF_UP);

        // 2. Add the original cost basis of those shares back
        this.totalCostBasisInPortfolioCurrency = this.totalCostBasisInPortfolioCurrency.add(costBasisOfRevertedQuantity);

        // 3. Recalculate average cost per unit
        if (this.quantity.compareTo(BigDecimal.ZERO) > 0) {
            this.averageCostPerUnitInPortfolioCurrency = this.totalCostBasisInPortfolioCurrency.divide(this.quantity);
        } else {
            // If quantity becomes zero (e.g., if you had 100, sold 100, then reversed that sale, and now have 100 back)
            // Or if after a complex series of partial sales and reversals, the total quantity for the holding goes to 0
            this.averageCostPerUnitInPortfolioCurrency = Money.ZERO(this.totalCostBasisInPortfolioCurrency.currency()); // Or remove holding
        }
    }

    public void reverseAddition(BigDecimal quantityToRemove, Money grossAssetCostInPortfolioCurrency) {
  Objects.requireNonNull(quantityToRemove, "Quantity to remove cannot be null.");
        Objects.requireNonNull(grossAssetCostInPortfolioCurrency, "Gross asset cost in portfolio currency cannot be null.");

        if (quantityToRemove.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity to remove must be greater than zero.");
        }

        // Basic check: Ensure we don't try to remove more than we have
        // This is crucial for avoiding negative quantities, though for average cost,
        // it might allow removing cost even if quantity is slightly off due to rounding.
        if (this.quantity.compareTo(quantityToRemove) < 0) {
            throw new IllegalArgumentException("Cannot reverse addition: Not enough quantity (" + quantityToRemove + ") to remove from holding (" + this.quantity + ").");
        }

        // Ensure currency consistency for cost basis
        if (!this.totalCostBasisInPortfolioCurrency.currency().equals(grossAssetCostInPortfolioCurrency.currency())) {
            throw new IllegalArgumentException("Currency of gross asset cost to remove must match portfolio currency for reversal.");
        }

        // 1. Subtract the quantity
        this.quantity = this.quantity.subtract(quantityToRemove).setScale(6, RoundingMode.HALF_UP);

        // 2. Subtract the cost basis
        this.totalCostBasisInPortfolioCurrency = this.totalCostBasisInPortfolioCurrency.subtract(grossAssetCostInPortfolioCurrency);

        // 3. Recalculate average cost per unit
        if (this.quantity.compareTo(BigDecimal.ZERO) > 0) {
            // If there are still shares left, recalculate average cost
            this.averageCostPerUnitInPortfolioCurrency = this.totalCostBasisInPortfolioCurrency.divide(this.quantity);
        } else {
            // If quantity becomes zero, reset total cost basis and average cost to zero
            this.totalCostBasisInPortfolioCurrency = Money.ZERO(this.totalCostBasisInPortfolioCurrency.currency());
            this.averageCostPerUnitInPortfolioCurrency = Money.ZERO(this.averageCostPerUnitInPortfolioCurrency.currency());
            // Note: If totalQuantity hits zero, the Portfolio might decide to remove this AssetHolding from its list.
        }
    }

}
