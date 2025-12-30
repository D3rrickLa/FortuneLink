package com.laderrco.fortunelink.portfolio_management.application.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AccountNotEmptyExceptionTest {
    @Test
    void testGeneric() {
        try {
            if (true) {
                throw new AccountNotEmptyException("Test");
            }
        }
        catch (AccountNotEmptyException e) {
            assertEquals(e, e);
        }
    }
}
