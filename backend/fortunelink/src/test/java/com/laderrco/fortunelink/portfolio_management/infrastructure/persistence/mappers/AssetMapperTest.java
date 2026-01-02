package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CashIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CryptoIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.AccountEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.AssetEntity;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

@DisplayName("AssetMapper Tests")
class AssetMapperTest {

    private AssetMapper mapper;
    private AccountEntity testAccountEntity;
    private Instant testDate;

    @BeforeEach
    void setUp() {
        mapper = new AssetMapper();
        testDate = LocalDateTime.of(2025, 1, 2, 10, 0).toInstant(ZoneOffset.UTC);
        
        testAccountEntity = new AccountEntity();
        testAccountEntity.setId(UUID.randomUUID());
        testAccountEntity.setName("Test Account");
        testAccountEntity.setAccountType(AccountType.TFSA.toString());
    }

    @Nested
    @DisplayName("toEntity() - Domain to Entity Mapping")
    class ToEntityTests {

        @Test
        @DisplayName("Should map Asset with MarketIdentifier to entity")
        void shouldMapMarketAssetToEntity() {
            // Given
            MarketIdentifier identifier = new MarketIdentifier(
                "AAPL",
                Map.of("ISIN", "US0378331005", "CUSIP", "037833100"),
                AssetType.STOCK,
                "Apple Inc.",
                "shares",
                Map.of("exchange", "NASDAQ", "sector", "Technology")
            );
            
            Asset domain = createAsset(identifier, new BigDecimal("100"), new BigDecimal("15000.00"));

            // When
            AssetEntity entity = mapper.toEntity(domain, testAccountEntity);

            // Then
            assertThat(entity.getId()).isEqualTo(domain.getAssetId().assetId());
            assertThat(entity.getAccount()).isEqualTo(testAccountEntity);
            assertThat(entity.getIdentifierType()).isEqualTo("MARKET");
            assertThat(entity.getPrimaryId()).isEqualTo("AAPL");
            assertThat(entity.getName()).isEqualTo("Apple Inc.");
            assertThat(entity.getAssetType()).isEqualTo("STOCK");
            assertThat(entity.getSecondaryIds())
                .containsEntry("ISIN", "US0378331005")
                .containsEntry("CUSIP", "037833100");
            assertThat(entity.getUnitOfTrade()).isEqualTo("shares");
            assertThat(entity.getMetadata())
                .containsEntry("exchange", "NASDAQ")
                .containsEntry("sector", "Technology");
            assertThat(entity.getQuantity()).isEqualByComparingTo(new BigDecimal("100"));
            assertThat(entity.getCostBasisAmount()).isEqualByComparingTo(new BigDecimal("15000.00"));
            assertThat(entity.getCostBasisCurrency()).isEqualTo("USD");
            assertThat(entity.getAcquiredDate()).isEqualTo(testDate);
            assertThat(entity.getLastInteraction()).isEqualTo(testDate);
        }

        @Test
        @DisplayName("Should map Asset with CashIdentifier to entity")
        void shouldMapCashAssetToEntity() {
            // Given
            CashIdentifier identifier = new CashIdentifier("USD", ValidatedCurrency.of("USD"));
            Asset domain = createAsset(identifier, new BigDecimal("50000.00"), new BigDecimal("50000.00"));

            // When
            AssetEntity entity = mapper.toEntity(domain, testAccountEntity);

            // Then
            assertThat(entity.getIdentifierType()).isEqualTo("CASH");
            assertThat(entity.getPrimaryId()).isEqualTo("USD");
            assertThat(entity.getAssetType()).isEqualTo("CASH");
            assertThat(entity.getSecondaryIds()).isEmpty();
            assertThat(entity.getUnitOfTrade()).isNull();
            assertThat(entity.getMetadata()).isEmpty();
            assertThat(entity.getQuantity()).isEqualByComparingTo(new BigDecimal("50000.00"));
        }

        @Test
        @DisplayName("Should map Asset with CryptoIdentifier to entity")
        void shouldMapCryptoAssetToEntity() {
            // Given
            CryptoIdentifier identifier = new CryptoIdentifier(
                "BTC",
                "Bitcoin",
                AssetType.CRYPTO,
                "coins",
                Map.of("blockchain", "Bitcoin", "network", "mainnet")
            );
            Asset domain = createAsset(identifier, new BigDecimal("2.5"), new BigDecimal("112500.00"));

            // When
            AssetEntity entity = mapper.toEntity(domain, testAccountEntity);

            // Then
            assertThat(entity.getIdentifierType()).isEqualTo("CRYPTO");
            assertThat(entity.getPrimaryId()).isEqualTo("BTC");
            assertThat(entity.getName()).isEqualTo("Bitcoin");
            assertThat(entity.getAssetType()).isEqualTo("CRYPTO");
            assertThat(entity.getSecondaryIds()).isEmpty();
            assertThat(entity.getUnitOfTrade()).isEqualTo("coins");
            assertThat(entity.getMetadata())
                .containsEntry("blockchain", "Bitcoin")
                .containsEntry("network", "mainnet");
        }

