package com.laderrco.fortunelink.sharedkernel.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MoneyTest {
    private BigDecimal amount;
    private PortfolioCurrency currency;
    private Money money;

    @BeforeEach
    void init() {
        amount = new BigDecimal(100);
        currency = new PortfolioCurrency(Currency.getInstance("USD"));
        money = new Money(amount, currency);
    }

    @Test
    void testAdd() {
        Money newAmount = money.add(new Money(amount, currency));
        assertTrue(newAmount.amount().doubleValue() == 200D);

        // testing if we add null to 'add' function
        assertThrows(NullPointerException.class, () -> money.add(null));

        // testing the ability to add money that aren't the same currency
        PortfolioCurrency currency2 = new PortfolioCurrency(Currency.getInstance("CAD"));
        Money money2 = new Money(amount, currency2);

        assertThrows(IllegalArgumentException.class, () -> money.add(money2));
    }

    @Test
    void testAmount() {
        assertEquals(100D, amount.doubleValue());
    }

    @Test
    void testCompareTo() {
        PortfolioCurrency currency2 = new PortfolioCurrency(Currency.getInstance("CAD"));
        Money money2 = new Money(amount, currency2);

        assertThrows(IllegalArgumentException.class, () -> money.compareTo(money2));
        assertFalse(money.compareTo(money) == 1);
    }

    @Test
    void testCurrency() {
        PortfolioCurrency currency2 = new PortfolioCurrency(Currency.getInstance("USD"));
        assertEquals(currency, currency2);
    }

    @Test
    void testDivide() {
        Money dividedMoney = money.divide(new BigDecimal(10));
        assertEquals(10D, dividedMoney.amount().doubleValue());

        // testing dividing by 0
        assertThrows(ArithmeticException.class, () -> money.divide(new BigDecimal(0)));

        // test passing in null
        assertThrows(NullPointerException.class, () -> money.divide(null));

    }

    @Test
    void testEquals() {

        assertTrue(money.equals(money));
        assertFalse(money.equals(null));
        assertFalse(money.equals(new Object()));
        
        PortfolioCurrency currency2 = new PortfolioCurrency(Currency.getInstance("CAD"));
        Money money2 = new Money(amount, currency2);
        
        assertFalse(money2.equals(money));
        assertFalse(money.equals(new Money(amount, currency2)));
        assertFalse(money.equals(new Money(new BigDecimal(2), currency)));

        PortfolioCurrency currency3 = new PortfolioCurrency(Currency.getInstance("USD"));
        Money money3 = new Money(amount, currency3);
        assertTrue(money.equals(money3));
    }

    @Test
    void testHashCode() {
        PortfolioCurrency currency2 = new PortfolioCurrency(Currency.getInstance("CAD"));
        Money money2 = new Money(amount, currency2);

        assertNotEquals(money2.hashCode(), money.hashCode());

        PortfolioCurrency currency3 = new PortfolioCurrency(Currency.getInstance("USD"));
        Money money3 = new Money(amount, currency3);
        assertEquals(money3.hashCode(), money.hashCode());

    }

    @Test
    void testMulitply() {
        long multiplier = 2;
        Money multiMoney = money.multiply(multiplier);
        assertTrue(multiMoney.amount().doubleValue() == (2 * money.amount().doubleValue()));
    }

    @Test
    void testMultiplyBigDecimal() {
        BigDecimal multiplier = new BigDecimal(2);
        Money multiMoney = money.mulitply(multiplier);
        assertTrue(multiMoney.amount().doubleValue() == (2 * money.amount().doubleValue()));

    }

    @Test
    void testSubtract() {
        PortfolioCurrency currency2 = new PortfolioCurrency(Currency.getInstance("CAD"));
        Money money2 = new Money(amount, currency2);

        // testing if money to subtract is null
        assertThrows(NullPointerException.class, () -> money.subtract(null));

        // testing if mistmatch currency are throwing an error
        assertThrows(IllegalArgumentException.class, () -> money.subtract(money2));

        PortfolioCurrency currency3 = new PortfolioCurrency(Currency.getInstance("USD"));
        Money money3 = new Money(amount, currency3);
        Money subtracted = money.subtract(money3);
        assertEquals(0D, subtracted.amount().doubleValue());

    }

    @Test
    void testToString() {
        assertTrue(!money.toString().equals(null));
    }

    @Test
    void testZERO() {
        assertTrue(new Money(BigDecimal.ZERO, currency).equals(Money.ZERO(currency)));
    }

}
