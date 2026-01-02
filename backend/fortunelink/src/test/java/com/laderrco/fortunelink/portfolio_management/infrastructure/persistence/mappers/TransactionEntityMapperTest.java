package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.FeeType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CashIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CryptoIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.AccountEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.TransactionEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.TransactionFeeEntity;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("TransactionEntityMapper Tests")
class TransactionEntityMapperTest {

    private TransactionEntityMapper mapper;
    private AccountEntity testAccountEntity;
    private Instant testDate;

    @BeforeEach
    void setUp() {
        mapper = new TransactionEntityMapper();
        testDate = LocalDateTime.of(2025, 1, 2, 10, 0).toInstant(ZoneOffset.UTC);

        testAccountEntity = new AccountEntity();
        testAccountEntity.setId(UUID.randomUUID());
        testAccountEntity.setName("Test Account");
        testAccountEntity.setAccountType(AccountType.TFSA.toString());
    }

    @Nested
    @DisplayName("toDomain() - Entity to Domain Mapping")
    class ToDomainTests {

        @Test
        @DisplayName("Should map basic BUY transaction with MarketIdentifier")
        void shouldMapBasicBuyTransaction() {
            // Given
            TransactionEntity entity = createBasicTransactionEntity(TransactionType.BUY);
            entity.setAssetType("STOCK");
            entity.setPrimaryId("AAPL");
            entity.setSecondaryIds(Map.of("ISIN", "US0378331005"));
            entity.setDisplayName("Apple Inc.");
            entity.setUnitOfTrade("shares");
            entity.setMetadata(Map.of("exchange", "NASDAQ"));
            entity.setIsDrip(false);

            // When
            Transaction domain = mapper.toDomain(entity);

            // Then
            assertThat(domain).isNotNull();
            assertThat(domain.getTransactionId().transactionId()).isEqualTo(entity.getId());
            assertThat(domain.getTransactionType()).isEqualTo(TransactionType.BUY);
            assertThat(domain.getQuantity()).isEqualByComparingTo(new BigDecimal("10"));
            assertThat(domain.getPricePerUnit().amount()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(domain.getPricePerUnit().currency().getSymbol()).isEqualTo("US$");

            assertThat(domain.getAssetIdentifier()).isInstanceOf(MarketIdentifier.class);
            MarketIdentifier identifier = (MarketIdentifier) domain.getAssetIdentifier();
            assertThat(identifier.getPrimaryId()).isEqualTo("AAPL");
            assertThat(identifier.name()).isEqualTo("Apple Inc.");
            assertThat(identifier.unitOfTrade()).isEqualTo("shares");
        }

        @Test
        @DisplayName("Should map transaction with CashIdentifier")
        void shouldMapCashTransaction() {
            // Given
            TransactionEntity entity = createBasicTransactionEntity(TransactionType.DEPOSIT);
            entity.setAssetType("CASH");
            entity.setPrimaryId("USD");
            entity.setIsDrip(false);

            // When
            Transaction domain = mapper.toDomain(entity);

            // Then
            assertThat(domain.getAssetIdentifier()).isInstanceOf(CashIdentifier.class);
            CashIdentifier identifier = (CashIdentifier) domain.getAssetIdentifier();
            assertThat(identifier.getPrimaryId()).isEqualTo("USD");
            assertThat(identifier.currency().getCode()).isEqualTo("USD");
        }

        @Test
        @DisplayName("Should map transaction with CryptoIdentifier")
        void shouldMapCryptoTransaction() {
            // Given
            TransactionEntity entity = createBasicTransactionEntity(TransactionType.BUY);
            entity.setAssetType("CRYPTO");
            entity.setPrimaryId("BTC");
            entity.setDisplayName("Bitcoin");
            entity.setUnitOfTrade("coins");
            entity.setMetadata(Map.of("blockchain", "Bitcoin"));
            entity.setIsDrip(false);

            // When
            Transaction domain = mapper.toDomain(entity);

            // Then
            assertThat(domain.getAssetIdentifier()).isInstanceOf(CryptoIdentifier.class);
            CryptoIdentifier identifier = (CryptoIdentifier) domain.getAssetIdentifier();
            assertThat(identifier.getPrimaryId()).isEqualTo("BTC");
            assertThat(identifier.displayName()).isEqualTo("Bitcoin");
            assertThat(identifier.unitOfTrade()).isEqualTo("coins");
        }

        @Test
        @DisplayName("Should map DEPOSIT transaction without asset identifier")
        void shouldMapDeposit() {
            // Given
            TransactionEntity entity = createBasicTransactionEntity(TransactionType.DEPOSIT);
            entity.setAssetType(AssetType.CASH.toString()); // DEPOSIT can have null identifier (cash deposit)
            entity.setIsDrip(false);
            entity.setPrimaryId("USD");
            // When
            Transaction domain = mapper.toDomain(entity);

            // Then
            assertThat(domain.getAssetIdentifier()).isNotNull();
            assertThat(domain.getAssetIdentifier()).isInstanceOf(CashIdentifier.class);
            assertThat(domain.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        }

        @Test
        @DisplayName("Should map WITHDRAWAL transaction without asset identifier")
        void shouldMapWithdrawal() {
            // Given
            TransactionEntity entity = createBasicTransactionEntity(TransactionType.WITHDRAWAL);
            entity.setAssetType(AssetType.CASH.toString()); // WITHDRAWAL can have null identifier (cash withdrawal)
            entity.setIsDrip(false);
            entity.setPrimaryId("USD");

            // When
            Transaction domain = mapper.toDomain(entity);

            // Then
            assertThat(domain.getAssetIdentifier()).isNotNull();
            assertThat(domain.getAssetIdentifier()).isInstanceOf(CashIdentifier.class);
            assertThat(domain.getTransactionType()).isEqualTo(TransactionType.WITHDRAWAL);
        }

        @Test
        @DisplayName("Should reject BUY transaction without asset identifier")
        void shouldRejectBuyWithoutIdentifier() {
            // Given
            TransactionEntity entity = createBasicTransactionEntity(TransactionType.BUY);
            entity.setAssetType(null); // BUY requires an identifier

            // When/Then - Domain validation should fail
            assertThatThrownBy(() -> mapper.toDomain(entity))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("BUY has no asset identifier");
        }

        @Test
        @DisplayName("Should reject SELL transaction without asset identifier")
        void shouldRejectSellWithoutIdentifier() {
            // Given
            TransactionEntity entity = createBasicTransactionEntity(TransactionType.SELL);
            entity.setAssetType(null); // SELL requires an identifier

            // When/Then
            assertThatThrownBy(() -> mapper.toDomain(entity))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SELL has no asset identifier");
        }

        @Test
        @DisplayName("Should reject DIVIDEND transaction without asset identifier")
        void shouldRejectDividendWithoutIdentifier() {
            // Given
            TransactionEntity entity = createBasicTransactionEntity(TransactionType.DIVIDEND);
            entity.setAssetType(null); // DIVIDEND requires an identifier

            // When/Then
            assertThatThrownBy(() -> mapper.toDomain(entity))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DIVIDEND has no asset identifier");
        }

        @Test
        @DisplayName("Should map DIVIDEND transaction with dividend amount")
        void shouldMapDividendTransaction() {
            // Given
            TransactionEntity entity = createBasicTransactionEntity(TransactionType.DIVIDEND);
            entity.setPrimaryId("AAPL");
            entity.setSecondaryIds(null);
            entity.setDisplayName("APPLE");
            entity.setUnitOfTrade("USD");

            entity.setAssetType("STOCK");

            entity.setDividendAmount(new BigDecimal("25.50"));
            entity.setDividendCurrency("USD");
            entity.setIsDrip(true);

            // When
            Transaction domain = mapper.toDomain(entity);

            // Then
            assertThat(domain.getDividendAmount()).isNotNull();
            assertThat(domain.getDividendAmount().amount()).isEqualByComparingTo(new BigDecimal("25.50"));
            assertThat(domain.getDividendAmount().currency().getCode()).isEqualTo("USD");
            assertThat(domain.isDrip()).isTrue();
        }

        @Test
        @DisplayName("Should map transaction without dividend amount")
        void shouldMapTransactionWithoutDividend() {
            // Given
            TransactionEntity entity = createBasicTransactionEntity(TransactionType.BUY);
            entity.setDividendAmount(null);
            entity.setIsDrip(false);
            entity.setPrimaryId("AAPL");
            entity.setSecondaryIds(null);
            entity.setDisplayName("APPLE");
            entity.setUnitOfTrade("USD");

            entity.setAssetType("STOCK");

            // When
            Transaction domain = mapper.toDomain(entity);

            // Then
            assertThrows(IllegalStateException.class, () -> domain.getDividendAmount());
        }

        @Test
        @DisplayName("Should map transaction with fees")
        void shouldMapTransactionWithFees() {
            // Given
            TransactionEntity entity = createBasicTransactionEntity(TransactionType.BUY);
            entity.setDividendAmount(null);
            entity.setIsDrip(false);
            entity.setPrimaryId("AAPL");
            entity.setSecondaryIds(null);
            entity.setDisplayName("APPLE");
            entity.setUnitOfTrade("USD");

            entity.setAssetType("STOCK");


            TransactionFeeEntity fee1 = new TransactionFeeEntity();
            fee1.setFeeType(FeeType.COMMISSION);
            fee1.setAmount(new BigDecimal("9.99"));
            fee1.setCurrency("USD");
            fee1.setFeeDate(testDate);
            fee1.setFromCurrency("USD");
            fee1.setToCurrency("USD");
            fee1.setRate(BigDecimal.ONE);
            fee1.setExchangeRateDate(testDate);

            TransactionFeeEntity fee2 = new TransactionFeeEntity();
            fee2.setFeeType(FeeType.EXCHANGE_FEE);
            fee2.setAmount(new BigDecimal("2.50"));
            fee2.setCurrency("CAD");
            fee2.setRate(new BigDecimal("1.35"));
            fee2.setFromCurrency("CAD");
            fee2.setToCurrency("USD");
            fee2.setExchangeRateDate(testDate);
            fee2.setRateSource("Bank");
            fee2.setFeeDate(testDate);

            entity.setFees(List.of(fee1, fee2));

            // When
            Transaction domain = mapper.toDomain(entity);

            // Then
            assertThat(domain.getFees()).hasSize(2);

            Fee domainFee1 = domain.getFees().get(0);
            assertThat(domainFee1.feeType()).isEqualTo(FeeType.COMMISSION);
            assertThat(domainFee1.amountInNativeCurrency().amount()).isEqualByComparingTo(new BigDecimal("9.99"));
            assertThat(domainFee1.exchangeRate().rate()).isEqualTo(BigDecimal.ONE.setScale(6));

            Fee domainFee2 = domain.getFees().get(1);
            assertThat(domainFee2.feeType()).isEqualTo(FeeType.EXCHANGE_FEE);
            assertThat(domainFee2.exchangeRate()).isNotNull();
            assertThat(domainFee2.exchangeRate().rate()).isEqualByComparingTo(new BigDecimal("1.35"));
        }

        @Test
        @DisplayName("Should throw exception for unknown asset type")
        void shouldThrowForUnknownAssetType() {
            // Given
            TransactionEntity entity = createBasicTransactionEntity(TransactionType.BUY);
            entity.setAssetType("UNKNOWN_TYPE");
            entity.setPrimaryId("TEST");

            // When/Then
            assertThatThrownBy(() -> mapper.toDomain(entity))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported asset snapshot type");
        }

        @Test
        @DisplayName("Should detect data corruption when BUY has no identifier")
        void shouldDetectCorruptedBuyTransaction() {
            // Given - Simulating corrupted DB state
            TransactionEntity corruptedEntity = createBasicTransactionEntity(TransactionType.BUY);
            corruptedEntity.setAssetType(null);
            corruptedEntity.setPrimaryId(null);

            // When/Then - Should fail with clear message about corruption
            assertThatThrownBy(() -> mapper.toDomain(corruptedEntity))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Data integrity violation")
                    .hasMessageContaining("corrupted database state");
        }
    }

    @Nested
    @DisplayName("toEntity() - Domain to Entity Mapping")
    class ToEntityTests {

        @Test
        @DisplayName("Should map basic domain transaction to entity")
        void shouldMapBasicDomainTransaction() {
            // Given - BUY requires a MarketIdentifier
            MarketIdentifier identifier = new MarketIdentifier(
                    "AAPL",
                    Collections.emptyMap(),
                    AssetType.STOCK,
                    "Apple Inc.",
                    "shares",
                    Collections.emptyMap());

            Transaction domain = Transaction.reconstitute(
                    new TransactionId(UUID.randomUUID()),
                    new AccountId(testAccountEntity.getId()),
                    TransactionType.BUY,
                    identifier,
                    new BigDecimal("10"),
                    new Money(new BigDecimal("150.00"), ValidatedCurrency.of("USD")),
                    null,
                    Collections.emptyList(),
                    testDate,
                    "Test notes",
                    false);

            // When
            TransactionEntity entity = mapper.toEntity(domain, testAccountEntity);

            // Then
            assertThat(entity.getId()).isEqualTo(domain.getTransactionId().transactionId());
            assertThat(entity.getAccount()).isEqualTo(testAccountEntity);
            assertThat(entity.getPortfolioId()).isEqualTo(testAccountEntity.getId());
            assertThat(entity.getTransactionType()).isEqualTo(TransactionType.BUY);
            assertThat(entity.getQuantity()).isEqualByComparingTo(new BigDecimal("10"));
            assertThat(entity.getPriceAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(entity.getPriceCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("Should map MarketIdentifier to entity fields")
        void shouldMapMarketIdentifier() {
            // Given
            MarketIdentifier identifier = new MarketIdentifier(
                    "AAPL",
                    Map.of("ISIN", "US0378331005"),
                    AssetType.STOCK,
                    "Apple Inc.",
                    "shares",
                    Map.of("exchange", "NASDAQ"));

            Transaction domain = Transaction.reconstitute(
                    new TransactionId(UUID.randomUUID()),
                    new AccountId(testAccountEntity.getId()),
                    TransactionType.BUY,
                    identifier,
                    new BigDecimal("10"),
                    new Money(new BigDecimal("150.00"), ValidatedCurrency.of("USD")),
                    null,
                    Collections.emptyList(),
                    testDate,
                    "Test notes",
                    false);

            // When
            TransactionEntity entity = mapper.toEntity(domain, testAccountEntity);

            // Then
            assertThat(entity.getAssetType()).isEqualTo("STOCK");
            assertThat(entity.getPrimaryId()).isEqualTo("AAPL");
            assertThat(entity.getSecondaryIds()).containsEntry("ISIN", "US0378331005");
            assertThat(entity.getDisplayName()).isEqualTo("Apple Inc.");
            assertThat(entity.getUnitOfTrade()).isEqualTo("shares");
            assertThat(entity.getMetadata()).containsEntry("exchange", "NASDAQ");
        }

        @Test
        @DisplayName("Should map CashIdentifier to entity fields")
        void shouldMapCashIdentifier() {
            // Given
            CashIdentifier identifier = new CashIdentifier("USD", ValidatedCurrency.of("USD"));

            Transaction domain = Transaction.reconstitute(
                    new TransactionId(UUID.randomUUID()),
                    new AccountId(testAccountEntity.getId()),
                    TransactionType.DEPOSIT,
                    identifier,
                    new BigDecimal("1000"),
                    new Money(new BigDecimal("1.00"), ValidatedCurrency.of("USD")),
                    null,
                    Collections.emptyList(),
                    testDate,
                    null,
                    false);

            // When
            TransactionEntity entity = mapper.toEntity(domain, testAccountEntity);

            // Then
            assertThat(entity.getAssetType()).isEqualTo("CASH");
            assertThat(entity.getPrimaryId()).isEqualTo("USD");
        }

        @Test
        @DisplayName("Should map CryptoIdentifier to entity fields")
        void shouldMapCryptoIdentifier() {
            // Given
            CryptoIdentifier identifier = new CryptoIdentifier(
                    "BTC",
                    "Bitcoin",
                    AssetType.CRYPTO,
                    "coins",
                    Map.of("blockchain", "Bitcoin"));

            Transaction domain = Transaction.reconstitute(
                    new TransactionId(UUID.randomUUID()),
                    new AccountId(testAccountEntity.getId()),
                    TransactionType.BUY,
                    identifier,
                    new BigDecimal("0.5"),
                    new Money(new BigDecimal("45000.00"), ValidatedCurrency.of("USD")),
                    null,
                    Collections.emptyList(),
                    testDate,
                    null,
                    false);

            // When
            TransactionEntity entity = mapper.toEntity(domain, testAccountEntity);

            // Then
            assertThat(entity.getAssetType()).isEqualTo("CRYPTO");
            assertThat(entity.getPrimaryId()).isEqualTo("BTC");
            assertThat(entity.getDisplayName()).isEqualTo("Bitcoin");
            assertThat(entity.getUnitOfTrade()).isEqualTo("coins");
        }

        @Test
        @DisplayName("Should map DIVIDEND transaction with dividend amount and DRIP flag")
        void shouldMapDividendWithDripFlag() {
            // Given
            MarketIdentifier identifier = new MarketIdentifier(
                    "MSFT",
                    Collections.emptyMap(),
                    AssetType.STOCK,
                    "Microsoft Corporation",
                    "shares",
                    Collections.emptyMap());

            Transaction domain = Transaction.reconstitute(
                    new TransactionId(UUID.randomUUID()),
                    new AccountId(testAccountEntity.getId()),
                    TransactionType.DIVIDEND,
                    identifier,
                    new BigDecimal("10"),
                    new Money(new BigDecimal("2.50"), ValidatedCurrency.of("USD")),
                    new Money(new BigDecimal("25.00"), ValidatedCurrency.of("USD")),
                    Collections.emptyList(),
                    testDate,
                    "Quarterly dividend",
                    true);

            // When
            TransactionEntity entity = mapper.toEntity(domain, testAccountEntity);

            // Then
            assertThat(entity.getDividendAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
            assertThat(entity.getDividendCurrency()).isEqualTo("USD");
            assertThat(entity.getIsDrip()).isTrue();
        }

        @Test
        @DisplayName("Should map transaction with multiple fees")
        void shouldMapTransactionWithMultipleFees() {
            // Given
            MarketIdentifier identifier = new MarketIdentifier(
                    "MSFT",
                    Collections.emptyMap(),
                    AssetType.STOCK,
                    "Microsoft Corporation",
                    "shares",
                    Collections.emptyMap());
            List<Fee> fees = List.of(
                    new Fee(
                            FeeType.COMMISSION,
                            new Money(new BigDecimal("9.99"), ValidatedCurrency.of("USD")),
                            ExchangeRate.createSingle(ValidatedCurrency.USD, null),
                            Collections.emptyMap(),
                            testDate),
                    new Fee(
                            FeeType.EXCHANGE_FEE,
                            new Money(new BigDecimal("5.00"), ValidatedCurrency.of("CAD")),
                            new ExchangeRate(
                                    ValidatedCurrency.of("CAD"),
                                    ValidatedCurrency.of("USD"),
                                    new BigDecimal("1.35"),
                                    testDate,
                                    "Bank"),
                            Map.of("provider", "FX Provider"),
                            testDate));

            Transaction domain = Transaction.reconstitute(
                    new TransactionId(UUID.randomUUID()),
                    new AccountId(testAccountEntity.getId()),
                    TransactionType.BUY,
                    identifier,
                    new BigDecimal("10"),
                    new Money(new BigDecimal("150.00"), ValidatedCurrency.of("USD")),
                    null,
                    fees,
                    testDate,
                    null,
                    false);

            // When
            TransactionEntity entity = mapper.toEntity(domain, testAccountEntity);

            // Then
            assertThat(entity.getFees()).hasSize(2);

            TransactionFeeEntity fee1 = entity.getFees().get(0);
            assertThat(fee1.getFeeType()).isEqualTo(FeeType.COMMISSION);
            assertThat(fee1.getAmount()).isEqualByComparingTo(new BigDecimal("9.99"));
            assertThat(fee1.getRate().setScale(6)).isEqualTo(BigDecimal.ONE.setScale(6));

            TransactionFeeEntity fee2 = entity.getFees().get(1);
            assertThat(fee2.getFeeType()).isEqualTo(FeeType.EXCHANGE_FEE);
            assertThat(fee2.getRate()).isEqualByComparingTo(new BigDecimal("1.35"));
            assertThat(fee2.getFromCurrency()).isEqualTo("CAD");
            assertThat(fee2.getToCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("Should map DEPOSIT with CashIdentifier")
        void shouldMapDepositWithCashIdentifier() {
            // Given - DEPOSIT transactions should have a CashIdentifier
            CashIdentifier cashIdentifier = new CashIdentifier("USD", ValidatedCurrency.of("USD"));

            Transaction domain = Transaction.reconstitute(
                    new TransactionId(UUID.randomUUID()),
                    new AccountId(testAccountEntity.getId()),
                    TransactionType.DEPOSIT,
                    cashIdentifier, // Cash deposits need identifier
                    new BigDecimal("1000"),
                    new Money(new BigDecimal("1.00"), ValidatedCurrency.of("USD")),
                    null,
                    Collections.emptyList(),
                    testDate,
                    "Cash deposit",
                    false);

            // When
            TransactionEntity entity = mapper.toEntity(domain, testAccountEntity);

            // Then
            assertThat(entity.getAssetType()).isEqualTo("CASH");
            assertThat(entity.getPrimaryId()).isEqualTo("USD");
        }

        @Test
        @DisplayName("Should map WITHDRAWAL with CashIdentifier")
        void shouldMapWithdrawalWithCashIdentifier() {
            // Given - WITHDRAWAL transactions should have a CashIdentifier
            CashIdentifier cashIdentifier = new CashIdentifier("CAD", ValidatedCurrency.of("CAD"));

            Transaction domain = Transaction.reconstitute(
                    new TransactionId(UUID.randomUUID()),
                    new AccountId(testAccountEntity.getId()),
                    TransactionType.WITHDRAWAL,
                    cashIdentifier,
                    new BigDecimal("500"),
                    new Money(new BigDecimal("1.00"), ValidatedCurrency.of("CAD")),
                    null,
                    Collections.emptyList(),
                    testDate,
                    "ATM withdrawal",
                    false);

            // When
            TransactionEntity entity = mapper.toEntity(domain, testAccountEntity);

            // Then
            assertThat(entity.getAssetType()).isEqualTo("CASH");
            assertThat(entity.getPrimaryId()).isEqualTo("CAD");
        }

        @Test
        @DisplayName("Should handle null fees list")
        void shouldHandleNullFees() {
            // Given
            MarketIdentifier identifier = mock(MarketIdentifier.class);
            Transaction domain = Transaction.reconstitute(
                    new TransactionId(UUID.randomUUID()),
                    new AccountId(testAccountEntity.getId()),
                    TransactionType.BUY,
                    identifier,
                    new BigDecimal("10"),
                    new Money(new BigDecimal("150.00"), ValidatedCurrency.of("USD")),
                    null,
                    null, // Null fees
                    testDate,
                    null,
                    false);

            // When
            when(identifier.getAssetType()).thenReturn(AssetType.ETF);
            TransactionEntity entity = mapper.toEntity(domain, testAccountEntity);

            // Then
            assertThat(entity.getFees()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Round-Trip Mapping")
    class RoundTripTests {

        @ParameterizedTest
        @EnumSource(TransactionType.class)
        @DisplayName("Should maintain data integrity for all transaction types")
        void shouldMaintainDataIntegrity(TransactionType type) {
            // Given
            TransactionEntity originalEntity = createBasicTransactionEntity(TransactionType.BUY);
            originalEntity.setAssetType("STOCK");
            originalEntity.setPrimaryId("AAPL");
            originalEntity.setDisplayName("Apple Inc.");
            originalEntity.setUnitOfTrade("USD");
            originalEntity.setIsDrip(false);

            // When
            Transaction domain = mapper.toDomain(originalEntity);
            TransactionEntity mappedEntity = mapper.toEntity(domain, testAccountEntity);

            // Then
            assertThat(mappedEntity.getTransactionType()).isEqualTo(originalEntity.getTransactionType());
            assertThat(mappedEntity.getQuantity()).isEqualByComparingTo(originalEntity.getQuantity());
            assertThat(mappedEntity.getPriceAmount()).isEqualByComparingTo(originalEntity.getPriceAmount());
            assertThat(mappedEntity.getPriceCurrency()).isEqualTo(originalEntity.getPriceCurrency());
            assertThat(mappedEntity.getAssetType()).isEqualTo(originalEntity.getAssetType());
            assertThat(mappedEntity.getPrimaryId()).isEqualTo(originalEntity.getPrimaryId());
        }
    }

    // Helper Methods

    private TransactionEntity createBasicTransactionEntity(TransactionType type) {
        TransactionEntity entity = new TransactionEntity();
        entity.setId(UUID.randomUUID());
        entity.setAccount(testAccountEntity);
        entity.setTransactionType(type);
        entity.setQuantity(new BigDecimal("10"));
        entity.setPriceAmount(new BigDecimal("150.00"));
        entity.setPriceCurrency("USD");
        entity.setTransactionDate(testDate);
        entity.setFees(new ArrayList<>());

        return entity;
    }

    private Transaction createBasicDomainTransaction(TransactionType type) {
        return Transaction.reconstitute(
                new TransactionId(UUID.randomUUID()),
                new AccountId(testAccountEntity.getId()),
                type,
                null,
                new BigDecimal("10"),
                new Money(new BigDecimal("150.00"), ValidatedCurrency.of("USD")),
                null,
                Collections.emptyList(),
                testDate,
                "Test notes",
                false);
    }
}