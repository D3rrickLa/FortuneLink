package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.laderrco.fortunelink.portfolio.domain.model.enums.FeeType;

public record Fee(FeeType feeType, Money nativeAmount, Money accountAmount, ExchangeRate exchangeRate, Instant occurredAt, FeeMetadata metadata) {
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
        return new Fee(feeType, nativeAmount, accountAmount, appliedRate, occurredAt, new FeeMetadata(Map.of()));
    }

    public static Fee ZERO(Currency currency) {
        return new Fee(FeeType.NONE, Money.ZERO(currency), null, null, Instant.now(), new FeeMetadata(Map.of()));
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