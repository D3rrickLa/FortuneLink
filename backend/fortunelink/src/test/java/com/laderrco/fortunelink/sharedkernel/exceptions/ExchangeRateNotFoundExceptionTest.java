package com.laderrco.fortunelink.sharedkernel.exceptions;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.CryptoSymbols;

public class ExchangeRateNotFoundExceptionTest {
    @Test
    void testThrow() {
        if (!CryptoSymbols.isCrypto("USD")) {
            assertThrows(ExchangeRateNotFoundException.class, () ->{
                throw new ExchangeRateNotFoundException("Some messasge");

            });
        }
    }
}
