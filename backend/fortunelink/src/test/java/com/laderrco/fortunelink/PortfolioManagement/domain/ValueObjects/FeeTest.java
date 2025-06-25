package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.FeeType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class FeeTest {

    private FeeType feeType;
    private Money money;
    private Fee fee;

    @BeforeEach
    void init() {
        feeType = FeeType.COMMISSION;
        money = new Money(new BigDecimal(100), new PortfolioCurrency(Currency.getInstance("USD")));
        fee = new Fee(feeType, money);
    }

    @Test
    void testConstructor() {
        assertThrows(IllegalArgumentException.class,
                () -> new Fee(feeType, new Money(new BigDecimal(-1), money.currency())));
    }

    @Test
    void testGetters() {
        assertEquals(feeType, fee.feeType());
        assertEquals(money, fee.amount());
    }

    @Test
    void testEquals() {
        FeeType ft1 = FeeType.COMMISSION;
        Money m1 = new Money(new BigDecimal(100), new PortfolioCurrency(Currency.getInstance("USD")));
        Fee f1 = new Fee(ft1, m1);
        
        assertTrue(fee.equals(f1));
        assertTrue(fee.equals(fee));
        assertFalse(fee.equals(null));
        assertFalse(fee.equals(new Object()));
        assertFalse(fee.equals(new Fee(FeeType.ACCOUNT_MAINTENANCE, m1)));
        assertFalse(fee.equals(new Fee(ft1, m1.multiply(2L))));
    }
    
    @Test
    void testHashCode() {
        FeeType ft1 = FeeType.COMMISSION;
        Money m1 = new Money(new BigDecimal(100), new PortfolioCurrency(Currency.getInstance("USD")));
        Fee f1 = new Fee(ft1, m1);
        assertTrue(f1.hashCode() == fee.hashCode());
    }

    @Test
    void testToString() {
        assertNotNull(fee.toString());
    }
}
