package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.application.repositories.TransactionQueryRepository;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.TransactionJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.mappers.TransactionDomainMapper;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.valueobjects.FeeAggregationResult;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository, TransactionQueryRepository {
  private final JpaTransactionRepository jpaRepository;
  private final TransactionDomainMapper mapper;

  /**
   * Persists a transaction.
   * <p>
   * If the transaction already exists (same UUID), only exclusion state is
   * updated — all other fields are immutable by design. This avoids a full
   * re-insert of fee rows on every exclude/restore operation.
   * 
   * NOTE: the findPortfolioIdByAccountId query crosses from the transaction
   * table into the account/portfolio tables. This works but you should add
   * a @Query annotation to JpaTransactionRepository that joins through
   * AccountJpaEntity, already done in the delivered JpaTransactionRepository.
   * Make sure AccountJpaEntity is visible to JPQL in that context, which it will
   * be since both are in the same persistence unit.
   */
  @Override
  public Transaction save(Transaction domain) {
    Objects.requireNonNull(domain, "Transaction cannot be null");

    UUID id = UUID.fromString(domain.transactionId().toString());

    Optional<TransactionJpaEntity> existing = jpaRepository.findById(id);
    TransactionJpaEntity entity;

    if (existing.isPresent()) {
      // Only exclusion state can change post-creation
      entity = existing.get();
      mapper.applyExclusionState(domain, entity);
    } else {
      // New transaction, need portfolio ID for the denormalized column.
      // The caller must have loaded the portfolio context, so we derive
      // it from the TransactionMetadata or require the caller to pass it.
      // For now we look it up via account ownership query.
      UUID portfolioId = jpaRepository.findPortfolioIdByAccountId(
          UUID.fromString(domain.accountId().toString()));
      entity = mapper.toEntity(domain, portfolioId);
    }

    TransactionJpaEntity saved = jpaRepository.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public int deleteExpiredTransactions(AccountId accountId, Instant cutoff) {
    return jpaRepository.deleteExpiredTransactions(UUID.fromString(accountId.toString()), cutoff);
  }

  @Override
  public int deleteAllExpiredTransactions(Instant cutoff) {
    return jpaRepository.deleteAllExpiredTransactions(cutoff);
  }

  // =========================================================================
  // Read — domain repository interface
  // =========================================================================

  @Override
  public List<Transaction> findByPortfolioIdAndUserIdAndAccountId(
      PortfolioId portfolioId, UserId userId, AccountId accountId) {

    return jpaRepository.findByPortfolioIdAndAccountId(UUID.fromString(portfolioId.toString()),
        UUID.fromString(accountId.toString()))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Transaction> findByAccountIdAndSymbol(AccountId accountId, AssetSymbol symbol) {
    return jpaRepository.findByAccountIdAndSymbol(
        UUID.fromString(accountId.toString()),
        symbol.symbol())
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Transaction> findByAccountIdAndDateRange(AccountId accountId,
      Instant start, Instant end) {
    return jpaRepository.findByAccountIdAndOccurredAtBetween(
        UUID.fromString(accountId.toString()), start, end)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public Optional<Transaction> findByIdAndPortfolioIdAndUserIdAndAccountId(
      TransactionId id, PortfolioId portfolioId, UserId userId, AccountId accountId) {

    return jpaRepository.findByIdAndPortfolioIdAndAccountId(
        UUID.fromString(id.toString()),
        UUID.fromString(portfolioId.toString()),
        UUID.fromString(accountId.toString()))
        .map(mapper::toDomain);
  }

  /**
   * Aggregates BUY fees by account and symbol for the fee display breakdown.
   * <p>
   * This uses a JPQL projection query rather than loading all transactions —
   * N transactions → 1 aggregate query. The result is grouped in Java because
   * returning a nested Map directly from JPQL requires a custom result class.
   */
  @Override
  public Map<AccountId, Map<AssetSymbol, Money>> sumBuyFeesByAccountAndSymbol(
      List<AccountId> accountIds) {

    if (accountIds == null || accountIds.isEmpty())
      return Map.of();

    List<UUID> uuids = accountIds.stream()
        .map(id -> UUID.fromString(id.toString()))
        .toList();

    List<FeeAggregationResult> rows = jpaRepository.sumBuyFeesByAccountAndSymbol(uuids);

    Map<AccountId, Map<AssetSymbol, Money>> result = new LinkedHashMap<>();
    for (FeeAggregationResult row : rows) {
      UUID accountUuid = row.getAccountId();
      String symbol = row.getSymbol();
      BigDecimal amount = row.getTotalFees();
      String currency = row.getCurrency();

      AccountId accountId = AccountId.fromString(accountUuid.toString());
      AssetSymbol assetSymbol = new AssetSymbol(symbol);
      Money fee = new Money(amount, Currency.of(currency));

      result.computeIfAbsent(accountId, k -> new LinkedHashMap<>())
          .merge(assetSymbol, fee, Money::add);
    }
    return Collections.unmodifiableMap(result);
  }

  // =========================================================================
  // TransactionQueryRepository (paginated reads, application layer interface)
  // =========================================================================
  @Override
  public Page<Transaction> findByAccountId(AccountId accountId, Pageable pageable) {
    return jpaRepository.findByAccountId(UUID.fromString(accountId.toString()), pageable)
        .map(mapper::toDomain);
  }

  @Override
  public Page<Transaction> findByAccountIdAndDateRange(AccountId accountId, Instant start, Instant end,
      Pageable pageable) {
    return jpaRepository.findByAccountIdAndOccurredAtBetween(
        UUID.fromString(accountId.toString()), start, end, pageable)
        .map(mapper::toDomain);
  }

  @Override
  public Page<Transaction> findByAccountIdAndSymbol(AccountId accountId,
      AssetSymbol symbol,
      Pageable pageable) {
    return jpaRepository.findByAccountIdAndSymbol(
        UUID.fromString(accountId.toString()), symbol.symbol(), pageable)
        .map(mapper::toDomain);
  }
}
