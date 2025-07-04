package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.FeeType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class FeeTest {

    private FeeType feeType;
    private Money feeAmount;
    private Fee testFee01;

    @BeforeEach
    void init() {
        feeType = FeeType.ACCOUNT_MAINTENANCE;
        feeAmount = new Money(new BigDecimal(20), new PortfolioCurrency(Currency.getInstance("CAD")));
    }
    
    @Test
    void testContstructor() {
        testFee01 = new Fee(feeType, feeAmount);
        assertNotNull(testFee01);
    }

    @Test 
    void testContstructorBadNull() {
        Exception e1 = assertThrowsExactly(NullPointerException.class, () -> new Fee(null, feeAmount));
        assertEquals("Fee type cannot be null.", e1.getMessage());
        Exception e2 = assertThrowsExactly(NullPointerException.class, () -> new Fee(feeType, null));
        assertEquals("Fee amount cannot be null.", e2.getMessage());
    }
    
    @Test 
    void testContstructorBadFeeLessThanZero() {

        Exception e1 = assertThrowsExactly(IllegalArgumentException.class, () -> new Fee(feeType, new Money(new BigDecimal(-1), new PortfolioCurrency(Currency.getInstance("CAD")))));
        assertEquals("Fee amount cannot be a negative value.", e1.getMessage());
        
    }
}
