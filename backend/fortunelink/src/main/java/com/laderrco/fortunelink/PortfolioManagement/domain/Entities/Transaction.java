package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import java.time.Instant;
import java.util.UUID;

import org.springframework.transaction.TransactionStatus;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionDetails;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionSource;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

public class Transaction {
    // should this class be considered a super or abstract class in some way?
    // the thinking is that when making a new transaction, we have to pass both
    // liabilityId and asHoldingId
    // when we really just need one of them
    // so we have a factory for creating the Transaction class. we did this because
    // there was too much logic in a single constructor
    // to house all types of transaction that can happen here.
    // With that in mind. even with the factory class, we would still need to pass
    // in all the variables, else someone could call a null varaiable
    // the solution to this is another class called TransactionDetail, which we
    // would extend to be asDetails and liabilityDetails, etc.
    // TransactionDetail is a variable in Transaction and we just passed that into
    // the constructor
    private UUID transactionId;
    private UUID portfolioId;
    private TransactionType transactionType;
    private TransactionStatus transactionStatus;
    private TransactionSource transactionSource;
    private Money totalTransactionAmount;
    private Instant transactionDate;
    private String transactionDescription;

    private TransactionDetails transactionDetails; // the payload

    private boolean isVoided;
    private String voidReason;
    private UUID relatedTransactionId; // handles voiding/correction, if corrections are needed, a new transaction is
                                       // made and the reference is stored here

    private Transaction(UUID transactionId, UUID portfolioId, TransactionType transactionType,
            TransactionStatus transactionStatus, TransactionSource transactionSource, Money totalTransactionAmount,
            Instant transactionDate, String transactionDescription, TransactionDetails transactionDetails,
            boolean isVoided, String voidReason, UUID relatedTransactionId) {
        this.transactionId = transactionId;
        this.portfolioId = portfolioId;
        this.transactionType = transactionType;
        this.transactionStatus = transactionStatus;
        this.transactionSource = transactionSource;
        this.totalTransactionAmount = totalTransactionAmount;
        this.transactionDate = transactionDate;
        this.transactionDescription = transactionDescription;
        this.transactionDetails = transactionDetails;
        this.isVoided = isVoided;
        this.voidReason = voidReason;
        this.relatedTransactionId = relatedTransactionId;
    }

    public Transaction(Builder builder) {
        this(builder.transactionId, builder.portfolioId, builder.transactionType, builder.transactionStatus,
                builder.transactionSource, builder.totalTransactionAmount, builder.transactionDate,
                builder.transactionDescription, builder.transactionDetails, builder.isVoided, builder.voidReason,
                builder.relatedTransactionId);
    }

    /*
     * What can a transcation do?
     * 
     * NOUN -> transaction represent a user financial action. This action could be
     * on different products like stocks or crypto
     * 
     * VERBS
     * - record transaction (constructor) (DONE)
     * - update transaction? <- used only if  to manual input (X - not impl)
     * - delete a transaction? <- used only if  to manual input (X - not impl)
     * - Getters (DONE)
     * - void a transaction? <- used for updating a record, impelmentation depends
     * if we have the update/delete functionality (UPDATE: voiding only)
     * 
     * UPDATE: going with the voided option
     */

    public void markAsVoided(UUID voidingTransactionId, String reason) {

    }

    public static class Builder {
        private UUID transactionId;
        private UUID portfolioId;
        private TransactionType transactionType;
        private TransactionStatus transactionStatus;
        private TransactionSource transactionSource;
        private Money totalTransactionAmount;
        private Instant transactionDate;
        private String transactionDescription;
        private TransactionDetails transactionDetails;
        private boolean isVoided;
        private String voidReason;
        private UUID relatedTransactionId;

        public UUID getTransactionId() {
            return transactionId;
        }

        public UUID getPortfolioId() {
            return portfolioId;
        }

        public TransactionType getTransactionType() {
            return transactionType;
        }

        public TransactionStatus getTransactionStatus() {
            return transactionStatus;
        }

        public TransactionSource getTransactionSource() {
            return transactionSource;
        }

        public Money getTotalTransactionAmount() {
            return totalTransactionAmount;
        }

        public Instant getTransactionDate() {
            return transactionDate;
        }

        public String getTransactionDescription() {
            return transactionDescription;
        }

        public TransactionDetails getTransactionDetails() {
            return transactionDetails;
        }

        public boolean isVoided() {
            return isVoided;
        }

        public String getVoidReason() {
            return voidReason;
        }

        public UUID getRelatedTransactionId() {
            return relatedTransactionId;
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

        public Builder transactionStatus(TransactionStatus transactionStatus) {
            this.transactionStatus = transactionStatus;
            return this;
        }

        public Builder transactionSource(TransactionSource transactionSource) {
            this.transactionSource = transactionSource;
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

        public Builder transactionDescription(String transactionDescription) {
            this.transactionDescription = transactionDescription;
            return this;
        }

        public Builder transactionDetails(TransactionDetails transactionDetails) {
            this.transactionDetails = transactionDetails;
            return this;
        }

        public Builder voided(boolean isVoided) {
            this.isVoided = isVoided;
            return this;
        }

        public Builder voidReason(String voidReason) {
            this.voidReason = voidReason;
            return this;
        }

        public Builder relatedTransactionId(UUID relatedTransactionId) {
            this.relatedTransactionId = relatedTransactionId;
            return this;
        }

        public Transaction build() {
            return new Transaction(this);
        }
    }
}
