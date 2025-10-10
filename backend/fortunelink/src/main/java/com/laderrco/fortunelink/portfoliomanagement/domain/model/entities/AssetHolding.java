package com.laderrco.fortunelink.portfoliomanagement.domain.model.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.events.DomainEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.HoldingIncreasedEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidHoldingCostBasisException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidHoldingOperationException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidHoldingQuantityException;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Price;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Quantity;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.shared.domain.valueobjects.Money;
import com.laderrco.fortunelink.shared.domain.valueobjects.Percentage;

import lombok.Builder;

public class AssetHolding {
    private final AssetHoldingId assetHoldingId;
    private final PortfolioId portfolioId;
    private final AssetIdentifier assetIdentifier;
    private final AssetType assetType;
    private final Currency baseCurrency;

    // State //
    private Quantity totalQuantity;
    private Price averageCostBasis; // ACB per unit
    private Money totalCostBasis;

    // Metadata //
    private final Instant createdAt;
    private Instant lastTransactionAt;
    private Instant updatedAt;
    private int version;

    // Domain Events //
    private final List<DomainEvent> domainEvents;

    @Builder
    private AssetHolding(
            AssetHoldingId assetHoldingId,
            PortfolioId portfolioId,
            AssetIdentifier assetIdentifier,
            AssetType assetType, 
            Quantity totalQuantity,
            Price averageCostBasis,
            Money totalCostBasis,
            Instant createdAt,
            Instant lastTransactionAt,
            Instant updatedAt,
            int version) 
    {
        this.assetHoldingId = Objects.requireNonNull(assetHoldingId);
        this.portfolioId = Objects.requireNonNull(portfolioId);
        this.assetIdentifier = Objects.requireNonNull(assetIdentifier);
        this.assetType = Objects.requireNonNull(assetType);
        this.totalQuantity = Objects.requireNonNull(totalQuantity);
        this.averageCostBasis = Objects.requireNonNull(averageCostBasis);
        this.totalCostBasis = Objects.requireNonNull(totalCostBasis);
        this.baseCurrency = this.totalCostBasis.currency();
        this.createdAt = Objects.requireNonNull(createdAt);
        this.lastTransactionAt = Objects.requireNonNull(lastTransactionAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.version = version;
        
        validate();
        this.domainEvents = new ArrayList<>();
    }

    public static AssetHolding createInitialHolding(
            PortfolioId portfolioId,
            AssetHoldingId assetHoldingId,
            AssetIdentifier assetIdentifier,
            AssetType assetType,
            Quantity quantity,
            Price pricePerUnit,
            Instant transactionDate) {

        Money totalCost = pricePerUnit.pricePerUnit().multiply(quantity.amount());
        return AssetHolding.builder()
            .portfolioId(portfolioId)
            .assetHoldingId(assetHoldingId)
            .assetIdentifier(assetIdentifier)
            .assetType(assetType)
            .totalQuantity(quantity)
            .averageCostBasis(pricePerUnit)
            .totalCostBasis(totalCost)
            .createdAt(transactionDate)
            .updatedAt(transactionDate)
            .lastTransactionAt(transactionDate)
            .build();  
    }


    public static AssetHolding reconstruct(
        PortfolioId portfolioId,
        AssetHoldingId assetHoldingId,
        AssetIdentifier assetIdentifier,
        AssetType assetType,
        BigDecimal totalQuantity,
        Money averageCostBasis,
        Money totalCostBasis,
        Instant lastTransactionAt,
        int version,
        Instant createdAt,
        Instant updatedAt
    ) {
        return null;
    }

    // For ACB rules in CAD, it's the purchase price PLUS any expense to acquire it
    public void increasePosition(Quantity quantity, Price pricePerUnit, Instant transactionDate) {
        Objects.requireNonNull(quantity);
        Objects.requireNonNull(pricePerUnit);
        Objects.requireNonNull(transactionDate);


        validateQuantity(quantity);
        validateCurrency(pricePerUnit.pricePerUnit());
        validatePricePerUnit(pricePerUnit);
        
        Money purchaseCost = pricePerUnit.pricePerUnit().multiply(quantity.amount());
        Money newTotalCostBasis = this.totalCostBasis.add(purchaseCost);
        Quantity newTotalQuantity = this.totalQuantity.add(quantity);
        Money newAverageCostBasis = newTotalCostBasis.divide(newTotalQuantity.amount());

        this.totalQuantity = this.totalQuantity.add(quantity);
        this.averageCostBasis = new Price(newAverageCostBasis);
        this.totalCostBasis = newTotalCostBasis;
        this.lastTransactionAt = transactionDate;

        addDomainEvent(new HoldingIncreasedEvent(
            this.portfolioId,
            this.assetHoldingId,
            quantity,
            pricePerUnit,
            transactionDate
        ));
        updateMetadata();

    }

    public void decreasePosition(Quantity quantity, Price pricePerUnit, Instant transactionDate) {

    }

    public void recordDividendReceived(Money dividendAmount, Instant recievedAt) {

    }

    public void processDividendReinvestment( 
        Money dividendAmount,
        Quantity sharesReceived,
        Price pricePerShare,
        Instant reinvestmentDate
    ) {

    }

    public void processReturnOfCapital(Money rocAmount, Instant effectiveDate) {

    }

    public void processStockSplit(BigDecimal splitRatio, Instant effectiveDate) {

    }

    // Query Methods // 
    public Money getCurrentMarketValue(Money currentPrice) {
        return null;
    }

    public Money getUnrealizedGainLoss(Money currentPrice) {
        return null;
    }

    public Percentage getUnrealizedGainLossPercentage(Money currentPrice) {
        return null;
    }

    public Money calculateCapitalGainLoss(Quantity quantitySold, Price salePrice) {
        return null;
    }

    public Money getACBPerShare() {
        return null;
    }

    public Money getTotalACB() {
        return null;
    }

    public Money getCostBasisForQuantity(Quantity quantity) {
        return null;
    }

    public boolean isEmpty() {
        return this.totalQuantity.amount().compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean hasPosition() {
        return false;
    }

    public boolean canSell(Quantity requestedQuantity) {
        return false;
    }

    public boolean shouldBeRemoved() {
        return false;
    }

    // Domain Events //
    public void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    public List<DomainEvent> getUncommittedEvents() {
        return this.domainEvents;
    }

    public void markEventsAsCommitted() {
        
    }

    public boolean hasUncommittedEvents() {
        return this.domainEvents.size() > 0;
    }
    
    // Getters //
    public AssetHoldingId getAssetHoldingId() {
        return assetHoldingId;
    }

    public PortfolioId getPortfolioId() {
        return portfolioId;
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

    public Quantity getTotalQuantity() {
        return totalQuantity;
    }

    public Price getAverageCostBasis() {
        return averageCostBasis;
    }

    public Money getTotalCostBasis() {
        return totalCostBasis;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastTransactionAt() {
        return lastTransactionAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public int getVersion() {
        return version;
    }

    public List<DomainEvent> getDomainEvents() {
        return domainEvents;
    }    

    // Private Helpers // 
    private void validate() {
        // on constructor creation
        if (totalQuantity.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidHoldingQuantityException("Quantity cannot be less than 0");
        }
        if (averageCostBasis.pricePerUnit().isNegative()) {
            throw new InvalidHoldingCostBasisException("Average cost basis cannot be negative");
        }
        if (totalCostBasis.isNegative()) {
            throw new InvalidHoldingCostBasisException("Total cost basis cannot be negative");
        }
        if (!averageCostBasis.pricePerUnit().currency().equals(totalCostBasis.currency())) {
            throw new InvalidHoldingOperationException("Currency mismatch in cost basis");
        }
    }

    private void validateCurrency(Money money) {
        if (!money.currency().equals(this.baseCurrency)) {
            throw new InvalidHoldingOperationException(String.format("Currency mismatch. Expected %s but got %s", this.baseCurrency, money.currency()));
        }
    }    
    
    private void validateQuantity(Quantity quantity) {
        if (quantity.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidHoldingQuantityException("quantity cannot be less than 0");
        }
    }

    private void validatePricePerUnit(Price price) {
        if (price.pricePerUnit().amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidHoldingOperationException("price per unit cannot be less than 0");
        }
    }

    private void requirePosition() {
        if (isEmpty()) {
            throw new InvalidHoldingOperationException("Operation requries an active position");
        }
    }

    private void updateMetadata() {
        this.updatedAt = Instant.now();
        this.version++;
    }

}
