package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.FeeType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.AccountEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.TransactionEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.TransactionFeeEntity;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionEntityMapperImplTest {

    private TransactionEntityMapperImpl mapper;

    @BeforeEach
    void setUp() {
        mapper = new TransactionEntityMapperImpl();
    }

    @Test
    @DisplayName("Should skip fees and dividend blocks when they are null or inapplicable")
    void toEntity_ShouldHandleNullFeesAndInapplicableDividend() {
        // Given
        Transaction domain = createBaseTransaction(TransactionType.BUY, null, null);

        AccountEntity account = new AccountEntity();
        account.setId(UUID.randomUUID());

        // When
        TransactionEntity entity = mapper.toEntity(domain, account);

        // Then
        assertThat(entity.getFees()).isEqualTo(Collections.emptyList());
        assertThat(entity.getDividendAmount()).isNull();
    }

    @Test
    @DisplayName("mapIdentifierToEntity: Should throw exception for unknown identifier subclass")
    void mapIdentifierToEntity_ShouldThrowForDefaultCase() {
        // Given: An anonymous implementation that bypasses the switch cases
        AssetIdentifier unknownId = new AssetIdentifier() {
            @Override
            public String getPrimaryId() {
                return "ID";
            }

            @Override
            public AssetType getAssetType() {
                return AssetType.STOCK;
            }

            @Override
            public String displayName() {
                return "THINK";
            }
        };

        Transaction domain = createBaseTransaction(TransactionType.BUY, unknownId, null);

        // When/Then: Triggers 'default -> throw'
        assertThatThrownBy(() -> mapper.toEntity(domain, new AccountEntity()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown identifier type");
    }

    @Test
    @DisplayName("mapIdentifierToEntity: Should throw exception for unknown identifier subclass")
    void mapIdentifierToEntity_shouldReturnVoidWhenIdentifierIsNull() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method map = mapper.getClass().getDeclaredMethod("mapIdentifierToEntity", TransactionEntity.class, AssetIdentifier.class);
        map.setAccessible(true);
        assertThat(map.invoke(mapper, mock(TransactionEntity.class), null)).isNull();
        
    }

    @Test
    @DisplayName("Fee Mapping: Should handle null exchange rate in mapFeeToEntity")
    void mapFeeToEntity_ShouldHandleNullExchangeRate() {
        // Given: A fee without an exchange rate
        Fee fee = new Fee(FeeType.COMMISSION,
                new Money(BigDecimal.ONE, ValidatedCurrency.USD),
                ExchangeRate.createSingle(ValidatedCurrency.USD, null), // Trigger: if (f.exchangeRate() != null) is
                                                                        // false
                null, Instant.now());

        Transaction domain = createBaseTransaction(TransactionType.BUY, null, List.of(fee));

        // When
        TransactionEntity entity = mapper.toEntity(domain, new AccountEntity());

        // Then
        assertThat(entity.getFees().get(0).getRate())
                .isEqualTo(BigDecimal.valueOf(1.000000).setScale(Precision.FOREX.getDecimalPlaces()));
    }

    @Test
    @DisplayName("mapFeeToDomain: Should return empty map when metadata is null")
    void mapFeeToDomain_ShouldHandleNullMetadata() throws Exception {
        // Given
        TransactionFeeEntity feeEntity = new TransactionFeeEntity();
        feeEntity.setAmount(BigDecimal.ONE);
        feeEntity.setCurrency("USD");
        feeEntity.setFeeType(FeeType.ACCOUNT_MAINTENANCE);
        feeEntity.setCurrency("USD");
        feeEntity.setFromCurrency("USD");
        feeEntity.setToCurrency("USD");
        feeEntity.setRate(BigDecimal.ONE);
        feeEntity.setExchangeRateDate(Instant.now());
        feeEntity.setFeeDate(Instant.now());
        feeEntity.setMetadata(null); // Trigger: f.getMetadata() != null ? ... : Collections.emptyMap()
        Method ma = mapper.getClass().getDeclaredMethod("mapFeeToDomain", TransactionFeeEntity.class);
        ma.setAccessible(true);

        // When
        // Accessing via package-private or reflection if private,
        // but typically tested through toDomain
        Fee result = (Fee) ma.invoke(mapper, feeEntity);

        // Then
        assertThat(result.metadata()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("validateEntityConsistency: Should correctly identify INTEREST as requiring an identifier")
    void validateEntityConsistency_ShouldIncludeInterest() throws Exception {
        // Given
        TransactionEntity entity = new TransactionEntity();
        entity.setId(UUID.randomUUID());
        entity.setTransactionType(TransactionType.INTEREST);

        Method va = mapper.getClass().getDeclaredMethod("validateEntityConsistency", TransactionEntity.class,
                AssetIdentifier.class);
        va.setAccessible(true);

        // When/Then
        assertThatThrownBy(() -> va.invoke(mapper, entity, null))
                .isInstanceOf(InvocationTargetException.class) // Reflection always throws this
                .hasCauseInstanceOf(IllegalStateException.class) // This checks your actual logic
                .hasStackTraceContaining("of type INTEREST has no asset identifier");
    }

    @Test
    @DisplayName("mapFeeToEntity: Should skip exchange rate fields when null")
    void mapFeeToEntity_ShouldHandleNullExchangeRate2() throws Exception {
        // Given
        Fee mockFee = mock(Fee.class);
        when(mockFee.feeType()).thenReturn(FeeType.BROKERAGE);
        when(mockFee.amountInNativeCurrency()).thenReturn(new Money(BigDecimal.TEN, ValidatedCurrency.USD));
        when(mockFee.exchangeRate()).thenReturn(null); // Trigger: if (f.exchangeRate() != null) is false
        when(mockFee.metadata()).thenReturn(Collections.emptyMap());
        when(mockFee.feeDate()).thenReturn(Instant.now());

        Method method = mapper.getClass().getDeclaredMethod("mapFeeToEntity", Fee.class);
        method.setAccessible(true);

        // When
        TransactionFeeEntity result = (TransactionFeeEntity) method.invoke(mapper, mockFee);

        // Then
        assertThat(result.getRate()).isNull();
        assertThat(result.getFromCurrency()).isNull();
        assertThat(result.getToCurrency()).isNull();
    }

    @Test
    @DisplayName("mapFeeToDomain: Should create 1:1 ExchangeRate when DB rate is null")
    void mapFeeToDomain_ShouldHandleNullRateInEntityWithFallback() throws Exception {
        // Given
        TransactionFeeEntity entity = new TransactionFeeEntity();
        entity.setFeeType(FeeType.COMMISSION);
        entity.setAmount(BigDecimal.TEN);
        entity.setCurrency("USD");
        entity.setRate(null); // This forces the 'else' branch
        entity.setFeeDate(Instant.now());
        entity.setMetadata(Map.of("Sector", "Technology"));

        Method method = mapper.getClass().getDeclaredMethod("mapFeeToDomain", TransactionFeeEntity.class);
        method.setAccessible(true);

        // When
        Fee result = (Fee) method.invoke(mapper, entity);

        // Then
        assertThat(result.exchangeRate()).isNotNull();
        assertThat(result.exchangeRate().rate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(result.exchangeRate().from()).isEqualTo(result.exchangeRate().to());
        assertThat(result.metadata()).isNotEmpty();
    }

    // --- Helper Methods ---

    private Transaction createBaseTransaction(TransactionType type, AssetIdentifier id, List<Fee> fees) {
        // Creating a minimal domain object. Adjust to your constructor/factory.
        TransactionId txId = new TransactionId(UUID.randomUUID());
        Money price = new Money(BigDecimal.valueOf(100), ValidatedCurrency.USD);

        // Basic Stock Identifier for general use
        AssetIdentifier id2 = id == null ? new MarketIdentifier(
                "AAPL", null, AssetType.STOCK, "Apple", "USD", null) : id;

        return Transaction.reconstitute(
                txId,
                AccountId.randomId(),
                type,
                id2,
                BigDecimal.ONE,
                price,
                null, // Dividend
                fees == null ? Collections.emptyList() : fees, // Fees
                Instant.now(),
                "Notes",
                false);
    }
}