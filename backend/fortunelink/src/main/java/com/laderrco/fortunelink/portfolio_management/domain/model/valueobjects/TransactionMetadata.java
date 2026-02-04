package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects;

import java.util.HashMap;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;

public record TransactionMetadata(AssetType assetType, String source, Map<String, String> values) {
    public TransactionMetadata {
        values = values == null ? Map.of() : Map.copyOf(values);
    }

    public static TransactionMetadata empty() {
        return new TransactionMetadata(null, null, Map.of());
    }

    public String get(String key) {
        return values.get(key);
    }

    public String getOrDefault(String key, String defaultValue) {
        return values.getOrDefault(key, defaultValue);
    }

    public TransactionMetadata with(String key, String value) {
        Map<String, String> copy = new HashMap<>(values);
        copy.put(key, value);
        return new TransactionMetadata(assetType, source, copy);
    }

    public TransactionMetadata withAll(Map<String, String> additionalMetadata) {
        Map<String, String> copy = new HashMap<>(values);
        copy.putAll(additionalMetadata);
        return new TransactionMetadata(assetType, source, copy);
    }

    public boolean containsKey(String key) {
        return values.containsKey(key);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}