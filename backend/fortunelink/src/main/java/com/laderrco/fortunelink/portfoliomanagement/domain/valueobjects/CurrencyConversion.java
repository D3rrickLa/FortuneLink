package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.DecimalPrecision;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.ExchangeRateGeneralException;

public record CurrencyConversion(
    Currency fromCurrency,
    Currency toCurrency,
    BigDecimal exchangeRate,
    Instant exchangeRateDate
) {
    private final static int FOREX_SCALE = DecimalPrecision.FOREX.getDecimalPlaces();

    public CurrencyConversion {
        validateParameter(fromCurrency, "From currency");
        validateParameter(toCurrency, "To currency");
        validateParameter(exchangeRate, "Exchange rate");
        validateParameter(exchangeRateDate, "Exchange rate date");
        
        // NOT NEEDED ANYMOTE, BUSINESS RULES CHANGED
        // if (fromCurrency.equals(toCurrency)) {
        //     throw new CurrencyMismatchException("Cannot convert currency to itself.");
        // }

        if (exchangeRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new ExchangeRateGeneralException("Exchange rate must be positive.");
        }

        if (exchangeRate.scale() != FOREX_SCALE) {
            exchangeRate = exchangeRate.setScale(FOREX_SCALE, RoundingMode.HALF_UP);
        }
    }

    public CurrencyConversion(String fromCurrency, String toCurrency, BigDecimal exchangeRate, Instant exchangeRateDate) {
        this(Currency.getInstance(fromCurrency), Currency.getInstance(toCurrency), exchangeRate, exchangeRateDate);
    }

    public CurrencyConversion(Currency fromCurrency, Currency toCurrency, BigDecimal exchangeRate) {
        this(fromCurrency, toCurrency, exchangeRate, Instant.now());
    }

    public static CurrencyConversion of(String fromCurrency, String toCurrency, double exchangeRate, Instant date) {
        return new CurrencyConversion(Currency.getInstance(fromCurrency), Currency.getInstance(toCurrency), BigDecimal.valueOf(exchangeRate), date);
    }

    public static CurrencyConversion identity(Currency currency) {
        return new CurrencyConversion(currency, currency, BigDecimal.ONE, Instant.now());
    }

    public static CurrencyConversion identity(String currency) {
        return new CurrencyConversion(Currency.getInstance(currency), Currency.getInstance(currency), BigDecimal.ONE, Instant.now());
    }

    public Money convert(Money amount) {
        amount = Objects.requireNonNull(amount, "Amount cannot be null.");
        if (isIdentity()) {
            return amount;
        }
        return new Money(amount.amount().multiply(this.exchangeRate), this.toCurrency);
    }

    public Money convertBack(Money amount) {
        if (!amount.currency().equals(toCurrency)) {
            throw new IllegalArgumentException("Amount currency does not match exchange rate toCurrency");
        }
        BigDecimal nativeAmount = amount.amount().divide(this.exchangeRate, toCurrency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        return Money.of(nativeAmount, fromCurrency);
    }
    
    public boolean isIdentity() {
        return fromCurrency.equals(toCurrency) && this.exchangeRate.equals(BigDecimal.ONE.setScale(FOREX_SCALE));
    }

    /**
     * No instance state, therefore static
     * @param other
     * @param parameterName
     */
    private static void validateParameter(Object other, String parameterName) {
        Objects.requireNonNull(other, String.format("%s cannot be null.", parameterName));
    }
}