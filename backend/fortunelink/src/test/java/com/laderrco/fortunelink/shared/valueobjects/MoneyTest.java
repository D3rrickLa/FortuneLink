package com.laderrco.fortunelink.shared.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MoneyTest {
    
    Currency cad;
    Currency usd;
    BigDecimal defaultAmount;

    @BeforeEach
    void init() {
        cad = Currency.getInstance("CAD");
        usd = Currency.getInstance("USD");
        defaultAmount = new BigDecimal("24.21");
    }

    @Test 
    void testConstructorValid() {
        BigDecimal amount = new BigDecimal("20.45"); // <-- Use "20.45" instead of 20.45 (double)
        Money testMoney = new Money(amount, cad);

        assertNotNull(testMoney);

        // Do NOT assert on testMoney.amount().scale() matching FINANCIAL_MATH_CONTEXT.getPrecision().
        // scale() tells you decimal places. getPrecision() tells you total digits.
        // For "20.45", the scale is 2, not 34 or 48.
        // The precision of the 'amount' BigDecimal when it's initialized directly will be its natural precision.
        // The FINANCIAL_MATH_CONTEXT.getPrecision() (34) applies to *calculations* performed on Money objects,
        // not necessarily the initial scale of the BigDecimal passed into the constructor.
        
        // Correct assertion for the amount value:
        assertEquals(0, testMoney.amount().compareTo(new BigDecimal("20.45")), "The amount should be exactly 20.45");
        
        // If you want to assert the scale for this specific input, you'd assert its actual scale:
        assertEquals(2, testMoney.amount().scale(), "The scale of 20.45 should be 2"); 
    }   

    @Test 
    void testConstructorInValid() {
        assertThrows(NullPointerException.class, ()->new Money((BigDecimal) null, cad));
        assertThrows(NullPointerException.class, ()->new Money((String) null, cad));
        assertThrows(NullPointerException.class, ()->new Money(defaultAmount, null));
    }
    
    @Test
    void testZERO() {
        Money testMoneyZeroed = Money.ZERO(cad);
        assertEquals(BigDecimal.ZERO, testMoneyZeroed.amount());
        assertEquals(cad, testMoneyZeroed.currency());
    }

    @Test
    void testAbs() {
        BigDecimal amount = new BigDecimal(-20.45689271422222222);
        Money testMoney = new Money(amount, cad);
        Money absMoney = testMoney.abs();
        assertEquals(amount.negate(), absMoney.amount());

    }

    @Test
    void testAddValid() {
        BigDecimal amount = new BigDecimal("20.45567");
        Money testMoney = new Money(amount, cad);
        Money testMoney2 = new Money(amount, cad);
        Money addedMoney = testMoney.add(testMoney2);
        BigDecimal expected = new BigDecimal("40.91134"); 
        
        // Assert that the full-precision amounts are equal.
        assertEquals(expected, addedMoney.amount(), "The sum should be mathematically exact.");

    }
    
    @Test 
    void testAddedInValidNull() {
        Money testMoney = new Money(defaultAmount, cad);
        Exception e1 = assertThrows(NullPointerException.class, () -> testMoney.add(null));
        assert e1.getMessage() == "Cannot pass null to the 'add' method.";
        
    }

    @Test 
    void testAddedInValidDifferentCurrency() {
        BigDecimal amount = new BigDecimal("20.45567");
        Money testMoney = new Money(amount, cad);
        Money testMoney2 = new Money(amount, usd);
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> testMoney.add(testMoney2));
        assertEquals("Cannot add money with different currencies. Please convert them to be the same.", e1.getMessage());
    }

    @Test
    void testCompareTo() {
        BigDecimal amount = new BigDecimal("20.45567");
        Money testMoney = new Money(amount, cad);
        Money testMoney2 = new Money(amount, cad);
        Money testMoney3 = new Money(amount.multiply(BigDecimal.TWO), cad);
        Money testMoney4 = new Money(amount.subtract(amount), cad);
    
        assertTrue( testMoney.compareTo(testMoney2) == 0);
        assertTrue( testMoney.compareTo(testMoney3) == -1);
        assertTrue( testMoney.compareTo(testMoney4) == 1);
    }
    
    @Test
    void testCompareToInValidNull(){        
        BigDecimal amount = new BigDecimal("20.45567");
        Money testMoney = new Money(amount, cad);
        Exception e1 = assertThrows(NullPointerException.class, () -> testMoney.compareTo(null));
        assert e1.getMessage() == "Cannot pass null to the 'compareTo' method.";


    }
    
    @Test 
    void testCompareToWrongCurrency() {
        BigDecimal amount = new BigDecimal("20.45567");
        Money testMoney = new Money(amount, cad);
        Money testMoney2 = new Money(amount, usd);
        
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> testMoney.compareTo(testMoney2));
        assertEquals("Cannot compareTo money with different currencies. Please convert them to be the same.", e1.getMessage());

    }

    @Test
    void testConvertTo() {
        Money cadMoney = new Money(new BigDecimal("20.46"), cad);  // Already at CAD precision
        ExchangeRate exchangeRate = new ExchangeRate(cad, usd, new BigDecimal("0.72"), Instant.now(), "SOME SOURCE");
        
        Money usdMoney = cadMoney.convertTo(usd, exchangeRate);
        
        // 20.46 * 0.72 = 14.7312
        assertEquals(usd, usdMoney.currency());
        assertEquals(new BigDecimal("14.73120000"), usdMoney.amount());
    }

    @Test 
    void testConvertToInValidNull() {
        ExchangeRate exchangeRate = new ExchangeRate(cad, usd, new BigDecimal("0.72"), Instant.now(), "SOME SOURCE");
        Money testMoney = new Money(defaultAmount, cad);
        
        Exception e1 = assertThrows(NullPointerException.class, () -> testMoney.convertTo(null, exchangeRate));
        Exception e2 = assertThrows(NullPointerException.class, () -> testMoney.convertTo(usd, null));
        assertEquals("Cannot pass target currency as null to the 'convertTo' method.", e1.getMessage());
        assertEquals("Cannot pass exchange rate as null to the 'convertTo' method.", e2.getMessage());
    }
    
    @Test 
    void testConvertToInValidCurrencyNotEqualToExchangeRate() {
        ExchangeRate exchangeRate = new ExchangeRate(cad, usd, new BigDecimal("0.72"), Instant.now(), "SOME SOURCE");
        Money testMoney = new Money(defaultAmount, cad);
        Money testMoney2 = new Money(defaultAmount, usd);
        
        Currency jpy = Currency.getInstance("JPY");
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> testMoney.convertTo(jpy, exchangeRate));
        assertEquals("Exchange rate currencies don't match conversion request.", e1.getMessage());

        Exception e2 = assertThrows(IllegalArgumentException.class, () -> testMoney2.convertTo(cad, exchangeRate));
        assertEquals("Exchange rate currencies don't match conversion request.", e2.getMessage());
        
        
    }
    
    @Test
    void testDivide() {
        Money testMoney = new Money(defaultAmount, cad);
        Money testMoney2 = testMoney.divide(new BigDecimal(2));
        BigDecimal expectedAmount = new BigDecimal("12.105");
        
        assertEquals(expectedAmount, testMoney2.amount());
    }
    
    @Test
    void testDivideInValidNull() {
        Money testMoney = new Money(defaultAmount, cad);
        Exception e1 = assertThrows(NullPointerException.class, () -> testMoney.divide((BigDecimal) null));
        assertEquals("Divisor cannot be null.", e1.getMessage());
    }
    
    @Test 
    void testDivideInValidDivideByZero() {
        Money testMoney = new Money(defaultAmount, cad);
        Exception e1 = assertThrows(ArithmeticException.class, () -> testMoney.divide((new BigDecimal(0))));
        assertEquals("Divisor cannot be zero.", e1.getMessage());
    }
    
    @Test
    void testDivideLong() {
        Money testMoney = new Money(defaultAmount, cad);
        Money testMoney2 = testMoney.divide(2L);
        BigDecimal expectedAmount = new BigDecimal("12.105");
        
        assertEquals(expectedAmount, testMoney2.amount());
        
    }

    @Test
    void testIsNegative() {
        Money testMoney = new Money(defaultAmount, cad);
        Money testMoney2 = new Money(defaultAmount.negate(), cad);
    
        assertFalse(testMoney.isNegative());
        assertTrue(testMoney2.isNegative());
    }
    
    @Test
    void testIsPositive() {
        Money testMoney = new Money(defaultAmount, cad);
        Money testMoney2 = new Money(defaultAmount.negate(), cad);
    
        assertTrue(testMoney.isPositive());
        assertFalse(testMoney2.isPositive());   
    }
    
    @Test
    void testIsZero() {
        Money testMoney = new Money(defaultAmount, cad);
        Money testMoney2 = new Money(BigDecimal.ZERO.negate(), cad);
    
        assertFalse(testMoney.isZero());
        assertTrue(testMoney2.isZero());

    }

    @Test
    void testMultiply() {
        Money testMoney = new Money(defaultAmount, cad);
        Money testMoney2 = testMoney.multiply(new BigDecimal(2));
        BigDecimal expectedAmount = new BigDecimal("24.21").multiply(new BigDecimal("2"));
        
        assertEquals(expectedAmount, testMoney2.amount());
    }
    
    @Test 
    void testMultiplyInValidNull() {
        Money testMoney = new Money(defaultAmount, cad);
        Exception e1 = assertThrows(NullPointerException.class, () -> testMoney.multiply((BigDecimal) null));
        assertEquals("Multiplier cannot be null.", e1.getMessage());
    }
    
    @Test
    void testMultiplyLong() {
        Money testMoney = new Money(defaultAmount, cad);
        Money testMoney2 = testMoney.multiply(2L);
        BigDecimal expectedAmount = new BigDecimal("24.21").multiply(new BigDecimal("2"));
        
        assertEquals(expectedAmount, testMoney2.amount());

    }

    @Test
    void testNegate() {
        Money testMoney = new Money(defaultAmount, cad);
        Money negateMoney = testMoney.negate();
        BigDecimal expectedValue = new BigDecimal("-24.21").setScale( cad.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
        assertEquals(expectedValue, negateMoney.amount());
    }

    @Test
    void testSubtract() {
        BigDecimal amount = new BigDecimal("20.45567");
        Money testMoney = new Money(amount, cad);
        Money testMoney2 = new Money(new BigDecimal("10.44"), cad);
        
        Money subtractedMoney = testMoney.subtract(testMoney2);
        
        BigDecimal expected = new BigDecimal("10.01567");
        
        assertEquals(expected, subtractedMoney.amount());
    }

    @Test 
    void testSubtractInValidNull() {
        Money testMoney = new Money(defaultAmount, cad);
        Exception e1 = assertThrows(NullPointerException.class, () -> testMoney.subtract(null));
        assert e1.getMessage() == "Cannot pass null to the 'subtract' method.";
        
    }

    @Test 
    void testSubtractInValidDifferentCurrency() {
        BigDecimal amount = new BigDecimal("20.45567");
        Money testMoney = new Money(amount, cad);
        Money testMoney2 = new Money(new BigDecimal("10.44"), usd);
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> testMoney.subtract(testMoney2));
        assertEquals("Cannot subtract money with different currencies. Please convert them to be the same.", e1.getMessage());
    }
}
