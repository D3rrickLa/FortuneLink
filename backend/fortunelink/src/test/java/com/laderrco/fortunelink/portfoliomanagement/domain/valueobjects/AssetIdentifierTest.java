package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.AssetType;

class AssetIdentifierTest {

    // --- Helper constants ---
    private static final String VALID_ISIN = "US0378331005"; // Apple ISIN
    private static final String INVALID_ISIN = "INVALID123";
    private static final String CRYPTO_ID = "0x123456789abcdef";
    
    // --- Normal construction ---
    @Test
    void constructor_shouldCreateStockWithISIN() {
        AssetIdentifier asset = new AssetIdentifier(
                AssetType.STOCK,
                VALID_ISIN,
                Map.of("CUSIP", "037833100"),
                "Apple Inc.",
                "NASDAQ",
                "USD"
        );

        assertThat(asset.primaryId()).isEqualTo(VALID_ISIN.toUpperCase());
        assertThat(asset.alternativeIds()).containsEntry("CUSIP", "037833100");
        assertThat(asset.assetName()).isEqualTo("Apple Inc.");
        assertThat(asset.market()).isEqualTo("NASDAQ");
        assertThat(asset.unitOfTrade()).isEqualTo("USD");
    }

    @Test
    void constructor_shouldCreateCryptoWithContractAddress() {
        AssetIdentifier asset = new AssetIdentifier(
                AssetType.CRYPTO,
                CRYPTO_ID,
                null,
                "Ethereum",
                "Ethereum",
                "ETH"
        );

        assertThat(asset.primaryId()).isEqualTo(CRYPTO_ID.toUpperCase());
        assertThat(asset.alternativeIds()).isEmpty();
    }

    @Test
    void constructor_shouldTrimAndUppercasePrimaryId() {
        AssetIdentifier asset = new AssetIdentifier(
                AssetType.STOCK,
                "US0378331005",
                null,
                "Apple Inc.",
                "NASDAQ",
                "USD"
        );

        assertThat(asset.primaryId()).isEqualTo("US0378331005");
    }

    @Test
    void constructor_shouldTrimAndUppercasePrimaryIdWhenDefaultCase() {
        AssetIdentifier asset = new AssetIdentifier(
                AssetType.REAL_ESTATE,
                "888 Hollywood Blvd",
                null,
                "The Beast",
                "PRIVATE",
                "USD"
        );

        assertThat(asset.primaryId()).isEqualTo("888 HOLLYWOOD BLVD");
    }

    // --- PrimaryId validation ---
    @Test
    void constructor_shouldThrowForInvalidISIN() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AssetIdentifier(
                        AssetType.STOCK,
                        INVALID_ISIN,
                        null,
                        "Apple",
                        "NASDAQ",
                        "USD"
                ))
                .withMessageContaining("PrimaryId must be a valid ISIN");
    }
    
    @Test
    void constructor_shouldThrowForBlankId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AssetIdentifier(
                        AssetType.STOCK,
                        "\r\r",
                        null,
                        "Apple",
                        "NASDAQ",
                        "USD"
                ))
                .withMessageContaining("PrimaryId must be a valid ISIN");
    }

    @Test
    void constructor_shouldThrowForBlankCryptoId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AssetIdentifier(
                        AssetType.CRYPTO,
                        " ",
                        null,
                        "Ethereum",
                        "Ethereum",
                        "ETH"
                ))
                .withMessageContaining("contract address or symbol");
    }

    @Test
    void constructor_shouldThrowForBlankPrimaryIdOtherTypes() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AssetIdentifier(
                        AssetType.REAL_ESTATE,
                        "  ",
                        null,
                        "Parcel",
                        "Registry",
                        "sqft"
                ))
                .withMessageContaining("PrimaryId cannot be blank");
    }
    

    // --- Non-blank fields validation ---
    @Test
    void constructor_shouldThrowForBlankAssetName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AssetIdentifier(
                        AssetType.STOCK,
                        VALID_ISIN,
                        null,
                        " ",
                        "NASDAQ",
                        "USD"
                ))
                .withMessageContaining("Asset name cannot be blank");
    }

    @Test
    void constructor_shouldThrowForNullAssetName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AssetIdentifier(
                        AssetType.STOCK,
                        "US0378331005",
                        null,
                        null,
                        "NASDAQ",
                        "USD"
                ))
                .withMessageContaining("Asset name cannot be blank");
    }

    @Test
    void constructor_shouldThrowForBlankMarket() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AssetIdentifier(
                        AssetType.STOCK,
                        VALID_ISIN,
                        null,
                        "Apple",
                        " ",
                        "USD"
                ))
                .withMessageContaining("Market cannot be blank");
    }

    @Test
    void constructor_shouldThrowForBlankUnitOfTrade() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AssetIdentifier(
                        AssetType.STOCK,
                        VALID_ISIN,
                        null,
                        "Apple",
                        "NASDAQ",
                        " "
                ))
                .withMessageContaining("Unit of trade cannot be blank");
    }

    // --- Alternative IDs handling ---
    @Test
    void constructor_shouldCopyAlternativeIds() {
        Map<String, String> ids = Map.of("CUSIP", "037833100");
        AssetIdentifier asset = new AssetIdentifier(
                AssetType.STOCK,
                VALID_ISIN,
                ids,
                "Apple",
                "NASDAQ",
                "USD"
        );

        assertThat(asset.alternativeIds()).isEqualTo(ids);
        assertThatThrownBy(() -> asset.alternativeIds().put("SEDOL", "1234567"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void constructor_shouldSetEmptyMapIfAlternativeIdsNull() {
        AssetIdentifier asset = new AssetIdentifier(
                AssetType.STOCK,
                VALID_ISIN,
                null,
                "Apple",
                "NASDAQ",
                "USD"
        );

        assertThat(asset.alternativeIds()).isEmpty();
    }

}