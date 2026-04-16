package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetAllAccountsQuery;
import com.laderrco.fortunelink.portfolio.application.repositories.AccountQueryRepository;
import com.laderrco.fortunelink.portfolio.application.utils.AccountViewBuilder;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioAccessUtils;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountLifecycleState;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.projections.AccountSummaryProjection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account-level read operations.
 * <p>
 * getAllAccounts: paginated, does NOT load the Portfolio aggregate. Queries accounts table directly
 * -> batch-fetches symbols -> batch-fetches quotes. Three DB/cache hits total regardless of how
 * many accounts are on the page.
 * <p>
 * getAccountSummary: single account, DOES load the aggregate because it needs full position detail
 * (quantity, cost basis per lot, etc.) for the detail view.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountQueryService {
  private final MarketDataService marketDataService;
  private final TransactionRepository transactionRepository;
  private final AccountQueryRepository accountQueryRepository;
  private final PortfolioLoader portfolioLoader;
  private final AccountViewBuilder accountViewBuilder;

  public Page<AccountView> getAllAccounts(GetAllAccountsQuery query) {
    portfolioLoader.validateOwnership(query.portfolioId(), query.userId());

    Pageable pageable = query.pageable();
    Page<AccountSummaryProjection> page = accountQueryRepository.findByPortfolioId(
        query.portfolioId(), pageable);

    if (page.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, page.getTotalElements());
    }

    List<AccountSummaryProjection> projections = page.getContent();

    boolean hasActiveAccounts = projections.stream().anyMatch(
        p -> !AccountLifecycleState.CLOSED.name().equals(p.getLifecycleState())
            && !AccountLifecycleState.REPLAYING.name().equals(p.getLifecycleState()));

    if (!hasActiveAccounts) {
      List<AccountView> content = projections.stream()
          .map(p -> accountViewBuilder.buildFromProjection(p, Map.of(), Map.of(), Map.of()))
          .toList();
      return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    List<AccountId> accountIds = projections.stream()
        .map(a -> new AccountId(a.getId())).toList();

    Map<AccountId, Map<AssetSymbol, Quantity>> quantitiesByAccount = accountQueryRepository
      .findQuantitiesForAccounts(accountIds);

    Set<AssetSymbol> allSymbols = quantitiesByAccount.values().stream()
        .flatMap(m -> m.keySet().stream()).collect(Collectors.toSet());

    Map<AssetSymbol, MarketAssetQuote> quoteCache =
        allSymbols.isEmpty() ? Map.of() : marketDataService.getBatchQuotes(allSymbols);

    List<AccountView> content = projections.stream().map(projection -> {
      AccountId currentId = AccountId.fromString(projection.getId().toString());
      var accountQuantities = quantitiesByAccount.getOrDefault(currentId, Map.of());

      return accountViewBuilder.buildFromProjection(projection, accountQuantities, quoteCache,
          Map.of());
    }).toList();

    return new PageImpl<>(content, pageable, page.getTotalElements());
  }

  public AccountView getAccountSummary(GetAccountSummaryQuery query) {
    Objects.requireNonNull(query, "GetAccountSummaryQuery cannot be null");

    Account account = accountQueryRepository.findByIdWithDetails(query.accountId(),
            query.portfolioId(), query.userId())
        .orElseThrow(() -> new AccountNotFoundException(query.accountId(), query.portfolioId()));

    if (account.getPositionCount() == 0) {
      return accountViewBuilder.build(account, Map.of(), Map.of());
    }

    Set<AssetSymbol> symbols = PortfolioAccessUtils.extractSymbolsByAccount(account);
    Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(symbols);
    Map<AssetSymbol, Money> feeBreakdown = transactionRepository.sumBuyFeesBySymbolForAccount(
        account.getAccountId());

    return accountViewBuilder.build(account, quoteCache, feeBreakdown);
  }
}