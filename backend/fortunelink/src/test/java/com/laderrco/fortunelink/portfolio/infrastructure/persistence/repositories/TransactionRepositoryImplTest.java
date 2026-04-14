package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.TransactionJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.mappers.TransactionDomainMapper;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.valueobjects.FeeAggregationResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionRepositoryImpl Unit Tests")
class TransactionRepositoryImplTest {

  private static final Pageable PAGEABLE = PageRequest.of(0, 10);
  private static final UUID TX_UUID = UUID.randomUUID();
  private static final UUID PORTFOLIO_UUID = UUID.randomUUID();
  private static final UUID USER_UUID = UUID.randomUUID();
  private static final UUID ACCOUNT_UUID = UUID.randomUUID();
  private static final UUID IDEM_UUID = UUID.randomUUID();
  private static final TransactionId TX_ID = new TransactionId(TX_UUID);
  private static final PortfolioId PORTFOLIO_ID = PortfolioId.fromString(PORTFOLIO_UUID.toString());
  private static final UserId USER_ID = new UserId(USER_UUID);
  private static final AccountId ACCOUNT_ID = new AccountId(ACCOUNT_UUID);
  private static final AssetSymbol SYMBOL = new AssetSymbol("MSFT");
  @Mock
  private JpaTransactionRepository jpaRepository;
  @Mock
  private TransactionDomainMapper mapper;
  @Mock
  private CacheManager cacheManager;
  @Mock
  private Cache cache;
  @InjectMocks
  private TransactionRepositoryImpl repository;

  private TransactionJpaEntity createTransaction() {
    return TransactionJpaEntity.create(TX_UUID, PORTFOLIO_UUID, ACCOUNT_UUID, "BUY", null, null,
        null, null, null, null, null, null, null, null, false, null, IDEM_UUID, null, null, null,
        null, ACCOUNT_UUID, null);
  }

  @Nested
  @DisplayName("Save Logic Branching")
  class SaveLogic {

    @Test
    @DisplayName("save should insert new entity when transaction does not exist")
    void saveShouldInsertNewWhenNotExists() {
      Transaction domain = mock(Transaction.class);
      TransactionJpaEntity entity = createTransaction();
      when(domain.transactionId()).thenReturn(TX_ID);
      when(jpaRepository.findById(TX_UUID)).thenReturn(Optional.empty());
      when(mapper.toEntity(eq(domain), eq(PORTFOLIO_UUID), anyString())).thenReturn(entity);
      when(jpaRepository.save(entity)).thenReturn(entity);
      when(mapper.toDomain(entity)).thenReturn(domain);

      Transaction result = repository.save(domain, PORTFOLIO_ID, IDEM_UUID);

      assertThat(result).isEqualTo(domain);
      verify(mapper).toEntity(domain, PORTFOLIO_UUID, IDEM_UUID.toString());
      verify(mapper, never()).applyExclusionState(any(), any());
    }

