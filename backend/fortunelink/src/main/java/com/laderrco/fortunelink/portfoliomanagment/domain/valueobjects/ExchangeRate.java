package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.DecimalPrecision;

public record ExchangeRate(
    Currency fromCurrency, 
    Currency toCurrency,
    BigDecimal exchangeRate,
    Instant exchangeRateDate
) {
    private final static int FOREX_SCALE = DecimalPrecision.FOREX.getDecimalPlaces();
    public ExchangeRate {
        Objects.requireNonNull(fromCurrency, "From Currency cannot be null.");
        Objects.requireNonNull(toCurrency, "To Currency cannot be null.");
        Objects.requireNonNull(exchangeRate, "Exchange Rate cannot be null.");
        Objects.requireNonNull(exchangeRateDate, "Exchange Rate Date cannot be null.");

        if (fromCurrency.equals(toCurrency)) {
            throw new IllegalArgumentException("Cannot convert currency to itself.");
        }

        if (exchangeRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Exchange rate must be positive.");
        }

        if (exchangeRate.scale() != FOREX_SCALE) {
            exchangeRate = exchangeRate.setScale(FOREX_SCALE, RoundingMode.HALF_UP);
        }
    }

    public ExchangeRate getInverseRate() {
        BigDecimal inverseRate = BigDecimal.ONE.divide(this.exchangeRate, FOREX_SCALE, RoundingMode.HALF_UP);
        return new ExchangeRate(this.toCurrency, this.fromCurrency, inverseRate, this.exchangeRateDate);
    }

    public Boolean isExpired() {
        return exchangeRateDate.isBefore(Instant.now().minus(Duration.ofHours(24)));
    }
    
    public Boolean isExpired(Duration maxAge) {
        return exchangeRateDate.isBefore(Instant.now().minus(maxAge));
    }
}