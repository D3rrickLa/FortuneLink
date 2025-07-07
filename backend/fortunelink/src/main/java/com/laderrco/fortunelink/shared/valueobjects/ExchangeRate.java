package com.laderrco.fortunelink.shared.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

// rate is always from -> to (i.e. CAD -> USD. So 1 CAD is 0.72 USD)
public record ExchangeRate(Currency fromCurrency, Currency toCurrency, BigDecimal rate, Instant rateDate, String source) {
    public ExchangeRate {
        Objects.requireNonNull(fromCurrency, "Curreny converting from cannot be null.");
        Objects.requireNonNull(toCurrency, "Curreny converting to cannot be null.");
        Objects.requireNonNull(rate, "Exchange rate cannot be null.");
        Objects.requireNonNull(rateDate, "Exchange rate date cannot be null.");

        // an exchagne rate of 0 means the currency has no value
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive.");
        }
        
        if (fromCurrency.equals(toCurrency)) {
            throw new IllegalArgumentException("Cannot convert currency to itself.");
        }

        if (rate.scale() < 6) {
            rate = rate.setScale(6, RoundingMode.HALF_UP);
        }
    }

    public ExchangeRate getInverseRate() {
        BigDecimal inverseRate = BigDecimal.ONE.divide(this.rate, 6, RoundingMode.HALF_UP);
        return new ExchangeRate(this.toCurrency, this.fromCurrency, inverseRate, this.rateDate, this.source);
    }

    // self life for an exchange rate
    public boolean isExpired() {
        return rateDate.isBefore(Instant.now().minus(Duration.ofHours(24)));
    }

    public boolean isExpired(Duration maxAge) {
        return rateDate.isBefore(Instant.now().minus(maxAge));
    }
}
