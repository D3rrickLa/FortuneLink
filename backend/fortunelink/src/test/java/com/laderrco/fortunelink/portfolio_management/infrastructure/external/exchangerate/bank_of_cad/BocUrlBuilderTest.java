package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.bank_of_cad;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

public class BocUrlBuilderTest {
    
    @Test
    void builderReturnsNull_whenQueryIsEmpty() {
        BocUrlBuilder bocUrlBuilder = new BocUrlBuilder("test.com");
        String s = bocUrlBuilder.build();
        assertEquals(s, String.valueOf("test.com"));
    }
}
