package com.laderrco.fortunelink.portfoliomanagement.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.DividendReceivedEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.DividendReinvestedEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.DomainEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.EligibleDividendReceivedEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.HoldingDecreasedEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.HoldingIncreasedEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.ReturnOfCapitalProcessedEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.StockSplitACBEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InsufficientHoldingException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidHoldingCostBasisException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidHoldingOperationException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidHoldingQuantityException;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Percentage;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.PortfolioId;

public class AssetHolding {
    private final AssetHoldingId assetHoldingId;
    private final PortfolioId portfolioId;
    private final AssetIdentifier assetIdentifier;
    private final Instant createdAt;

    private BigDecimal totalQuantity;
    private Money averageCostBasis; // your avg cost per shares across al purchases
    private Money totalCostBasis; // grand total, what you spent total

    private Instant lastTransactionAt;
    private int version;
    private Instant updatedAt;
    
    private final List<DomainEvent> domainEvents;

    public AssetHolding(Builder builder) {
        this.assetHoldingId = Objects.requireNonNull(builder.assetHoldingId, "Asset ID cannot be null");
        this.portfolioId = Objects.requireNonNull(builder.portfolioId, "Portfolio ID cannot be null");
        this.assetIdentifier = Objects.requireNonNull(builder.assetIdentifier, "Asset identifier cannot be null");
        this.totalQuantity = Objects.requireNonNull(builder.totalQuantity, "Total quantity cannot be null");
        this.averageCostBasis = Objects.requireNonNull(builder.averageCostBasis, "Average cost basis cannot be null");
        this.totalCostBasis = Objects.requireNonNull(builder.totalCostBasis, "Total cost basis cannot be null");
        this.createdAt = Objects.requireNonNull(builder.createdAt, "Created at cannot be null");
        this.updatedAt = Objects.requireNonNull(builder.updatedAt, "Updated at cannot be null");
        this.lastTransactionAt = Objects.requireNonNull(builder.lastTransactionAt, "Last transaction at cannot be null");
        
        this.version = builder.version;

        AssetHoldingValidator.validateQuantity(builder.totalQuantity);
        AssetHoldingValidator.validateCostBasis(builder.averageCostBasis, builder.totalCostBasis);

        this.domainEvents = new ArrayList<>();

    }

    /*
     * assetId
     * quantity
     * assetIdentifier
     * averageCostBasis
     * totalCostBasis
     * acquiredAt
     * updatedAt
     * 
     * addToPosition
     * removeFromPosition
     * getPosition
     * isEmpty
     * getCurrentValue
     * getUnrealizedGanLoss 
     * getACB
     * markAsInactive
     * markAsActive
     */

    public static AssetHolding createInitialHolding(
        PortfolioId portfolioId,
        AssetHoldingId assetHoldingId,
        AssetIdentifier assetIdentifier,
        BigDecimal quantity,
        Money pricePerUnit,
        Instant transactionDate
    ) {
        Money totalCost = pricePerUnit.multiply(quantity);

        return new Builder()
            .portfolioId(portfolioId)
            .assetHoldingId(assetHoldingId)
            .assetIdentifier(assetIdentifier)
            .totalQuantity(quantity)  
            .averageCostBasis(pricePerUnit)  
            .totalCostBasis(totalCost)
            .lastTransactionAt(transactionDate)
            .build();
    }

