package com.laderrco.fortunelink.portfolio_management.domain.models.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.shared.enums.Precision;

public class AssetTypeTest {
    @Test
    void testGetDefaultQuantityPrecision() {
        AssetType tAssetType = AssetType.CASH;
        assertEquals(Precision.CASH, tAssetType.getDefaultQuantityPrecision());
    }
}
