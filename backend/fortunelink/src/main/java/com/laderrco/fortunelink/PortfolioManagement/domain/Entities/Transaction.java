package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionSource;
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
    private Instant voidedAt;

    private final TransactionSource transactionSource;

    // Transaction will only know about these, not the other classes
    private UUID assetHoldingId;
    private UUID liabilityId;

    private BigDecimal quantity;
    private BigDecimal pricePerUnit;

    // Full constructor with all validation logic and constraints
    public Transaction(UUID transiactionId, UUID portfolioId, TransactionType transactionType, Money amount,
            Instant transactionDate, String description, BigDecimal quantity, BigDecimal pricePerUnit,
            TransactionStatus transactionStatus, String voidReason, Instant voidedAt,
            UUID assetHoldingId, UUID liabilityId, TransactionSource transactionSource) {

        Objects.requireNonNull(transiactionId, "Transaction ID cannot be null.");
        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
        Objects.requireNonNull(transactionType, "Transaction type cannot be null.");
        Objects.requireNonNull(amount, "Amount cannot be null.");
        Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
        Objects.requireNonNull(description, "Description cannot be null."); // Or allow null/empty based on rules
        Objects.requireNonNull(transactionStatus, "Transaction status must not be null.");
        Objects.requireNonNull(transactionSource, "Transaction source must not be null.");

        // AI coded
        // Rule: A transaction must be associated with an asset or liability ID,
        // unless it's a specific cash-only type (deposit, withdrawal, dividend,
        // interest).

        // --- 2. Validation for Quantity and PricePerUnit (Specific to BUY/SELL) ---
        // This validation was in the wrong place in your code, and also duplicated.
        // It belongs here for types that require them.
        if ((transactionType == TransactionType.BUY || transactionType == TransactionType.SELL)) {
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Quantity is required and must be positive for BUY/SELL transactions.");
            }
            if (pricePerUnit == null || pricePerUnit.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Price per unit is required and must be positive for BUY/SELL transactions.");
            }
        }
        // CONSIDER: If other types (e.g., DIVIDEND) might have quantity/price,
        // adjust this logic or ensure they are properly handled as optional/null.

        // --- 3. Asset/Liability ID Linkage Validation ---
        // Rule: A transaction must be associated with an asset or liability ID,
        // unless it's a specific cash-only type.
        boolean isCashOnlyTransaction = (transactionType == TransactionType.DEPOSIT ||
                transactionType == TransactionType.WITHDRAWAL ||
                transactionType == TransactionType.VOID_WITHDRAWAL ||
                transactionType == TransactionType.DIVIDEND ||
                transactionType == TransactionType.INTEREST_INCOME);

        if (assetHoldingId == null && liabilityId == null && !isCashOnlyTransaction) {
            throw new IllegalArgumentException(
                    "Transactions must be linked to an Asset Holding or Liability, unless it's a cash-only type (DEPOSIT, WITHDRAWAL, DIVIDEND, INTEREST_INCOME).");
        }
        // Further rule: Should not have BOTH assetHoldingId and liabilityId unless
        // specific type (e.g. transfer)
        if (assetHoldingId != null && liabilityId != null &&
                !(transactionType == TransactionType.TRANSFER_IN
                        || transactionType == TransactionType.TRANSFER_OUT /* add relevant types */)) {
            // Adjust based on your specific transfer types or if you allow both for other
            // reasons
            throw new IllegalArgumentException(
                    "Transaction cannot be linked to both an Asset Holding and a Liability unless it's a specific transfer type.");
        }

        // Validation for voidReason and voidedAt if status is VOIDED
        if (transactionStatus == TransactionStatus.VOIDED) {
            Objects.requireNonNull(voidReason, "Void reason must be provided if status is VOIDED.");
            if (voidReason.trim().isEmpty()) {
                throw new IllegalArgumentException("Void reason cannot be empty or blank if status is VOIDED.");
            }
            Objects.requireNonNull(voidedAt, "Voided timestamp must be provided if status is VOIDED.");
        } 
        else {
            // Ensure voidReason and voidedAt are null if not voided
            if (voidReason != null || voidedAt != null) {
                throw new IllegalArgumentException("Void reason and timestamp must be null if status is not VOIDED.");
            }
        }
        // ---

        this.transactionId = transiactionId;
        this.portfolioId = portfolioId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.description = description;
        this.assetHoldingId = assetHoldingId;
        this.liabilityId = liabilityId;
        this.quantity = quantity; // Can be null for non-asset transactions
        this.pricePerUnit = pricePerUnit; // Can be null for non-asset transactions
        this.voidReason = voidReason;
        this.voidedAt = voidedAt;
        this.transactionStatus = transactionStatus;
        this.transactionSource = transactionSource;

    }

    // Convenience Constructor for MANUAL_INPUT, ACTIVE transactions
    // This constructor should call the full constructor, providing defaults.
    public Transaction(UUID transactionId, UUID portfolioId, TransactionType transactionType, Money amount,
            Instant transactionDate, String description, BigDecimal quantity, BigDecimal pricePerUnit,
            UUID assetHoldingId, UUID liabilityId) {

        // delegate the full constructor, providing default values for omitted params
        this(transactionId, portfolioId, transactionType, amount, transactionDate, description, quantity, pricePerUnit,
                TransactionStatus.ACTIVE, null, null, assetHoldingId, liabilityId, TransactionSource.MANUAL_INPUT);

    }

    // AI coded
    public void markAsVoided(String reason) {
        // 1. Check for null/empty/blank reason
        Objects.requireNonNull(reason, "The reason for the transaction to be voided cannot be null or empty.");
        if (reason.trim().isEmpty()) {
            throw new IllegalArgumentException("The reason for the transaction to be voided cannot be empty or blank.");
        }

        // 2. Rule: Transaction can only be voided if its status is ACTIVE
        // This check should come BEFORE the source check, as it's a more fundamental
        // "can this state transition happen at all?" question.
        // If it's already voided/completed, it doesn't matter what its source is.
        if (this.transactionStatus != TransactionStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Transaction can only be voided if its status is ACTIVE. Current status: "
                            + this.transactionStatus);
        }

        // 3. Rule: Platform-synced transactions cannot be manually voided
        // This check comes AFTER the status check because it's a specific restriction
        // on *how* an ACTIVE transaction can be voided.
        if (this.transactionSource == TransactionSource.PLATFORM_SYNC) {
            throw new IllegalStateException("Platform-synced transactions cannot be manually voided.");
        }

        // If all checks pass, update the state of THIS object
        this.transactionStatus = TransactionStatus.VOIDED;
        this.voidReason = reason.trim(); // Trim the reason when assigning
        this.voidedAt = Instant.now(); // Set the voided timestamp

    }
    // --

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

    public TransactionSource getTransactionSource() {
        return transactionSource;
    }

    public Instant getVoidedAt() {
        return voidedAt;
    }

}
