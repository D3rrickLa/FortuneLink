package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.FeeType;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

/*
so this this should handle almost all scenario, for exmaple
CAD Account, USD purchase, but charged EUR fee. the 'nativeAmount'
in this case is EUR and we would want to store the conversion of EUR to CAD.
We care about broker's impact. If a transaction was in USD,
*/
public record Fee(
        FeeType feeType,
        Money nativeAmount,
        Money accountAmount, // optional, what left the account if a different currency
        ExchangeRate appliedRate, // optional
        Instant occurredAt,
        FeeMetadata metadata

) implements ClassValidation {
    public Fee {
        ClassValidation.validateParameter(feeType, "Fee type cannot be null");
        ClassValidation.validateParameter(nativeAmount, "Native amount cannot be null");
        ClassValidation.validateParameter(occurredAt, "Occurred at cannot be null");
        ClassValidation.validateParameter(metadata, "Metadata at cannot be null");

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
        return new Fee(feeType, nativeAmount, accountAmount, appliedRate, occurredAt, new FeeMetadata(Map.of()));
    }

    public static Fee ZERO(Currency currency, Instant occurredAt) {
        return new Fee(FeeType.NONE, Money.ZERO(currency), null, null, occurredAt, new FeeMetadata(Map.of()));
    }

    /**
     * @return the amount in the original (broker) currency.
     */
    public Money amountInNativeCurrency() {
        return nativeAmount;
    }

    /**
     * @return the amount in the transaction/account currency.
     *         May be null if no conversion.
     */
    public Money amountInTransactionCurrency() {
        return accountAmount != null ? accountAmount : nativeAmount;
    }

    public boolean isMultiCurrency() {
        return accountAmount != null && !nativeAmount.currency().equals(accountAmount.currency());
    }

    public String toAuditString() {
        if (isMultiCurrency()) {
            return String.format("%s: %s (native) = %s (transaction) @ rate %s",
                    feeType, nativeAmount, accountAmount, appliedRate);
        } else {
            return String.format("%s: %s", feeType, nativeAmount);
        }
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
