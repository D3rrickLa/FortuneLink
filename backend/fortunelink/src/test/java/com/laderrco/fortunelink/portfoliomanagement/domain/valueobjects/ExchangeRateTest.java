package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;


import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.ExchangeRateGeneralException;

public class ExchangeRateTest {
 private static final int FOREX_SCALE = 6; // assume DecimalPrecision.FOREX.getDecimalPlaces() = 6

    @Test
    void constructor_shouldCreateExchangeRateSuccessfully() {
        Instant now = Instant.now();
        ExchangeRate rate = new ExchangeRate(
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
    void constructor_sameCurrency_shouldThrow() {
        assertThrows(CurrencyMismatchException.class, () -> {
            new ExchangeRate(
                    Currency.getInstance("USD"),
                    Currency.getInstance("USD"),
                    BigDecimal.ONE,
                    Instant.now()
            );
        });
    }

    @Test
    void constructor_negativeRate_shouldThrow() {
        assertThrows(ExchangeRateGeneralException.class, () -> {
            new ExchangeRate(
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
        ExchangeRate er = new ExchangeRate(
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
        ExchangeRate er = new ExchangeRate(
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
            new ExchangeRate(
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
            new ExchangeRate(
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
            new ExchangeRate(
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
            new ExchangeRate(
                    Currency.getInstance("USD"),
                    Currency.getInstance("EUR"),
                    BigDecimal.ONE,
                    null
            );
        });
    }

    @Test
    void convenienceConstructor_shouldSetExchangeRateDateToNow() {
        ExchangeRate rate = new ExchangeRate(
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
        ExchangeRate rate = ExchangeRate.of("USD", "EUR", 1.234567, now);

        assertEquals(Currency.getInstance("USD"), rate.fromCurrency());
        assertEquals(Currency.getInstance("EUR"), rate.toCurrency());
        assertEquals(BigDecimal.valueOf(1.234567).setScale(FOREX_SCALE, RoundingMode.HALF_UP), rate.exchangeRate());
        assertEquals(now, rate.exchangeRateDate());
    }
}
