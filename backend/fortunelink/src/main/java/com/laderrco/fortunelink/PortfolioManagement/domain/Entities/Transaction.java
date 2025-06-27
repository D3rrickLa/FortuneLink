package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Fee;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionDetails;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionMetadata;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.VoidInfo;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionType;
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
     */

    public void markAsVoided(UUID voidingTransactionId, String reason) {
        // errors to handle: if status is active, if source is platform and if reason is
        // null

        this.voidInfo = Optional.of(new VoidInfo(voidingTransactionId));
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
