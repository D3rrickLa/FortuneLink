package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class NetWorthSummaryTest {
    @Test
    void testCalculateNetWorth() {
        Money totalAssets = new Money(new BigDecimal(1000), new PortfolioCurrency(Currency.getInstance("USD")));
        Money totalLiabilities = new Money(new BigDecimal(450), new PortfolioCurrency(Currency.getInstance("USD")));

        NetWorthSummary nws = new NetWorthSummary(totalAssets, totalLiabilities);

        assertEquals(new Money(new BigDecimal(550), new PortfolioCurrency(Currency.getInstance("USD"))),
                nws.calculateNetWorth());
    }

    @Test
    void testEquals() {
        Money totalAssets = new Money(new BigDecimal(1000), new PortfolioCurrency(Currency.getInstance("USD")));
        Money totalLiabilities = new Money(new BigDecimal(450), new PortfolioCurrency(Currency.getInstance("USD")));
        
        NetWorthSummary nws = new NetWorthSummary(totalAssets, totalLiabilities);
        NetWorthSummary nws2 = new NetWorthSummary(totalAssets, totalLiabilities);
        assertTrue(nws.equals(nws2));
        assertTrue(nws.equals(nws));
        assertFalse(nws.equals(null));
        assertFalse(nws.equals(new Object()));
        
        Money totalAssets1 = new Money(new BigDecimal(1000), new PortfolioCurrency(Currency.getInstance("CAD")));
        Money totalLiabilities1 = new Money(new BigDecimal(674), new PortfolioCurrency(Currency.getInstance("USD")));
        assertFalse(nws.equals(new NetWorthSummary(totalAssets1, totalLiabilities)));
        assertFalse(nws.equals(new NetWorthSummary(totalAssets, totalLiabilities1)));
        
    }
    
    @Test 
    void testHashCode() {
        Money totalAssets = new Money(new BigDecimal(1000), new PortfolioCurrency(Currency.getInstance("USD")));
        Money totalLiabilities = new Money(new BigDecimal(450), new PortfolioCurrency(Currency.getInstance("USD")));
        
        NetWorthSummary nws = new NetWorthSummary(totalAssets, totalLiabilities);
        NetWorthSummary nws2 = new NetWorthSummary(totalAssets, totalLiabilities);
        assertEquals(nws.hashCode(), nws2.hashCode());
        assertNotEquals(nws.hashCode(), new Object().hashCode());
        
    }
}
