package com.laderrco.fortunelink.portfolio.domain.repositories;

import com.laderrco.fortunelink.portfolio.application.repositories.TransactionQueryRepository;
import com.laderrco.fortunelink.portfolio.application.services.TransactionPurgeService;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
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

/**
 * Repository responsible for persistence and lifecycle operations on {@link Transaction} entities.
 *
 * <p><b>Read/Query Separation:</b> Paginated and filtered list queries (history, date range,
 * symbol filter) live in {@link TransactionQueryRepository} in the application layer. This
 * interface owns single-record lookups, mutations, aggregations, and cleanup.
 */
public interface TransactionRepository {
  /**
   * Persists or updates a transaction in the underlying data store.
   *
   * @param transaction The transaction entity to save.
   * @return The persisted {@link Transaction} instance.
   */
  Transaction save(Transaction transaction);

  /**
   * Removes excluded transactions for a specific account that occurred before the cutoff date.
   *
   * @param accountId The account ID.
   * @param cutoff    The threshold timestamp for deletion.
   * @return The number of records deleted.
   */
  int deleteExpiredTransactions(AccountId accountId, Instant cutoff);

  /**
   * Removes all expired transactions across the entire system.
   *
   * @param cutoff The threshold timestamp for deletion.
   * @return The number of records deleted.
   * @implNote Used in {@link TransactionPurgeService}
   */
  int deleteAllExpiredTransactions(Instant cutoff);

  /**
   * Finds transactions for a specific asset symbol within an account.
   *
   * @param accountId The account ID.
   * @param symbol    The asset symbol (e.g., ticker).
   * @return A list of {@link Transaction} entities matching the criteria.
   */
  List<Transaction> findByAccountIdAndSymbol(AccountId accountId, AssetSymbol symbol);

  /**
   * Retrieves a specific transaction scoped by its parent entities.
   *
   * @param id          The transaction ID.
   * @param portfolioId The ID of the portfolio containing the transaction.
   * @param userId      The ID of the user who owns the account.
   * @param accountId   The ID of the account.
   * @return An {@link Optional} containing the transaction if found, otherwise empty.
   */
  Optional<Transaction> findByIdAndPortfolioIdAndUserIdAndAccountId(TransactionId id,
      PortfolioId portfolioId, UserId userId, AccountId accountId);


  /**
   * Aggregates buy fees by account and asset symbol.
   *
   * @param accountIds A list of account IDs to include in the report.
   * @return A map where keys are {@link AccountId} and values are maps of {@link AssetSymbol} to
   * total {@link Money}.
   */
  Map<AccountId, Map<AssetSymbol, Money>> sumBuyFeesByAccountAndSymbol(List<AccountId> accountIds);
}
