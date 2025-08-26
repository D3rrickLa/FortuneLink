package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.CurrencyMismatchException;

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

        if (!conversion.fromCurrency().equals(nativeAmount.currency())) {
            throw new CurrencyMismatchException("Conversion from-currency must match native amount currency.");  
        }
    }

    public static MonetaryAmount of(Money amount, CurrencyConversion conversion) {
        return new MonetaryAmount(amount, conversion);
    }

    // pure static factory
    public static MonetaryAmount of(Money nativeAmount, Currency portfolioCurrency, BigDecimal exchangeRate, Instant date) {
        if (nativeAmount.currency().equals(portfolioCurrency)) {
            return of(nativeAmount, CurrencyConversion.identity(portfolioCurrency));
        }
        
        CurrencyConversion conversion = new CurrencyConversion(
            nativeAmount.currency(),
            portfolioCurrency, 
            exchangeRate,
            date
        );
        return of(nativeAmount, conversion);
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
        if (!this.nativeAmount.currency().equals(other.nativeAmount.currency())) {
            throw new CurrencyMismatchException(
                String.format("Cannot add amounts with differenet native currencies: %s vs %s", this.nativeAmount.currency(), other.nativeAmount.currency()));
        }

        // this is technically wrong, the message, because we use this for conversions other that 'to-portfolio'
        if (!this.conversion.toCurrency().equals(other.conversion.toCurrency())) {
            throw new CurrencyMismatchException("Cannot add amounts with different portfolio currencies.");
        }

        Money newNative = this.nativeAmount.add(other.nativeAmount);
        // Use this conversion rate - assuming same date/rate for addition
        return new MonetaryAmount(newNative, this.conversion);
    }

    public MonetaryAmount subtract(MonetaryAmount other) {
        if (!this.nativeAmount.currency().equals(other.nativeAmount.currency())) {
            throw new IllegalArgumentException(
                "Cannot subtract amounts with different native currencies");
        }

        if (!this.conversion.toCurrency().equals(other.conversion.toCurrency())) {
            throw new IllegalArgumentException(
                "Cannot subtract amounts with different portfolio currencies");
        }
        
        Money newNative = this.nativeAmount.subtract(other.nativeAmount);
        return new MonetaryAmount(newNative, this.conversion);
    }

    public MonetaryAmount multiply(BigDecimal multiplier) {
        Objects.requireNonNull(multiplier, "Multiplier cannot be null.");
        return new MonetaryAmount(this.nativeAmount.multiply(multiplier), this.conversion);
    }

    

    public Money getPortfolioAmount() {
        return this.conversion.convert(this.nativeAmount);
    }

    public Money getConversionAmount() {
        return getPortfolioAmount();
    }

    public boolean isZero() {
        return nativeAmount.isZero();
    }

    public boolean isPositive() {
        return nativeAmount.isPositive();
    }
    
    public boolean isNegative() {
        return nativeAmount.isNegative();
    }
    
    public boolean isMultiCurrency() {
        return !nativeAmount.currency().equals(conversion.toCurrency());
    }

    // Absolute value
    public MonetaryAmount abs() {
        return new MonetaryAmount(nativeAmount.abs(), conversion);
    }

    // Negate - useful for expense transactions
    public MonetaryAmount negate() {
        return new MonetaryAmount(nativeAmount.negate(), conversion);
    }    
    
    
}
