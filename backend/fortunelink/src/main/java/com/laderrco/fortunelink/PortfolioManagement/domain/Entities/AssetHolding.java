package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;

public class AssetHolding {
    private UUID assetHoldingId;
    private UUID portfolioId; // root aggeragate
    private AssetIdentifier assetIdentifier;
    private BigDecimal quantity;
    private Money costBasis;
    private LocalDate acquisitionDate;
    // private Money currentMarketPrice; //TODO: Remove this line, market price is
    // too volatile

    private Instant createdAt;
    private Instant updatedAt;

    public AssetHolding(UUID portfolioUuid, AssetIdentifier assetIdentifier,
            BigDecimal initialQuantity, Money initialCostBasis, // This is total cost for initial purchase
            LocalDate acquisitionDate) {

        // all can't be null
        // quant must be greater than 0
        // cost base must be greater than 0

        this.assetHoldingId = UUID.randomUUID(); // Generate ID here!
        this.portfolioId = portfolioUuid;
        this.assetIdentifier = assetIdentifier;
        this.quantity = initialQuantity;
        this.costBasis = initialCostBasis;
        this.acquisitionDate = acquisitionDate; // This is the acquisition date of the first lot.
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();

    }
    // we can an asset do?
    // buy, sell, remove

    public void recordAdditionalPurchase(BigDecimal additionalQuantity, Money additionalCostTotal) {
        Objects.requireNonNull(additionalQuantity, "Additional quantity cannot be null.");
        Objects.requireNonNull(additionalCostTotal, "Additional cost total cannot be null.");

        if (additionalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Additional purchase quantity must be greater than 0");
        }

        // Ensure currency consistency
        if (!this.costBasis.currencyCode().equals(additionalCostTotal.currencyCode())) {
            throw new IllegalArgumentException("Currency mismatch for additional purchase.");
        }

        BigDecimal currentTotalCost = this.costBasis.amount(); // This is already the total cost for current quantity
        BigDecimal newTotalQuantity = this.quantity.add(additionalQuantity);
        BigDecimal combinedCost = currentTotalCost.add(additionalCostTotal.amount());

        this.quantity = newTotalQuantity;
        this.costBasis = new Money(combinedCost, this.costBasis.currencyCode()); // Update total cost basis

        this.updatedAt = Instant.now();

    }

    public void recordSale(BigDecimal quantitySold, Money salePricePerUnit) {
        Objects.requireNonNull(quantitySold, "Quantity sold cannot be null.");
        Objects.requireNonNull(salePricePerUnit, "Sale price per unit cannot be null.");

        if (quantitySold.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity sold must be positive.");
        }
        if (quantitySold.compareTo(this.quantity) > 0) {
            throw new IllegalArgumentException("Cannot sell more than available quantity.");
        }

        // Calculate the proportion of cost basis to remove
        BigDecimal costPerUnit = this.costBasis.amount().divide(this.quantity, MathContext.DECIMAL128); // Average cost per unit
        BigDecimal costOfSoldQuantity = costPerUnit.multiply(quantitySold);

        this.quantity = this.quantity.subtract(quantitySold);
        this.costBasis = new Money(this.costBasis.amount().subtract(costOfSoldQuantity),
                this.costBasis.currencyCode());

        // If quantity becomes zero, consider if the AssetHolding should be removed or
        // marked inactive later by Portfolio
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AssetHolding that = (AssetHolding) o;
        return assetHoldingId != null && assetHoldingId.equals(that.assetHoldingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetHoldingId);
    }
}
