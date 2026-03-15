package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.portfolio.domain.model.enums.FeeType;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record Fee(FeeType feeType, Money nativeAmount, Money accountAmount,
                  ExchangeRate exchangeRate, Instant occurredAt, FeeMetadata metadata) {
  public Fee {
    notNull(feeType, "Fee type cannot be null");
    notNull(nativeAmount, "Native amount cannot be null");
    notNull(occurredAt, "Occurred at cannot be null");
    notNull(metadata, "Metadata at cannot be null");

    if (nativeAmount.isNegative()) {
      throw new IllegalArgumentException("Fee amount cannot be negative");
    }
  }

  public static Fee of(FeeType feeType, Money amount, Instant occurredAt) {
    return new Fee(feeType, amount, null, null, occurredAt, new FeeMetadata(Map.of()));
  }

  public static Fee of(FeeType feeType, Money amount, Instant occurredAt, FeeMetadata metadata) {
    return new Fee(feeType, amount, null, null, occurredAt, metadata);
  }

  public static Fee withConversion(FeeType feeType, Money nativeAmount, Money accountAmount,
      ExchangeRate appliedRate, Instant occurredAt) {
    return new Fee(
        feeType, nativeAmount, accountAmount, appliedRate, occurredAt, new FeeMetadata(Map.of()));
  }

  public static Fee zero(Currency currency) {
    return new Fee(
        FeeType.NONE, Money.zero(currency), null, null, Instant.now(), new FeeMetadata(Map.of()));
  }

  /**
   * Sum fees in account currency. Prefers accountAmount, falls back to nativeAmount
   * <p>
   * only if no conversion was applied (same currency). Null-safe on the list.
   */
  public static Money totalInAccountCurrency(List<Fee> fees, Currency accountCurrency) {
    if (fees == null || fees.isEmpty()) {
      return Money.zero(accountCurrency);
    }
    Money total = Money.zero(accountCurrency);
    for (Fee fee : fees) {
      Money amount = fee.accountAmount() != null ? fee.accountAmount() : fee.nativeAmount();
      if (!amount.currency().equals(accountCurrency)) {
        throw new IllegalArgumentException(
            "Fee currency mismatch: expected " + accountCurrency + ", got " + amount.currency()
                + " — ensure accountAmount is set for cross-currency fees");
      }
      total = total.add(amount);
    }
    return total;
  }

  public record FeeMetadata(Map<String, String> values) {
    public FeeMetadata {
      values = values == null ? Map.of() : Map.copyOf(values);
    }

    public String get(String key) {
      return values.get(key);
    }

    public String getOrDefault(String key, String defaultValue) {
      return values.getOrDefault(key, defaultValue);
    }

    public FeeMetadata with(String key, String value) {
      Map<String, String> copy = new HashMap<>(values);
      copy.put(key, value);
      return new FeeMetadata(copy);
    }

    public FeeMetadata withAll(Map<String, String> additionalMetadata) {
      Map<String, String> copy = new HashMap<>(values);
      copy.putAll(additionalMetadata);
      return new FeeMetadata(copy);
    }

    public boolean containsKey(String key) {
      return values.containsKey(key);
    }

    public boolean isEmpty() {
      return values.isEmpty();
    }
  }
}