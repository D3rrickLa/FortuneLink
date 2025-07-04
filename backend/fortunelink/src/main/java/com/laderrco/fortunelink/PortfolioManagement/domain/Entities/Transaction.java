package com.laderrco.fortunelink.portfoliomanagement.domain.entities;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.interfaces.TransactionDetails;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

import lombok.Builder;

@Builder
public class Transaction {
    private final UUID transactionId;
    private final UUID portfolioId;
    private final TransactionType transactionType;
    private final Money totalTransactionAmount;
    private final Instant transactionDate;
    private final TransactionDetails transactionDetails;
    private final TransactionMetadata transactionMetadata;
    private final List<Fee> fees;
    private boolean hidden;

    public Transaction(
        UUID transactionId, 
        UUID portfolioId, 
        TransactionType transactionType,
        Money totalTransactionAmount, 
        Instant transactionDate, 
        TransactionDetails transactionDetails,
        TransactionMetadata transactionMetadata, 
        List<Fee> fees, 
        boolean hidden
    ) {
        Objects.requireNonNull(transactionId, "Transaction ID cannot be null.");
        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
        Objects.requireNonNull(transactionType, "Transaction Type cannot be null.");
        Objects.requireNonNull(totalTransactionAmount, "Total Transaction Amount cannot be null.");
        Objects.requireNonNull(transactionDate, "Transaction Date cannot be null.");
        Objects.requireNonNull(transactionDetails, "Transaction Details cannot be null.");
        Objects.requireNonNull(transactionMetadata, "Transaction Metadata cannot be null.");
        Objects.requireNonNull(fees, "Fees list cannot be null (can be empty).");

        this.transactionId = transactionId;
        this.portfolioId = portfolioId;
        this.transactionType = transactionType;
        this.totalTransactionAmount = totalTransactionAmount;
        this.transactionDate = transactionDate;
        this.transactionDetails = transactionDetails;
        this.transactionMetadata = transactionMetadata;
        this.fees = fees;
        this.hidden = hidden; // flag is typically stt by the business logic, not directly in a generic constructor
    }
        
    void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    // NOTE: we don't need Optional<VoidInfo> because the ReversalTransactoinDetails already
    // handles the linking to the original transaction Id
    public UUID getTransactionId() { return transactionId; }
    public UUID getPortfolioId() { return portfolioId; }
    public TransactionType getTransactionType() { return transactionType; }
    public Money getTotalTransactionAmount() { return totalTransactionAmount; }
    public Instant getTransactionDate() { return transactionDate; }
    public TransactionDetails getTransactionDetails() { return transactionDetails; }
    public TransactionMetadata getTransactionMetadata() { return transactionMetadata; }
    public List<Fee> getFees() { return fees; }
    public boolean isHidden() { return hidden; }    

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return hidden == that.hidden &&
               Objects.equals(transactionId, that.transactionId) &&
               Objects.equals(portfolioId, that.portfolioId) &&
               transactionType == that.transactionType &&
               Objects.equals(totalTransactionAmount, that.totalTransactionAmount) &&
               Objects.equals(transactionDate, that.transactionDate) &&
               Objects.equals(transactionDetails, that.transactionDetails) &&
               Objects.equals(transactionMetadata, that.transactionMetadata) &&
               Objects.equals(fees, that.fees);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, portfolioId, transactionType, totalTransactionAmount,
                            transactionDate, transactionDetails, transactionMetadata, fees, hidden);
    }
}

