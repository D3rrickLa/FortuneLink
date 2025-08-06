package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.DecimalPrecision;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    private final Currency USD = Currency.getInstance("USD");
    private final Currency EUR = Currency.getInstance("EUR");

    @Test
    void constructor_shouldThrowException_whenAmountIsNull() {
        assertThrows(NullPointerException.class, () -> new Money(null, USD));
    }

    @Test
    void constructor_shouldThrowException_whenCurrencyIsNull() {
        assertThrows(NullPointerException.class, () -> new Money(BigDecimal.ONE, null));
    }

    @Test
    void add_shouldReturnCorrectSum_whenSameCurrency() {
        Money m1 = new Money(BigDecimal.valueOf(100), USD);
        Money m2 = new Money(BigDecimal.valueOf(50), USD);
        Money result = m1.add(m2);
        assertEquals(BigDecimal.valueOf(150).setScale(DecimalPrecision.MONEY.getDecimalPlaces()), result.amount());
    }

    @Test
    void add_shouldThrowException_whenDifferentCurrency() {
        Money m1 = new Money(BigDecimal.valueOf(100), USD);
        Money m2 = new Money(BigDecimal.valueOf(50), EUR);
        assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
    }

    @Test
    void subtract_shouldReturnCorrectResult_whenSameCurrency() {
        Money m1 = new Money(BigDecimal.valueOf(100), USD);
        Money m2 = new Money(BigDecimal.valueOf(40), USD);
        Money result = m1.subtract(m2);
        assertEquals(BigDecimal.valueOf(60).setScale(DecimalPrecision.MONEY.getDecimalPlaces()), result.amount());
    }

    @Test
    void subtract_shouldThrowException_whenDifferentCurrency() {
        Money m1 = new Money(BigDecimal.valueOf(100), USD);
        Money m2 = new Money(BigDecimal.valueOf(40), EUR);
        assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));
    }

    @Test
    void multiply_shouldReturnScaledAmount() {
        Money money = new Money(BigDecimal.valueOf(20), USD);
        Money result = money.multiply(2.5);
        assertEquals(BigDecimal.valueOf(50.0).setScale(DecimalPrecision.MONEY.getDecimalPlaces()), result.amount());
    }

    @Test
    void divide_shouldThrowException_whenZeroDivisor() {
        Money money = new Money(BigDecimal.valueOf(100), USD);
        assertThrows(ArithmeticException.class, () -> money.divide(BigDecimal.ZERO));
    }
    
    @Test 
    void divide_shouldReturnAmount() {
        Money money = new Money(BigDecimal.valueOf(100), USD);
        Money dividedMoney = money.divide(2.0);
        assertEquals(Money.of(BigDecimal.valueOf(50).setScale(DecimalPrecision.MONEY.getDecimalPlaces()), USD).compareTo(dividedMoney), 0);
    }

    @Test
    void negate_shouldReturnNegativeAmount() {
        Money money = new Money(BigDecimal.valueOf(100), USD);
        Money result = money.negate();
        assertEquals(BigDecimal.valueOf(-100).setScale(DecimalPrecision.MONEY.getDecimalPlaces()), result.amount());
    }

    @Test
    void abs_shouldReturnPositiveAmount() {
        Money money = new Money(BigDecimal.valueOf(-100), USD);
        Money result = money.abs();
        assertEquals(BigDecimal.valueOf(100).setScale(DecimalPrecision.MONEY.getDecimalPlaces()), result.amount());
    }

    @Test
    void isZero_shouldReturnTrueForZero() {
        Money money = new Money(BigDecimal.ZERO, USD);
        assertTrue(money.isZero());
    }
    @Test

    void isZero_shouldReturnFalseForZero() {
        Money money = new Money(BigDecimal.TEN, USD);
        assertFalse(money.isZero());
    }

    @Test
    void isPositive_shouldReturnTrueForPositive() {
        Money money = new Money(BigDecimal.valueOf(10), USD);
        assertTrue(money.isPositive());
    }
    
    @Test 
    void isPositive_shouldReturnFalseForNegative() {
        Money money = new Money(BigDecimal.valueOf(-10), USD);
        assertFalse(money.isPositive());

    }

    @Test
    void isNegative_shouldReturnTrueForNegative() {
        Money money = new Money(BigDecimal.valueOf(-10), USD);
        assertTrue(money.isNegative());
    }
    
    @Test
    void isNegative_shouldReturnFalseForNegative() {
        Money money = new Money(BigDecimal.valueOf(10), USD);
        assertFalse(money.isNegative());
    }

    @Test
    void compareTo_shouldReturnPositive_whenGreaterThanOther() {
        Money m1 = new Money(BigDecimal.valueOf(100), USD);
        Money m2 = new Money(BigDecimal.valueOf(50), USD);
        assertTrue(m1.compareTo(m2) > 0);
    }

    @Test
    void compareTo_shouldThrowException_whenDifferentCurrency() {
        Money m1 = new Money(BigDecimal.valueOf(100), USD);
        Money m2 = new Money(BigDecimal.valueOf(50), EUR);
        assertThrows(IllegalArgumentException.class, () -> m1.compareTo(m2));
    }

    @Test
    void convertTo_shouldConvertCorrectly() {
        ExchangeRate rate = new ExchangeRate(USD, EUR, BigDecimal.valueOf(0.9), Instant.now());
        Money m1 = new Money(BigDecimal.valueOf(100), USD);
        Money converted = m1.convertTo(EUR, rate);
        assertEquals(BigDecimal.valueOf(90.0).setScale(DecimalPrecision.MONEY.getDecimalPlaces()), converted.amount());
        assertEquals(EUR, converted.currency());
    }

    @Test
    void convertTo_shouldThrowException_whenExchangeRateMismatch() {
        ExchangeRate rate = new ExchangeRate(EUR, USD, BigDecimal.valueOf(1.1), Instant.now());
        Money m1 = new Money(BigDecimal.valueOf(100), USD);
        assertThrows(IllegalArgumentException.class, () -> m1.convertTo(EUR, rate));
        Money m2 = new Money(BigDecimal.valueOf(100), EUR);
        assertThrows(IllegalArgumentException.class, () -> m2.convertTo(Currency.getInstance("CAD"), rate));
    }

    @Test
    void zeroFactory_shouldReturnMoneyWithZeroAmount() {
        Money zero = Money.ZERO(USD);
        assertEquals(BigDecimal.ZERO.setScale(DecimalPrecision.MONEY.getDecimalPlaces()), zero.normalizedForDisplay().amount());
    }

    @Test
    void ofFactory_shouldCreateMoneyWithCorrectAmount() {
        Money money = Money.of(123.45, USD);
        assertEquals(BigDecimal.valueOf(123.45).setScale(DecimalPrecision.MONEY.getDecimalPlaces()), money.amount());
    }
}