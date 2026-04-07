package com.laderrco.fortunelink.portfolio.domain.repositories;

import com.laderrco.fortunelink.portfolio.application.repositories.TransactionQueryRepository;
import com.laderrco.fortunelink.portfolio.application.services.TransactionPurgeService;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository responsible for persistence and lifecycle operations on {@link Transaction} entities.
 * <p>
 * <b>Read/Query Separation:</b> Paginated and filtered list queries live in
 * {@link TransactionQueryRepository} in the application layer. This interface owns single-record
 * lookups, mutations, aggregations, and cleanup.
 */
public interface TransactionRepository {
  /**
   * Persists a transaction. {@code portfolioId} is required for new transactions because it is
   * stored as a denormalized column for efficient joins in the recalculation engine. For
   * exclusion/restore updates to existing transactions, the portfolio ID is already on the managed
   * JPA entity — it is still required here to satisfy the contract uniformly and to avoid a
   * secondary lookup.
   *
   * <p>
   * The caller always has the portfolioId available in the command or context object, so passing it
   * here costs nothing and eliminates an extra DB round-trip.
   *
   * @param transaction The transaction entity to save.
   * @param portfolioId The owning portfolio — used for the denormalized column on inserts.
   * @return The persisted {@link Transaction} instance.
   */
  Transaction save(Transaction transaction, PortfolioId portfolioId, UUID idempotencyKey);

  /**
   * Removes excluded transactions for a specific account that occurred before the cutoff date.
   */
  int deleteExpiredTransactions(AccountId accountId, Instant cutoff);

  /**
   * Removes all expired transactions across the entire system.
   *
   * @implNote Used in {@link TransactionPurgeService}
   */
  int deleteAllExpiredTransactions(Instant cutoff);

  /**
   * Finds transactions from a portfolio id, user id, and account id.
   */
  List<Transaction> findByPortfolioIdAndUserIdAndAccountId(PortfolioId portfolioId, UserId userId,
      AccountId accountId);

  /**
   * Finds transactions for a specific asset symbol within an account.
   */
  List<Transaction> findByAccountIdAndSymbol(AccountId accountId, AssetSymbol symbol);

  /**
   * Finds transactions from a specific account between two date ranges.
   */
  List<Transaction> findByAccountIdAndDateRange(AccountId accountId, Instant start, Instant end);

  /**
   * Retrieves a specific transaction scoped by its parent entities.
   */
  Optional<Transaction> findByIdAndPortfolioIdAndUserIdAndAccountId(TransactionId id,
      PortfolioId portfolioId, UserId userId, AccountId accountId);

  /**
   * Sums BUY transaction fees by asset symbol for a single account. This is the cacheable
   * single-account variant. Callers should prefer this over the batch method for read paths to
   * benefit from caching.
   */
  Map<AssetSymbol, Money> sumBuyFeesBySymbolForAccount(AccountId accountId);

  /**
   * Sums BUY transaction fees by asset symbol for a single account. This is the cacheable
   * single-account variant. Callers should prefer this over the batch method for read paths to
   * benefit from caching.
   */
  Map<AccountId, Map<AssetSymbol, Money>> sumBuyFeesBySymbolForAccounts(List<AccountId> accountId);

  boolean existsConflict(AccountId accountId, TransactionType transactionType, AssetSymbol symbol,
      Instant start, Instant end);

  Optional<Transaction> findByIdempotencyKey(UUID idempotencyKey);
}