    // for reconstructing from persistence
    public static AssetHolding reconstruct(
        PortfolioId portfolioId,
        AssetHoldingId assetHoldingId,
        AssetIdentifier assetIdentifier,
        BigDecimal totalQuantity,
        Money averageCostBasis,
        Money totalCostBasis,
        Instant lastTransactionAt,
        int version,
        Instant createdAt,
        Instant updatedAt
    ) {
        return new Builder()
            .portfolioId(portfolioId)
            .assetHoldingId(assetHoldingId)
            .assetIdentifier(assetIdentifier)
            .totalQuantity(totalQuantity)
            .averageCostBasis(averageCostBasis)
            .totalCostBasis(totalCostBasis)
            .lastTransactionAt(lastTransactionAt)
            .version(version)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    // business operations //
    public void increasePosition(BigDecimal quantity, Money pricePerUnit, Instant transactionDate) {
        Objects.requireNonNull(quantity, "Quantity cannot be null");
        Objects.requireNonNull(pricePerUnit, "Price per unit cannot be null");
        Objects.requireNonNull(transactionDate, "Transaction date cannot be null");

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidHoldingOperationException("Cannot increase position with zero or negative quantity");
        }
        validateCurrency(pricePerUnit);

        Money purchaseCost = pricePerUnit.multiply(quantity);
        Money newTotalCostBasis = this.totalCostBasis.add(purchaseCost);
        BigDecimal newTotalQuantity = this.totalQuantity.add(quantity);
        Money newAverageCostBasis = newTotalCostBasis.divide(newTotalQuantity);

        this.totalQuantity = newTotalQuantity;
        this.averageCostBasis = newAverageCostBasis;
        this.totalCostBasis = newTotalCostBasis;
        this.lastTransactionAt = transactionDate;

        addDomainEvent(new HoldingIncreasedEvent(this.portfolioId, this.assetHoldingId, quantity, pricePerUnit));
        updateHolding();
    }

    public void decreasePosition(BigDecimal quantity, Money pricePerUnit, Instant transactionDate) {
        Objects.requireNonNull(quantity, "Quantity cannot be null");
        Objects.requireNonNull(pricePerUnit, "Price per unit cannot be null");
        Objects.requireNonNull(transactionDate, "Transaction date cannot be null");

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidHoldingOperationException("Cannot decrease position with zero or negative quantity");
        }

        if (quantity.compareTo(this.totalQuantity) > 0) {
            throw new InsufficientHoldingException("Cannot sell more than current holding: " + 
                "requested=" + quantity + ", available=" + this.totalQuantity);
        }

        validateCurrency(pricePerUnit);

        Money soldCostBasis = this.averageCostBasis.multiply(quantity);
        Money newTotalCostBasis = this.totalCostBasis.subtract(soldCostBasis);
        BigDecimal newTotalQuantity = this.totalQuantity.subtract(quantity);

        // Calculate realized gain/loss
        Money saleValue = pricePerUnit.multiply(quantity);
        Money realizedGainLoss = saleValue.subtract(soldCostBasis);

        this.totalQuantity = newTotalQuantity;
        this.totalCostBasis = newTotalCostBasis;
        this.lastTransactionAt = transactionDate;

        // Update average cost basis only if we still have holdings
        if (newTotalQuantity.compareTo(BigDecimal.ZERO) != 0) {
            this.averageCostBasis = newTotalCostBasis.divide(newTotalQuantity);
        } else {
            this.averageCostBasis = Money.ZERO(this.totalCostBasis.currency());
            this.totalCostBasis = Money.ZERO(this.totalCostBasis.currency());
        }

