package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class CashTransactionDetailsTest {
    @Test 
    void testConstructor() {
        CashTransactionDetails cashTransactionDetails = new CashTransactionDetails();
        assertNotNull(cashTransactionDetails);
    }
}
