package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.DecimalPrecision;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.CurrencyMismatchException;

class MonetaryAmountTest {

    Currency USD = Currency.getInstance("USD");
    Currency CAD = Currency.getInstance("CAD");
    Currency EUR = Currency.getInstance("EUR");
    Instant now = Instant.now();

    Money usd100 = Money.of(new BigDecimal("100.00"), USD);
    Money usd50 = Money.of(new BigDecimal("50.00"), USD);
    Money cad100 = Money.of(new BigDecimal("100.00"), CAD);

    CurrencyConversion usdToCad = new CurrencyConversion(USD, CAD, new BigDecimal("1.25"), now);
    CurrencyConversion cadToUsd = new CurrencyConversion(CAD, USD, new BigDecimal("0.75"), now);
    CurrencyConversion cadToEur = new CurrencyConversion(CAD, EUR, new BigDecimal("0.62"), now);
    CurrencyConversion identityCad = CurrencyConversion.identity(CAD);

    @Test
    void constructor_shouldThrowIfNativeCurrencyMismatch() {
        Money money = Money.of(BigDecimal.TEN, CAD);
        CurrencyConversion conversion = new CurrencyConversion(USD, CAD, BigDecimal.ONE, now);

        Exception ex = assertThrows(CurrencyMismatchException.class, () ->
            new MonetaryAmount(money, conversion)
        );
        assertEquals("Conversion from-currency must match native amount currency.", ex.getMessage());
    }

    @Test
    void factory_of_shouldCreateValidAmount() {
        MonetaryAmount amount = MonetaryAmount.of(usd100, usdToCad);
        assertEquals(usd100, amount.nativeAmount());
        assertEquals(CAD, amount.conversion().toCurrency());
    }

    @Test
    // this uses the pure static factory
    void factory_of_withSameCurrency_shouldUseIdentityConversion() {
        MonetaryAmount amount = MonetaryAmount.of(usd100, USD, BigDecimal.ONE, now);
        assertEquals(USD, amount.conversion().toCurrency());
        assertEquals(BigDecimal.ONE.setScale(DecimalPrecision.FOREX.getDecimalPlaces()), amount.conversion().exchangeRate());
    }
    @Test
    void factory_of_withSameCurrency_shouldCreateNewObjects() {
        MonetaryAmount amount = MonetaryAmount.of(usd100, CAD, BigDecimal.valueOf(1.25), now);
        assertEquals(CAD, amount.conversion().toCurrency());
        assertEquals(BigDecimal.valueOf(1.25).setScale(DecimalPrecision.FOREX.getDecimalPlaces()), amount.conversion().exchangeRate());
    }

    @Test
    void factory_ZERO_singleCurrency_shouldCreateZeroAmount() {
        MonetaryAmount zero = MonetaryAmount.ZERO(CAD);
        assertTrue(zero.isZero());
        assertEquals(CAD, zero.nativeAmount().currency());
        assertEquals(CAD, zero.conversion().toCurrency());
    }

    @Test
    void factory_ZERO_dualCurrency_shouldCreateZeroAmountWithConversion() {
        MonetaryAmount zero = MonetaryAmount.ZERO(USD, CAD);
        assertTrue(zero.isZero());
        assertEquals(USD, zero.nativeAmount().currency());
        assertEquals(CAD, zero.conversion().toCurrency());
    }

    @Test
    void add_shouldSucceedWithMatchingCurrencies() {
        MonetaryAmount a = MonetaryAmount.of(usd100, usdToCad);
        MonetaryAmount b = MonetaryAmount.of(usd50, usdToCad);

        MonetaryAmount result = a.add(b);
        assertEquals(new BigDecimal("150.00").setScale(DecimalPrecision.MONEY.getDecimalPlaces()), result.nativeAmount().amount());
    }

    @Test
    void add_shouldFailWithDifferentNativeCurrencies() {
        MonetaryAmount a = MonetaryAmount.of(usd100, usdToCad);
        MonetaryAmount b = MonetaryAmount.of(cad100, cadToUsd);

        assertThrows(CurrencyMismatchException.class, () -> a.add(b));
    }

    @Test
    void add_shouldFailWithDifferentPortfolioCurrencies() {
        MonetaryAmount a = MonetaryAmount.of(usd100, usdToCad);
        MonetaryAmount b = MonetaryAmount.of(usd50, CurrencyConversion.identity(USD));

        assertThrows(CurrencyMismatchException.class, () -> a.add(b));
    }

    @Test
    void subtract_shouldSucceedWithMatchingCurrencies() {
        MonetaryAmount a = MonetaryAmount.of(usd100, usdToCad);
        MonetaryAmount b = MonetaryAmount.of(usd50, usdToCad);

        MonetaryAmount result = a.subtract(b);
        assertEquals(new BigDecimal("50.00").setScale(DecimalPrecision.MONEY.getDecimalPlaces()), result.nativeAmount().amount());
    }

    @Test
    void subtract_shouldFailWithDifferentCurrencies() {
        MonetaryAmount a = MonetaryAmount.of(usd100, usdToCad);
        MonetaryAmount b = MonetaryAmount.of(cad100, cadToUsd);

        assertThrows(IllegalArgumentException.class, () -> a.subtract(b));
    }

    @Test
    void subtract_shouldFailWithDIfferentToCurrencies() {
        MonetaryAmount a = MonetaryAmount.of(cad100, cadToUsd);
        MonetaryAmount b = MonetaryAmount.of(cad100, cadToEur);

        assertThrows(IllegalArgumentException.class, () -> a.subtract(b));
    }

    @Test
    void multiply_shouldScaleAmount() {
        MonetaryAmount a = MonetaryAmount.of(usd100, usdToCad);
        MonetaryAmount result = a.multiply(new BigDecimal("2.5"));

        assertEquals(new BigDecimal("250.00").setScale(DecimalPrecision.MONEY.getDecimalPlaces()), result.nativeAmount().amount());
    }

    @Test
    void multiply_shouldThrowOnNullMultiplier() {
        MonetaryAmount a = MonetaryAmount.of(usd100, usdToCad);
        assertThrows(NullPointerException.class, () -> a.multiply(null));
    }

    @Test
    void getPortfolioAmount_shouldConvertCorrectly() {
        MonetaryAmount a = MonetaryAmount.of(usd100, usdToCad);
        Money converted = a.getPortfolioAmount();

        assertEquals(new BigDecimal("125.00").setScale(DecimalPrecision.MONEY.getDecimalPlaces()), converted.amount());
        assertEquals(CAD, converted.currency());
        assertEquals(CAD, a.getConversionAmount().currency());
    }

    @Test
    void utilityMethods_shouldBehaveCorrectly() {
        MonetaryAmount a = MonetaryAmount.of(usd100, usdToCad);
        assertFalse(a.isZero());
        assertTrue(a.isPositive());
        assertFalse(a.isNegative());
        assertTrue(a.isMultiCurrency());

        MonetaryAmount negated = a.negate();
        assertEquals(new BigDecimal("-100.00").setScale(DecimalPrecision.MONEY.getDecimalPlaces()), negated.nativeAmount().amount());

        MonetaryAmount abs = negated.abs();
        assertEquals(new BigDecimal("100.00").setScale(DecimalPrecision.MONEY.getDecimalPlaces()), abs.nativeAmount().amount());
        MonetaryAmount b = MonetaryAmount.of(usd100, CurrencyConversion.identity(USD));
        assertFalse(b.isMultiCurrency());
        
    }
}
