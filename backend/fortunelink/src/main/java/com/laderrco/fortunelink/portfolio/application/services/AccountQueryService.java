package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.queries.GetAccountPositionQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetAllAccountsQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetAssetQuery;
import com.laderrco.fortunelink.portfolio.application.utils.AccountViewBuilder;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioAccessUtils;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.PositionView;
import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles account and position-level read operations.
 * <p>
 * Responsibility boundary: everything inside an account — positions, individual assets,
 * account-level totals. Portfolio identity and aggregate-level metrics (net worth, performance,
 * allocation) belong in PortfolioQueryService.
 * <p>
 * API call discipline: ONE getBatchQuotes() call per request, scoped to the account(s) being
 * queried. Never fetches quotes for positions outside the requested scope.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountQueryService {
  private final MarketDataService marketDataService;
  private final TransactionRepository transactionRepository;

  private final PortfolioLoader portfolioLoader;
  private final PortfolioViewMapper portfolioViewMapper;

  private final AccountViewBuilder accountViewBuilder;

  public List<AccountView> getAllAccounts(GetAllAccountsQuery query) {
    Objects.requireNonNull(query, "GetAllAccountsQuery cannot be null");

    Portfolio portfolio = portfolioLoader.loadUserPortfolio(query.portfolioId(), query.userId());

    Set<AssetSymbol> allSymbols = PortfolioAccessUtils.extractSymbols(portfolio);
    Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(allSymbols);

    List<AccountId> accountIds = portfolio.getAccounts().stream().map(Account::getAccountId)
        .toList();

    Map<AccountId, Map<AssetSymbol, Money>> feeCache = transactionRepository.sumBuyFeesByAccountAndSymbol(
        accountIds);

    return portfolio.getAccounts().stream().map(
        account -> accountViewBuilder.build(account, quoteCache,
            feeCache.getOrDefault(account.getAccountId(), Map.of()))).toList();
  }

  public AccountView getAccountSummary(GetAccountSummaryQuery query) {
    Objects.requireNonNull(query, "GetAccountSummaryQuery cannot be null");

    Portfolio portfolio = portfolioLoader.loadUserPortfolio(query.portfolioId(), query.userId());
    Account account = portfolio.findAccount(query.accountId())
        .orElseThrow(() -> new AccountNotFoundException(query.accountId(), query.portfolioId()));

    Set<AssetSymbol> symbols = PortfolioAccessUtils.extractSymbolsByAccount(account);
    Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(symbols);

    Map<AccountId, Map<AssetSymbol, Money>> feeCache = transactionRepository.sumBuyFeesByAccountAndSymbol(
        List.of(account.getAccountId()));

    Map<AssetSymbol, Money> feeBreakdown = feeCache.getOrDefault(account.getAccountId(), Map.of());

    return accountViewBuilder.build(account, quoteCache, feeBreakdown);
  }

  // Note: Don't know why we even have this now thinking about it. This is redundant as positions
  // list is already inside AccountView.assets(), the frontend doesn't need a separate endpoint for this
  // the data can be rendered from the return of getAccountSummary()
  @Deprecated
  public List<PositionView> getAccountPositions(GetAccountPositionQuery query) {
    Objects.requireNonNull(query, "GetAccountSummaryQuery cannot be null");

    Portfolio portfolio = portfolioLoader.loadUserPortfolio(query.portfolioId(), query.userId());
    Account account = portfolio.findAccount(query.accountId())
        .orElseThrow(() -> new AccountNotFoundException(query.accountId(), query.portfolioId()));

    // Intentionally lightweight: no fee data. Use getAccountSummary() if fee
    // breakdown is needed. Mirrors AccountViewBuilder.buildSummary() contract.
    Set<AssetSymbol> symbols = PortfolioAccessUtils.extractSymbolsByAccount(account);
    Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(symbols);

    return account.getPositionEntries().stream().map(
        entry -> portfolioViewMapper.toPositionView(entry.getValue(),
            quoteCache.get(entry.getKey()))).toList();
  }

  // Wrong tool, think the original idea was for stock lookup, but we don't need the user id
  // and this also belongs to a MarketDataQueryService
  @Deprecated
  public PositionView getAssetSummary(GetAssetQuery query) {
    Objects.requireNonNull(query, "GetAssetQuery cannot be null");

    Portfolio portfolio = portfolioLoader.loadUserPortfolio(query.portfolioId(), query.userId());
    Account account = portfolio.findAccount(query.accountId())
        .orElseThrow(() -> new AccountNotFoundException(query.accountId(), query.portfolioId()));

    var position = account.getPosition(query.symbol())
        .orElseThrow(() -> new AssetNotFoundException(query.symbol()));

    // Intentionally lightweight: no fee data. Call site gets cost basis only.
    // If fee breakdown is required, add a dedicated getAssetDetail() method.
    Map<AssetSymbol, MarketAssetQuote> quotes = marketDataService.getBatchQuotes(
        Set.of(query.symbol()));
    MarketAssetQuote quote = quotes.get(query.symbol());

    return portfolioViewMapper.toPositionView(position, quote);
  }
}