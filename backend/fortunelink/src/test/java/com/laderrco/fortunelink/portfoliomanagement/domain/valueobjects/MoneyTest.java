package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.DecimalPrecision;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.Rounding;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.CurrencyMismatchException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final int SCALE = DecimalPrecision.MONEY.getDecimalPlaces();

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        void ofBigDecimalCurrency() {
            Money m = Money.of(BigDecimal.valueOf(10.12345), USD);
            assertEquals(USD, m.currency());
            assertEquals(SCALE, m.amount().scale());
        }

        @Test
        void ofDoubleCurrency() {
            Money m = Money.of(10.25, USD);
            assertEquals(BigDecimal.valueOf(10.25).setScale(DecimalPrecision.MONEY.getDecimalPlaces(), Rounding.MONEY.getMode()), m.amount());
        }

        @Test
        void ofBigDecimalStringCurrency() {
            Money m = Money.of(BigDecimal.TEN, "USD");
            assertEquals(USD, m.currency());
        }
        
        @Test
        void ofDoubleStringCurrency() {
            Money m = Money.of(10.5, "USD");
            assertEquals(USD, m.currency());
        }

        @Test
        void zeroCurrency() {
            Money m = Money.ZERO(USD);
            assertEquals(BigDecimal.ZERO.setScale(SCALE, Rounding.MONEY.getMode()), m.amount());
        }

        @Test
        void zeroStringCurrency() {
            Money m = Money.ZERO("USD");
            assertEquals(USD, m.currency());
        }
    }

    @Nested
    @DisplayName("Arithmetic")
    class Arithmetic {

        @Test
        void addSameCurrency() {
            Money a = Money.of(10, USD);
            Money b = Money.of(5, USD);
            assertEquals(Money.of(15, USD), a.add(b));
        }

        @Test
        void addDifferentCurrencyThrows() {
            Money a = Money.of(10, USD);
            Money b = Money.of(5, EUR);
            assertThrows(CurrencyMismatchException.class, () -> a.add(b));
        }

        @Test
        void subtractSameCurrency() {
            Money a = Money.of(10, USD);
            Money b = Money.of(3, USD);
            assertEquals(Money.of(7, USD), a.subtract(b));
        }

        @Test
        void multiplyBigDecimal() {
            Money a = Money.of(10, USD);
            assertEquals(Money.of(20, USD), a.multiply(BigDecimal.valueOf(2)));
        }

        @Test
        void multiplyDouble() {
            Money a = Money.of(10, USD);
            assertEquals(Money.of(25, USD), a.multiply(2.5));
        }

        @Test
        void divideBigDecimal() {
            Money a = Money.of(10, USD);
            assertEquals(Money.of(5, USD), a.divided(BigDecimal.valueOf(2)));
        }

        @Test
        void divideDouble() {
            Money a = Money.of(10, USD);
            assertEquals(Money.of(2, USD), a.divided(5.0));
        }
    }

    @Nested
    @DisplayName("Conversion")
    class Conversion {

        @Test
        void convertToValidExchangeRate() {
            Money usd = Money.of(10, USD);
            CurrencyConversion rate = new CurrencyConversion(USD, EUR, BigDecimal.valueOf(2));
            Money eur = usd.convertTo(EUR, rate);
            assertEquals(Money.of(20, EUR), eur);
        }

        @Test
        void convertToMismatchedExchangeRateThrows() {
            Money usd = Money.of(10, USD);
            CurrencyConversion wrongRate = new CurrencyConversion(EUR, USD, BigDecimal.ONE);
            assertThrows(CurrencyMismatchException.class, () -> usd.convertTo(EUR, wrongRate));
        }
    }

    @Nested
    @DisplayName("Unary operations")
    class UnaryOps {

        @Test
        void negate() {
            Money a = Money.of(10, USD);
            assertEquals(Money.of(-10, USD), a.negate());
        }

        @Test
        void absPositive() {
            Money a = Money.of(10, USD);
            assertEquals(Money.of(10, USD), a.abs());
        }

        @Test
        void absNegative() {
            Money a = Money.of(-10, USD);
            assertEquals(Money.of(10, USD), a.abs());
        }
    }

    @Nested
    @DisplayName("Comparisons")
    class Comparisons {

        @Test
        void isPositive() {
            assertTrue(Money.of(1, USD).isPositive());
            assertFalse(Money.of(0, USD).isPositive());
        }

        @Test
        void isNegative() {
            assertTrue(Money.of(-1, USD).isNegative());
            assertFalse(Money.of(0, USD).isNegative());
        }

        @Test
        void isZero() {
            assertTrue(Money.of(0, USD).isZero());
            assertFalse(Money.of(1, USD).isZero());
        }

        @Test
        void compareToSameCurrency() {
            Money a = Money.of(10, USD);
            Money b = Money.of(20, USD);
            assertTrue(a.compareTo(b) < 0);
            assertTrue(b.compareTo(a) > 0);
            assertEquals(0, a.compareTo(Money.of(10, USD)));
        }

        @Test
        void compareToDifferentCurrencyThrows() {
            Money a = Money.of(10, USD);
            Money b = Money.of(10, EUR);
            assertThrows(CurrencyMismatchException.class, () -> a.compareTo(b));
        }

        @Test
        void minAndMax() {
            Money a = Money.of(10, USD);
            Money b = Money.of(20, USD);
            assertEquals(a, a.min(b));
            assertEquals(b, a.max(b));
        }

        @Test
        void greaterAndLessThan() {
            Money a = Money.of(10, USD);
            Money b = Money.of(20, USD);
            assertEquals(true, a.isLessThan(b));
            assertEquals(true, b.isGreaterThan(a));

            assertFalse(a.isGreaterThan(b));
            assertFalse(b.isLessThan(a));
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void nullAmountThrows() {
            assertThrows(NullPointerException.class, () -> new Money(null, USD));
        }

        @Test
        void nullCurrencyThrows() {
            assertThrows(NullPointerException.class, () -> new Money(BigDecimal.TEN, null));
        }

        @Test
        void arithmeticWithNullThrows() {
            Money a = Money.of(10, USD);
            assertThrows(NullPointerException.class, () -> a.add(null));
            assertThrows(NullPointerException.class, () -> a.subtract(null));
        }

        @Test
        void multiplyNullThrows() {
            Money a = Money.of(10, USD);
            assertThrows(NullPointerException.class, () -> a.multiply((BigDecimal) null));
        }

        @Test
        void divideNullThrows() {
            Money a = Money.of(10, USD);
            assertThrows(NullPointerException.class, () -> a.divided((BigDecimal) null));
        }

        @Test
        void convertToNullThrows() {
            Money a = Money.of(10, USD);
            assertThrows(NullPointerException.class, () -> a.convertTo(null, new CurrencyConversion(USD, EUR, BigDecimal.ONE, Instant.now())));
            assertThrows(NullPointerException.class, () -> a.convertTo(EUR, null));
        }

        @Test
        void convertTo_shouldThrowException_whenExchangeRateMismatch() {
            CurrencyConversion rate = new CurrencyConversion(EUR, USD, BigDecimal.valueOf(1.1), Instant.now());
            Money m1 = new Money(BigDecimal.valueOf(100), USD);
            assertThrows(CurrencyMismatchException.class, () -> m1.convertTo(EUR, rate));
            Money m2 = new Money(BigDecimal.valueOf(100), EUR);
            assertThrows(CurrencyMismatchException.class, () -> m2.convertTo(Currency.getInstance("CAD"), rate));
        }
    }
}
