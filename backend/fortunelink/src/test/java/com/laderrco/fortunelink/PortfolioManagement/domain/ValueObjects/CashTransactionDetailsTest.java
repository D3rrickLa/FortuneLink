package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Test;

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
    }
}
