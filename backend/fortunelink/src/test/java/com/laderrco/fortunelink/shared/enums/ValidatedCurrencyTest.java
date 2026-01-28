package com.laderrco.fortunelink.shared.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ValidatedCurrencyTest {
    private ValidatedCurrency testValidatedCurrency;

    @BeforeEach
    void init() {
        testValidatedCurrency = ValidatedCurrency.of("CAD");
    }

    @Test
    void testValidatedCurrency() {
        assertEquals("CAD", testValidatedCurrency.getCode());
        String symbol = Currency.getInstance(java.util.Locale.CANADA).getSymbol();
        assertEquals("$", symbol);
        assertFalse(testValidatedCurrency.equals(new Object()));
        assertEquals(testValidatedCurrency.hashCode(), testValidatedCurrency.hashCode());
    }

}
