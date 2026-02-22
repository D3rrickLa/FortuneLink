package com.laderrco.fortunelink.portfolio.domain.model.entities;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.CashImpact;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TransactionDate;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

// THIS IS AN IMMUTABLE STATE
// account and portfolio reconstruct state from these transaction(s)
public record Transaction(
        TransactionId transactionId,
        AccountId accountId,
        TransactionType transactionType,
        TradeExecution execution,
        SplitDetails split,
        Money cashDelta,
        List<Fee> fees,
        String notes,
        TransactionDate occurredAt,
        TransactionId relatedTransactionId,
        TransactionMetadata metadata

) {
    public Transaction {
        notNull(transactionId, "transactionId");
        notNull(accountId, "accountId");
        notNull(transactionType, "transactionType");
        notNull(cashDelta, "cashDelta");
        notNull(fees, "fees");
        notNull(occurredAt, "occurredAt");
        notNull(notes, "notes");

        fees = List.copyOf(fees);
        notes = notes.trim();

        validateConsistency("execution details",
                transactionType.requiresExecution(), execution != null);

        validateConsistency("split details",
                transactionType.requiresSplitDetails(), split != null);

        if (transactionType.cashImpact() == CashImpact.NONE && !cashDelta.isZero()) {
            throw new IllegalArgumentException(transactionType + " cannot affect cash");
        }

        if (!transactionType.requiresExecution()) {
            if (!fees.isEmpty()) {
                throw new IllegalArgumentException(transactionType + " cannot have fees");
            }
        } else {
            // Only run trade consistency if execution is required
            validateTradeConsistency(execution, transactionType, cashDelta, fees);
        }
    }

    public Transaction markAsExcluded(UserId userId, String reason) {
        if (metadata == null) {
            throw new IllegalStateException("Cannot exclude transaction without metadata");
        }
        TransactionMetadata updatedMetadata = metadata.markAsExcluded(userId, reason);
        return new Transaction(
                transactionId,
                accountId,
                transactionType,
                execution,
                split,
                cashDelta,
                fees,
                notes,
                occurredAt,
                relatedTransactionId,
                updatedMetadata);
    }

    public Transaction restore() {
        if (metadata == null) {
            throw new IllegalStateException("Cannot restore transaction without metadata");
        }
        TransactionMetadata updatedMetadata = metadata.restore();
        return new Transaction(
                transactionId,
                accountId,
                transactionType,
                execution,
                split,
                cashDelta,
                fees,
                notes,
                occurredAt,
                relatedTransactionId,
                updatedMetadata);
    }

    public boolean isExcluded() {
        return metadata != null && metadata.excluded();
    }

    public Money totalFeesInAccountCurrency() {
        return Fee.totalInAccountCurrency(fees, cashDelta.currency());
        // Currency currency = cashDelta.currency();
        // return fees.stream().map(Fee::accountAmount)
                // .reduce(Money.ZERO(currency), Money::add);
    }

    private void validateConsistency(String label, boolean isRequired, boolean isPresent) {
        if (isRequired && !isPresent) {
            throw new IllegalArgumentException(transactionType + " requires " + label);
        }
        if (!isRequired && isPresent) {
            throw new IllegalArgumentException(transactionType + " cannot have " + label);
        }
    }

    private void validateTradeConsistency(TradeExecution execution, TransactionType type, Money cashDelta,
            List<Fee> fees) {
        Money grossValue = execution.grossValue();
        Money totalFees = totalFeesInAccountCurrency();

        Money expectedCashDelta = switch (type.cashImpact()) {
            case IN -> grossValue.subtract(totalFees);
            case OUT -> grossValue.add(totalFees).negate();
            case NONE -> Money.ZERO(cashDelta.currency());
        };

        if (!cashDelta.equals(expectedCashDelta)) {
            throw new IllegalArgumentException(
                    "Cash delta mismatch for " + type + ". Expected: " + expectedCashDelta + ", got: " + cashDelta);
        }
    }

    // old code before, in 90f8c03428f84c687a02cc8d6835a35743a11899
    // did computation in th sense that
    // it knows how cash should be computed
    // this is bad, we are now only enforcing invarients
    public record TradeExecution(AssetSymbol asset, Quantity quantity, Price pricePerUnit) {
        public TradeExecution {
            notNull(asset, "Asset symbol cannot be null");
            notNull(quantity, "Quantity cannot be null");
            notNull(pricePerUnit, "Price per unit cannot be null");

            if (quantity.isZero()) {
                throw new IllegalArgumentException("Trade quantity cannot be zero");
            }

            if (pricePerUnit.pricePerUnit().isNegative()) {
                throw new IllegalArgumentException(
                        "Price per unit cannot be negative (got: " + pricePerUnit + ")");
            }
        }

        /**
         * Gross value of the trade before fees. This is qty × price,
         * representing the market value.
         */
        public Money grossValue() {
            return pricePerUnit.pricePerUnit().multiply(quantity.amount().abs());
        }
    }

    public record SplitDetails(double ratio) {
    }

    public record TransactionMetadata(
            AssetType assetType,
            String source,
            boolean excluded, // ← NEW
            Instant excludedAt, // ← NEW
            UserId excludedBy, // ← NEW
            String excludedReason, // ← NEW
            Map<String, String> additionalData) {

        public TransactionMetadata {
            notNull(assetType, "AssetType");
            source = source == null ? "UNKNOWN" : source.trim();
            additionalData = additionalData == null ? Map.of() : Map.copyOf(additionalData);

            // Validate exclusion consistency
            if (excluded && (excludedAt == null || excludedBy == null)) {
                throw new IllegalArgumentException("excludedAt and excludedBy required when excluded=true");
            }
            if (!excluded && (excludedAt != null || excludedBy != null || excludedReason != null)) {
                throw new IllegalArgumentException("Cannot have exclusion metadata when excluded=false");
            }
        }

        public static TransactionMetadata manual(AssetType assetType) {
            return new TransactionMetadata(assetType, "MANUAL", false, null, null, null, Map.of());
        }

        public static TransactionMetadata csvImport(AssetType assetType, String filename) {
            return new TransactionMetadata(assetType, "CSV_IMPORT", false, null, null, null,
                    Map.of("filename", filename));
        }

        // New method to mark as excluded
        public TransactionMetadata markAsExcluded(UserId userId, String reason) {
            if (excluded) {
                throw new IllegalStateException("Transaction already excluded");
            }
            return new TransactionMetadata(
                    assetType,
                    source,
                    true,
                    Instant.now(),
                    userId,
                    reason,
                    additionalData);
        }

        // New method to restore
        public TransactionMetadata restore() {
            if (!excluded) {
                throw new IllegalStateException("Transaction is not excluded");
            }
            return new TransactionMetadata(
                    assetType,
                    source,
                    false,
                    null,
                    null,
                    null,
                    additionalData);
        }

        public String get(String key) {
            return additionalData.get(key);
        }

        public String getOrDefault(String key, String defaultValue) {
            return additionalData.getOrDefault(key, defaultValue);
        }

        public TransactionMetadata with(String key, String value) {
            Map<String, String> copy = new HashMap<>(additionalData);
            copy.put(key, value);
            return new TransactionMetadata(assetType, source, excluded, excludedAt, excludedBy, excludedReason, copy);
        }

        public TransactionMetadata withAll(Map<String, String> additionalMetadata) {
            Map<String, String> copy = new HashMap<>(additionalData);
            copy.putAll(additionalMetadata);
            return new TransactionMetadata(assetType, source, excluded, excludedAt, excludedBy, excludedReason, copy);
        }

        public boolean containsKey(String key) {
            return additionalData.containsKey(key);
        }

        public boolean isEmpty() {
            return additionalData.isEmpty();
        }
    }
}