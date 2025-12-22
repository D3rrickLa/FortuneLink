package com.laderrco.fortunelink.portfolio_management.application.commands;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class RecordIncomeCommandTest {
    @Test
    void testConstructorExceptionIsDripTrueSharesNulls() {
        assertThrows(IllegalArgumentException.class, () ->
            new RecordIncomeCommand(null, null, null, null, null, true, null, null, null)
        );
    }

    @Test
    void testConstructorExceptionIsDripTrueSharesNegative() {
        assertThrows(IllegalArgumentException.class, () ->
            new RecordIncomeCommand(null, null, null, null, null, true, BigDecimal.valueOf(-2), null, null)
        );
    }

    @Test
    void testConstructorExceptionIsDripTrueSharesIsZero() {
        assertThrows(IllegalArgumentException.class, () ->
            new RecordIncomeCommand(null, null, null, null, null, true, BigDecimal.valueOf(0), null, null)
        );
    }

    @Test
    void testConstructorExceptionIsDripTrueSharesIsOne() {
        assertDoesNotThrow(() ->
            new RecordIncomeCommand(null, null, null, null, null, true, BigDecimal.valueOf(1), null, null)
        );
    }

    @Test
    void testConstructorExceptionIsDripFalseSharesIsNull() {
        assertDoesNotThrow(() ->
            new RecordIncomeCommand(null, null, null, null, null, false, null, null, null)
        );
    }

    @Test
    void testConstructorExceptionIsDripFalseSharesNegative() {
        assertDoesNotThrow(() ->
            new RecordIncomeCommand(null, null, null, null, null, false, BigDecimal.valueOf(-2), null, null)
        );
    }

    @Test
    void testConstructorExceptionIsDripFalseSharesIsZero() {
        assertDoesNotThrow(() ->
            new RecordIncomeCommand(null, null, null, null, null, false, BigDecimal.valueOf(0), null, null)
        );
    }

    @Test
    void testConstructorExceptionIsDripFalseSharesIsOne() {
        assertDoesNotThrow(() ->
            new RecordIncomeCommand(null, null, null, null, null, false, BigDecimal.valueOf(1), null, null)
        );
    }

}
