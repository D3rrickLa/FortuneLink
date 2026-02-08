package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.CashImpact;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.TransactionDate;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

// THIS IS AN IMMUTABLE STATE
// accoutn and portfolio reconstruct state from these
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

) implements ClassValidation {
    public Transaction {
        ClassValidation.validateParameter(transactionId);
        ClassValidation.validateParameter(accountId);
        ClassValidation.validateParameter(transactionType);
        ClassValidation.validateParameter(cashDelta);
        ClassValidation.validateParameter(fees);
        ClassValidation.validateParameter(occurredAt);
        ClassValidation.validateParameter(notes);

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

    public Money totalFeesInAccountCurrency() {
        Currency currency = cashDelta.currency();
        return fees.stream().map(Fee::accountAmount)
                .reduce(Money.ZERO(currency), Money::add);
    }

    private void validateConsistency(String label, boolean isRequired, boolean isPresent) {
        if (isRequired && !isPresent) {
            throw new IllegalArgumentException(transactionType + " requires " + label);
        }
        if (!isRequired && isPresent) {
            throw new IllegalArgumentException(transactionType + " cannot have " + label);
        }
    }

    private void validateTradeConsistency(TradeExecution execution, TransactionType type, Money cashDelta, List<Fee> fees) {
        Money grossValue = execution.grossValue();
        Money totalFees = fees.stream()
                .map(Fee::accountAmount)
                .reduce(Money.ZERO(cashDelta.currency()), Money::add);

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
    public record TradeExecution(AssetSymbol asset, Quantity quantity, Money pricePerUnit) {
        public TradeExecution {
            ClassValidation.validateParameter(asset, "Asset symbol cannot be null");
            ClassValidation.validateParameter(quantity, "Quantity cannot be null");
            ClassValidation.validateParameter(pricePerUnit, "Price per unit cannot be null");

            if (quantity.isZero()) {
                throw new IllegalArgumentException("Trade quantity cannot be zero");
            }

            if (pricePerUnit.isNegative()) {
                throw new IllegalArgumentException(
                        "Price per unit cannot be negative (got: " + pricePerUnit + ")");
            }
        }

        /**
         * Gross value of the trade before fees. This is qty × price,
         * representing the market value.
         */
        public Money grossValue() {
            return pricePerUnit.multiply(quantity.amount().abs());
        }
    }

    public record SplitDetails(double ratio) {
    }

    public record TransactionMetadata(AssetType assetType, String source, Map<String, String> additionalData) {
        public TransactionMetadata {
            ClassValidation.validateParameter(assetType, "AssetType");
            source = source == null ? "UNKNOWN" : source.trim();
            additionalData = additionalData == null ? Map.of() : Map.copyOf(additionalData);
        }

        public static TransactionMetadata manual(AssetType assetType) {
            return new TransactionMetadata(assetType, "MANUAL", Map.of());
        }

        public static TransactionMetadata csvImport(AssetType assetType, String filename) {
            return new TransactionMetadata(assetType, "CSV_IMPORT", Map.of("filename", filename));
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
            return new TransactionMetadata(assetType, source, copy);
        }

        public TransactionMetadata withAll(Map<String, String> additionalMetadata) {
            Map<String, String> copy = new HashMap<>(additionalData);
            copy.putAll(additionalMetadata);
            return new TransactionMetadata(assetType, source, copy);
        }

        public boolean containsKey(String key) {
            return additionalData.containsKey(key);
        }

        public boolean isEmpty() {
            return additionalData.isEmpty();
        }
    }
}