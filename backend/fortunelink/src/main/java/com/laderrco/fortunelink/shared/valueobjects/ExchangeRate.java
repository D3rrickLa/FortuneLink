package com.laderrco.fortunelink.shared.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

public record ExchangeRate(ValidatedCurrency from, ValidatedCurrency to, BigDecimal rate, Instant exchangeRateDate, String source) implements ClassValidation {
    private final static int FOREX_SCALE = Precision.FOREX.getDecimalPlaces();

    public ExchangeRate {
        from = ClassValidation.validateParameter(from);
        to = ClassValidation.validateParameter(to);
        rate = ClassValidation.validateParameter(rate);
        exchangeRateDate = ClassValidation.validateParameter(exchangeRateDate);


        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive."); // TODO: add better throw execeptions class
        }
        
        // we have a 'is identity check' we are allowing USD = USD
        // if (from.equals(to)) {
        //     throw new IllegalArgumentException("Cannot convert currency to itself.");
        // }

        if (rate.scale() < 6) {
            rate = rate.setScale(6, RoundingMode.HALF_UP);
        }

        rate = rate.setScale(FOREX_SCALE);
    }

        public static ExchangeRate create(String from, String to, double rate, Instant date, String source) {
        return new ExchangeRate(ValidatedCurrency.of(from), ValidatedCurrency.of(to), BigDecimal.valueOf(rate).setScale(FOREX_SCALE), date, source);
    }
    public static ExchangeRate create(ValidatedCurrency from, ValidatedCurrency to, double rate, Instant date, String source) {
        return new ExchangeRate(from, to, BigDecimal.valueOf(rate).setScale(FOREX_SCALE), date, source);
    }

    public Money convert(Money other) {
        ClassValidation.validateParameter(other);

        if (isIdentity()) {
            return other;
        }
        else if(!other.currency().equals(this.from)) {
            throw new IllegalArgumentException("Currency provided does not match `from`");
        }

        return new Money(other.amount().multiply(this.rate), this.to);
    }

    public Money invert(Money other) {
        ClassValidation.validateParameter(other);
        
        if (isIdentity()) {
            return other;
        }
        else if(!other.currency().equals(this.to)) {
            throw new IllegalArgumentException("Currency provided does not match `to`");
        }

        BigDecimal nativeAmount = other.amount().divide(this.rate, this.to.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
        return new Money(nativeAmount, this.from);
    }


    private boolean isIdentity() {
        return this.from.equals(this.to) && this.rate.equals(BigDecimal.ONE.setScale(FOREX_SCALE));
    }
}
