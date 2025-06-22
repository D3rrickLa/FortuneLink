package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class NetWorthSummaryTest {
    @Test
    void testCalculateNetWorth() {
        Money totalAssets = new Money(new BigDecimal(1000), new PortfolioCurrency(Currency.getInstance("USD")));
        Money totalLiabilities = new Money(new BigDecimal(450), new PortfolioCurrency(Currency.getInstance("USD")));

        NetWorthSummary nws = new NetWorthSummary(totalAssets, totalLiabilities);

        assertEquals(new Money(new BigDecimal(550), new PortfolioCurrency(Currency.getInstance("USD"))), nws.calculateNetWorth());
    }
}
