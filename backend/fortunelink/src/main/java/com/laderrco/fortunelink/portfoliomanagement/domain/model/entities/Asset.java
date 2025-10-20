package com.laderrco.fortunelink.portfoliomanagement.domain.model.entities;

import java.math.BigDecimal;
import java.time.Instant;
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
 */
public class Asset {
    private AssetId assetId;
    private AssetIdentifier assetIdentifier;
    private AssetType assetType;
    private Currency baseCurrency;

    private Quantity quantity;
    private Money costBasis;  // sum of all costs

    private final Instant accquiredOn;
    private Instant lastTransactionAt; // for calculating when you last interacted with this asset
    private int version;


    public void recalculateCostBasis(Price pricePerUnit, Quantity quantity, Money totalFees) {
        Objects.requireNonNull(pricePerUnit);
        Objects.requireNonNull(quantity);

        if (quantity.compareTo(this.quantity) <= 0) {
            throw new IllegalArgumentException();
        }

        if (pricePerUnit.getAmount().compareTo(BigDecimal.ZERO) <= 0) { // also check if the currency are the same
            throw new IllegalArgumentException();
        }

        Money totalCost
    }

    public void increaseQuantity(Quantity quantity) {
        Objects.requireNonNull(quantity);
        if (quantity.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity provided must be greater than 0");
        }
        
    }

    public void decreaseQuantity(Quantity quantity) {
        Objects.requireNonNull(quantity);
        if (quantity.compareTo(this.quantity) > 0) {
            // what about shorting? I know that this isn't MVP, but still... we could have a separate method
            throw new IllegalArgumentException("Cannot decrease quantity of asset with more than you have");
        }
        this.quantity = this.quantity.subtract(quantity);
    }

    public Price getAverageCostBasis() {
        return new Price(costBasis.divide(quantity));
    }

    public Money calculateCurrentValue(Price pricePerUnit) {
        return null;
    }

    public Money calculateUnrealizedGainLoss(Price pricePerUnit) {
        return null;
    }


}
