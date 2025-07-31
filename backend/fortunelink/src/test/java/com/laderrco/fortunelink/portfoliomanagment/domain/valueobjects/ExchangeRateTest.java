package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.DecimalPrecision;

public class ExchangeRateTest {
        private final Currency USD = Currency.getInstance("USD");
    private final Currency EUR = Currency.getInstance("EUR");

    private final Instant validDate = Instant.now().minus(Duration.ofHours(1));
    private final BigDecimal validRate = BigDecimal.valueOf(1.234567);

    @Test
    void constructor_shouldCreateValidExchangeRate() {
        ExchangeRate rate = new ExchangeRate(USD, EUR, validRate, validDate);
        assertEquals(USD, rate.fromCurrency());
        assertEquals(EUR, rate.toCurrency());
        assertEquals(validRate.setScale(6), rate.exchangeRate());
        assertEquals(validDate, rate.exchangeRateDate());
    }

    @Test
    void constructor_shouldThrowException_whenFromCurrencyIsNull() {
        assertThrows(NullPointerException.class, () ->
            new ExchangeRate(null, EUR, validRate, validDate)
        );
    }

    @Test
    void constructor_shouldThrowException_whenToCurrencyIsNull() {
        assertThrows(NullPointerException.class, () ->
            new ExchangeRate(USD, null, validRate, validDate)
        );
    }

    @Test
    void constructor_shouldThrowException_whenRateIsNull() {
        assertThrows(NullPointerException.class, () ->
            new ExchangeRate(USD, EUR, null, validDate)
        );
    }

    @Test
    void constructor_shouldThrowException_whenDateIsNull() {
        assertThrows(NullPointerException.class, () ->
            new ExchangeRate(USD, EUR, validRate, null)
        );
    }

    @Test
    void constructor_shouldThrowException_whenRateIsNegative() {
        BigDecimal negativeRate = BigDecimal.valueOf(-0.75);
        assertThrows(IllegalArgumentException.class, () ->
            new ExchangeRate(USD, EUR, negativeRate, validDate)
        );
    }

    @Test
    void constructor_shouldThrowException_whenCurrenciesAreSame() {
        assertThrows(IllegalArgumentException.class, () ->
            new ExchangeRate(USD, USD, validRate, validDate)
        );
    }

    @Test
    void constructor_shouldRoundScale_whenRateHasIncorrectScale() {
        BigDecimal unscaledRate = BigDecimal.valueOf(1.23); // Scale is 2
        ExchangeRate rate = new ExchangeRate(USD, EUR, unscaledRate, validDate);
        assertEquals(6, rate.exchangeRate().scale());
    }

    @Test
    void getInverseRate_shouldReturnCorrectReversedExchangeRate() {
        ExchangeRate rate = new ExchangeRate(USD, EUR, BigDecimal.valueOf(2.000000), validDate);
        ExchangeRate inverse = rate.getInverseRate();
        assertEquals(EUR, inverse.fromCurrency());
        assertEquals(USD, inverse.toCurrency());
        assertEquals(BigDecimal.valueOf(0.500000).setScale(DecimalPrecision.FOREX.getDecimalPlaces()), inverse.exchangeRate());
        assertEquals(validDate, inverse.exchangeRateDate());
    }

    @Test
    void isExpired_shouldReturnFalse_whenWithin24Hours() {
        ExchangeRate rate = new ExchangeRate(USD, EUR, validRate, Instant.now().minus(Duration.ofHours(2)));
        assertFalse(rate.isExpired());
    }

    @Test
    void isExpired_shouldReturnTrue_whenOlderThan24Hours() {
        ExchangeRate rate = new ExchangeRate(USD, EUR, validRate, Instant.now().minus(Duration.ofHours(25)));
        assertTrue(rate.isExpired());
    }

    @Test
    void isExpiredWithDuration_shouldReturnTrue_whenOlderThanDuration() {
        ExchangeRate rate = new ExchangeRate(USD, EUR, validRate, Instant.now().minus(Duration.ofHours(3)));
        assertTrue(rate.isExpired(Duration.ofHours(2)));
    }

    @Test
    void isExpiredWithDuration_shouldReturnFalse_whenWithinDuration() {
        ExchangeRate rate = new ExchangeRate(USD, EUR, validRate, Instant.now().minus(Duration.ofMinutes(30)));
        assertFalse(rate.isExpired(Duration.ofHours(2)));
    }
    
}
