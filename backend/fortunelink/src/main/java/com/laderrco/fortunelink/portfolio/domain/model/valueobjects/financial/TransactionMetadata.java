package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Carries auditing information, source tracking, and exclusion status for a
 * transaction.
 * <p>
 * This record uses an optional {@link ExclusionRecord} to handle the lifecycle
 * of transactions that
 * are hidden from portfolio performance or balance calculations.
 * </p>
 *
 * @param assetType      The category of asset (STOCK, CRYPTO, etc.).
 * @param source         The origin of the data (e.g., "MANUAL", "CSV_IMPORT",
 *                       "API").
 * @param exclusion      Details regarding why and when this transaction was
 *                       excluded, if
 *                       applicable.
 * @param additionalData Extensible key-value pairs for vendor-specific or
 *                       custom identifiers.
 */
public record TransactionMetadata(
    AssetType assetType,
    String source,
    ExclusionRecord exclusion,
    Map<String, String> additionalData) {
  public static final String KEY_SYMBOL = "symbol";
  public static final String KEY_FEE_TYPE = "feeType";

  public TransactionMetadata {
    notNull(assetType, "AssetType");
    source = (source == null) ? "UNKNOWN" : source.trim();
    additionalData = (additionalData == null) ? Map.of() : Map.copyOf(additionalData);
  }

  public static TransactionMetadata manual(AssetType assetType) {
    return new TransactionMetadata(assetType, "MANUAL", null, Map.of());
  }

  public static TransactionMetadata csvImport(AssetType assetType, String filename) {
    return new TransactionMetadata(assetType, "CSV_IMPORT", null, Map.of("filename", filename));
  }

  /**
   * Checks if the transaction is currently excluded from calculations.
   */
  public boolean isExcluded() {
    return exclusion != null;
  }

  /**
   * Marks the transaction as excluded. * @throws IllegalStateException if the
   * transaction is
   * already excluded.
   */
  public TransactionMetadata markAsExcluded(UserId userId, String reason) {
    if (isExcluded()) {
      throw new IllegalStateException("Transaction is already excluded");
    }
    return new TransactionMetadata(assetType, source,
        new ExclusionRecord(Instant.now(), userId, reason), additionalData);
  }

  /**
   * Restores a transaction to an active state by removing the exclusion record.
   * * @throws
   * IllegalStateException if the transaction is not currently excluded.
   */
  public TransactionMetadata restore() {
    if (!isExcluded()) {
      throw new IllegalStateException("Transaction is not excluded");
    }
    return new TransactionMetadata(assetType, source, null, additionalData);
  }

  /**
   * Flattens the metadata into a single-level map for UI display or logging.
   */
  public Map<String, String> asFlatMap() {
    Map<String, String> flat = new HashMap<>(additionalData);
    flat.put("assetType", assetType.name());
    flat.put("source", source);
    flat.put("excluded", String.valueOf(isExcluded()));

    if (exclusion != null) {
      flat.put("excludedAt", exclusion.occurredAt().toString());
      flat.put("excludedBy", exclusion.by().id().toString());
      if (exclusion.reason() != null) {
        flat.put("excludedReason", exclusion.reason());
      }
    }
    return Collections.unmodifiableMap(flat);
  }

  public String get(String key) {
    return additionalData.get(key);
  }

  public boolean containsKey(String key) {
    return additionalData.containsKey(key);
  }

  public boolean isEmpty() {
    return additionalData.isEmpty();
  }

  // --- Wither Methods (Immutable updates) ---

  public TransactionMetadata with(String key, String value) {
    Map<String, String> copy = new HashMap<>(additionalData);
    copy.put(key, value);
    return new TransactionMetadata(assetType, source, exclusion, copy);
  }

  public TransactionMetadata withAll(Map<String, String> additionalMetadata) {
    Map<String, String> copy = new HashMap<>(additionalData);
    copy.putAll(additionalMetadata);
    return new TransactionMetadata(assetType, source, exclusion, copy);
  }

  /**
   * Audit record representing the "Who, When, and Why" of a transaction's
   * exclusion.
   */
  public record ExclusionRecord(Instant occurredAt, UserId by, String reason) {
    public ExclusionRecord {
      notNull(occurredAt, "Exclusion timestamp");
      notNull(by, "User performing exclusion");
    }
  }
}