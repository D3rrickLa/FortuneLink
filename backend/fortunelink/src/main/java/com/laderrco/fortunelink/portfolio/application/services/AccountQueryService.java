package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetAllAccountsQuery;
import com.laderrco.fortunelink.portfolio.application.utils.AccountViewBuilder;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioAccessUtils;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;

import lombok.RequiredArgsConstructor;

import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles account and position-level read operations.
 * <p>
 * Responsibility boundary: everything inside an account, positions, individual
 * assets,
 * account-level totals. Portfolio identity and aggregate-level metrics (net
 * worth, performance,
 * allocation) belong in PortfolioQueryService.
 * <p>
 * API call discipline: ONE getBatchQuotes() call per request, scoped to the
 * account(s) being
 * queried. Never fetches quotes for positions outside the requested scope.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountQueryService {
  private final MarketDataService marketDataService;
  private final TransactionRepository transactionRepository;

  private final PortfolioLoader portfolioLoader;

  private final AccountViewBuilder accountViewBuilder;

  public Page<AccountView> getAllAccounts(GetAllAccountsQuery query) {
    Objects.requireNonNull(query, "GetAllAccountsQuery cannot be null");

    Pageable pageable = query.pageable();
    // TODO: a fix for when we have multiple accoutns, 100+ is rather than fetching accounts,
    // through the Portfolio aggregate, we query an AccountRepository directly like
    // accountRepository.findByPortoflioId(portoflioId, pageable)
    Portfolio portfolio = portfolioLoader.loadUserPortfolio(query.portfolioId(), query.userId());

    if (!portfolio.hasAccounts()) {
      return Page.empty(pageable);
    }

    List<Account> allAccounts = new ArrayList<>(portfolio.getAccounts());
    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), allAccounts.size());

    // out-of-bounds offsets check
    if (start > allAccounts.size()) {
      return new PageImpl<>(List.of(), pageable, allAccounts.size());
    }

    List<Account> pagedAccounts = allAccounts.subList(start, end);

    // ONLY fetch market data/fees for the paged subset
    Set<AssetSymbol> pagedSymbols = PortfolioAccessUtils.extractSymbolsFromAccounts(pagedAccounts);
    Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(pagedSymbols);

    List<AccountView> content = pagedAccounts.stream()
        .map(account -> accountViewBuilder.build(
            account,
            quoteCache,
            transactionRepository.sumBuyFeesBySymbolForAccount(account.getAccountId())))
        .toList();

    // Wrap in PageImpl to provide total count and pagination metadata
    return new PageImpl<>(content, pageable, allAccounts.size());
  }

  public AccountView getAccountSummary(GetAccountSummaryQuery query) {
    Objects.requireNonNull(query, "GetAccountSummaryQuery cannot be null");

    Portfolio portfolio = portfolioLoader.loadUserPortfolio(query.portfolioId(), query.userId());
    Account account = portfolio.findAccount(query.accountId())
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