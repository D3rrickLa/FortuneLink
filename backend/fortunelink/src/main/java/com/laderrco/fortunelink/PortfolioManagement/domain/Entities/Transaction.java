package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;


import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionStatus;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionType;

public class Transaction {
    private UUID transactionId;
    private UUID portfolioId;
    private TransactionType transactionType;
    private Money amount; // total cost of the transaction
    private Instant transactionDate;
    private String description;

    private TransactionStatus transactionStatus;
    private String voidReason;

    private UUID assetHoldingId;
    private UUID liabilityId;

    private BigDecimal quantity;
    private BigDecimal pricePerUnit;

    public Transaction(UUID transiactionUuid, UUID portfolioUuid, TransactionType transactionType, Money amount, Instant transactionDate,
            String description, BigDecimal quantity, BigDecimal costPerUnit, UUID assetHoldingId, UUID liabilityId) {

        Objects.requireNonNull(portfolioUuid, "Portfolio ID cannot be null.");
        Objects.requireNonNull(transactionType, "Transaction type cannot be null.");
        Objects.requireNonNull(amount, "Amount cannot be null.");
        Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
        Objects.requireNonNull(description, "Description cannot be null."); // Or allow null/empty based on rules

        if (assetHoldingId == null && liabilityId == null
                && !(transactionType == TransactionType.DEPOSIT || transactionType == TransactionType.WITHDRAWAL
                        || transactionType == TransactionType.DIVIDEND
                        || transactionType == TransactionType.INTEREST_INCOME)) {
            // logic based on the transaction type -> desposit and or withdrawl might have
            // no asset/liability ID
        }

        this.transactionId = transiactionUuid;
        this.portfolioId = portfolioUuid;
        this.transactionType = transactionType;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.description = description;
        this.assetHoldingId = assetHoldingId;
        this.liabilityId = liabilityId;
        this.quantity = quantity; // Can be null for non-asset transactions
        this.pricePerUnit = costPerUnit; // Can be null for non-asset transactions
        this.transactionStatus = TransactionStatus.ACTIVE;

    }

    public void markAsVoided(String reason) {
        Objects.requireNonNull(reason, "The reason for the transaction to be voided cannot be null.");
        if (this.transactionStatus != TransactionStatus.ACTIVE) {
            throw new IllegalArgumentException("Transaction cannot be voided if the transaction is marked as ACTIVE.");
        }
        this.transactionStatus = TransactionStatus.VOIDED;
        this.voidReason = reason;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Transaction that = (Transaction) o;
        return transactionId != null && transactionId.equals(that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
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

    public Money getAmount() {
        return amount;
    }

    public Instant getTransactionDate() {
        return transactionDate;
    }

    public String getDescription() {
        return description;
    }

    public UUID getAssetHoldingId() {
        return assetHoldingId;
    }

    public UUID getLiabilityId() {
        return liabilityId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public TransactionStatus getTransactionStatus() {
        return transactionStatus;
    }

    public String getVoidReason() {
        return voidReason;
    }

}
