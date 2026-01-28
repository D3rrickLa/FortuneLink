package com.laderrco.fortunelink.portfolio_management.domain.exceptions;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class PortfolioNotFoundExceptionTest {
    @Test
    void testException() {
        assertThrows(PortfolioNotFoundException.class, () -> {
            if ("123".length() != 12) {
                throw new PortfolioNotFoundException("test");
            }
        });
    }
}
