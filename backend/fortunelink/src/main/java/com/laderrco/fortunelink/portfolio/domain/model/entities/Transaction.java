package com.laderrco.fortunelink.portfolio.domain.model.entities;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.CashImpact;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Ratio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;

/**
 * Represents an immutable record of a financial event within an account.
 * <p>
 * This is the "source of truth" used to reconstruct account balances and portfolio positions. It
 * enforces strict invariants between the transaction type, trade execution details, and the
 * resulting impact on cash (cashDelta).
 * </p>
 *
 * @param transactionId        Unique identifier for this transaction.
 * @param accountId            The account to which this transaction belongs.
 * @param transactionType      The nature of the event (e.g., BUY, SELL, DIVIDEND, SPLIT).
 * @param execution            Details of the asset trade (required for trade-based types).
 * @param split                Ratio details (required for stock splits).
 * @param cashDelta            The net change in account cash ('+' for inflows, '-' for outflows).
 * @param fees                 A list of charges associated with this transaction.
 * @param notes                User or system-generated remarks.
 * @param occurredAt           The date and time the transaction took place.
 * @param relatedTransactionId Reference to another transaction (e.g., for reversals or linked
 *                             trades).
 * @param metadata             Audit data, source tracking, and exclusion status.
 */
@Builder
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
    TransactionMetadata metadata) {
  public Transaction {
    notNull(transactionId, "transactionId");
    notNull(accountId, "accountId");
    notNull(transactionType, "transactionType");
    notNull(cashDelta, "cashDelta");
    notNull(fees, "fees");
    notNull(metadata, "metadata");
    notNull(occurredAt, "occurredAt");
    notNull(notes, "notes");

    validateConsistency("execution", transactionType.requiresExecution(), execution != null);
    validateConsistency("split details", transactionType.requiresSplitDetails(), split != null);

    fees = List.copyOf(fees);
    notes = notes.trim();

    if (transactionType.cashImpact() == CashImpact.NONE && !cashDelta.isZero()) {
      throw new IllegalArgumentException(transactionType + " cannot affect cash");
    }

    if (transactionType.requiresExecution()) {
      validateTradeConsistency(execution, transactionType, cashDelta, fees);
    } else if (!fees.isEmpty()) {
      throw new IllegalArgumentException(transactionType + " cannot have fees");
    }
  }

  /**
   * Creates a copy of this transaction marked as excluded from portfolio calculations.
   */
  public Transaction markAsExcluded(UserId userId, String reason) {
    return new Transaction(transactionId, accountId, transactionType, execution, split, cashDelta,
        fees, notes, occurredAt, relatedTransactionId, metadata.markAsExcluded(userId, reason));
  }

  /**
   * Restores an excluded transaction to an active state.
   */
  public Transaction restore() {
    return new Transaction(transactionId, accountId, transactionType, execution, split, cashDelta,
        fees, notes, occurredAt, relatedTransactionId, metadata.restore());
  }

  public boolean isExcluded() {
    return metadata.excluded();
  }

  /**
   * Sums all fees associated with this transaction in the currency of the cashDelta.
   */
  public Money totalFeesInAccountCurrency() {
    return Fee.totalInAccountCurrency(fees, cashDelta.currency());
  }

  private void validateConsistency(String label, boolean isRequired, boolean isPresent) {
    if (isRequired && !isPresent) {
      throw new IllegalArgumentException(transactionType + " requires " + label);
    }
    if (!isRequired && isPresent) {
      throw new IllegalArgumentException(transactionType + " cannot have " + label);
    }
  }

  private void validateTradeConsistency(TradeExecution execution, TransactionType type,
      Money cashDelta, List<Fee> fees) {
    Money grossValue = execution.grossValue();
    Money totalFees = Fee.totalInAccountCurrency(fees, cashDelta.currency());

    Money expectedCashDelta = switch (type.cashImpact()) {
      case IN -> grossValue.subtract(totalFees);
      case OUT -> grossValue.add(totalFees).negate();
      case NONE -> Money.zero(cashDelta.currency());
    };

    if (!cashDelta.equals(expectedCashDelta)) {
      throw new IllegalArgumentException(
          "Cash delta mismatch. Expected: " + expectedCashDelta + ", got: " + cashDelta);
    }
  }

  public record TradeExecution(AssetSymbol asset, Quantity quantity, Price pricePerUnit) {
    public TradeExecution {
      notNull(asset, "Asset symbol cannot be null");
      notNull(quantity, "Quantity cannot be null");
      notNull(pricePerUnit, "Price per unit cannot be null");

      if (quantity.isZero()) {
        throw new IllegalArgumentException("Trade quantity cannot be zero");
      }
    }

    public Money grossValue() {
      // Gross value of the trade before fees.
      return pricePerUnit.pricePerUnit().multiply(quantity.amount().abs());
    }
  }

  public record SplitDetails(Ratio ratio) {
  }

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
      return new TransactionMetadata(assetType, source, false, null, null, null, additionalData);
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

  public record TransactionDate(Instant timestamp) {
    public TransactionDate {
      notNull(timestamp, "timestamp");
    }

    public static TransactionDate of(Instant timestamp) {
      return new TransactionDate(timestamp);
    }

    public static TransactionDate now() {
      return new TransactionDate(Instant.now());
    }

    public boolean isBefore(TransactionDate other) {
      return timestamp.isBefore(other.timestamp());
    }

    public boolean isAfter(TransactionDate other) {
      return timestamp.isAfter(other.timestamp());
    }

    public long toEpochMilli() {
      return timestamp.toEpochMilli();
    }
  }
}
