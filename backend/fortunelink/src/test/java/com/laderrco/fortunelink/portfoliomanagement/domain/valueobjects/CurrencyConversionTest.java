package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;


import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.ExchangeRateGeneralException;

public class CurrencyConversionTest {
 private static final int FOREX_SCALE = 6; // assume DecimalPrecision.FOREX.getDecimalPlaces() = 6

    @Test
    void constructor_shouldCreateExchangeRateSuccessfully() {
        Instant now = Instant.now();
        CurrencyConversion rate = new CurrencyConversion(
                Currency.getInstance("USD"),
                Currency.getInstance("EUR"),
                new BigDecimal("1.234567"),
                now
        );

        assertEquals(Currency.getInstance("USD"), rate.fromCurrency());
        assertEquals(Currency.getInstance("EUR"), rate.toCurrency());
        assertEquals(new BigDecimal("1.234567"), rate.exchangeRate());
        assertEquals(now, rate.exchangeRateDate());
    }

    @Test
    void constructor_negativeRate_shouldThrow() {
        assertThrows(ExchangeRateGeneralException.class, () -> {
            new CurrencyConversion(
                    Currency.getInstance("USD"),
                    Currency.getInstance("EUR"),
                    new BigDecimal("-1.0"),
                    Instant.now()
            );
        });
    }

    @Test
    void constructor_wrongScale_shouldRound() {
        BigDecimal rateValue = new BigDecimal("1.23456789"); // more digits than FOREX_SCALE
        CurrencyConversion er = new CurrencyConversion(
                Currency.getInstance("USD"),
                Currency.getInstance("EUR"),
                rateValue,
                Instant.now()
        );

        assertEquals(rateValue.setScale(FOREX_SCALE, RoundingMode.HALF_UP), er.exchangeRate());
    }

    @Test
    void constructor_exactScale_shouldKeepValue() {
        BigDecimal rateValue = new BigDecimal("1.234567"); // exactly FOREX_SCALE
        CurrencyConversion er = new CurrencyConversion(
                Currency.getInstance("USD"),
                Currency.getInstance("EUR"),
                rateValue,
                Instant.now()
        );

        assertEquals(rateValue, er.exchangeRate());
    }

    @Test
    void constructor_nullFromCurrency_shouldThrow() {
        assertThrows(NullPointerException.class, () -> {
            new CurrencyConversion(
                    null,
                    Currency.getInstance("EUR"),
                    BigDecimal.ONE,
                    Instant.now()
            );
        });
    }

    @Test
    void constructor_nullToCurrency_shouldThrow() {
        assertThrows(NullPointerException.class, () -> {
            new CurrencyConversion(
                    Currency.getInstance("USD"),
                    null,
                    BigDecimal.ONE,
                    Instant.now()
            );
        });
    }

    @Test
    void constructor_nullExchangeRate_shouldThrow() {
        assertThrows(NullPointerException.class, () -> {
            new CurrencyConversion(
                    Currency.getInstance("USD"),
                    Currency.getInstance("EUR"),
                    null,
                    Instant.now()
            );
        });
    }

    @Test
    void constructor_nullExchangeRateDate_shouldThrow() {
        assertThrows(NullPointerException.class, () -> {
            new CurrencyConversion(
                    Currency.getInstance("USD"),
                    Currency.getInstance("EUR"),
                    BigDecimal.ONE,
                    null
            );
        });
    }

    @Test
    void convenienceConstructor_shouldSetExchangeRateDateToNow() {
        CurrencyConversion rate = new CurrencyConversion(
                Currency.getInstance("USD"),
                Currency.getInstance("EUR"),
                new BigDecimal("1.234567")
        );

        assertEquals(Currency.getInstance("USD"), rate.fromCurrency());
        assertEquals(Currency.getInstance("EUR"), rate.toCurrency());
        assertEquals(new BigDecimal("1.234567"), rate.exchangeRate());
        assertNotNull(rate.exchangeRateDate());
    }

