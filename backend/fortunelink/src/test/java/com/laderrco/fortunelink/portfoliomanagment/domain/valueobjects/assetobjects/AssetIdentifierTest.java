package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;

public class AssetIdentifierTest {
    private static final String VALID_ISIN = "US1234567890";

    @Test
    void constructor_shouldCreateValidInstance() {
        AssetIdentifier identifier = new AssetIdentifier(
            AssetType.STOCK,
            "AAPL",
            VALID_ISIN,
            "Apple Inc.",
            "NASDAQ"
        );

        assertEquals(AssetType.STOCK, identifier.type());
        assertEquals("US1234567890", identifier.isin()); // Should be uppercase
        assertEquals("APPLE INC.", identifier.assetName().toUpperCase());
        assertEquals("NASDAQ", identifier.assetExchangeName());
    }

    @Test
    void constructor_shouldThrowIfTypeIsNull() {
        Exception ex = assertThrows(NullPointerException.class, () ->
            new AssetIdentifier(null, "AAPL", VALID_ISIN, "Apple", "NASDAQ")
        );
        assertTrue(ex.getMessage().contains("Asset Type cannot be null."));
    }

    @Test
    void constructor_shouldThrowIfSymbolIsNull() {
        Exception ex = assertThrows(NullPointerException.class, () ->
            new AssetIdentifier(AssetType.STOCK, null, VALID_ISIN, "Apple", "NASDAQ")
        );
        assertTrue(ex.getMessage().contains("Asset Symbol cannot be null."));
    }

    @Test
    void constructor_shouldThrowIfISINIsNull() {
        Exception ex = assertThrows(NullPointerException.class, () ->
            new AssetIdentifier(AssetType.STOCK, "AAPL", null, "Apple", "NASDAQ")
        );
        assertTrue(ex.getMessage().contains("ISIN cannot be null."));
    }

    @Test
    void constructor_shouldThrowIfAssetNameIsNull() {
        Exception ex = assertThrows(NullPointerException.class, () ->
            new AssetIdentifier(AssetType.STOCK, "AAPL", VALID_ISIN, null, "NASDAQ")
        );
        assertTrue(ex.getMessage().contains("Assest Name cannot be null."));
    }

    @Test
    void constructor_shouldThrowIfAssetExchangeIsNull() {
        Exception ex = assertThrows(NullPointerException.class, () ->
            new AssetIdentifier(AssetType.STOCK, "AAPL", VALID_ISIN, "Apple", null)
        );
        assertTrue(ex.getMessage().contains("Asset Exchange cannot be null."));
    }

    @Test
    void constructor_shouldThrowIfAssetNameIsBlank() {
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
            new AssetIdentifier(AssetType.STOCK, "AAPL", VALID_ISIN, "   ", "NASDAQ")
        );
        assertTrue(ex.getMessage().contains("Asset name cannot be blank."));
    }

    @Test
    void constructor_shouldThrowIfExchangeIsBlank() {
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
            new AssetIdentifier(AssetType.STOCK, "AAPL", VALID_ISIN, "Apple", "   ")
        );
        assertTrue(ex.getMessage().contains("Asset name cannot be blank."));
    }

    @Test
    void constructor_shouldThrowIfISINFormatIsInvalid() {
        String badIsin = "123456789"; // Too short
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
            new AssetIdentifier(AssetType.STOCK, "AAPL", badIsin, "Apple", "NASDAQ")
        );
        assertTrue(ex.getMessage().contains("Invalid ISIN format."));
    }

    @Test
    void isCrypto_shouldReturnTrueForCryptoType() {
        AssetIdentifier identifier = new AssetIdentifier(
            AssetType.CRYPTO, "BTC", VALID_ISIN, "Bitcoin", "Coinbase"
        );
        assertTrue(identifier.isCrypto());
    }

    @Test
    void isCrypto_shouldReturnFalseForNonCryptoType() {
        AssetIdentifier identifier = new AssetIdentifier(
            AssetType.STOCK, "AAPL", VALID_ISIN, "Apple", "NASDAQ"
        );
        assertFalse(identifier.isCrypto());
    }

    @Test
    void isStockOrEtf_shouldReturnTrueForStockOrEtf() {
        AssetIdentifier stock = new AssetIdentifier(
            AssetType.STOCK, "AAPL", VALID_ISIN, "Apple", "NASDAQ"
        );
        AssetIdentifier etf = new AssetIdentifier(
            AssetType.ETF, "VOO", VALID_ISIN, "Vanguard", "NYSE"
        );
        assertTrue(stock.isStockOrEtf());
        assertTrue(etf.isStockOrEtf());
    }

    @Test
    void isStockOrEtf_shouldReturnFalseForOtherAssetType() {
        AssetIdentifier crypto = new AssetIdentifier(
            AssetType.CRYPTO, "ETH", VALID_ISIN, "Ethereum", "Binance"
        );
        assertFalse(crypto.isStockOrEtf());
    }
}
