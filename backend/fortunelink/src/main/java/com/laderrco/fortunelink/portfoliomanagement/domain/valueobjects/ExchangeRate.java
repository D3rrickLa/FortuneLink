package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.DecimalPrecision;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.ExchangeRateGeneralException;

public record ExchangeRate(
    Currency fromCurrency,
    Currency toCurrency,
    BigDecimal exchangeRate,
    Instant exchangeRateDate
) {
    private final static int FOREX_SCALE = DecimalPrecision.FOREX.getDecimalPlaces();

    public ExchangeRate {
        validateParameter(fromCurrency, "From currency");
        validateParameter(toCurrency, "To currency");
        validateParameter(exchangeRate, "Exchange rate");
        validateParameter(exchangeRateDate, "Exchange rate date");
        
        if (fromCurrency.equals(toCurrency)) {
            throw new CurrencyMismatchException("Cannot convert currency to itself.");
        }

        if (exchangeRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new ExchangeRateGeneralException("Exchange rate must be positive.");
        }

        if (exchangeRate.scale() != FOREX_SCALE) {
            exchangeRate = exchangeRate.setScale(FOREX_SCALE, RoundingMode.HALF_UP);
        }
    }

    public ExchangeRate(Currency fromCurrency, Currency toCurrency, BigDecimal exchangeRate) {
        this(fromCurrency, toCurrency, exchangeRate, Instant.now());
    }

    public static ExchangeRate of(String fromCurrency, String toCurrency, double exchangeRate, Instant date) {
        return new ExchangeRate(Currency.getInstance(fromCurrency), Currency.getInstance(toCurrency), BigDecimal.valueOf(exchangeRate), date);
    }

    /**
     * No instance state, therefore static
     * @param other
     * @param parameterName
     */
    private static  void validateParameter(Object other, String parameterName) {
        Objects.requireNonNull(other, String.format("%s cannot be null.", parameterName));
    }
}