package com.laderrco.fortunelink.shared.exceptions;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

public class CurrencyAreTheSameExceptionTest {
    @Test
    void testException() {
        ValidatedCurrency USD = ValidatedCurrency.USD;
        ValidatedCurrency USD_2 = ValidatedCurrency.USD;

        assertThrows(CurrencyAreTheSameException.class, ()->{if (USD.equals(USD_2)) {
            throw new CurrencyAreTheSameException("Currency are the same, this can't be");
        }}
        );
        
    }
}