    @Test
    @DisplayName("save should update exclusion state when transaction exists")
    void saveShouldUpdateExclusionStateWhenExists() {
      Transaction domain = mock(Transaction.class);
      TransactionJpaEntity existingEntity = createTransaction();
      when(domain.transactionId()).thenReturn(TX_ID);
      when(jpaRepository.findById(TX_UUID)).thenReturn(Optional.of(existingEntity));
      when(jpaRepository.save(existingEntity)).thenReturn(existingEntity);
      when(mapper.toDomain(existingEntity)).thenReturn(domain);

      repository.save(domain, PORTFOLIO_ID, IDEM_UUID);

      verify(mapper).applyExclusionState(domain, existingEntity);
      verify(mapper, never()).toEntity(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("Paginated and List Queries")
  class QueryOperations {
    @Nested
    @DisplayName("Symbol Based Queries")
    class SymbolQueries {

      @Test
      @DisplayName("findByAccountIdAndSymbol should unwrap symbol and map results")
      void findByAccountIdAndSymbolShouldReturnMappedList() {

        TransactionJpaEntity entity = createTransaction();
        when(jpaRepository.findByAccountIdAndExecutionSymbol(ACCOUNT_UUID, "MSFT")).thenReturn(
            List.of(entity));

        Transaction domainMock = mock(Transaction.class);
        when(mapper.toDomain(entity)).thenReturn(domainMock);

        List<Transaction> results = repository.findByAccountIdAndSymbol(ACCOUNT_ID, SYMBOL);

        assertThat(results).hasSize(1).containsExactly(domainMock);

        verify(jpaRepository).findByAccountIdAndExecutionSymbol(ACCOUNT_UUID, "MSFT");
        verify(mapper).toDomain(entity);
      }

      @Test
      @DisplayName("findByAccountIdAndSymbol should return empty list when no transactions found")
      void findByAccountIdAndSymbolShouldReturnEmptyList() {

        when(jpaRepository.findByAccountIdAndExecutionSymbol(any(), anyString())).thenReturn(
            Collections.emptyList());

        List<Transaction> results = repository.findByAccountIdAndSymbol(ACCOUNT_ID, SYMBOL);

        assertThat(results).isEmpty();
        verify(mapper, never()).toDomain(any());
      }
    }
  }

  @Nested
  @DisplayName("Cache and Batch Aggregation")
  class CacheAndAggregation {

    @Test
    @DisplayName("sumBuyFeesBySymbolForAccounts should return from cache when available")
    void sumBuyFeesShouldCheckCacheFirst() {
      when(cacheManager.getCache("fees:buy")).thenReturn(cache);
      Map<AssetSymbol, Money> cachedData = Map.of(SYMBOL, mock(Money.class));
      when(cache.get("account:" + ACCOUNT_UUID, Map.class)).thenReturn(cachedData);

      Map<AccountId, Map<AssetSymbol, Money>> result = repository.sumBuyFeesBySymbolForAccounts(
          Set.of(ACCOUNT_ID));

      assertThat(result).containsKey(ACCOUNT_ID);
      assertThat(result.get(ACCOUNT_ID)).isEqualTo(cachedData);
      verifyNoInteractions(jpaRepository);
    }

    @Test
    @DisplayName("sumBuyFeesBySymbolForAccounts should fetch from DB and populate cache on miss")
    void sumBuyFeesShouldFetchFromDbOnMiss() {
      when(cacheManager.getCache("fees:buy")).thenReturn(cache);
      when(cache.get(anyString(), eq(Map.class))).thenReturn(null);
      when(jpaRepository.sumBuyFeesByAccountAndSymbol(anyList())).thenReturn(
          Collections.emptyList());

      Map<AccountId, Map<AssetSymbol, Money>> result = repository.sumBuyFeesBySymbolForAccounts(
          Set.of(ACCOUNT_ID));

      assertThat(result).containsKey(ACCOUNT_ID);
      verify(jpaRepository).sumBuyFeesByAccountAndSymbol(List.of(ACCOUNT_UUID));
      verify(cache).put(eq("account:" + ACCOUNT_UUID), anyMap());
    }
  }

  @Nested
  @DisplayName("Deletion and Conflict Logic")
  class DeletionAndConflict {

    @Test
    @DisplayName("deleteExpiredTransactions should delegate to jpa")
    void deleteExpiredShouldCallJpa() {
      Instant now = Instant.now();
      repository.deleteExpiredTransactions(ACCOUNT_ID, now);
      verify(jpaRepository).deleteExpiredTransactions(ACCOUNT_UUID, now);
    }

    @Test
    @DisplayName("existsConflict should pass raw values to jpa")
    void existsConflictShouldCallJpa() {
      Instant now = Instant.now();
      repository.existsConflict(ACCOUNT_ID, null, SYMBOL, now, now);
      verify(jpaRepository).existsConflict(ACCOUNT_UUID, null, "MSFT", now, now);
    }
  }

  @Nested
  @DisplayName("Idempotency Operations")
  class Idempotency {

    @Test
    @DisplayName("findByIdempotencyKey should convert UUID to String for JPA")
    void findByIdempotencyKeyShouldConvertToString() {
      when(jpaRepository.findByIdempotencyKey(IDEM_UUID.toString())).thenReturn(Optional.empty());
      repository.findByIdempotencyKey(IDEM_UUID);
      verify(jpaRepository).findByIdempotencyKey(IDEM_UUID.toString());
    }
  }

  @Nested
  @DisplayName("Maintenance Operations")
  class MaintenanceOperations {

    @Test
    @DisplayName("deleteAllExpiredTransactions should delegate to jpaRepository")
    void deleteAllExpiredTransactionsShouldCallJpa() {
      Instant cutoff = Instant.now();
      when(jpaRepository.deleteAllExpiredTransactions(cutoff)).thenReturn(5);

      int result = repository.deleteAllExpiredTransactions(cutoff);

      assertThat(result).isEqualTo(5);
      verify(jpaRepository).deleteAllExpiredTransactions(cutoff);
    }
  }

  @Nested
  @DisplayName("Read Operations - Domain Interface")
  class DomainReadOperations {

    @Test
    @DisplayName("findByPortfolioIdAndUserIdAndAccountId should map results to domain")
    void findByPortfolioIdAndUserIdAndAccountIdShouldReturnDomainList() {
      TransactionJpaEntity entity = createTransaction();
      when(jpaRepository.findByPortfolioIdAndUserIdAndAccountId(PORTFOLIO_UUID, USER_UUID,
          ACCOUNT_UUID)).thenReturn(List.of(entity));
      when(mapper.toDomain(entity)).thenReturn(mock(Transaction.class));

      List<Transaction> results = repository.findByPortfolioIdAndUserIdAndAccountId(PORTFOLIO_ID,
          USER_ID, ACCOUNT_ID);

      assertThat(results).hasSize(1);
      verify(jpaRepository).findByPortfolioIdAndUserIdAndAccountId(PORTFOLIO_UUID, USER_UUID,
          ACCOUNT_UUID);
    }

    @Test
    @DisplayName("findByAccountIdAndDateRange should map results to domain")
    void findByAccountIdAndDateRangeShouldReturnDomainList() {
      Instant start = Instant.now().minusSeconds(3600);
      Instant end = Instant.now();
      when(jpaRepository.findByAccountIdAndOccurredAtBetween(ACCOUNT_UUID, start, end)).thenReturn(
          List.of(createTransaction()));
      when(mapper.toDomain(any())).thenReturn(mock(Transaction.class));

      List<Transaction> results = repository.findByAccountIdAndDateRange(ACCOUNT_ID, start, end);

      assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("findByIdAndPortfolioIdAndUserIdAndAccountId should map optional domain")
    void findByIdAndPortfolioIdAndUserIdAndAccountIdShouldReturnOptional() {
      TransactionJpaEntity entity = createTransaction();
      when(jpaRepository.findByIdAndPortfolioIdAndAccountId(TX_UUID, PORTFOLIO_UUID,
          ACCOUNT_UUID)).thenReturn(Optional.of(entity));
      when(mapper.toDomain(entity)).thenReturn(mock(Transaction.class));

      Optional<Transaction> result = repository.findByIdAndPortfolioIdAndUserIdAndAccountId(TX_ID,
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID);

      assertThat(result).isPresent();
    }
  }

  @Nested
  @DisplayName("Query Repository - Advanced Filtering")
  class AdvancedQueryOperations {

    @Test
    @DisplayName("findTransactionsDynamic should pass all parameters to JPA")
    void findTransactionsDynamicShouldCallJpa() {
      Instant start = Instant.now();
      Instant end = Instant.now();
      when(jpaRepository.findTransactionsDynamic(ACCOUNT_UUID, "MSFT", start, end,
          PAGEABLE)).thenReturn(Page.empty());

      repository.findTransactionsDynamic(ACCOUNT_ID, SYMBOL, start, end, PAGEABLE);

      verify(jpaRepository).findTransactionsDynamic(ACCOUNT_UUID, "MSFT", start, end, PAGEABLE);
    }

    @Test
    @DisplayName("sumBuyFeesBySymbolForAccount should handle single account and cache result")
    void sumBuyFeesBySymbolForAccountShouldCallBatchMethod() {

      when(cacheManager.getCache("fees:buy")).thenReturn(cache);
      Map<AssetSymbol, Money> fees = Map.of(SYMBOL, mock(Money.class));
      when(cache.get("account:" + ACCOUNT_UUID, Map.class)).thenReturn(fees);

      Map<AssetSymbol, Money> result = repository.sumBuyFeesBySymbolForAccount(ACCOUNT_ID);

      assertThat(result).isEqualTo(fees);
    }
  }

  @Nested
  @DisplayName("Miscellaneous Logic")
  class MiscOperations {

    @Test
    @DisplayName("findByIdempotencyKeyAndPortfolioId should convert key to string")
    void findByIdempotencyKeyAndPortfolioIdShouldCallJpa() {
      when(jpaRepository.findByIdempotencyKeyAndPortfolioId(IDEM_UUID.toString(),
          PORTFOLIO_UUID)).thenReturn(Optional.empty());

      repository.findByIdempotencyKeyAndPortfolioId(IDEM_UUID, PORTFOLIO_ID);

      verify(jpaRepository).findByIdempotencyKeyAndPortfolioId(IDEM_UUID.toString(),
          PORTFOLIO_UUID);
    }

    @Test
    @DisplayName("countExcludedPositionAffecting should return int cast from jpa long")
    void countExcludedPositionAffectingShouldReturnInt() {
      when(jpaRepository.countExcludedPositionAffecting(ACCOUNT_UUID)).thenReturn(12L);

      int count = repository.countExcludedPositionAffecting(ACCOUNT_ID);

      assertThat(count).isEqualTo(12);
      verify(jpaRepository).countExcludedPositionAffecting(ACCOUNT_UUID);
    }
  }

  @Nested
  @DisplayName("Batch Fee Aggregation and Caching")
  class BatchFeeAggregation {

    @Test
    @DisplayName("sumBuyFeesBySymbolForAccounts should return empty map for null or empty input")
    void sumBuyFeesShouldHandleNullOrEmpty() {
      assertThat(repository.sumBuyFeesBySymbolForAccounts(null)).isEmpty();
      assertThat(repository.sumBuyFeesBySymbolForAccounts(Set.of())).isEmpty();

      verifyNoInteractions(cacheManager, jpaRepository);
    }

    @Test
    @DisplayName("sumBuyFeesBySymbolForAccounts should correctly map DB rows to nested map")
    void sumBuyFeesShouldMapDbResultsCorrectly() {

      when(cacheManager.getCache("fees:buy")).thenReturn(null);

      FeeAggregationResult mockResult = mock(FeeAggregationResult.class);
      when(mockResult.getAccountId()).thenReturn(ACCOUNT_UUID);
      when(mockResult.getSymbol()).thenReturn("BTC");
      when(mockResult.getTotalFees()).thenReturn(new BigDecimal("10.50"));
      when(mockResult.getCurrency()).thenReturn("USD");

      when(jpaRepository.sumBuyFeesByAccountAndSymbol(anyList())).thenReturn(List.of(mockResult));

      Map<AccountId, Map<AssetSymbol, Money>> result = repository.sumBuyFeesBySymbolForAccounts(
          Set.of(ACCOUNT_ID));

      assertThat(result).containsKey(ACCOUNT_ID);
      Map<AssetSymbol, Money> accountFees = result.get(ACCOUNT_ID);
      assertThat(accountFees).containsKey(new AssetSymbol("BTC"));
      assertThat(accountFees.get(new AssetSymbol("BTC")).amount()).isEqualByComparingTo("10.50");
    }

    @Test
    @DisplayName("sumBuyFeesBySymbolForAccounts should remain null-safe when cache is missing")
    void sumBuyFeesShouldBeNullSafeWhenCacheIsNull() {

      when(cacheManager.getCache("fees:buy")).thenReturn(null);
      when(jpaRepository.sumBuyFeesByAccountAndSymbol(anyList())).thenReturn(List.of());

      Map<AccountId, Map<AssetSymbol, Money>> result = repository.sumBuyFeesBySymbolForAccounts(
          Set.of(ACCOUNT_ID));

      assertThat(result).containsKey(ACCOUNT_ID);
      assertThat(result.get(ACCOUNT_ID)).isEmpty();
      verify(jpaRepository).sumBuyFeesByAccountAndSymbol(anyList());
    }

    @Test
    @DisplayName("sumBuyFeesBySymbolForAccounts should populate cache when cache is present")
    void sumBuyFeesShouldPopulateCacheWhenAvailable() {

      when(cacheManager.getCache("fees:buy")).thenReturn(cache);
      when(cache.get(anyString(), eq(Map.class))).thenReturn(null);
      when(jpaRepository.sumBuyFeesByAccountAndSymbol(anyList())).thenReturn(List.of());

      repository.sumBuyFeesBySymbolForAccounts(Set.of(ACCOUNT_ID));

      verify(cache).put(eq("account:" + ACCOUNT_UUID), anyMap());
    }
  }
}