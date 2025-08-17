package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.CorrelationId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.TransactionDetails;

// NOTE: TREAT EVERY FINANCE INTERACT AS DISTINCT AND INDEPENDENT
public class Transaction {
    private final TransactionId transactionId;
    private final CorrelationId correlationId; // for when an event generates multiple transactions
    private final TransactionId parentTransactionId;
    private final PortfolioId portfolioId;
    
    private final TransactionType type;
    private TransactionStatus status;
    private final TransactionDetails transactionDetails;

    private final Money transactionNetImpact; // in portfolio's currency
    private final Instant transactionDate;

    private boolean hidden;
    private int version;

    private final Instant createdAt;
    private Instant updatedAt;

    // Valid reversal transaction types
    private static final Set<TransactionType> REVERSAL_TYPES = Set.of(
        TransactionType.REVERSAL,
        TransactionType.REVERSAL_BUY,
        TransactionType.REVERSAL_DEPOSIT,
        TransactionType.REVERSAL_SELL,
        TransactionType.REVERSAL_WITHDRAWAL,
        TransactionType.REVERSE_STOCK_SPLIT
    );

    

    public Transaction(
        TransactionId transactionId, 
        CorrelationId correlationId, 
        TransactionId parentTransactionId,
        PortfolioId portfolioId, 
        TransactionType type, 
        TransactionStatus status,
        TransactionDetails transactionDetails, 
        Money transactionNetImpact, 
        Instant transactionDate,
        Instant createdAt
    ) {
        // Validation
        this.transactionId = Objects.requireNonNull(transactionId, "Transaction ID cannot be null");
        this.correlationId = Objects.requireNonNull(correlationId, "Correlation ID cannot be null");
        this.parentTransactionId = parentTransactionId; // Can be null
        this.portfolioId = Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null");
        this.type = Objects.requireNonNull(type, "Transaction type cannot be null");
        this.status = Objects.requireNonNull(status, "Transaction status cannot be null");
        this.transactionDetails = Objects.requireNonNull(transactionDetails, "Transaction details cannot be null");
        this.transactionNetImpact = Objects.requireNonNull(transactionNetImpact, "Transaction net impact cannot be null");
        this.transactionDate = Objects.requireNonNull(transactionDate, "Transaction date cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");
        
        this.updatedAt = createdAt;
        this.hidden = false;
        this.version = 1;
    }

    private void updateVersion() {
        this.version += 1;
    }

    public void updateStatus(TransactionStatus newStatus, Instant updatedAt) {
        Objects.requireNonNull(newStatus, "New status cannot be null");
        Objects.requireNonNull(updatedAt, "Updated at cannot be null");
        
        if (updatedAt.isBefore(this.updatedAt)) {
            throw new IllegalArgumentException("Updated at cannot be before current updated at");
        }
        
        this.status = newStatus;
        this.updatedAt = updatedAt;
        updateVersion();
    }

    public void hide(Instant updatedAt) {
        Objects.requireNonNull(updatedAt, "Updated at cannot be null");
        
        if (updatedAt.isBefore(this.updatedAt)) {
            throw new IllegalArgumentException("Updated at cannot be before current updated at");
        }
        
        this.hidden = true;
        this.updatedAt = updatedAt;
        updateVersion();
    }

    public void unhide(Instant updatedAt) {
        Objects.requireNonNull(updatedAt, "Updated at cannot be null");
        
        if (updatedAt.isBefore(this.updatedAt)) {
            throw new IllegalArgumentException("Updated at cannot be before current updated at");
        }
        
        this.hidden = false;
        this.updatedAt = updatedAt;
        updateVersion();
    }

    public boolean isReversal() {
        return REVERSAL_TYPES.contains(this.type);
    }

    public boolean canBeUpdated() {
        return this.status != TransactionStatus.FINALIZED && 
               this.status != TransactionStatus.CANCELLED;
    }

    // Getters
    public TransactionId getTransactionId() { return transactionId; }
    public CorrelationId getCorrelationId() { return correlationId; }
    public TransactionId getParentTransactionId() { return parentTransactionId; }
    public PortfolioId getPortfolioId() { return portfolioId; }
    public TransactionType getType() { return type; }
    public TransactionStatus getStatus() { return status; }
    public TransactionDetails getTransactionDetails() { return transactionDetails; }
    public Money getTransactionNetImpact() { return transactionNetImpact; }
    public Instant getTransactionDate() { return transactionDate; }
    public boolean isHidden() { return hidden; }
    public int getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Transaction that = (Transaction) obj;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId=" + transactionId +
                ", type=" + type +
                ", status=" + status +
                ", netImpact=" + transactionNetImpact +
                ", version=" + version +
                '}';
    }
}
