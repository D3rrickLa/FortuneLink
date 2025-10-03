package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.CurrencyAreTheSameException;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.Precision;

public record ExchangeRate(Currency fromCurrency, Currency toCurrency, BigDecimal rate, Instant exchangeDate) {
    private final static int FOREX_SCALE = Precision.FOREX.getDecimalPlaces();

    public ExchangeRate {
        validateParameter(fromCurrency, "fromCurrency");
        validateParameter(toCurrency, "toCurrency");
        validateParameter(rate, "rate");
        validateParameter(exchangeDate, "exchange date");

        if(fromCurrency.equals(toCurrency)) {
            throw new CurrencyAreTheSameException("'fromCurrency' and 'toCurrency' are the same");
        }

        rate = rate.setScale(FOREX_SCALE);
    }

    public static ExchangeRate create(String fromCurrency, String toCurrency, double rate, Instant date) {
        return new ExchangeRate(Currency.getInstance(fromCurrency), Currency.getInstance(toCurrency), BigDecimal.valueOf(rate), date);
    }

    public Money convertTo(Money other) {
        validateParameter(other, "money");
        if (isIdentity()) {
            return other;
        }
        else if(!other.currency().equals(this.toCurrency) && !other.currency().equals(this.fromCurrency)) {
            throw new IllegalArgumentException("Currency provided does not match either the `toCurrency` or `fromCurrency`");
        }

        return new Money(other.amount().multiply(this.rate), this.toCurrency);
    }

    public Money convertBack(Money other) {
        return null;
    }

    private void validateParameter(Object object, String variableName) {
        Objects.requireNonNull(object, String.format("%s cannot be null", variableName));
    }

    private boolean isIdentity() {
        return this.fromCurrency.equals(this.toCurrency) && this.rate.equals(BigDecimal.ONE.setScale(FOREX_SCALE));
    }
}
