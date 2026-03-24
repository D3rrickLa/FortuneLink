package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.queries.GetNetWorthQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetPortfolioByIdQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetPortfoliosByUserIdQuery;
import com.laderrco.fortunelink.portfolio.application.utils.AccountViewBuilder;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioAccessUtils;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.NetWorthView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioSummaryView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles portfolio-level read operations only.
 * <p>
 * Responsibilities: portfolio aggregate identity, total valuation, net worth, performance, and
 * allocation.
 * <p>
 * Account/position detail lives in AccountQueryService.
 * <p>
 * API call discipline: ONE getBatchQuotes() call per request, resolved here at the service layer
 * and passed down into mappers and domain services. Domain services must NOT independently call
 * MarketDataService.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioQueryService {
  private final MarketDataService marketDataService;
  private final PortfolioValuationService portfolioValuationService;
  private final TransactionRepository transactionRepository;

  private final PortfolioViewMapper portfolioViewMapper;
  private final AccountViewBuilder accountViewBuilder;
  private final PortfolioLoader portfolioLoader;

  public PortfolioView getPortfolioById(GetPortfolioByIdQuery query) {
    Objects.requireNonNull(query, "GetPortfolioByIdQuery cannot be null");

    Portfolio portfolio = portfolioLoader.loadUserPortfolio(query.portfolioId(), query.userId());
    Currency displayCurrency = portfolio.getDisplayCurrency();

    Collection<Account> accounts = portfolio.getAccounts();
    List<AccountId> accountIds = accounts.stream().map(Account::getAccountId).toList();

    Set<AssetSymbol> symbols = accounts.stream()
        .flatMap(a -> PortfolioAccessUtils.extractSymbolsByAccount(a).stream())
        .collect(Collectors.toSet());

    Map<AssetSymbol, MarketAssetQuote> quoteCache = fetchQuotes(symbols);

    Map<AccountId, Map<AssetSymbol, Money>> feeCache = transactionRepository.sumBuyFeesByAccountAndSymbol(
        accountIds);

    List<AccountView> accountViews = accounts.stream().map(
        account -> accountViewBuilder.build(account, quoteCache,
            feeCache.getOrDefault(account.getAccountId(), Map.of()))).toList();

    Money totalValue = portfolioValuationService.calculateTotalValue(portfolio, displayCurrency,
        quoteCache);

    boolean hasStaleData = accounts.stream().anyMatch(Account::isStale);

    return portfolioViewMapper.toPortfolioView(portfolio, accountViews, totalValue, hasStaleData);
  }

  public List<PortfolioSummaryView> getPortfolioSummaries(GetPortfoliosByUserIdQuery query) {
    Objects.requireNonNull(query, "GetPortfoliosByUserIdQuery cannot be null");

    List<Portfolio> portfolios = portfolioLoader.loadAllUserPortfolios(query.userId());

    if (portfolios.isEmpty()) {
      return List.of();
    }

    Map<AssetSymbol, MarketAssetQuote> quoteCache = fetchQuotesForMultiple(portfolios);

    // PortfolioSummaryView contains only totalValue - fees are position-level
    // detail
    // and intentionally excluded here. Do NOT add fee loading to this method.
    return portfolios.stream().map(p -> {
      Money totalValue = portfolioValuationService.calculateTotalValue(p, p.getDisplayCurrency(),
          quoteCache);
      return portfolioViewMapper.toPortfolioSummaryView(p, totalValue);
    }).toList();
  }

  /**
   * Calculates net worth for a user's portfolio.
   * <p>
   * Net Worth = Total Assets - Total Liabilities are currently zero (future: ACL into Loan/Debt
   * context).
   */
  public NetWorthView getNetWorth(GetNetWorthQuery query) {
    Objects.requireNonNull(query, "ViewNetWorthQuery cannot be null");

    Portfolio portfolio = portfolioLoader.loadUserPortfolio(query.portfolioId(), query.userId());
    Currency displayCurrency = portfolio.getDisplayCurrency();

    Map<AssetSymbol, MarketAssetQuote> quoteCache = fetchQuotes(portfolio);

    Money totalAssets = portfolioValuationService.calculateTotalValue(portfolio, displayCurrency,
        quoteCache);

    // TODO: integrate liabilities via ACL (Loan / Debt context)
    Money totalLiabilities = Money.zero(displayCurrency);

    Money netWorth = totalAssets.subtract(totalLiabilities);

    boolean isStale = portfolio.getAccounts().stream().anyMatch(Account::isStale);
    return new NetWorthView(totalAssets, totalLiabilities, netWorth, displayCurrency, isStale,
        Instant.now());
  }

  /**
   * Fetches all market quotes for positions in a portfolio. This is the ONLY place getBatchQuotes()
   * should be called for portfolio queries.
   */
  private Map<AssetSymbol, MarketAssetQuote> fetchQuotes(Set<AssetSymbol> symbols) {
    return symbols.isEmpty() ? Map.of() : marketDataService.getBatchQuotes(symbols);
  }

  private Map<AssetSymbol, MarketAssetQuote> fetchQuotes(Portfolio portfolio) {
    return fetchQuotes(PortfolioAccessUtils.extractSymbols(portfolio));
  }

  /**
   * Fetches all quotes for all portfolios a user has
   */
  private Map<AssetSymbol, MarketAssetQuote> fetchQuotesForMultiple(List<Portfolio> portfolios) {
    Set<AssetSymbol> allSymbols = portfolios.stream()
        .flatMap(p -> PortfolioAccessUtils.extractSymbols(p).stream()).collect(Collectors.toSet());

    return allSymbols.isEmpty() ? Map.of() : marketDataService.getBatchQuotes(allSymbols);
  }
}
