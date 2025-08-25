package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Encapsualtes both native and portfolio values
 * 
 * --- EXAMPLES ---
 * 
 * // European stock dividend received in EUR, portfolio in CAD
    MonetaryAmount dividend = MonetaryAmount.of(
        Money.of(100.00, EUR),                        // Native: what you received
        CurrencyConversion.of(1.45, EUR, CAD)         // 1 EUR = 1.45 CAD
    );
    // Native: €100.00
    // Portfolio: CAD $145.00

    // US brokerage fee charged in USD, portfolio in CAD  
    MonetaryAmount fee = MonetaryAmount.of(
        Money.of(9.95, USD),                          // Native: what you were charged
        CurrencyConversion.of(1.35, USD, CAD)         // 1 USD = 1.35 CAD
    );
    // Native: $9.95 USD
 *  // Portfolio: CAD $13.43
 * 
 */
public record MonetaryAmount(
    Money nativeAmount, // what was actually charged/paid
    CurrencyConversion conversion // native -> portfolio conversion
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

    public Money getConversionAmount() {
        return this.conversion.convert(this.nativeAmount);
    }
}
