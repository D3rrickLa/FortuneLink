package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models;

import static org.junit.Assert.assertThrows;

import org.junit.Test;

import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.ProviderAssetInfo;

public class ProviderAssetInfoTest {

    @Test
    public void testConstructorThrowIllegalArgExceptionWHenSymbolIsBlank() {
        assertThrows(IllegalArgumentException.class, () ->
            new ProviderAssetInfo("", "null", "null", "null", "null", "null", "null", "null")
        );
    }
    @Test
    public void testConstructorThrowIllegalArgExceptionWHenSymbolIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new ProviderAssetInfo(null, "null", "null", "null", "null", "null", "null", "null")
        );
    }
}
