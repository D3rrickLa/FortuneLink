package com.laderrco.fortunelink.portfoliomanagement.domain.model.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Price;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Quantity;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.shared.enums.Currency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

/*
 * DDD -> read only calculation, any state changes goes to the root
 * Assets are what you own CURRENTLY
 */
public class Asset {
    private final AssetId assetId;
    private final AssetIdentifier assetIdentifier;
    private final AssetType assetType;
    private final Currency baseCurrency;

    private Quantity quantity;
    private Money costBasis;  // total cost of all purchases (fees included)

    private final LocalDateTime acquiredOn;
    private LocalDateTime lastSystemInteraction; // for calculating when you last interacted with this asset
    private int version;


    private Asset(AssetId assetId, AssetIdentifier assetIdentifier, AssetType assetType, Currency baseCurrency,
            Quantity quantity, Money costBasis, LocalDateTime acquiredOn, LocalDateTime lastSystemInteraction, int version) {
        
        Objects.requireNonNull(assetId);
        Objects.requireNonNull(assetIdentifier);
        Objects.requireNonNull(assetType);
        Objects.requireNonNull(baseCurrency);
        Objects.requireNonNull(quantity);
        Objects.requireNonNull(costBasis);
        Objects.requireNonNull(acquiredOn);
        Objects.requireNonNull(lastSystemInteraction);

        this.assetId = assetId;
        this.assetIdentifier = assetIdentifier;
        this.assetType = assetType;
        this.baseCurrency = baseCurrency;
        this.quantity = quantity;
        this.costBasis = costBasis;
        this.acquiredOn = acquiredOn;
        this.lastSystemInteraction = lastSystemInteraction;
        this.version = version;
    }

    public Asset(AssetId assetId, AssetIdentifier assetIdentifier, AssetType assetType, Quantity quantity, Money costBasis, LocalDateTime acquiredOn) {
        this(
            assetId,
            assetIdentifier,
            assetType,
            costBasis.currency(),
            quantity,
            costBasis,
            acquiredOn,
            acquiredOn,
            1
        );

    }

    public static Asset create(AssetIdentifier assetIdentifier, AssetType assetType, Quantity quantity, Money costBasis, LocalDateTime acquiredOn) {
        return new Asset(
            AssetId.randomId(),
            assetIdentifier,
            assetType,
            costBasis.currency(),
            quantity,
            costBasis,
            acquiredOn,
            acquiredOn,
            1
        );
    }

    // MUTATION METHODS (package-private - only Portfolio can call)
    void adjustQuantity(Quantity additionalQuantity) {
        Objects.requireNonNull(additionalQuantity, "Quantity cannot be null");
        this.quantity = this.quantity.add(additionalQuantity);
        updateMetadata();
    }

    void reduceQuantity(Quantity quantityToRemove) {
        Objects.requireNonNull(quantityToRemove, "Quantity cannot be null");
        
        Quantity newQuantity = this.quantity.subtract(quantityToRemove);
        
        if (newQuantity.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                "Cannot remove " + quantityToRemove + " from position of " + this.quantity
            );
        }
        
        this.quantity = newQuantity;
        updateMetadata();
    }

    void updateCostBasis(Money newCostBasis) {
        Objects.requireNonNull(newCostBasis, "Cost basis cannot be null");
        
        if (!newCostBasis.currency().equals(baseCurrency)) {
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

    // QUERY METHODS (public - anyone can call)
    public Money getAverageCostBasis() {
        if (quantity.amount().compareTo(BigDecimal.ZERO) == 0) {
            return Money.ZERO(costBasis.currency());
        }
        return costBasis.divide(quantity.amount());
    }

    public Money calculateCurrentValue(Price currentPrice) {
        Objects.requireNonNull(currentPrice, "Price cannot be null"); 
        
        if (!currentPrice.getCurrency().equals(baseCurrency)) {
            throw new IllegalArgumentException("Price currency " + currentPrice.getCurrency() + " does not match asset currency " + baseCurrency);
        }
  
        return currentPrice.calculateValue(quantity);
    }

    public Money calculateUnrealizedGainLoss(Price currentPrice) {
        Objects.requireNonNull(currentPrice, "Price cannot be null");
        
        Money currentValue = calculateCurrentValue(currentPrice);
        return currentValue.subtract(costBasis);
    }

    public boolean hasZeroQuantity() {
        return this.quantity.isZero();
    }

    public AssetId getAssetId() {
        return assetId;
    }

    public AssetIdentifier getAssetIdentifier() {
        return assetIdentifier;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public Currency getBaseCurrency() {
        return baseCurrency;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public Money getCostBasis() {
        return costBasis;
    }

    public LocalDateTime getacquiredOn() {
        return acquiredOn;
    }

    public LocalDateTime getLastSystemInteraction() {
        return lastSystemInteraction;
    }

    public int getVersion() {
        return version;
    }

    private void updateMetadata() {
        version++;
        lastSystemInteraction = LocalDateTime.now();
    }

}
