package com.laderrco.fortunelink.portfoliomanagement.domain.entities;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.TransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.VoidInfo;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.TransactionType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

public class Transaction {
    private final UUID transactionId;
    private final UUID portfolioId;
    private final TransactionType transactionType;
    private final Money totalTransactionAmount; // amount of $$$ for this transaction (fees. included)
    private final Instant transactionDate;
    private final TransactionDetails transactionDetails;

    private final TransactionMetadata transactionMetadata;
    private Optional<VoidInfo> voidInfo; // different to VoidTransactionDetails. Who did the voiding? quick search
                                         // compare to TransactionDetails

    // we need a new feild for fees
    public List<Fee> fees;

    private Transaction(UUID transactionId, UUID portfolioId, TransactionType transactionType,
            Money totalTransactionAmount, Instant transactionDate, TransactionDetails transactionDetails,
            TransactionMetadata transactionMetadata, Optional<VoidInfo> voidInfo, List<Fee> fees) {
        
        Objects.requireNonNull(transactionId, "Transaction ID cannot be null.");
        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
        Objects.requireNonNull(transactionType, "Transaction Type cannot be null.");
        Objects.requireNonNull(totalTransactionAmount, "Total Transaction Amount cannot be null.");
        Objects.requireNonNull(transactionDate, "Transaction Date cannot be null.");
        Objects.requireNonNull(transactionDetails, "Transaction Details cannot be null.");
        Objects.requireNonNull(transactionMetadata, "Transaction Metadata cannot be null.");
        
        this.transactionId = transactionId;
        this.portfolioId = portfolioId;
        this.transactionType = transactionType;
        this.totalTransactionAmount = totalTransactionAmount;
        this.transactionDate = transactionDate;
        this.transactionDetails = transactionDetails;
        this.transactionMetadata = transactionMetadata;
        this.voidInfo = voidInfo;
        this.fees = fees != null ? List.copyOf(fees) : List.of();
    }

    public Transaction(Builder builder) {
        this(builder.transactionId, builder.portfolioId, builder.transactionType, builder.totalTransactionAmount,
                builder.transactionDate, builder.transactionDetails, builder.transactionMetadata, builder.voidInfo,
                builder.fees);
    }

    /*
     * What can a transaction do?
     * 
     * NOUN -> transaction represent a user financial action. This action could be
     * on different products like stocks or crypto
     * 
     * VERBS
     * - record transaction (constructor) (DONE)
     * - update transaction? <- used only if to manual input (X - not impl)
     * - delete a transaction? <- used only if to manual input (X - not impl)
     * - Getters (DONE)
     * - void a transaction? <- used for updating a record, impelmentation depends
     * if we have the update/delete functionality (UPDATE: voiding only)
     * 
     * UPDATE: going with the voided option
     * 
     * 
     * HOW IT WILL WORK
     * we have transaction abc
     * we then have transaction xyz to void abc
     * these transaction are both created
     * we then pass xyz id to transaction abc.markAsVoided function
     * // Example for ArrayList (replace the old version with the new one)
        // transactions.replaceAll(tx -> tx.getTransactionId().equals(originalTransactionId) ? updatedVoidedTransaction : tx);
     *
     * NOTE: this was AI assisted 
     */

    public Transaction markAsVoided(UUID voidingTransactionId, String reason) {
        // errors to handle: if status is active, if source is platform and if reason is
        // null
        Objects.requireNonNull(voidingTransactionId, "Transaction ID to void cannot be null.");
        Objects.requireNonNull(reason, "Reason to void cannot be null.");

        if (reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be empty.");
        }

        // Error handling for current status (e.g., only ACTIVE or PENDING can be
        // voided)
        if (!Set.of(TransactionStatus.ACTIVE, TransactionStatus.PENDING).contains(this.transactionMetadata.transactionStatus())) {
            throw new IllegalStateException("Only ACTIVE or PENDING transactions can be voided. Current status: "+ this.transactionMetadata.transactionStatus());
        }

        VoidInfo newVoidInfo = new VoidInfo(voidingTransactionId);

        TransactionMetadata newMetadata = this.transactionMetadata.withStatusAndUpdatedAt(TransactionStatus.VOIDED, Instant.now());

        // return a transaction
        return new Transaction.Builder()
            .transactionId(this.transactionId) // reuse the ID
            .portfolioId(this.portfolioId)
            .transactionType(this.transactionType)
            .totalTransactionAmount(this.totalTransactionAmount)
            .transactionDate(this.transactionDate)
            .transactionDetails(this.transactionDetails)
            .transactionMetadata(newMetadata) // Use the new metadata
            .voidInfo(Optional.of(newVoidInfo)) // Use the new void info
            .fees(this.fees) // Pass existing fees (assuming fees are immutable)
            .build();

    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getPortfolioId() {
        return portfolioId;
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

    public Optional<VoidInfo> getVoidInfo() {
        return voidInfo;
    }

    public List<Fee> getFees() {
        return fees;
    }

    public static class Builder {
        private UUID transactionId;
        private UUID portfolioId;
        private TransactionType transactionType;
        private Money totalTransactionAmount; // $$$ for this transaction
        private Instant transactionDate;
        private TransactionDetails transactionDetails;
        private TransactionMetadata transactionMetadata;
        private Optional<VoidInfo> voidInfo;
        private List<Fee> fees;

        public Builder transactionId(UUID transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder portfolioId(UUID portfolioId) {
            this.portfolioId = portfolioId;
            return this;
        }

        public Builder transactionType(TransactionType transactionType) {
            this.transactionType = transactionType;
            return this;
        }

        public Builder totalTransactionAmount(Money totalTransactionAmount) {
            this.totalTransactionAmount = totalTransactionAmount;
            return this;
        }

        public Builder transactionDate(Instant transactionDate) {
            this.transactionDate = transactionDate;
            return this;
        }

        public Builder transactionDetails(TransactionDetails transactionDetails) {
            this.transactionDetails = transactionDetails;
            return this;
        }

        public Builder transactionMetadata(TransactionMetadata transactionMetadata) {
            this.transactionMetadata = transactionMetadata;
            return this;
        }

        public Builder voidInfo(Optional<VoidInfo> voidInfo) {
            this.voidInfo = voidInfo;
            return this;

        }

        public Builder fees(List<Fee> fees) {
            this.fees = fees;
            return this;
        }

        public Transaction build() {
            return new Transaction(this);
        }
    }
}
