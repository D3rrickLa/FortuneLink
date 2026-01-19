package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
public class ProviderAssetInfoTest {

    @Test
    void testConstructorThrowIllegalArgExceptionWHenSymbolIsBlank() {
        assertThrows(IllegalArgumentException.class, () ->
            new ProviderAssetInfo(" ", "null", "null", "null", "null", "null", "null", "null")
        );
    }
    @Test
    void testConstructorThrowIllegalArgExceptionWHenSymbolIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new ProviderAssetInfo(null, "null", "null", "null", "null", "null", "null", "null")
        );
    }

    @Test
    void successfulConstructor() {
        assertDoesNotThrow(() -> new ProviderAssetInfo("something", "null", "null", "null", "null", "null", "null", "null"));
    }
}