        addDomainEvent(new HoldingDecreasedEvent(this.portfolioId, this.assetHoldingId, quantity, 
            pricePerUnit, realizedGainLoss));
        updateHolding();
    }

    public void recordDividend(Money dividendAmount, Instant receivedAt) {
        Objects.requireNonNull(dividendAmount, "Dividend amount cannot be null");
        Objects.requireNonNull(receivedAt, "Received at cannot be null");

        if (this.totalQuantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new InvalidHoldingOperationException("Cannot record dividend for zero quantity holding");
        }

        validateCurrency(dividendAmount);

        addDomainEvent(new DividendReceivedEvent(this.portfolioId, this.assetHoldingId, dividendAmount, receivedAt));
        this.lastTransactionAt = receivedAt;
        updateHolding();
    }

    public void processDividendReinvestment(Money dividendAmount, BigDecimal sharesReceived, Money pricePerShare, Instant reinvestmentDate) {
        Objects.requireNonNull(dividendAmount, "Dividend amount cannot be null");
        Objects.requireNonNull(sharesReceived, "Shares received cannot be null");
        Objects.requireNonNull(pricePerShare, "Price per share cannot be null");
        Objects.requireNonNull(reinvestmentDate, "Reinvestment date cannot be null");

        if (sharesReceived.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidHoldingOperationException("Shares received must be positive");
        }
        validateCurrency(dividendAmount);

        // For ACB purposes, reinvested dividends increase the cost base
        Money reinvestmentCost = pricePerShare.multiply(sharesReceived);
        Money newTotalCostBasis = this.totalCostBasis.add(reinvestmentCost);
        BigDecimal newTotalQuantity = this.totalQuantity.add(sharesReceived);
        Money newAverageCostBasis = newTotalCostBasis.divide(newTotalQuantity);

        this.totalQuantity = newTotalQuantity;
        this.averageCostBasis = newAverageCostBasis;
        this.totalCostBasis = newTotalCostBasis;
        this.lastTransactionAt = reinvestmentDate;

        addDomainEvent(new DividendReinvestedEvent(this.portfolioId, this.assetHoldingId, dividendAmount, sharesReceived, pricePerShare, reinvestmentDate));
        updateHolding(); 
    }

    public void processReturnOfCaptial(Money rocAmount, Instant effectiveDate) {
        Objects.requireNonNull(rocAmount, "ROC amount cannot be null");
        Objects.requireNonNull(effectiveDate, "Effective date cannot be null");

        if (rocAmount.isNegative()) {
            throw new InvalidHoldingOperationException("Return of capital amount cannot be negative");
        }

        if (this.totalQuantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new InvalidHoldingOperationException("Cannot process ROC for zero quantity holding");
        }

        validateCurrency(rocAmount);

        // ROC reduces ACB without creating a disposition
        Money newTotalCostBasis = this.totalCostBasis.subtract(rocAmount);
        
        // ACB cannot go negative - excess becomes a capital gain
        Money excessROC = Money.ZERO(this.totalCostBasis.currency());
        if (newTotalCostBasis.isNegative()) {
            excessROC = newTotalCostBasis.abs();
            newTotalCostBasis = Money.ZERO(this.totalCostBasis.currency());
        }

        Money newAverageCostBasis = this.totalQuantity.compareTo(BigDecimal.ZERO) == 0 ? 
            Money.ZERO(this.totalCostBasis.currency()) : 
            newTotalCostBasis.divide(this.totalQuantity);

        this.totalCostBasis = newTotalCostBasis;
        this.averageCostBasis = newAverageCostBasis;
        this.lastTransactionAt = effectiveDate;

        addDomainEvent(new ReturnOfCapitalProcessedEvent(this.portfolioId, this.assetHoldingId, rocAmount, excessROC, effectiveDate));
        updateHolding();
    }

    public void processStockSplitACB(BigDecimal splitRatio, Instant effectiveDate) {
        Objects.requireNonNull(splitRatio, "Split ratio cannot be null");
        Objects.requireNonNull(effectiveDate, "Effective date cannot be null");
        
        if (splitRatio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidHoldingOperationException("Split ratio must be positive");
        }
        
        // For ACB: total cost base stays same, shares increase, ACB per share decreases
        BigDecimal newQuantity = this.totalQuantity.multiply(splitRatio);
        Money newAverageCostBasis = this.totalCostBasis.divide(newQuantity);
        
        this.totalQuantity = newQuantity;
        this.averageCostBasis = newAverageCostBasis;
        // totalCostBasis remains unchanged
        this.lastTransactionAt = effectiveDate;
        
        addDomainEvent(new StockSplitACBEvent(this.portfolioId, this.assetHoldingId, splitRatio, effectiveDate));
        updateHolding();
    }

    public void recordEligibleDividend(Money dividendAmount, Money grossUpAmount, Instant receivedAt) {
        Objects.requireNonNull(dividendAmount, "Dividend amount cannot be null");
        Objects.requireNonNull(grossUpAmount, "Gross up amount cannot be null");
        Objects.requireNonNull(receivedAt, "Received at cannot be null");

        if (this.totalQuantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new InvalidHoldingOperationException("Cannot record dividend for zero quantity holding");
        }

        validateCurrency(dividendAmount);
        validateCurrency(grossUpAmount);

        addDomainEvent(new EligibleDividendReceivedEvent(this.portfolioId, this.assetHoldingId, dividendAmount, grossUpAmount, receivedAt));
        this.lastTransactionAt = receivedAt;
        updateHolding();
    }

    // QUERY METHODS //
    public Money getCurrentMarketValue(Money currentPrice) {
        Objects.requireNonNull(currentPrice, "Current price cannot be null");
        return currentPrice.multiply(this.totalQuantity); 
    }

    public Money getUnrealizedGainLoss(Money currentPrice) {
        Money currentValue = getCurrentMarketValue(currentPrice);
        return currentValue.subtract(this.totalCostBasis);
    }

    public Percentage getUnrealizedGainLossPercentage(Money currentPrice) {
        if (this.totalCostBasis.isZero()) {
            return Percentage.of(0);
        }
        Money gainLoss = getUnrealizedGainLoss(currentPrice);
        return Percentage.of(gainLoss.divide(this.totalCostBasis.amount()).amount());
    }

    public Money calculateCapitalGainLoss(BigDecimal quantitySold, Money salePrice) {
        Objects.requireNonNull(quantitySold, "Quantity sold cannot be null.");
        Objects.requireNonNull(salePrice, "Sale price cannot be null.");
        if (quantitySold.compareTo(this.totalQuantity) > 0) {
            throw new InvalidHoldingOperationException("Cannot sell more than current holding.");
        }

        Money saleProceeds = salePrice.multiply(quantitySold);
        Money acbOfSoldShares = this.averageCostBasis.multiply(quantitySold);
        return saleProceeds.subtract(acbOfSoldShares);
    }

    public Money getACBPerShare() {
        return this.averageCostBasis;
    }

    public Money getTotalACB() {
        return this.totalCostBasis;
    }

    public boolean shouldBeRemoved() {
        return isEmpty() && this.totalCostBasis.isZero();
    }

    public boolean isACBBelowThreshold(Money threshold) {
        return this.totalCostBasis.isLessThan(threshold);
    }

    public boolean isEligibleForSuperficialLossRule() {
        // requires additional logic to track purchase/sale dates
        // and determine if shares were acquired within 30 days before/after 
        // this might be a app layer method
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    public boolean isEmpty() {
        return this.totalQuantity.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean hasPosition() {
        return !isEmpty();
    }

    public boolean canSell(BigDecimal requestedQuantity) {
        return hasPosition() && !(requestedQuantity.compareTo(this.totalQuantity) > 0);
    }

    public Money getCostBasisForQuantity(BigDecimal quantity) {
        if (quantity.compareTo(this.totalQuantity) > 0) {
            throw new InvalidHoldingOperationException("Requested quantity exceeds available holding");
        }
        return this.averageCostBasis.multiply(quantity);
    }

    public boolean isOfType(AssetType type) {
        return this.assetIdentifier.type().equals(type);
    }

    public boolean isStock() {
        return this.assetIdentifier.type() == AssetType.STOCK;
    }

    public boolean isETF() {
        return this.assetIdentifier.type() == AssetType.ETF;
    }

    public boolean isCrypto() {
        return this.assetIdentifier.type() == AssetType.CRYPTO;
    }

    public boolean isBond() {
        return this.assetIdentifier.type() == AssetType.BOND;
    }

    // DOMAIN EVENTS //
    public void addDomainEvent(DomainEvent event) {
        Objects.requireNonNull(event, "Domain event cannot be null.");
        this.domainEvents.add(event);
    }

    public List<DomainEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(this.domainEvents);
    }

    public void markEventsAsCommitted() {
        this.domainEvents.clear();
        updateUpdatedAt();
    }

    public boolean hasUncommittedEvents() {
        return !domainEvents.isEmpty();
    }

    // GETTERS //
    public AssetHoldingId getAssetId() { return assetHoldingId; }
    public PortfolioId getPortfolioId() { return portfolioId; }
    public AssetIdentifier getAssetIdentifier() { return assetIdentifier; }
    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public Money getAverageCostBasis() { return averageCostBasis; }
    public Money getTotalCostBasis() { return totalCostBasis; }
    public Instant getLastTransactionAt() { return lastTransactionAt; }
    public int getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // PRIVATE HELPER //
    private void updateHolding() {
        updateUpdatedAt();
        updateVersion();
    }

    private void updateUpdatedAt() {
        this.updatedAt = Instant.now();
    }

    private void updateVersion() {
        this.version++;
    }

    private void validateCurrency(Money money) {
        if (!money.currency().equals(this.totalCostBasis.currency())) {
            throw new InvalidHoldingOperationException("Currency mismatch");
        }
    }
    // BUILDER //   
    public static class Builder {
        private AssetHoldingId assetHoldingId;
        private PortfolioId portfolioId;
        private AssetIdentifier assetIdentifier;
        private Instant createdAt;

        private BigDecimal totalQuantity;
        private Money averageCostBasis;
        private Money totalCostBasis;

        private Instant lastTransactionAt;
        private int version = 0;
        private Instant updatedAt;

        public Builder() {
            Instant now = Instant.now();
            this.createdAt = now;
            this.updatedAt = now;
        }

        public Builder assetHoldingId(AssetHoldingId assetHoldingId) {
            this.assetHoldingId = assetHoldingId;
            return this;
        }

        public Builder portfolioId(PortfolioId portfolioId) {
            this.portfolioId = portfolioId;
            return this;
        }

        public Builder assetIdentifier(AssetIdentifier assetIdentifier) {
            this.assetIdentifier = assetIdentifier;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder totalQuantity(BigDecimal totalQuantity) {
            this.totalQuantity = totalQuantity;
            return this;
        }

        public Builder averageCostBasis(Money averageCostBasis) {
            this.averageCostBasis = averageCostBasis;
            return this;
        }

        public Builder totalCostBasis(Money totalCostBasis) {
            this.totalCostBasis = totalCostBasis;
            return this;
        }

        public Builder lastTransactionAt(Instant lastTransactionAt) {
            this.lastTransactionAt = lastTransactionAt;
            return this;
        }

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public AssetHolding build() {
            return new AssetHolding(this);
        }

    }
    
    private static class AssetHoldingValidator {
        static void validateQuantity(BigDecimal quantity) {
            Objects.requireNonNull(quantity, "Quantity cannot be null");
            
            if (quantity.compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidHoldingQuantityException("Holding quantity cannot be negative: " + quantity);
            }
        }

        static void validateCostBasis(Money averageCostBasis, Money totalCostBasis) {
            Objects.requireNonNull(averageCostBasis, "Average cost basis cannot be null");
            Objects.requireNonNull(totalCostBasis, "Total cost basis cannot be null");
            
            if (averageCostBasis.isNegative()) {
                throw new InvalidHoldingCostBasisException("Average cost basis cannot be negative");
            }
            
            if (totalCostBasis.isNegative()) {
                throw new InvalidHoldingCostBasisException("Total cost basis cannot be negative");
            }

            if (!averageCostBasis.currency().equals(totalCostBasis.currency())) {
                throw new InvalidHoldingOperationException(
                    "Currency mismatch between average cost basis and total cost basis");
            }
        }
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssetHolding that = (AssetHolding) o;
        return Objects.equals(this.assetHoldingId, that.assetHoldingId) && 
            Objects.equals(this.portfolioId, that.portfolioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.assetHoldingId, this.portfolioId);
    }

    @Override
    public String toString() {
        return "AssetHolding{" +
            "assetId=" + this.assetHoldingId +
            ", symbol='" + this.assetIdentifier.primaryId() + '\'' +
            ", quantity=" + totalQuantity +
            ", avgCost=" + averageCostBasis +
            ", totalCost=" + totalCostBasis +
            '}';
    }

}
