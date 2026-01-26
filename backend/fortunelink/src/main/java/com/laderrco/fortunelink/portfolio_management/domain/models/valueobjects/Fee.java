package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.FeeType;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.shared.exceptions.InvalidQuantityException;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public record Fee(FeeType feeType, Money amountInNativeCurrency, ExchangeRate exchangeRate, Map<String, String> metadata, Instant feeDate) implements ClassValidation {
    public Fee {
        feeType = ClassValidation.validateParameter(feeType, "Fee Type");
        amountInNativeCurrency = ClassValidation.validateParameter(amountInNativeCurrency, "Amount In Native Currency");
        exchangeRate = ClassValidation.validateParameter(exchangeRate, "Exchange Rate");
        feeDate = ClassValidation.validateParameter(feeDate, "Fee Date");

        if (amountInNativeCurrency.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidQuantityException("Fee amount cannot be negative.");
        }

        // normalize metadata
        metadata = metadata == null ? Map.of() : metadata;
        if (!metadata.isEmpty()) {
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key.isBlank() || value.isBlank()) {
                    throw new IllegalArgumentException("Metadata has empty properties in it, please remove");
                }
            }
        }

        metadata = Collections.unmodifiableMap(metadata);
    }

    /**
     * Converts the fee to the target currency using the HISTORICAL exchange rate
     * stored at the time of the transaction.
     * 
     * This ensures ACB and tax calculations use the correct historical rate,
     * not current market rates.
     */
    public Money toBaseCurrency(ValidatedCurrency targetCurrency) {
        if (amountInNativeCurrency.currency().equals(targetCurrency)) {
            return amountInNativeCurrency;
        }

        if (!exchangeRate.to().equals(targetCurrency)) {
            throw new CurrencyMismatchException(
                    String.format("Fee exchange rate (%s->%s) doesn't match target currency %s",
                            exchangeRate.from(), exchangeRate.to(), targetCurrency));
        }

        return exchangeRate.convert(amountInNativeCurrency);
    }

    public Money apply(Money baseAmount) {
        baseAmount = ClassValidation.validateParameter(baseAmount);
        if (!amountInNativeCurrency.currency().equals(baseAmount.currency())) {
            if (!exchangeRate.to().equals(baseAmount.currency())) {
                throw new CurrencyMismatchException(
                        "Fees cannot be subtracted forom base currency. Fees are not the same currency and or unknown exchange rate");
            } else {
                return baseAmount.subtract(exchangeRate.convert(amountInNativeCurrency));
            }
        } else {
            return baseAmount.subtract(amountInNativeCurrency);
        }
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

        public Builder amountInNativeCurrency(Money amountInNativeCurrency) {
            this.amountInNativeCurrency = amountInNativeCurrency;
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

        /**
         * Replaces metadata entirely.
         */
        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Adds a single metadata entry.
         */
        public Builder addMetadata(String key, String value) {
            if (this.metadata == null) {
                this.metadata = new HashMap<>();
            }
            this.metadata.put(key, value);
            return this;
        }

        public Fee build() {
            // Let the record constructor enforce all invariants
            return new Fee(
                    feeType,
                    amountInNativeCurrency,
                    exchangeRate,
                    metadata,
                    feeDate);
        }
    }
}
