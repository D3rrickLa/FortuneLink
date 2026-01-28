package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;

public class CryptoIdentifierTest {
    private CryptoIdentifier testIdentifier;
    private String Id;
    private AssetType assetType;
    private String name;
    private String unitOfTrade;

    @BeforeEach
    void init() {
        Id = "BTC";
        assetType = AssetType.CRYPTO;
        name = "Bitcoin";
        unitOfTrade = "Bitcoin";
        testIdentifier = new CryptoIdentifier(Id, name, assetType, unitOfTrade, null);
    }

    @Test
    void testConstructor_Success() {
        assertEquals("BTC", testIdentifier.getPrimaryId());;
        assertEquals("Bitcoin", testIdentifier.displayName());
        assertEquals(AssetType.CRYPTO, testIdentifier.getAssetType());
    }

    @Test
    void testConstructor_FailsWhenNullPass() {
        assertThrows(NullPointerException.class, () -> {
            new CryptoIdentifier(null, name, assetType, unitOfTrade, null);
            new CryptoIdentifier(Id, null, assetType, unitOfTrade, null);
            new CryptoIdentifier(Id, name, null, unitOfTrade, null);
            new CryptoIdentifier(Id, name, assetType, null, null);
        });
    }

    @Test
    void testConstructor_FailsWhenBlankNamePass() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CryptoIdentifier(Id, " ", assetType, unitOfTrade, null);
        });
    }

    @Test
    void testConstructor_FailsWhenTypeNotCrypto() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CryptoIdentifier(Id, "Bitcoin", AssetType.BOND, unitOfTrade, null);
        });
    }
}
