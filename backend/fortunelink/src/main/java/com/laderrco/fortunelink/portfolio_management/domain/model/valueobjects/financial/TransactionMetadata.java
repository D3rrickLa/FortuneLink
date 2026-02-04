package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import java.util.HashMap;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;

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
        return new TransactionMetadata(
                assetType,
                "CSV_IMPORT",
                Map.of("filename", filename));
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