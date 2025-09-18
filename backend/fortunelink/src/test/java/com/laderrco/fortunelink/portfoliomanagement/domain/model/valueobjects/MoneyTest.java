package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.CurrencyMismatchException;

public class MoneyTest {

    private Money testMoneyCAD;
    private Money testMoneyUSD;
    private BigDecimal amount;
    private Currency CAD;
    private Currency USD;

    @BeforeEach
    void initialSetup() {
        CAD = Currency.getInstance("CAD");
        USD = Currency.getInstance("USD");
        amount = BigDecimal.valueOf(100);
        testMoneyCAD = new Money(amount, CAD);
        testMoneyUSD = new Money(amount, USD);
    }


    @Test
    @DisplayName("givenMoney_whenAdd_thenReturnSummedMoney")
    public void givenMoney_whenAdd_thenReturnSummedMoney() {
        Money actualSummedMoneyCAD = testMoneyCAD.add(testMoneyCAD); 
        Money expectedSummedMoney = new Money(amount.multiply(BigDecimal.valueOf(2)), CAD);
        assertEquals(expectedSummedMoney, actualSummedMoneyCAD);
    }

    @Test
    @DisplayName("givenZero_whenAdd_thenReturnsSameAmount")
    public void givenZero_whenAdd_thenReturnsSameAmount() {
        Money actualSummedMoneyCAD = testMoneyCAD.add(new Money(BigDecimal.ZERO, CAD)); 
        Money expectedSummedMoney = new Money((BigDecimal.valueOf(100)), CAD);
        assertEquals(expectedSummedMoney, actualSummedMoneyCAD);
    }

    @Test 
    @DisplayName("givenNegativeValue_whenAdd_thenReturnsSameAmount")
    public void givenNegativeValue_whenAdd_thenReturnsSameAmount() {
        Money actualSummedMoneyCAD = testMoneyCAD.add(new Money(BigDecimal.valueOf(-25), CAD)); 
        Money expectedSummedMoney = new Money((BigDecimal.valueOf(75)), CAD);
        assertEquals(expectedSummedMoney, actualSummedMoneyCAD);
    }

    @Test
    @DisplayName("givenDifferentCurrency_whenAdd_thenThrowsCurrencyException")
    public void givenDifferentCurrency_whenAdd_thenThrowsCurrencyException() {
        assertThrows(CurrencyMismatchException.class, () -> 
            testMoneyCAD.add(testMoneyUSD)
        );
    }

}