        @Test
        @DisplayName("Should handle null metadata and secondaryIds gracefully")
        void shouldHandleNullCollections() {
            // Given - MarketIdentifier with null collections
            MarketIdentifier identifier = new MarketIdentifier(
                "MSFT",
                null, // null secondaryIds
                AssetType.STOCK,
                "Microsoft",
                "shares",
                null // null metadata
            );
            Asset domain = createAsset(identifier, BigDecimal.TEN, new BigDecimal("3000.00"));

            // When
            AssetEntity entity = mapper.toEntity(domain, testAccountEntity);

            // Then
            assertThat(entity.getSecondaryIds()).isNotNull().isEmpty();
            assertThat(entity.getMetadata()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should set version from domain")
        void shouldSetVersionFromDomain() {
            // Given
            MarketIdentifier identifier = new MarketIdentifier(
                "GOOGL", Collections.emptyMap(), AssetType.STOCK, 
                "Alphabet", "shares", Collections.emptyMap()
            );
            Asset domain = Asset.reconstitute(
                new AssetId(UUID.randomUUID()),
                identifier,
                "USD",
                new BigDecimal("50"),
                new BigDecimal("7500.00"),
                "USD",
                testDate,
                testDate,
                5 // version
            );

            // When
            AssetEntity entity = mapper.toEntity(domain, testAccountEntity);

            // Then
            assertThat(entity.getVersion()).isEqualTo(5L);
        }

        @Test
        @DisplayName("Should throw NPE when domain is null")
        void shouldThrowWhenDomainIsNull() {
            assertThatThrownBy(() -> mapper.toEntity(null, testAccountEntity))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Domain asset cannot be null");
        }

        @Test
        @DisplayName("Should throw NPE when account entity is null")
        void shouldThrowWhenAccountEntityIsNull() {
            MarketIdentifier identifier = new MarketIdentifier(
                "AAPL", Collections.emptyMap(), AssetType.STOCK,
                "Apple", "shares", Collections.emptyMap()
            );
            Asset domain = createAsset(identifier, BigDecimal.TEN, new BigDecimal("1500.00"));
            
            assertThatThrownBy(() -> mapper.toEntity(domain, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Account entity cannot be null");
        }
    }

    @Nested
    @DisplayName("updateEntityFromDomain() - Entity Update")
    class UpdateEntityTests {

        @Test
        @DisplayName("Should update existing entity with new domain state")
        void shouldUpdateEntity() {
            // Given
            AssetEntity existingEntity = new AssetEntity();
            existingEntity.setId(UUID.randomUUID());
            existingEntity.setAccount(testAccountEntity);
            existingEntity.setIdentifierType("MARKET");
            existingEntity.setPrimaryId("AAPL");
            existingEntity.setQuantity(new BigDecimal("50"));
            
            MarketIdentifier newIdentifier = new MarketIdentifier(
                "AAPL",
                Map.of("ISIN", "US0378331005"),
                AssetType.STOCK,
                "Apple Inc.",
                "shares",
                Map.of("exchange", "NASDAQ")
            );
            Asset updatedDomain = createAsset(newIdentifier, new BigDecimal("150"), new BigDecimal("22500.00"));

            // When
            mapper.updateEntityFromDomain(updatedDomain, existingEntity);

            // Then
            assertThat(existingEntity.getQuantity()).isEqualByComparingTo(new BigDecimal("150"));
            assertThat(existingEntity.getCostBasisAmount()).isEqualByComparingTo(new BigDecimal("22500.00"));
            assertThat(existingEntity.getSecondaryIds()).containsEntry("ISIN", "US0378331005");
            assertThat(existingEntity.getMetadata()).containsEntry("exchange", "NASDAQ");
        }

        @Test
        @DisplayName("Should throw NPE when domain is null")
        void shouldThrowWhenDomainIsNull() {
            AssetEntity entity = new AssetEntity();
            
            assertThatThrownBy(() -> mapper.updateEntityFromDomain(null, entity))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Domain asset cannot be null");
        }

        @Test
        @DisplayName("Should throw NPE when entity is null")
        void shouldThrowWhenEntityIsNull() {
            MarketIdentifier identifier = new MarketIdentifier(
                "AAPL", Collections.emptyMap(), AssetType.STOCK,
                "Apple", "shares", Collections.emptyMap()
            );
            Asset domain = createAsset(identifier, BigDecimal.TEN, new BigDecimal("1500.00"));
            
            assertThatThrownBy(() -> mapper.updateEntityFromDomain(domain, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Entity cannot be null");
        }
    }

    @Nested
    @DisplayName("toDomain() - Entity to Domain Mapping")
    class ToDomainTests {

        @Test
        @DisplayName("Should map entity with MARKET identifier to domain")
        void shouldMapMarketEntityToDomain() {
            // Given
            AssetEntity entity = createMarketEntity("AAPL", "Apple Inc.", AssetType.STOCK);

            // When
            Asset domain = mapper.toDomain(entity);

            // Then
            assertThat(domain.getAssetId().assetId()).isEqualTo(entity.getId());
            assertThat(domain.getAssetIdentifier()).isInstanceOf(MarketIdentifier.class);
            
            MarketIdentifier identifier = (MarketIdentifier) domain.getAssetIdentifier();
            assertThat(identifier.getPrimaryId()).isEqualTo("AAPL");
            assertThat(identifier.name()).isEqualTo("Apple Inc.");
            assertThat(identifier.assetType()).isEqualTo(AssetType.STOCK);
            assertThat(identifier.secondaryIds()).containsEntry("ISIN", "US0378331005");
            
            assertThat(domain.getQuantity()).isEqualByComparingTo(new BigDecimal("100"));
            assertThat(domain.getCostBasis().amount()).isEqualByComparingTo(new BigDecimal("15000.00"));
            assertThat(domain.getCostBasis().currency().getCode()).isEqualTo("USD");
            assertThat(domain.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should map entity with CASH identifier to domain")
        void shouldMapCashEntityToDomain() {
            // Given
            AssetEntity entity = createCashEntity("EUR");

            // When
            Asset domain = mapper.toDomain(entity);

            // Then
            assertThat(domain.getAssetIdentifier()).isInstanceOf(CashIdentifier.class);
            
            CashIdentifier identifier = (CashIdentifier) domain.getAssetIdentifier();
            assertThat(identifier.getPrimaryId()).isEqualTo("EUR");
            assertThat(identifier.currency().getCode()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("Should map entity with CRYPTO identifier to domain")
        void shouldMapCryptoEntityToDomain() {
            // Given
            AssetEntity entity = createCryptoEntity("ETH", "Ethereum");

            // When
            Asset domain = mapper.toDomain(entity);

            // Then
            assertThat(domain.getAssetIdentifier()).isInstanceOf(CryptoIdentifier.class);
            
            CryptoIdentifier identifier = (CryptoIdentifier) domain.getAssetIdentifier();
            assertThat(identifier.getPrimaryId()).isEqualTo("ETH");
            assertThat(identifier.displayName()).isEqualTo("Ethereum");
            assertThat(identifier.assetType()).isEqualTo(AssetType.CRYPTO);
        }

        @Test
        @DisplayName("Should handle null version as version 0")
        void shouldHandleNullVersion() {
            // Given
            AssetEntity entity = createMarketEntity("TSLA", "Tesla", AssetType.STOCK);
            entity.setVersion(null);

            // When
            Asset domain = mapper.toDomain(entity);

            // Then
            assertThat(domain.getVersion()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle null collections in entity")
        void shouldHandleNullCollectionsInEntity() {
            // Given
            AssetEntity entity = createMarketEntity("NVDA", "NVIDIA", AssetType.STOCK);
            entity.setSecondaryIds(null);
            entity.setMetadata(null);

            // When
            Asset domain = mapper.toDomain(entity);

            // Then
            MarketIdentifier identifier = (MarketIdentifier) domain.getAssetIdentifier();
            assertThat(identifier.secondaryIds()).isEmpty();
            assertThat(identifier.metadata()).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception for null entity")
        void shouldThrowForNullEntity() {
            assertThatThrownBy(() -> mapper.toDomain(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Entity cannot be null");
        }

        @Test
        @DisplayName("Should throw exception for unknown identifier type")
        void shouldThrowForUnknownIdentifierType() {
            // Given
            AssetEntity entity = createMarketEntity("AAPL", "Apple", AssetType.STOCK);
            entity.setIdentifierType("UNKNOWN_TYPE");

            // When/Then
            assertThatThrownBy(() -> mapper.toDomain(entity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unknown identifier type 'UNKNOWN_TYPE'");
        }

        @Test
        @DisplayName("Should throw exception for null identifier type")
        void shouldThrowForNullIdentifierType() {
            // Given
            AssetEntity entity = createMarketEntity("AAPL", "Apple", AssetType.STOCK);
            entity.setIdentifierType(null);

            // When/Then
            assertThatThrownBy(() -> mapper.toDomain(entity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has null identifier type");
        }

        @Test
        @DisplayName("Should throw exception for invalid asset type")
        void shouldThrowForInvalidAssetType() {
            // Given
            AssetEntity entity = createMarketEntity("AAPL", "Apple", AssetType.STOCK);
            entity.setAssetType("INVALID_TYPE");

            // When/Then
            assertThatThrownBy(() -> mapper.toDomain(entity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid asset type 'INVALID_TYPE'")
                .hasMessageContaining("Valid types:");
        }

        @ParameterizedTest
        @ValueSource(strings = {"id", "identifierType", "primaryId", "quantity", "costBasisAmount", "costBasisCurrency"})
        @DisplayName("Should detect data corruption for missing required fields")
        void shouldDetectDataCorruption(String fieldToNull) {
            // Given
            AssetEntity entity = createMarketEntity("AAPL", "Apple", AssetType.STOCK);
            
            // Corrupt the entity by nulling a required field
            switch (fieldToNull) {
                case "id" -> entity.setId(null);
                case "identifierType" -> entity.setIdentifierType(null);
                case "primaryId" -> entity.setPrimaryId(null);
                case "quantity" -> entity.setQuantity(null);
                case "costBasisAmount" -> entity.setCostBasisAmount(null);
                case "costBasisCurrency" -> entity.setCostBasisCurrency(null);
            }

            // When/Then
            assertThatThrownBy(() -> mapper.toDomain(entity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("null");
        }
    }

    @Nested
    @DisplayName("toIdentifier() - Identifier Conversion")
    class ToIdentifierTests {

        @Test
        @DisplayName("Should convert MARKET entity to MarketIdentifier")
        void shouldConvertToMarketIdentifier() {
            // Given
            AssetEntity entity = createMarketEntity("AAPL", "Apple Inc.", AssetType.STOCK);

            // When
            AssetIdentifier identifier = mapper.toIdentifier(entity);

            // Then
            assertThat(identifier).isInstanceOf(MarketIdentifier.class);
            MarketIdentifier market = (MarketIdentifier) identifier;
            assertThat(market.getPrimaryId()).isEqualTo("AAPL");
            assertThat(market.assetType()).isEqualTo(AssetType.STOCK);
        }

        @Test
        @DisplayName("Should convert CASH entity to CashIdentifier")
        void shouldConvertToCashIdentifier() {
            // Given
            AssetEntity entity = createCashEntity("GBP");

            // When
            AssetIdentifier identifier = mapper.toIdentifier(entity);

            // Then
            assertThat(identifier).isInstanceOf(CashIdentifier.class);
            CashIdentifier cash = (CashIdentifier) identifier;
            assertThat(cash.getPrimaryId()).isEqualTo("GBP");
        }

        @Test
        @DisplayName("Should convert CRYPTO entity to CryptoIdentifier")
        void shouldConvertToCryptoIdentifier() {
            // Given
            AssetEntity entity = createCryptoEntity("BTC", "Bitcoin");

            // When
            AssetIdentifier identifier = mapper.toIdentifier(entity);

            // Then
            assertThat(identifier).isInstanceOf(CryptoIdentifier.class);
            CryptoIdentifier crypto = (CryptoIdentifier) identifier;
            assertThat(crypto.getPrimaryId()).isEqualTo("BTC");
        }
    }

    @Nested
    @DisplayName("Round-Trip Mapping")
    class RoundTripTests {

        @Test
        @DisplayName("Should maintain data integrity through entity->domain->entity cycle")
        void shouldMaintainIntegrityThroughRoundTrip() {
            // Given
            AssetEntity originalEntity = createMarketEntity("AAPL", "Apple Inc.", AssetType.STOCK);

            // When
            Asset domain = mapper.toDomain(originalEntity);
            AssetEntity mappedEntity = mapper.toEntity(domain, testAccountEntity);

            // Then
            assertThat(mappedEntity.getId()).isEqualTo(originalEntity.getId());
            assertThat(mappedEntity.getIdentifierType()).isEqualTo(originalEntity.getIdentifierType());
            assertThat(mappedEntity.getPrimaryId()).isEqualTo(originalEntity.getPrimaryId());
            assertThat(mappedEntity.getName()).isEqualTo(originalEntity.getName());
            assertThat(mappedEntity.getAssetType()).isEqualTo(originalEntity.getAssetType());
            assertThat(mappedEntity.getQuantity()).isEqualByComparingTo(originalEntity.getQuantity());
            assertThat(mappedEntity.getCostBasisAmount()).isEqualByComparingTo(originalEntity.getCostBasisAmount());
            assertThat(mappedEntity.getCostBasisCurrency()).isEqualTo(originalEntity.getCostBasisCurrency());
        }

        @Test
        @DisplayName("Should maintain data integrity for all identifier types")
        void shouldMaintainIntegrityForAllTypes() {
            // Test Market
            AssetEntity marketEntity = createMarketEntity("GOOGL", "Alphabet", AssetType.STOCK);
            Asset marketDomain = mapper.toDomain(marketEntity);
            AssetEntity marketMapped = mapper.toEntity(marketDomain, testAccountEntity);
            assertThat(marketMapped.getIdentifierType()).isEqualTo("MARKET");

            // Test Cash
            AssetEntity cashEntity = createCashEntity("JPY");
            Asset cashDomain = mapper.toDomain(cashEntity);
            AssetEntity cashMapped = mapper.toEntity(cashDomain, testAccountEntity);
            assertThat(cashMapped.getIdentifierType()).isEqualTo("CASH");

            // Test Crypto
            AssetEntity cryptoEntity = createCryptoEntity("SOL", "Solana");
            Asset cryptoDomain = mapper.toDomain(cryptoEntity);
            AssetEntity cryptoMapped = mapper.toEntity(cryptoDomain, testAccountEntity);
            assertThat(cryptoMapped.getIdentifierType()).isEqualTo("CRYPTO");
        }
    }

    // Helper Methods

    private Asset createAsset(AssetIdentifier identifier, BigDecimal quantity, BigDecimal costBasis) {
        return Asset.reconstitute(
            new AssetId(UUID.randomUUID()),
            identifier,
            "USD",
            quantity,
            costBasis,
            "USD",
            testDate,
            testDate,
            1
        );
    }

    private AssetEntity createMarketEntity(String symbol, String name, AssetType assetType) {
        AssetEntity entity = new AssetEntity();
        entity.setId(UUID.randomUUID());
        entity.setAccount(testAccountEntity);
        entity.setIdentifierType("MARKET");
        entity.setPrimaryId(symbol);
        entity.setName(name);
        entity.setAssetType(assetType.name());
        entity.setSecondaryIds(Map.of("ISIN", "US0378331005"));
        entity.setUnitOfTrade("shares");
        entity.setMetadata(Map.of("exchange", "NASDAQ"));
        entity.setQuantity(new BigDecimal("100"));
        entity.setCostBasisAmount(new BigDecimal("15000.00"));
        entity.setCostBasisCurrency("USD");
        entity.setAcquiredDate(testDate);
        entity.setLastInteraction(testDate);
        return entity;
    }

    private AssetEntity createCashEntity(String currency) {
        AssetEntity entity = new AssetEntity();
        entity.setId(UUID.randomUUID());
        entity.setAccount(testAccountEntity);
        entity.setIdentifierType("CASH");
        entity.setPrimaryId(currency);
        entity.setName(currency);
        entity.setAssetType("CASH");
        entity.setSecondaryIds(Collections.emptyMap());
        entity.setUnitOfTrade(null);
        entity.setMetadata(Collections.emptyMap());
        entity.setQuantity(new BigDecimal("50000.00"));
        entity.setCostBasisAmount(new BigDecimal("50000.00"));
        entity.setCostBasisCurrency(currency);
        entity.setAcquiredDate(testDate);
        entity.setLastInteraction(testDate);
        return entity;
    }

    private AssetEntity createCryptoEntity(String symbol, String name) {
        AssetEntity entity = new AssetEntity();
        entity.setId(UUID.randomUUID());
        entity.setAccount(testAccountEntity);
        entity.setIdentifierType("CRYPTO");
        entity.setPrimaryId(symbol);
        entity.setName(name);
        entity.setAssetType("CRYPTO");
        entity.setSecondaryIds(Collections.emptyMap());
        entity.setUnitOfTrade("coins");
        entity.setMetadata(Map.of("blockchain", symbol));
        entity.setQuantity(new BigDecimal("2.5"));
        entity.setCostBasisAmount(new BigDecimal("100000.00"));
        entity.setCostBasisCurrency("USD");
        entity.setAcquiredDate(testDate);
        entity.setLastInteraction(testDate);
        return entity;
    }
}