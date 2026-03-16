package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record TransactionMetadata(
    AssetType assetType,
    String source,
    boolean excluded,
    Instant excludedAt,
    UserId excludedBy,
    String excludedReason,
    Map<String, String> additionalData) {
  public static final String KEY_SYMBOL = "symbol";

  public TransactionMetadata {
    notNull(assetType, "AssetType");
    source = source == null ? "UNKNOWN" : source.trim();
    additionalData = additionalData == null ? Map.of() : Map.copyOf(additionalData);

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

  public TransactionMetadata markAsExcluded(UserId userId, String reason) {
    if (excluded) {
      throw new IllegalStateException("Transaction already excluded");
    }
    return new TransactionMetadata(assetType, source, true, Instant.now(), userId, reason,
        additionalData);
  }

  public TransactionMetadata restore() {
    if (!excluded) {
      throw new IllegalStateException("Transaction is not excluded");
    }
    return new TransactionMetadata(assetType, source, false, null, null, excludedReason,
        additionalData);
  }

  public Map<String, String> asFlatMap() {
    Map<String, String> flat = new HashMap<>(additionalData);

    // Core fields
    flat.put("assetType", assetType.name());
    flat.put("source", source);
    flat.put("excluded", String.valueOf(excluded));

    // Conditional fields (only add if present to keep the view clean)
    if (excluded) {
      flat.put("excludedAt", excludedAt.toString());
      flat.put("excludedBy", excludedBy.id().toString());
      if (excludedReason != null) {
        flat.put("excludedReason", excludedReason);
      }
    }

    return Collections.unmodifiableMap(flat);
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
    return new TransactionMetadata(assetType, source, excluded, excludedAt, excludedBy,
        excludedReason, copy);
  }

  public TransactionMetadata withAll(Map<String, String> additionalMetadata) {
    Map<String, String> copy = new HashMap<>(additionalData);
    copy.putAll(additionalMetadata);
    return new TransactionMetadata(assetType, source, excluded, excludedAt, excludedBy,
        excludedReason, copy);
  }

  public boolean containsKey(String key) {
    return additionalData.containsKey(key);
  }

  public boolean isEmpty() {
    return additionalData.isEmpty();
  }
}