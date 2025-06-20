package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PortfolioCurrencyTest {
    @Test
    void testCode() {
        PortfolioCurrency pc1 = new PortfolioCurrency("USD", "$");
        PortfolioCurrency pc2 = new PortfolioCurrency("USD", "$");

        assertTrue(pc1.code().equals(pc2.code()));
        assertTrue("USD".equals(pc1.code()));
    }

    @Test
    void testEquals() {
        PortfolioCurrency pc1 = new PortfolioCurrency("USD", "$");
        PortfolioCurrency pc2 = new PortfolioCurrency("USD", "$");

        assertTrue(pc1.equals(pc2));

        PortfolioCurrency pc3 = new PortfolioCurrency("EUR", "€");
        assertFalse(pc1.equals(pc3)); // Different code and symbol
        PortfolioCurrency pc4 = new PortfolioCurrency("USD", "€");
        assertFalse(pc1.equals(pc4)); // Same code, different symbol
        PortfolioCurrency pc5 = new PortfolioCurrency("EUR", "$");
        assertFalse(pc1.equals(pc5)); // Different code, same symbol
    }

    @Test
    void testHashCode() {
        PortfolioCurrency pc1 = new PortfolioCurrency("USD", "$");
        PortfolioCurrency pc2 = new PortfolioCurrency("USD", "$");
        assertTrue(pc1.hashCode() == pc2.hashCode());
    }

    // AI assisted
    @Test
    void testSymbol() {
        PortfolioCurrency pc = new PortfolioCurrency("USD", "$");
        assertTrue("$".equals(pc.symbol())); // Test exact symbol
        assertFalse("€".equals(pc.symbol())); // Test for incorrect symbol
    }

    // AI assisted
    @Test
    void testToString() {
        PortfolioCurrency pc = new PortfolioCurrency("USD", "$");
        // The exact string representation
        String expectedToString = "PortfolioCurrency[code=USD, symbol=$]";
        assertEquals(expectedToString, pc.toString());

        // You could also use contains for more flexibility if the exact format is less
        // strict
        // assertTrue(pc.toString().contains("USD"));
        // assertTrue(pc.toString().contains("$"));
    }

    // AI coded
    @Test
    void testConstructorThrowsOnNullCode() {
        // Assert that a NullPointerException is thrown when code is null
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            new PortfolioCurrency(null, "$");
        });
        assertEquals("An exchange code must not be null.", exception.getMessage());
    }

    @Test
    void testConstructorThrowsOnNullSymbol() {
        // Assert that a NullPointerException is thrown when symbol is null
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            new PortfolioCurrency("USD", null);
        });
        assertEquals("A currency symbol must not be null.", exception.getMessage());
    }

    @Test
    void testConstructorDoesNotThrowOnValidArguments() {
        // Assert that no exception is thrown for valid arguments
        assertDoesNotThrow(() -> {
            new PortfolioCurrency("USD", "$");
        });
    }
}
