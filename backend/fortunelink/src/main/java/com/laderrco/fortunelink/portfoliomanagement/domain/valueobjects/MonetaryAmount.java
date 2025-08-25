package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Encapsualtes both native and portfolio values
 */
public record MonetaryAmount(
    Money nativeAmount,
    CurrencyConversion conversion
) {
    public MonetaryAmount {
        nativeAmount = Objects.requireNonNull(nativeAmount, "Native amount cannot be null.");
        conversion = Objects.requireNonNull(conversion, "Currency conversion cannot be null.");
    }

    public static MonetaryAmount of(Money amount, CurrencyConversion conversion) {
        return new MonetaryAmount(amount, conversion);
    }

    // Currency-specific zero factory method
    public static MonetaryAmount ZERO(Currency portfolioCurrency) {
        return MonetaryAmount.of(
            Money.ZERO(portfolioCurrency), 
            CurrencyConversion.identity(portfolioCurrency)
        );
    }
    
    // Or if you need different native and portfolio currencies
    public static MonetaryAmount ZERO(Currency nativeCurrency, Currency portfolioCurrency) {
        return MonetaryAmount.of(
            Money.ZERO(nativeCurrency),
            new CurrencyConversion(nativeCurrency, portfolioCurrency, BigDecimal.ONE)
        );
    }
    public MonetaryAmount add(MonetaryAmount other) {
        return new MonetaryAmount(this.nativeAmount.add(other.nativeAmount), this.conversion);
    }

    public MonetaryAmount multiply(BigDecimal multiplier) {
        return new MonetaryAmount(this.nativeAmount.multiply(multiplier), this.conversion);
    }

    public Money getPortfolioAmount() {
        return this.conversion.convert(this.nativeAmount);
    }

}
