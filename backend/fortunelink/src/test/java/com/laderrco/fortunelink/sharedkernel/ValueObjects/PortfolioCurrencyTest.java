package com.laderrco.fortunelink.sharedkernel.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PortfolioCurrencyTest {
    private Currency javaCurrency;
    private PortfolioCurrency currency;

    @BeforeEach
    public void init() {
        javaCurrency = Currency.getInstance("USD");
        currency = new PortfolioCurrency(javaCurrency);
    }

    @Test
    void testCode() {
        assertEquals(javaCurrency.getCurrencyCode(), currency.code());

    }

    @Test
    void testGetDefaultScale() {
        assertTrue(currency.getDefaultScale() == 2);
    }

    @Test
    void testIsFiat() {
        assertTrue(currency.isFiat(currency.code()));
        assertFalse(currency.isFiat("XRP"));
    }
}
