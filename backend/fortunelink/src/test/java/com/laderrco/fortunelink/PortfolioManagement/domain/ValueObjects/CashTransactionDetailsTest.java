package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.CashTransactionDetails;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class CashTransactionDetailsTest {
    @Test 
    void testConstructor() {
        CashTransactionDetails cashTransactionDetails = new CashTransactionDetails(new Money(new BigDecimal(100), new PortfolioCurrency(Currency.getInstance("USD"))));
        assertThrows(NullPointerException.class, () ->new CashTransactionDetails(null));
        assertThrows(IllegalArgumentException.class, () ->new CashTransactionDetails(new Money(new BigDecimal(-1), new PortfolioCurrency(Currency.getInstance("USD")))));
        assertThrows(IllegalArgumentException.class, () ->new CashTransactionDetails(new Money(new BigDecimal(0), new PortfolioCurrency(Currency.getInstance("USD")))));
        assertNotNull(cashTransactionDetails);
        
        assertEquals(cashTransactionDetails.getNormalizedAmount(), new Money(new BigDecimal(100), new PortfolioCurrency(Currency.getInstance("USD"))));
        assertNotEquals(cashTransactionDetails.getNormalizedAmount(), new Money(new BigDecimal(100), new PortfolioCurrency(Currency.getInstance("CAD"))));
    }
    
    @Test
    void testEquals() {
        CashTransactionDetails cashTransactionDetails1 = new CashTransactionDetails(new Money(new BigDecimal(100), new PortfolioCurrency(Currency.getInstance("USD"))));
        CashTransactionDetails cashTransactionDetails2 = new CashTransactionDetails(new Money(new BigDecimal(100), new PortfolioCurrency(Currency.getInstance("USD"))));
        CashTransactionDetails cashTransactionDetails3 = new CashTransactionDetails(new Money(new BigDecimal(1000), new PortfolioCurrency(Currency.getInstance("CAD"))));
        
        assertTrue(cashTransactionDetails1.equals(cashTransactionDetails2));
        assertTrue(cashTransactionDetails1.equals(cashTransactionDetails1));
        assertFalse(cashTransactionDetails1.equals(cashTransactionDetails3));
        assertFalse(cashTransactionDetails1.equals(null));
        assertFalse(cashTransactionDetails1.equals(new Object()));
        assertFalse(cashTransactionDetails1.equals( new CashTransactionDetails(new Money(new BigDecimal(1000), new PortfolioCurrency(Currency.getInstance("USD"))))));
        assertFalse(cashTransactionDetails1.equals( new CashTransactionDetails(new Money(new BigDecimal(100), new PortfolioCurrency(Currency.getInstance("CAD"))))));
        
    }
    
    @Test
    void testHashCode() {
        CashTransactionDetails cashTransactionDetails1 = new CashTransactionDetails(new Money(new BigDecimal(100), new PortfolioCurrency(Currency.getInstance("USD"))));
        CashTransactionDetails cashTransactionDetails2 = new CashTransactionDetails(new Money(new BigDecimal(100), new PortfolioCurrency(Currency.getInstance("USD"))));
        CashTransactionDetails cashTransactionDetails3 = new CashTransactionDetails(new Money(new BigDecimal(1000), new PortfolioCurrency(Currency.getInstance("CAD"))));
        
        assertEquals(cashTransactionDetails1.hashCode(), cashTransactionDetails2.hashCode());
        assertNotEquals(cashTransactionDetails2.hashCode(), cashTransactionDetails3.hashCode());
    }
    
}
