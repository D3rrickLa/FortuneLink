package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidQuantityException;

// NOTE: a lot of the logic depends on fee holding the portfolio's native currency in MonetaryAmount.conversion
public record Fee(
    FeeType type, 
    MonetaryAmount amount, // Native currency → Portfolio currency conversion
    String description,
    Map<String, String> metadata, // optional, can be empty/null
    Instant time
) {
    public Fee {
        validateParameter(type, "Type");
        validateParameter(amount, "Amount");
        validateParameter(description, "Description");
        validateParameter(time, "Time");

        // this can't trigger because of the check in MonetaryAmount
        // update ^ this will now trigger, MontaryAmount shouldn't dicate what amount is good or bad, what if we get refunded??? 
        if (amount.nativeAmount().amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidQuantityException("Fee amount cannot be negative.");
        }

        if (description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank.");
        }

        metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
    }

    // --- Builder --- //
    public static class Builder {
        private FeeType type;
        private MonetaryAmount amount;
        private String description;
        private Map<String, String> metadata = Collections.emptyMap();
        private Instant time = Instant.now(); // default

        public Builder type(FeeType type) {
            this.type = type;
            return this;
        }

        public Builder amount(MonetaryAmount amount) {
            this.amount = amount;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder time(Instant time) {
            this.time = time;
            return this;
        }

        public Fee build() {
            return new Fee(type, amount, description, metadata, time);
        }
    }

    // Optional static factory for convenience
    public static Builder builder() {
        return new Builder();
    }

    private static void validateParameter(Object other, String parameterName) {
        Objects.requireNonNull(other, String.format("%s cannot be null.", parameterName));
    }
}
