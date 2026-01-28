package com.laderrco.fortunelink.portfolio_management.domain.models.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.laderrco.fortunelink.shared.enums.Precision;

public class AssetTypeTest {
    @Test
    void testGetDefaultQuantityPrecision() {
        AssetType tAssetType = AssetType.CASH;
        assertEquals(Precision.CASH, tAssetType.getDefaultQuantityPrecision());
    }

    @Test
    void testGetCategory() {
        AssetType tAssetType = AssetType.CASH;
        assertEquals(AssetCategory.CASH_EQUIVALENT, tAssetType.getCategory());
    }

    @ParameterizedTest
    @EnumSource(value = AssetType.class, names = {"BOND", "STOCK", "ETF"})
    void testIsMarketTraded(AssetType type) {
        AssetType tAssetType = AssetType.CASH;
        assertFalse(tAssetType.isMarketTraded());

        AssetType tAssetType2 = type;
        assertTrue(tAssetType2.isMarketTraded());
    }

    @Test
    void testIsCryptoTraded() {
        AssetType tAssetType = AssetType.CASH;
        assertFalse(tAssetType.isCryptoTraded());

        AssetType tAssetType2 = AssetType.CRYPTO;
        assertTrue(tAssetType2.isCryptoTraded());
    }
}