    @Test
    void factoryMethod_of_shouldCreateCorrectExchangeRate() {
        Instant now = Instant.now();
        CurrencyConversion rate = CurrencyConversion.of("USD", "EUR", 1.234567, now);

        assertEquals(Currency.getInstance("USD"), rate.fromCurrency());
        assertEquals(Currency.getInstance("EUR"), rate.toCurrency());
        assertEquals(BigDecimal.valueOf(1.234567).setScale(FOREX_SCALE, RoundingMode.HALF_UP), rate.exchangeRate());
        assertEquals(now, rate.exchangeRateDate());
    }

    @Test
    void factoryMethod_identityWithCurrrencyParameter() {
        CurrencyConversion conversion = CurrencyConversion.identity(Currency.getInstance("USD"));
        assertEquals(Currency.getInstance("USD"), conversion.toCurrency());
        assertEquals(BigDecimal.ONE.setScale(FOREX_SCALE), conversion.exchangeRate());        
    }

    @Test
    void factoryMethod_identity() {
        CurrencyConversion conversion = CurrencyConversion.identity("USD");
        assertEquals(Currency.getInstance("USD"), conversion.toCurrency());
        assertEquals(BigDecimal.ONE.setScale(FOREX_SCALE), conversion.exchangeRate());
    }

    @Test
    void test_Convert_IsIdentityTrue() {
        CurrencyConversion rate = new CurrencyConversion(
            Currency.getInstance("USD"),
            Currency.getInstance("USD"),
            BigDecimal.ONE,
            Instant.now()
        );
        Money usdMoney = Money.of(340, Currency.getInstance("USD"));
        Money converted = rate.convert(usdMoney);
        assertEquals(usdMoney, converted);
    }

    @Test
    void test_Convert_IsConverted() {
        CurrencyConversion rate = new CurrencyConversion(
            Currency.getInstance("USD"),
            Currency.getInstance("EUR"),
            new BigDecimal("1.234567"),
            Instant.now()
        );
        Money usdMoney = Money.of(100, Currency.getInstance("USD"));
        Money converted = rate.convert(usdMoney);
        assertEquals(converted, Money.of(123.4567, Currency.getInstance("EUR")));
    }

    @Test
    void test_ConvertBack_IsValid() {
        CurrencyConversion rate = new CurrencyConversion(
            Currency.getInstance("USD"), // from 
            Currency.getInstance("EUR"), // to
            new BigDecimal("1.234567"),
            Instant.now()
        );
        Money eurMoney = Money.of(100, Currency.getInstance("EUR"));
        Money convertedEUR = rate.convertBack(eurMoney);
        assertEquals(Money.of(81.0000, Currency.getInstance("USD")), convertedEUR);        
    }

    @Test
    void test_ConvertBack_ThrowsError() {
        CurrencyConversion rate = new CurrencyConversion(
            Currency.getInstance("USD"),
            Currency.getInstance("EUR"),
            new BigDecimal("1.234567"),
            Instant.now()
        );
        Money usdMoney = Money.of(100, Currency.getInstance("USD"));        
        assertThrows(IllegalArgumentException.class, ()-> rate.convertBack(usdMoney));
    }

    @Test
    void testIsIdentityTrue() {
        CurrencyConversion exchange = new CurrencyConversion("USD", "USD", BigDecimal.ONE, Instant.now());
        assertTrue(exchange.isIdentity(), "Should return true when currencies match and exchange rate is 1");
    }

    @Test
    void testIsIdentityFalseDifferentCurrency() {
        CurrencyConversion exchange = new CurrencyConversion("USD", "CAD", BigDecimal.ONE, Instant.now());
        assertFalse(exchange.isIdentity(), "Should return false when currencies differ");
    }

    @Test
    void testIsIdentityFalseDifferentRate() {
        CurrencyConversion exchange = new CurrencyConversion("USD", "USD", new BigDecimal("1.25"), Instant.now());
        assertFalse(exchange.isIdentity(), "Should return false when exchange rate is not 1");
    }
}
