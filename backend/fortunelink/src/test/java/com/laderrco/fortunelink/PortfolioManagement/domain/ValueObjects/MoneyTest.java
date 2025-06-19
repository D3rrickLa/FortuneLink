package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

}
