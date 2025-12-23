package com.laderrco.fortunelink.portfolio_management.domain.models.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import lombok.Builder;
import lombok.ToString;

@ToString
@Builder // TODO: remove this and implement your own
public class Asset {
    private final AssetId assetId;
    private final AssetIdentifier assetIdentifier;
    private final ValidatedCurrency currency; // the currency the asset is listed in
    private BigDecimal quantity;
    private Money costBasis; // total cost of all purchaes (feed included) E.i (pricePerUnit * quantity) + fees

    private final Instant acquiredOn;
    private Instant lastSystemInteraction;
    private int version;

    private Asset(AssetId assetId, AssetIdentifier assetIdentifier, ValidatedCurrency currency,
            BigDecimal quantity, Money costBasis, Instant acquiredOn, Instant lastSystemInteraction, int version) {
        
        Objects.requireNonNull(assetId);
        Objects.requireNonNull(assetIdentifier);
        Objects.requireNonNull(currency);
        Objects.requireNonNull(quantity);
        Objects.requireNonNull(costBasis);
        Objects.requireNonNull(acquiredOn);
        Objects.requireNonNull(lastSystemInteraction);

        this.assetId = assetId;
        this.assetIdentifier = assetIdentifier;
        this.currency = currency;
        this.quantity = quantity;
        this.costBasis = costBasis;
        this.acquiredOn = acquiredOn;
        this.lastSystemInteraction = lastSystemInteraction;
        this.version = version;
    }

    // package-private, only Accoutn can create
    Asset(AssetId assetId, AssetIdentifier assetIdentifier, BigDecimal quantity, Money costBasis, Instant acquiredOn) {
        this(
            assetId,
            assetIdentifier,
            costBasis.currency(),
            quantity,
            costBasis,
            acquiredOn,
            acquiredOn,
            1
        );
    }

    // MUTATION METHODS (package-private - only Portfolio can call) //
    void addQuantity(BigDecimal additionalQuantity) {
        Objects.requireNonNull(additionalQuantity, "Quantity cannot be null");
        
        this.quantity = this.quantity.add(additionalQuantity);
        updateMetadata();
    }

    void reduceQuantity(BigDecimal quantityToRemove) {
        Objects.requireNonNull(quantityToRemove, "Quantity cannot be null");
        
        BigDecimal newQuantity = this.quantity.subtract(quantityToRemove);

        if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                "Cannot remove " + quantityToRemove + " from position of " + this.quantity
            );
        }
        
        this.quantity = newQuantity;
        updateMetadata();
    }

    void updateCostBasis(Money newCostBasis) {
        Objects.requireNonNull(newCostBasis, "Cost basis cannot be null");
        
        if (!newCostBasis.currency().equals(this.currency)) {
            throw new IllegalArgumentException(
                "Cost basis currency must match asset base currency"
            );
        }

        if (newCostBasis.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cost basis cannot be negative");
        }
        
        this.costBasis = newCostBasis;
        updateMetadata();
    }

    // QUERY METHODS (public - anyone can call) //
    public AssetId getAssetId() {
        return assetId;
    }

    public AssetIdentifier getAssetIdentifier() {
        return assetIdentifier;
    }

    public ValidatedCurrency getCurrency() {
        return currency;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Money getCostBasis() {
        return costBasis;
    }

    public Instant getAcquiredOn() {
        return acquiredOn;
    }

    public Instant getLastSystemInteraction() {
        return lastSystemInteraction;
    }

    public int getVersion() {
        return version;
    }  

    // we should really rename this
    // call it getAverageCostPerShare
    public Money getCostPerUnit() {
        if (quantity.compareTo(BigDecimal.ZERO) == 0) {
            return Money.ZERO(costBasis.currency());
        }

        return costBasis.divide(quantity);
    }

    public Money calculateCurrentValue(Money currentPrice) {
        Objects.requireNonNull(currentPrice, "Price cannot be null"); 
        
        if (!currentPrice.currency().equals(this.currency)) {
            throw new IllegalArgumentException("Price currency " + currentPrice.currency() + " does not match asset currency " + this.currency);
        }
  
        return currentPrice.multiply(quantity);
    }

    public Money calculateUnrealizedGainLoss(Money currentPrice) {
        Objects.requireNonNull(currentPrice, "Price cannot be null");
        
        Money currentValue = calculateCurrentValue(currentPrice);
        return currentValue.subtract(costBasis);
    }

    public boolean hasZeroQuantity() {
        return this.quantity.compareTo(BigDecimal.ZERO) == 0;
    }

    private void updateMetadata() {
        version++;
        lastSystemInteraction = Instant.now();
    }


}
