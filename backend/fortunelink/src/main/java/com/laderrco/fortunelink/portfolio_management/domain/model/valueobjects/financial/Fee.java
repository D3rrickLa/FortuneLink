package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfolio_management.domain.model.ClassValidation;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.FeeType;

// VO probably shouldn’t implement the validator interface; just call validation statically.
public record Fee(
        FeeType feeType,
        Money amountInNativeCurrency,
        ExchangeRate exchangeRate,
        Map<String, String> metadata,
        Instant feeDate) implements ClassValidation {

    public Fee {
        ClassValidation.validateParameter(feeType, "Fee type is required");
        ClassValidation.validateParameter(amountInNativeCurrency, "Amount is required");
        ClassValidation.validateParameter(exchangeRate, "Exchange rate is required");
        ClassValidation.validateParameter(feeDate, "Fee date is required");

        if (amountInNativeCurrency.isNegative()) {
            throw new IllegalArgumentException("Fee amount cannot be negative");
        }

        // Normalize metadata
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (entry.getKey().isBlank() || entry.getValue().isBlank()) {
                throw new IllegalArgumentException("Metadata keys/values cannot be blank");
            }
        }
    }

    /**
     * Converts the fee to the target currency using the historical exchange rate.
     */
    public Money toBaseCurrency(Currency targetCurrency) {
        if (amountInNativeCurrency.currency().equals(targetCurrency)) {
            return amountInNativeCurrency;
        }
        if (!exchangeRate.to().equals(targetCurrency)) {
            throw new CurrencyMismatchException(
                    String.format("Exchange rate %s->%s cannot convert to target %s",
                            exchangeRate.from(), exchangeRate.to(), targetCurrency));
        }
        return exchangeRate.convert(amountInNativeCurrency);
    }

    /**
     * Applies the fee to a Money amount, converting if necessary.
     */
    public Money apply(Money baseAmount) {
        ClassValidation.validateParameter(baseAmount, "Base amount cannot be null");
        Money feeInBase = convertToBaseCurrency(baseAmount.currency());
        return baseAmount.subtract(feeInBase);
    }

    private Money convertToBaseCurrency(Currency targetCurrency) {
        if (amountInNativeCurrency.currency().equals(targetCurrency)) {
            return amountInNativeCurrency;
        }
        if (!exchangeRate.to().equals(targetCurrency)) {
            throw new CurrencyMismatchException(
                    String.format("Cannot convert fee from %s to %s", amountInNativeCurrency.currency(),
                            targetCurrency));
        }
        return exchangeRate.convert(amountInNativeCurrency);
    }

    /* -------------------- Builder -------------------- */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private FeeType feeType;
        private Money amountInNativeCurrency;
        private ExchangeRate exchangeRate;
        private Map<String, String> metadata;
        private Instant feeDate;

        private Builder() {
        }

        public Builder feeType(FeeType feeType) {
            this.feeType = feeType;
            return this;
        }

        public Builder amountInNativeCurrency(Money amount) {
            this.amountInNativeCurrency = amount;
            return this;
        }

        public Builder exchangeRate(ExchangeRate exchangeRate) {
            this.exchangeRate = exchangeRate;
            return this;
        }

        public Builder feeDate(Instant feeDate) {
            this.feeDate = feeDate;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder addMetadata(String key, String value) {
            if (this.metadata == null)
                this.metadata = new HashMap<>();
            this.metadata.put(key, value);
            return this;
        }

        public Fee build() {
            return new Fee(feeType, amountInNativeCurrency, exchangeRate, metadata, feeDate);
        }
    }
}