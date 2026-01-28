package com.laderrco.fortunelink.portfolio_management.domain.exceptions;


import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class InvalidAssetSymbolExceptionTest {
    @Test
    void testException() {
        assertThrows(InvalidAssetSymbolException.class, () -> {
            if ("123".length() != 12) {
                throw new InvalidAssetSymbolException("test");
            }
        });
    }

}
