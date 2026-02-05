package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.TransactionDate;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

public record Transaction(
        TransactionId transactionId,
        AccountId accountId,
        TransactionType transactionType,
        TradeExecution execution,
        Money cashDelta,
        List<Fee> fees,
        String notes,
        TransactionDate occurredAt,
        TransactionId relatedTransactionId,
        TransactionMetadata metadata

) implements ClassValidation {

    public record TradeExecution(AssetSymbol asset, Quantity quantity, Money pricePerUnit) {
        public TradeExecution {
            ClassValidation.validateParameter(asset, "Asset symbol cannot be null");
            ClassValidation.validateParameter(quantity, "Quantity cannot be null");
            ClassValidation.validateParameter(pricePerUnit, "Price per unit cannot be null");

            // Domain validation
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

    public record TransactionMetadata(AssetType assetType, String source, Map<String, String> additionalData) {
        public TransactionMetadata {
            if (assetType == null) {
                throw new IllegalArgumentException("Asset type cannot be null in transaction metadata");
            }
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