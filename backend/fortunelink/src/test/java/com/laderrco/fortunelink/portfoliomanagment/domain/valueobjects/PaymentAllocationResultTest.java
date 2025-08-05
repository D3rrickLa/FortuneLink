package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Currency;

import org.junit.jupiter.api.Test;

public class PaymentAllocationResultTest {

    private Currency USD = Currency.getInstance("USD");
    private Currency EUR = Currency.getInstance("EUR");

    @Test
    void isConstructorValid() {
        Money principalPaid = Money.of(100.0, USD);
        Money interestPaid = Money.of(10, USD);
        Money remainingBalance = Money.of(90, USD);
        
        assertDoesNotThrow(() -> new PaymentAllocationResult(principalPaid, interestPaid, remainingBalance));
    }
    
    @Test 
    void constructor_InValid_DifferentCurrency() {
        Money principalPaid = Money.of(100.0, USD);
        Money interestPaid = Money.of(10, EUR);
        Money remainingBalance = Money.of(90, USD);
        assertThrows(IllegalArgumentException.class, () -> new PaymentAllocationResult(principalPaid, interestPaid, remainingBalance));
    }
    
    @Test 
    void constructor_InValid_DifferentCurrency2() {
        Money principalPaid = Money.of(100.0, USD);
        Money interestPaid = Money.of(10, USD);
        Money remainingBalance = Money.of(90, EUR);
        assertThrows(IllegalArgumentException.class, () -> new PaymentAllocationResult(principalPaid, interestPaid, remainingBalance));
    }
}
