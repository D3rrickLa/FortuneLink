package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfolio_management.domain.model.ClassValidation;
import com.laderrco.fortunelink.portfolio_management.shared.enums.Precision;
import com.laderrco.fortunelink.portfolio_management.shared.enums.Rounding;

public record ExchangeRate(Currency from, Currency to, BigDecimal rate, Instant quotedAt) implements ClassValidation {
    private static final int SCALE = Precision.FOREX.getDecimalPlaces();
    private static final RoundingMode ROUNDING = Rounding.FOREX.getMode();

    public ExchangeRate {
        from = ClassValidation.validateParameter(from);
        to = ClassValidation.validateParameter(to);
        rate = ClassValidation.validateParameter(rate);
        quotedAt = ClassValidation.validateParameter(quotedAt);

        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }

        rate = rate.setScale(SCALE, ROUNDING);
    }

    public static ExchangeRate identity(Currency currency, Instant quotedAt) {
        return new ExchangeRate(currency, currency, BigDecimal.ONE, quotedAt);
    }

    public Money convert(Money money) {
        ClassValidation.validateParameter(money);

        if (!money.currency().equals(from)) {
            throw new CurrencyMismatchException(money.currency(), from);
        }

        return new Money(
                money.amount().multiply(rate),
                to);
    }

    public ExchangeRate inverse() {
        return new ExchangeRate(to, from, BigDecimal.ONE.divide(rate, SCALE, ROUNDING), quotedAt);
    }
}
