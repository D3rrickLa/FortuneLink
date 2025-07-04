package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.DecimalPrecision;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class NetWorthSummaryTest {
    @Test
    void testCalculateNetWorth() {
        Money totalAssets = new Money(new BigDecimal(1000), new PortfolioCurrency(Currency.getInstance("USD")));
        Money totalLiabilities = new Money(new BigDecimal(450), new PortfolioCurrency(Currency.getInstance("USD")));

        Money netWorthCalc = NetWorthSummary.calculateNetWorth(totalAssets, totalLiabilities);
        assertEquals(new Money(new BigDecimal(550).setScale(DecimalPrecision.CASH.getDecimalPlaces(), RoundingMode.HALF_EVEN), new PortfolioCurrency(Currency.getInstance("USD"))), netWorthCalc);
    }

    @Test 
    void testCalculcateNetWorthBadNull() {
        Money totalAssets = new Money(new BigDecimal(1000), new PortfolioCurrency(Currency.getInstance("USD")));
        Money totalLiabilities = new Money(new BigDecimal(450), new PortfolioCurrency(Currency.getInstance("USD")));

        Exception e1 = assertThrows(NullPointerException.class, () ->NetWorthSummary.calculateNetWorth(null, totalLiabilities));
        assertEquals("totalAssetsValue cannot be null.", e1.getMessage());
        Exception e2 = assertThrows(NullPointerException.class, () ->NetWorthSummary.calculateNetWorth(totalAssets, null));
        assertEquals("totalLiabilitiesValue cannot be null.", e2.getMessage());
    }
}
