package com.laderrco.fortunelink.sharedkernel.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

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
    void testEquals() {
        PortfolioCurrency currency2 = new PortfolioCurrency(javaCurrency);

        assertTrue(currency.equals(currency2));
        
    }

    @Test
    void testGetDefaultScale() {
        assertTrue(currency.getDefaultScale() == 2);
    }

    @Test
    void testGetSymbol() {
        assertTrue(currency.getSymbol().equals(javaCurrency.getSymbol()));
    }

    @Test
    void testHashCode() {
        PortfolioCurrency currency2 = new PortfolioCurrency(javaCurrency);
        assertEquals(currency.hashCode(), currency2.hashCode());
    }

    @Test
    void testIsFiat() {
        assertTrue(currency.isFiat(currency.code()));
        assertFalse(currency.isFiat("XRP"));
    }

    @Test
    void testJavaCurrency() {
        assertTrue(!javaCurrency.equals(null));
    }

    @Test
    void testToString() {
        assertTrue(!currency.toString().equals(null));
    }
}
