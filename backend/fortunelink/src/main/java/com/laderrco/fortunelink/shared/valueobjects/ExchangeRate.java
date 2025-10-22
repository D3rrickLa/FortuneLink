package com.laderrco.fortunelink.shared.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.shared.enums.Currency;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.exception.CurrencyAreTheSameException;
import com.laderrco.fortunelink.shared.exception.CurrencyMismatchException;

public record ExchangeRate(Currency from, Currency to, BigDecimal rate, Instant exchangeRateDate) {
    private final static int FOREX_SCALE = Precision.FOREX.getDecimalPlaces();

    public ExchangeRate {
        validateParameter(from, "from");
        validateParameter(to, "to");
        validateParameter(rate, "rate");
        validateParameter(exchangeRateDate, "exchange date");

        // TODO find out if this is needed or not, because we have the case where we want to say 1 = 1
        if(from.equals(to)) {
            throw new CurrencyAreTheSameException("'from' and 'to' are the same");
        }

        rate = rate.setScale(FOREX_SCALE);
    }

    public static ExchangeRate create(String from, String to, double rate, Instant date) {
        return new ExchangeRate(Currency.of(from), Currency.of(to), BigDecimal.valueOf(rate).setScale(FOREX_SCALE), date);
    }
    public static ExchangeRate create(Currency from, Currency to, double rate, Instant date) {
        return new ExchangeRate(from, to, BigDecimal.valueOf(rate).setScale(FOREX_SCALE), date);
    }

    public Money convertTo(Money other) {
        validateParameter(other, "money");
        if (isIdentity()) {
            return other;
        }
        else if(!other.currency().equals(this.from)) {
            throw new CurrencyMismatchException("Currency provided does not match `from`");
        }

        return new Money(other.amount().multiply(this.rate), this.to);
    }

    public Money convertBack(Money other) {
        validateParameter(other, "money");
        if (isIdentity()) {
            return other;
        }
        else if(!other.currency().equals(this.to)) {
            throw new IllegalArgumentException("Currency provided does not match `to`");
        }

        BigDecimal nativeAmount = other.amount().divide(this.rate, this.to.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
        return new Money(nativeAmount, this.from);
    }

    private void validateParameter(Object object, String variableName) {
        Objects.requireNonNull(object, String.format("%s cannot be null", variableName));
    }

    private boolean isIdentity() {
        return this.from.equals(this.to) && this.rate.equals(BigDecimal.ONE.setScale(FOREX_SCALE));
    }
} 
