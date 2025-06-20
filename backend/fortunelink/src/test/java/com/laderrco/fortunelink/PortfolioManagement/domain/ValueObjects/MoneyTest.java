package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class MoneyTest {

    @Test
    void test_ConstructorGeneration() {
        Money money = new Money(new BigDecimal(100), new PortfolioCurrency("CAD", "$"));
        assertTrue(money.amount().equals(new BigDecimal("100.0000")));
    }

    @Test
    void test_ConstructorGenerationIfClauses() {
        Money money = new Money(new BigDecimal(100), new PortfolioCurrency("CAD", "$"));
        assertEquals(4, money.amount().scale());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new Money(new BigDecimal(100), new PortfolioCurrency("CADXX", "$"));
        });
        assertEquals("Currency code must be 3 characters long.", exception.getMessage());
    }

    @Test
    void shouldCreateMoneyWithValidAmountAndCurrency() {
        Money money = new Money(new BigDecimal("10.50"), new PortfolioCurrency("CAD", "$"));
        assertNotNull(money);
        assertEquals(new BigDecimal("10.5000"), money.amount()); // Assuming scale 4 enforced

        Money t1 = new Money(new BigDecimal(0), new PortfolioCurrency("CAD", "$"));
        assertEquals(t1.currencyCode(), money.currencyCode());
    }

    @Test
    void shouldThrowExceptionForNullAmount() {
        assertThrows(NullPointerException.class, () -> new Money(null, new PortfolioCurrency("CAD", "$")));
    }

    @Test
    void shouldAddMoneyWithSameCurrency() {
        Money m1 = new Money(new BigDecimal("10"), new PortfolioCurrency("CAD", "$"));
        Money m2 = new Money(new BigDecimal("5"), new PortfolioCurrency("CAD", "$"));
        Money sum = m1.add(m2);
        assertEquals(new BigDecimal("15.0000"), sum.amount());
        assertEquals(m1.currencyCode(), sum.currencyCode());
    }

    @Test
    void shouldThrowExceptionWhenAddingDifferentCurrencies() {
        Money m1 = new Money(new BigDecimal("10"), new PortfolioCurrency("CAD", "$"));
        Money m2 = new Money(new BigDecimal("5"), new PortfolioCurrency("EUR", "$"));
        assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
    }

    @Test
    void testSubtractBranches() {
        Money m1 = new Money(new BigDecimal("10"), new PortfolioCurrency("CAD", "$"));
        Money m2 = new Money(new BigDecimal("15"), new PortfolioCurrency("USD", "$"));
        assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));

        Money m3 = new Money(new BigDecimal("10"), new PortfolioCurrency("CAD", "$"));
        Money m4 = new Money(new BigDecimal("15"), new PortfolioCurrency("CAD", "$"));
        assertEquals(new Money(new BigDecimal(-5), new PortfolioCurrency("CAD", "$")), m3.subtract(m4));
    }

    @Test
    void testIsPositive() {
        Money m1 = new Money(new BigDecimal("10"), new PortfolioCurrency("CAD", "$"));
        assertTrue(m1.isPositive());
        Money m2 = new Money(new BigDecimal("-5"), new PortfolioCurrency("USD", "$"));
        assertFalse(m2.isPositive());

    }

    @Test
    void testMultipleValid() {
        Money m1 = new Money(new BigDecimal("10"), new PortfolioCurrency("CAD", "$"));
        BigDecimal mult = new BigDecimal(5);
        assertEquals(new Money(new BigDecimal("50"), new PortfolioCurrency("CAD", "$")), m1.multiply(mult));
    }

    @Test
    void testMultipleInValid() {
        Money m1 = new Money(new BigDecimal("10"), new PortfolioCurrency("CAD", "$"));
        assertThrows(NullPointerException.class, () -> m1.multiply(null));
    }

    @Test
    void testNegateMoney() {
        Money m1 = new Money(new BigDecimal("10"), new PortfolioCurrency("CAD", "$"));
        m1 = m1.negate();
        assertEquals(new Money(new BigDecimal("-10"), new PortfolioCurrency("CAD", "$")), m1);
    }
}
