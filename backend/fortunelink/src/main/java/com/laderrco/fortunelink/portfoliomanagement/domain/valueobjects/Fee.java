package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidQuantityException;

public record Fee(
    FeeType type, 
    Money amount,
    String description,
    Instant time
) {
    public Fee {
        validateParameter(type, "Type");
        validateParameter(amount, "Amount");
        validateParameter(description, "Description");
        validateParameter(time, "Time");

        if (amount.amount().compareTo(BigDecimal.ZERO) < 0) { // we can and should be able to r
            throw new InvalidQuantityException("Fee amount cannot be negative.");
        }

        if (description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank.");
        }
    }

    // --- Builder --- //
    public static class Builder {
        private FeeType type;
        private Money amount;
        private String description;
        private Instant time = Instant.now(); // default

        public Builder type(FeeType type) {
            this.type = type;
            return this;
        }

        public Builder amount(Money amount) {
            this.amount = amount;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder time(Instant time) {
            this.time = time;
            return this;
        }

        public Fee build() {
            return new Fee(type, amount, description, time);
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
