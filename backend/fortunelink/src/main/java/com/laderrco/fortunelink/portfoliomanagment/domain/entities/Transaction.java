package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.TransactionDetails;

public class Transaction {
    private final UUID transactionId;
    private final UUID portfolioId;
    private final UUID correlationId; // for events that happen together, like conversion of money or stock splits
    private final UUID parentTransactionId;

    private final TransactionType transactionType;
    private final Money totalTransactionAmount; // portfolio's currency preference. Net cash impact of the transaction.
    private final Instant transactionDate;
    private final TransactionDetails transactionDetails; // cotnains both currencies 
    private final TransactionMetadata transactionMetadata;
    
    private final List<Fee> fees; // og currency
    private boolean hidden;
    private int version; // for updating if fixes are needed

    public Transaction(
        UUID transactionId, 
        UUID portfolioId, 
        UUID correlationId,
        UUID parentTransactionId,
        TransactionType transactionType, 
        Money totalTransactionAmount, 
        Instant transactionDate,
        TransactionDetails transactionDetails, 
        TransactionMetadata transactionMetadata, 
        List<Fee> fees,
        boolean hidden, 
        int version
    ) {
        Objects.requireNonNull(transactionId, "Transaction id cannot be null.");
        Objects.requireNonNull(portfolioId, "Portfolio id cannot be null.");
        Objects.requireNonNull(correlationId, "Correlation id cannot be null.");
        Objects.requireNonNull(transactionType, "Transactiont type cannot be null.");
        Objects.requireNonNull(totalTransactionAmount, "Total transaction amount cannot be null.");
        Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
        Objects.requireNonNull(transactionDetails, "Transaction details cannot be null.");
        Objects.requireNonNull(transactionMetadata, "Transaction metadata cannot be null.");
        Objects.requireNonNull(version, "Version cannot be null.");
        
        this.transactionId = transactionId;
        this.portfolioId = portfolioId;
        this.correlationId = correlationId;
        this.parentTransactionId = parentTransactionId;
        this.transactionType = transactionType;
        this.totalTransactionAmount = totalTransactionAmount;
        this.transactionDate = transactionDate;
        this.transactionDetails = transactionDetails;
        this.transactionMetadata = transactionMetadata;
        this.fees = fees != null ? fees : Collections.emptyList();
        this.hidden = hidden;
        this.version = version;
    }

    public boolean isReversed() {
        return Set.of(TransactionType.REVERSAL_BUY, TransactionType.REVERSAL_DEPOSIT, TransactionType.REVERSAL_SELL, TransactionType.REVERSAL_WITHDRAWAL).contains(this.transactionType);
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getPortfolioId() {
        return portfolioId;
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

    public Money getTotalTransactionAmount() {
        return totalTransactionAmount;
    }

    public Instant getTransactionDate() {
        return transactionDate;
    }

    public TransactionDetails getTransactionDetails() {
        return transactionDetails;
    }

    public TransactionMetadata getTransactionMetadata() {
        return transactionMetadata;
    }

    public List<Fee> getFees() {
        return fees;
    }

    public boolean isHidden() {
        return hidden;
    }

    public int getVersion() {
        return version;
    }
}
