package com.laderrco.fortunelink.portfolio.application.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.projections.AccountSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Read-only repository for account-level queries that bypass the Portfolio
 * aggregate.
 * <p>
 * Lives in the application layer (not domain) because it returns infrastructure
 * projections
 * rather than domain entities. This is intentional — these are query-side
 * optimizations,
 * not aggregate mutations.
 * <p>
 * Use this when you need a list or summary of accounts without paying the cost
 * of
 * loading full Portfolio -> Account -> Position -> RealizedGain graph.
 */
public interface AccountQueryRepository {

  /**
   * Paginated account list for a portfolio, without positions loaded.
   * Performs a single SELECT on the accounts table — no aggregate hydration.
   */
  Page<AccountSummaryProjection> findByPortfolioId(PortfolioId portfolioId, Pageable pageable);

  /**
   * Batch fetch of all open symbols (from positions table) for a given set of
   * accounts.
   * <p>
   * Returns a map of accountId -> set of symbols. Accounts with no open positions
   * are absent from the map (not present with an empty set) — callers should use
   * {@code getOrDefault(id, Set.of())}.
   * <p>
   * Uses positions table, not transactions, because we want current open holdings
   * only — not historical symbols that have since been fully sold.
   */
  Map<AccountId, Set<AssetSymbol>> findSymbolsForAccounts(List<AccountId> accountIds);

  Optional<Account> findByIdWithDetails(AccountId accountId, PortfolioId portfolioId, UserId userId);
}