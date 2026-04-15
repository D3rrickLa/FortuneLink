package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.application.repositories.TransactionQueryRepository;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
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
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository,
    TransactionQueryRepository {
  private final JpaTransactionRepository jpaRepository;
  private final TransactionDomainMapper mapper;
  private final CacheManager cacheManager;

  /**
   * Saves a transaction.
   *
   * <p>
   * <b>New transactions (insert path):</b> The portfolioId from the caller is
   * used directly as the denormalized FK column. No secondary lookup is fired.
   *
   * <p>
   * <b>Existing transactions (exclusion/restore path):</b> The managed JPA entity
   * is fetched by primary key, then only the exclusion state columns are updated in-place. The
   * portfolioId is already on the managed entity from the original insert.
   *
   * <p>
   * <b>Why portfolioId is required even for updates:</b> The interface contract
   * is uniform. The caller always has it available (from the command), so requiring it here
   * prevents future callers from accidentally triggering the old lookup pattern.
   */
  @Override
  public Transaction save(Transaction domain, PortfolioId portfolioId, UUID idempotencyKey) {
    Objects.requireNonNull(domain, "Transaction cannot be null");
    Objects.requireNonNull(portfolioId, "PortfolioId cannot be null, callers must always supply it");
    Optional<TransactionJpaEntity> existing = jpaRepository.findById(domain.transactionId().id());
    TransactionJpaEntity entity;
    
    if (existing.isPresent()) {
      // Exclusion / restore path: only mutation allowed post-creation.
      // portfolioId is already persisted on the managed row , no update needed.
      entity = existing.get();
      mapper.applyExclusionState(domain, entity);
    } else {
      // New transaction insert. Use the caller-supplied portfolioId directly.
      // Previously this fired: jpaRepository.findPortfolioIdByAccountId(accountId),
      //  an unnecessary extra query on every single transaction record.
      entity = mapper.toEntity(domain, portfolioId.id(), idempotencyKey.toString());
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
  // Read , domain repository interface
  // =========================================================================

  @Override
  public List<Transaction> findByPortfolioIdAndUserIdAndAccountId(PortfolioId portfolioId,
      UserId userId, AccountId accountId) {
    return jpaRepository.findByPortfolioIdAndUserIdAndAccountId(portfolioId.id(), userId.id(),
        accountId.id()).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Transaction> findByAccountIdAndSymbol(AccountId accountId, AssetSymbol symbol) {
    return jpaRepository.findByAccountIdAndExecutionSymbol(accountId.id(), symbol.symbol()).stream()
        .map(mapper::toDomain).toList();
  }

  @Override
  public List<Transaction> findByAccountIdAndDateRange(AccountId accountId, Instant start,
      Instant end) {
    return jpaRepository.findByAccountIdAndOccurredAtBetween(accountId.id(), start, end).stream()
        .map(mapper::toDomain).toList();
  }

  @Override
  public Optional<Transaction> findByIdAndPortfolioIdAndUserIdAndAccountId(TransactionId id,
      PortfolioId portfolioId, UserId userId, AccountId accountId) {
    return jpaRepository.findByIdAndPortfolioIdAndAccountId(id.id(), portfolioId.id(),
        accountId.id()).map(mapper::toDomain);
  }

  // =========================================================================
  // TransactionQueryRepository (paginated reads)
  // =========================================================================
  @Override
  @Cacheable(value = "fees:buy", key = "'account:' + #accountId.id()")
  public Map<AssetSymbol, Money> sumBuyFeesBySymbolForAccount(AccountId accountId) {
    return sumBuyFeesBySymbolForAccounts(Set.of(accountId)).getOrDefault(accountId, Map.of());
  }

  @Override
  public Map<AccountId, Map<AssetSymbol, Money>> sumBuyFeesBySymbolForAccounts(
      Set<AccountId> accountIds) {
    if (accountIds == null || accountIds.isEmpty()) {
      return Map.of();
    }

    Map<AccountId, Map<AssetSymbol, Money>> finalResult = new LinkedHashMap<>();
    Set<AccountId> missingAccountIds = new HashSet<>();
    Cache cache = cacheManager.getCache("fees:buy");

    // 1. Check Cache-Aside: See what we already have
    for (AccountId id : accountIds) {
      String cacheKey = "account:" + id.id();
      Map<AssetSymbol, Money> cachedValue = cache != null ? cache.get(cacheKey, Map.class) : null;

      if (cachedValue != null) {
        finalResult.put(id, cachedValue);
      } else {
        missingAccountIds.add(id);
      }
    }

    // 2. If nothing is missing, return immediately
    if (missingAccountIds.isEmpty()) {
      return Collections.unmodifiableMap(finalResult);
    }

    // 3. Fetch missing data in batches from DB
    List<UUID> missingUuids = missingAccountIds.stream().map(AccountId::id).toList();
    int batchSize = 500;

    for (int i = 0; i < missingUuids.size(); i += batchSize) {
      List<UUID> batch = missingUuids.subList(i, Math.min(i + batchSize, missingUuids.size()));
      List<FeeAggregationResult> dbResults = jpaRepository.sumBuyFeesByAccountAndSymbol(batch);

      // Map to hold results for this specific DB fetch
      Map<AccountId, Map<AssetSymbol, Money>> fetchedFromDb = new HashMap<>();

      for (FeeAggregationResult row : dbResults) {
        AccountId accountId = new AccountId(row.getAccountId());
        fetchedFromDb.computeIfAbsent(accountId, k -> new LinkedHashMap<>())
            .put(new AssetSymbol(row.getSymbol()),
                new Money(row.getTotalFees(), Currency.of(row.getCurrency())));
      }

      // 4. Back-fill final result and write to Cache for future calls
      for (AccountId id : missingAccountIds) {
        // Even if the DB returned nothing for an ID, we want to represent it
        Map<AssetSymbol, Money> accountFees = fetchedFromDb.getOrDefault(id, Map.of());
        Map<AssetSymbol, Money> immutableFees = Collections.unmodifiableMap(accountFees);

        finalResult.put(id, immutableFees);

        // Manual cache put to keep @Cacheable in sync
        if (cache != null) {
          cache.put("account:" + id.id(), immutableFees);
        }
      }
    }

    return Collections.unmodifiableMap(finalResult);
  }

  @Override
  public Page<Transaction> findTransactionsDynamic(AccountId accountId, AssetSymbol symbol,
      Instant start, Instant end, Pageable pageable) {
    return jpaRepository.findTransactionsDynamic(accountId.id(),
        symbol != null ? symbol.symbol() : null, start, end, pageable).map(mapper::toDomain);
  }

  @Override
  public boolean existsConflict(AccountId accountId, TransactionType transactionType,
      AssetSymbol symbol, Instant start, Instant end) {
    return jpaRepository.existsConflict(accountId.id(), transactionType, symbol.symbol(), start,
        end);
  }

  @Override
  public Optional<Transaction> findByIdempotencyKey(UUID key) {
    return jpaRepository.findByIdempotencyKey(key.toString()).map(mapper::toDomain);
  }

  @Override
  public Optional<Transaction> findByIdempotencyKeyAndPortfolioId(UUID idempotencyKey,
      PortfolioId portfolioId) {
    return jpaRepository.findByIdempotencyKeyAndPortfolioId(idempotencyKey.toString(),
        portfolioId.id()).map(mapper::toDomain);
  }

  @Override
  public int countExcludedPositionAffecting(AccountId accountId) {
    return (int) jpaRepository.countExcludedPositionAffecting(
        UUID.fromString(accountId.toString()));
  }
}