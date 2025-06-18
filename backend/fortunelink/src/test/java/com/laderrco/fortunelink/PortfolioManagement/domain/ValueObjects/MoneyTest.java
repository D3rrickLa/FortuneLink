package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class MoneyTest {

    @Test
    void test_ConstructorGeneration() {
        Money money = new Money(new BigDecimal(100), new PortfolioCurrency("CAD", "$"));
        assertTrue(money.amount().equals(new BigDecimal(100)));
    }
}
