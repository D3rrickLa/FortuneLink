package com.laderrco.fortunelink.portfolio_management.domain.models.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class FeeTypeTest {
    @Test
    void testGetCategory() {
        FeeType tFeeType = FeeType.BROKERAGE;
        assertEquals(FeeCategory.TRADING, tFeeType.getCategory());
    }
}
