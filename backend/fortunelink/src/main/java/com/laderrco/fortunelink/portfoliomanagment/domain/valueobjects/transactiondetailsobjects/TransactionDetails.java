package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.transaction.TransactionStatus;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;

public abstract class TransactionDetails {
    // put common transaction inputs in here
    private final UUID correlationId; // for when an event generates multiple transactions
    private final UUID parentTransactionId;
    private final TransactionType transactionType;
    
    // metadata will be in this class rather than a separate class
    private final TransactionStatus status;
    private final TransactionSource source;
    private final String description;
    private final List<Fee> fees;
    private final Instant createdAt;
    
    public TransactionDetails(
        UUID correlationId, 
        UUID parentTransactionId, 
        TransactionType transactionType,
        TransactionStatus status,
        TransactionSource source, 
        String description, 
        List<Fee> fees, 
        Instant createdAt
    ) {
        this.correlationId = correlationId;
        this.parentTransactionId = parentTransactionId;
        this.transactionType = transactionType;
        this.status = status;
        this.source = source;
        this.description = description;
        this.fees = fees;
        this.createdAt = createdAt;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public UUID getParentTransactionId() {
        return parentTransactionId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public TransactionSource getSource() {
        return source;
    }

    public String getDescription() {
        return description;
    }

    public List<Fee> getFees() {
        return fees;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
       
}
