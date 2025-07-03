package com.laderrco.fortunelink.sharedkernel.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MoneyTest {
    private BigDecimal amount;
    private PortfolioCurrency currency;
    private Money testMoney01;

    @BeforeEach
    void init() {
        amount = new BigDecimal(327.56);
        currency = new PortfolioCurrency(Currency.getInstance("CAD"));
        testMoney01 = new Money(amount, currency);
    }

    @Test
    void testZERO() {
        // Testing if we get back BigDeicmal or zero.
        Money zeroMoney = Money.ZERO(currency);
        System.out.println(zeroMoney.amount());
        assertTrue(BigDecimal.ZERO.setScale(currency.getDefaultScale(), RoundingMode.HALF_EVEN).equals(zeroMoney.amount()));
        assertFalse(zeroMoney.amount().equals(testMoney01.amount()));
    }

    @Test
    void testAddGood() {
        BigDecimal amount02 = new BigDecimal(34.31);
        Money money02 = new Money(amount02, currency);
        
        Money moneyAdded = testMoney01.add(money02);
        assertEquals(new BigDecimal(361.87).setScale(currency.getDefaultScale(), RoundingMode.HALF_EVEN), moneyAdded.amount());
    }
    
    @Test
    void testAddBad() {
        BigDecimal amount02 = new BigDecimal(34.31);
        Money money02 = new Money(amount02, new PortfolioCurrency(Currency.getInstance("USD")));
        Exception e01 = assertThrows(IllegalArgumentException.class, () -> testMoney01.add(money02));
        assertEquals("Cannot add Money with different currencies. Convert first: " + currency + " vs " + money02.currency(), e01.getMessage());
        
    }

    @Test
    void testCompareToGood() {
        BigDecimal amount02 = new BigDecimal(34.31);
        Money money02 = new Money(amount02, currency);
        assertTrue(testMoney01.compareTo(money02) == 1);
        
        amount02 = new BigDecimal(3400.31);
        money02 = new Money(amount02, currency);
        assertTrue(testMoney01.compareTo(money02) == -1);

        amount02 = new BigDecimal(327.56);
        money02 = new Money(amount02, currency);
        assertTrue(testMoney01.compareTo(money02) == 0);
    }

    @Test
    void testCompareToBad() {
        BigDecimal amount02 = new BigDecimal(34.31);
        Money money02 = new Money(amount02, new PortfolioCurrency(Currency.getInstance("USD")));
        Exception e01 = assertThrows(IllegalArgumentException.class, () -> testMoney01.compareTo(money02));
        assertEquals("Cannot compare Money with different currencies. Convert first: " + currency + " vs " + money02.currency(), e01.getMessage());
        
    }

    @Test
    void testConvertGood() {
        Currency testCurrency = Currency.getInstance("EUR");
        BigDecimal exchangeRate = new BigDecimal(1.47);
        RoundingMode mode = RoundingMode.HALF_UP;

        Money convertedMoney = testMoney01.convert(testCurrency, exchangeRate, mode);
        assertEquals(new BigDecimal(327.56*1.47).setScale(convertedMoney.currency().getDefaultScale(), mode), convertedMoney.amount());
    }
    
    
    
    @Test
    void testConvertGoodSameCurrency() {
        Currency testCurrency = Currency.getInstance("CAD");
        BigDecimal exchangeRate = new BigDecimal(1.47);
        RoundingMode mode = RoundingMode.HALF_UP;
        
        Money convertedMoney = testMoney01.convert(testCurrency, exchangeRate, mode);
        assertEquals(Currency.getInstance("CAD"), convertedMoney.currency().javaCurrency());
        
    }
    
    @Test
    void testConvertBadNulls() {
        Currency testCurrency = Currency.getInstance("CAD");
        BigDecimal exchangeRate = new BigDecimal(1.47);
        RoundingMode mode = RoundingMode.HALF_UP;
        Exception e01 = assertThrows(NullPointerException.class, () -> testMoney01.convert(null, exchangeRate, mode));
        assertEquals("Target currency cannot be null.", e01.getMessage());
        Exception e02 = assertThrows(NullPointerException.class, () -> testMoney01.convert(testCurrency, null, mode));
        assertEquals("Exchange rate cannot be null.", e02.getMessage());
        Exception e03 = assertThrows(NullPointerException.class, () -> testMoney01.convert(testCurrency, exchangeRate, null));
        assertEquals("Rounding mode cannot be null.", e03.getMessage());
    }

    @Test
    void testDivideGood() {
        BigDecimal divisor = new BigDecimal(2);
        Money dividedMoney = testMoney01.divide(divisor);
        assertEquals(new BigDecimal(327.56/2).setScale(dividedMoney.currency().getDefaultScale(), RoundingMode.HALF_EVEN), dividedMoney.amount());
    }

    
    @Test
    void testDivideBadNull() {
        assertThrows(NullPointerException.class, () -> testMoney01.divide(null));
    }
    
    @Test
    void testDivideBadZeroValue() {
        assertThrows(ArithmeticException.class, () -> testMoney01.divide(BigDecimal.ZERO));
        assertThrows(ArithmeticException.class, () -> testMoney01.divide(new BigDecimal(-0)));
        
    }

    @Test
    void testMultiplyGood() {
        Long mult01 = 2L;
        BigDecimal mult02 = new BigDecimal(2);
        Money multiMoney = testMoney01.multiply(mult01);
        Money multMoney2 = testMoney01.multiply(mult02);

        assertEquals(multiMoney, multMoney2);
        assertEquals(new BigDecimal(327.56*2).setScale(multiMoney.currency().getDefaultScale(), RoundingMode.HALF_EVEN), multiMoney.amount());
        assertEquals(new BigDecimal(327.56*2).setScale(multMoney2.currency().getDefaultScale(), RoundingMode.HALF_EVEN), multMoney2.amount());
    }

    @Test
    void testMultiplyBadNull() {
        Long multipler = null;
        assertThrows(NullPointerException.class, () -> testMoney01.multiply(multipler));
    }

    @Test
    void testNegate() {
        Money negateMoney = testMoney01.negate();
        assertEquals(new BigDecimal(-327.56).setScale(negateMoney.currency().getDefaultScale(), RoundingMode.HALF_EVEN), negateMoney.amount());
    }

    @Test
    void testSetScale() {
        Money newScaleMoney = testMoney01.setScale(2, RoundingMode.HALF_EVEN);
        assertEquals(2, newScaleMoney.amount().scale());
    }

    @Test
    void testSubtract() {
        PortfolioCurrency currency2 = new PortfolioCurrency(Currency.getInstance("USD"));
        Money money2 = new Money(amount, currency2);

        // testing if money to subtract is null
        assertThrows(NullPointerException.class, () -> testMoney01.subtract(null));

        // testing if mistmatch currency are throwing an error
        assertThrows(IllegalArgumentException.class, () -> testMoney01.subtract(money2));

        PortfolioCurrency currency3 = new PortfolioCurrency(Currency.getInstance("CAD"));
        Money money3 = new Money(amount, currency3);
        Money subtracted = testMoney01.subtract(money3);
        assertEquals(0D, subtracted.amount().doubleValue());
    }
}
