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

import java.time.Instant;
import java.util.*;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository, TransactionQueryRepository {
  private static final String BUY_FEE_CACHE = "fees:buy:";

  private final JpaTransactionRepository jpaRepository;
  private final TransactionDomainMapper mapper;

  /**
   * Saves a transaction.
   *
   * <p>
   * <b>New transactions (insert path):</b> The portfolioId from the caller is
   * used
   * directly as the denormalized FK column. No secondary lookup is fired.
   *
   * <p>
   * <b>Existing transactions (exclusion/restore path):</b> The managed JPA entity
   * is
   * fetched by primary key, then only the exclusion state columns are updated
   * in-place.
   * The portfolioId is already on the managed entity from the original insert.
   *
   * <p>
   * <b>Why portfolioId is required even for updates:</b> The interface contract
   * is
   * uniform. The caller always has it available (from the command), so requiring
   * it here
   * prevents future callers from accidentally triggering the old lookup pattern.
   */
  @Override
  public Transaction save(Transaction domain, PortfolioId portfolioId) {
    Objects.requireNonNull(domain, "Transaction cannot be null");
    Objects.requireNonNull(portfolioId, "PortfolioId cannot be null — callers must always supply it");

    UUID id = UUID.fromString(domain.transactionId().toString());
    Optional<TransactionJpaEntity> existing = jpaRepository.findById(id);
    TransactionJpaEntity entity;

    if (existing.isPresent()) {
      // Exclusion / restore path: only mutation allowed post-creation.
      // portfolioId is already persisted on the managed row — no update needed.
      entity = existing.get();
      mapper.applyExclusionState(domain, entity);
    } else {
      // New transaction insert. Use the caller-supplied portfolioId directly.
      // Previously this fired: jpaRepository.findPortfolioIdByAccountId(accountId)
      // — an unnecessary extra query on every single transaction record.
      entity = mapper.toEntity(domain, UUID.fromString(portfolioId.toString()));
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
    // NOTE: this might be wrong, the toString() returns a class description, not
    // the uuid
    return jpaRepository.findByPortfolioIdAndUserIdAndAccountId(
        UUID.fromString(portfolioId.toString()),
        UUID.fromString(userId.toString()),
        UUID.fromString(accountId.toString()))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Transaction> findByAccountIdAndSymbol(AccountId accountId, AssetSymbol symbol) {
    return jpaRepository.findByAccountIdAndExecutionSymbol(
        UUID.fromString(accountId.toString()),
        symbol.symbol())
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Transaction> findByAccountIdAndDateRange(
      AccountId accountId, Instant start, Instant end) {
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

  // =========================================================================
  // TransactionQueryRepository (paginated reads)
  // =========================================================================

  @Override
  public Page<Transaction> findByAccountId(AccountId accountId, Pageable pageable) {
    return jpaRepository.findByAccountId(UUID.fromString(accountId.toString()), pageable)
        .map(mapper::toDomain);
  }

  @Override
  public Page<Transaction> findByAccountIdAndDateRange(
      AccountId accountId, Instant start, Instant end, Pageable pageable) {
    return jpaRepository.findByAccountIdAndOccurredAtBetween(
        UUID.fromString(accountId.toString()), start, end, pageable)
        .map(mapper::toDomain);
  }

  @Override
  public Page<Transaction> findByAccountIdAndSymbol(
      AccountId accountId, AssetSymbol symbol, Pageable pageable) {
    // Uses corrected method name: findByAccountIdAndExecutionSymbol
    return jpaRepository.findByAccountIdAndExecutionSymbol(
        UUID.fromString(accountId.toString()), symbol.symbol(), pageable)
        .map(mapper::toDomain);
  }

  @Override
  @Cacheable(value = BUY_FEE_CACHE, key = "#accountId.id().toString()")
  public Map<AssetSymbol, Money> sumBuyFeesBySymbolForAccount(AccountId accountId) {
    List<FeeAggregationResult> rows = jpaRepository.sumBuyFeesByAccountAndSymbol(
        List.of(UUID.fromString(accountId.toString())));

    Map<AssetSymbol, Money> result = new LinkedHashMap<>();
    for (FeeAggregationResult row : rows) {
      result.put(
          new AssetSymbol(row.getSymbol()),
          new Money(row.getTotalFees(), Currency.of(row.getCurrency())));
    }
    return Collections.unmodifiableMap(result);
  }
}