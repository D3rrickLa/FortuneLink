package com.laderrco.fortunelink.shared.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExchangeRateTest {
    Currency cad;
    Currency usd;
    BigDecimal defaultRate;

    @BeforeEach
    void init() {
        cad = Currency.getInstance("CAD");
        usd = Currency.getInstance("USD");
        defaultRate = new BigDecimal("0.72");
    }

    @Test 
    void testConstructorValid() {
        Instant rateDate = Instant.now();
        String source = "source";
        ExchangeRate exchangeRate = new ExchangeRate(cad, usd, defaultRate, rateDate, source);
        assertNotNull(exchangeRate);
    }
    
    @Test
    void testConstructorInValidRate() {
        Instant rateDate = Instant.now();
        String source = "source";
        BigDecimal negativeRate = new BigDecimal("-0.56");
        
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> new ExchangeRate(cad, usd, negativeRate, rateDate, source));
        assertEquals("Exchange rate must be positive.", e1.getMessage());
        
        BigDecimal zeroRate = new BigDecimal("0.0000");
        
        Exception e2 = assertThrows(IllegalArgumentException.class, () -> new ExchangeRate(cad, usd, zeroRate, rateDate, source));
        assertEquals("Exchange rate must be positive.", e2.getMessage());
    }
    
    @Test
    void testConstructorInvalidSameCurrency() {        
        Instant rateDate = Instant.now();
        String source = "source";
    
        Exception e2 = assertThrows(IllegalArgumentException.class, () -> new ExchangeRate(cad, cad, defaultRate, rateDate, source));
        assertEquals("Cannot convert currency to itself.", e2.getMessage());
    }

    @Test
    void testGetInverseRate() {
        Instant rateDate = Instant.now();
        String source = "source";
        BigDecimal largeRate = new BigDecimal("0.7200000");

        ExchangeRate exchangeRate = new ExchangeRate(cad, usd, largeRate, rateDate, source);
        ExchangeRate convertedToCad = exchangeRate.getInverseRate();
        
        // Calculate the expected value the same way your method does
        BigDecimal expected = BigDecimal.ONE.divide(largeRate, 6, RoundingMode.HALF_UP);
        assertEquals(expected, convertedToCad.rate());
    }

    @Test
    void testIsExpired() {
        Instant rateDate = Instant.now();
        String source = "source";
        BigDecimal largeRate = new BigDecimal("0.7200000");

        ExchangeRate exchangeRate = new ExchangeRate(cad, usd, largeRate, rateDate, source);
        boolean expired = exchangeRate.isExpired();
        assertFalse(expired);
    }

    @Test
    void testIsExpiredWithDuraction() {
        Instant rateDate = Instant.now();
        String source = "source";
        BigDecimal largeRate = new BigDecimal("0.7200000");

        ExchangeRate exchangeRate = new ExchangeRate(cad, usd, largeRate, rateDate, source);
        Duration tesDuration = Duration.ofDays(-12L);
        boolean expired = exchangeRate.isExpired(tesDuration);
        assertTrue(expired);
    }
    
    @Test
    void testIsExpiredWithDuractionInValidNull() {
        Instant rateDate = Instant.now();
        String source = "source";
        BigDecimal largeRate = new BigDecimal("0.7200000");
    
        ExchangeRate exchangeRate = new ExchangeRate(cad, usd, largeRate, rateDate, source);
        assertThrows(NullPointerException.class, () -> exchangeRate.isExpired(null));
    }
}
