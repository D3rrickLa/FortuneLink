package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.portfolio.domain.model.enums.FeeType;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a service charge or transaction fee applied to an asset or
 * account. This record
 * supports multi-currency scenarios by tracking both the fee in its original
 * (native) currency and
 * its converted value in the account's base currency.
 *
 * @param feeType       The classification of the fee (e.g., COMMISSION, TAX,
 *                      SPREAD).
 * @param nativeAmount  The fee amount in the currency it was originally
 *                      charged.
 * @param accountAmount The fee amount converted into the user's account
 *                      currency.
 * @param exchangeRate  The specific rate used to convert nativeAmount to
 *                      accountAmount.
 * @param occurredAt    The timestamp of when the fee was incurred.
 * @param metadata      Additional key-value pairs for audit trails or
 *                      vendor-specific data.
 */
public record Fee(
    FeeType feeType,
    Money nativeAmount,
    Money accountAmount,
    ExchangeRate exchangeRate,
    Instant occurredAt,
    FeeMetadata metadata) {
  public Fee {
    notNull(feeType, "Fee type");
    notNull(nativeAmount, "Native amount");
    notNull(occurredAt, "Occurred at");
    notNull(metadata, "Metadata at");

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
    return new Fee(feeType, nativeAmount, accountAmount, appliedRate, occurredAt,
        new FeeMetadata(Map.of()));
  }

  public static Fee zero(Currency currency) {
    return new Fee(FeeType.NONE, Money.zero(currency), null, null, Instant.now(),
        new FeeMetadata(Map.of()));
  }

  /**
   * Calculates the total sum of a list of fees in the target account currency.
   * <p>
   * This method prioritizes {@code accountAmount}. If a fee only has a
   * {@code nativeAmount}, it
   * will be used only if its currency matches the target {@code accountCurrency}.
   * </p>
   *
   * @param fees            The list of fees to sum (null-safe).
   * @param accountCurrency The target currency for the total.
   * @return The total sum as a {@link Money} object.
   * @throws IllegalArgumentException if a fee's currency does not match the
   *                                  account currency.
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
                + " - ensure accountAmount is set for cross-currency fees");
      }
      total = total.add(amount);
    }
    return total;
  }

  // Ensure the convertedAmount currency actually matches the rate's target
  // currency
  public Fee withAccountAmount(Money convertedAmount, ExchangeRate rate) {
    return new Fee(this.feeType, this.nativeAmount, convertedAmount, rate, this.occurredAt, this.metadata);
  }

  /**
   * Extensible container for supplementary fee information. Used for storing
   * vendor IDs, tax codes,
   * or transaction references.
   */
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