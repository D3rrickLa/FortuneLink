package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class InvestmentPerformanceSummaryTest {
    @Test 
    void testConstructor() {
        Money totalGainLoss = new Money(new BigDecimal("100"), new PortfolioCurrency("USD", "$"));
        Percentage trp = new Percentage(new BigDecimal(100));
        Percentage ar = new Percentage(new BigDecimal(100));
        assertThrows(NullPointerException.class, () -> new InvestmentPerformanceSummary(null, trp, ar));
        assertThrows(NullPointerException.class, () -> new InvestmentPerformanceSummary(totalGainLoss, null, ar));
        assertThrows(NullPointerException.class, () -> new InvestmentPerformanceSummary(totalGainLoss, trp, null));
        
        new InvestmentPerformanceSummary(totalGainLoss, trp, ar);
    }
}
