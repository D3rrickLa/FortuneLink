package com.laderrco.fortunelink.portfolio.application.services;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laderrco.fortunelink.portfolio.application.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetAllAccountsQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetAssetQuery;
import com.laderrco.fortunelink.portfolio.application.utils.AccountViewBuilder;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioServiceUtils;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.PositionView;
import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;

import lombok.RequiredArgsConstructor;

/**
 * Handles account and position-level read operations.
 *
 * Responsibility boundary: everything inside an account — positions,
 * individual assets, account-level totals. Portfolio identity and
 * aggregate-level metrics (net worth, performance, allocation) belong
 * in PortfolioQueryService.
 *
 * API call discipline: ONE getBatchQuotes() call per request, scoped to
 * the account(s) being queried. Never fetches quotes for positions
 * outside the requested scope.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountQueryService {

	private final PortfolioRepository portfolioRepository;
	private final MarketDataService marketDataService;
	private final PortfolioViewMapper portfolioViewMapper;
	private final AccountViewBuilder accountViewBuilder;

	/**
	 * Returns summary views for all accounts in a user's portfolio.
	 *
	 * Fetches quotes for all positions across all accounts in one batch call.
	 */
	public List<AccountView> getAllAccounts(GetAllAccountsQuery query) {
		Objects.requireNonNull(query, "GetAllAccountsQuery cannot be null");

		Portfolio portfolio = loadUserPortfolio(query.portfolioId(), query.userId());

		// Batch fetch across all accounts in one shot
		Set<AssetSymbol> allSymbols = PortfolioServiceUtils.extractSymbols(portfolio);

		Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(allSymbols);

		return portfolio.getAccounts().stream()
				.map(account -> accountViewBuilder.build(account, quoteCache))
				.toList();
	}

	/**
	 * Returns a summary view for a single account.
	 *
	 * Fetches quotes only for positions in this account — not the whole portfolio.
	 * This is intentional: no wasted API calls for data we won't use.
	 */
	public AccountView getAccountSummary(GetAccountSummaryQuery query) {
		Objects.requireNonNull(query, "GetAccountSummaryQuery cannot be null");

		Portfolio portfolio = loadUserPortfolio(query.portfolioId(), query.userId());
		Account account = portfolio.findAccount(query.accountId())
				.orElseThrow(() -> new AccountNotFoundException(query.accountId(), query.portfolioId()));

		// Scoped batch call: only this account's symbols
		Set<AssetSymbol> symbols = PortfolioServiceUtils.extractSymbols(portfolio);
		Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(symbols);

		return accountViewBuilder.build(account, quoteCache);
	}

	/**
	 * Returns all positions (enriched with current prices) for a single account.
	 *
	 * If you only need positions and not the full account metadata, prefer this
	 * over getAccountSummary() — same API cost, cleaner contract for the caller.
	 */
	public List<PositionView> getAccountPositions(GetAccountSummaryQuery query) {
		Objects.requireNonNull(query, "GetAccountSummaryQuery cannot be null");

		Portfolio portfolio = loadUserPortfolio(query.portfolioId(), query.userId());
		Account account = portfolio.findAccount(query.accountId())
				.orElseThrow(() -> new AccountNotFoundException(query.accountId(), query.portfolioId()));

		Set<AssetSymbol> symbols = PortfolioServiceUtils.extractSymbols(portfolio);
		Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(symbols);

		return account.getPositionEntries().stream()
				.map(entry -> portfolioViewMapper
						.toPositionView(entry.getValue(), quoteCache.get(entry.getKey())))
				.toList();
	}

	/**
	 * Returns the current view for a single asset/position.
	 *
	 * Uses a single getCurrentQuote() rather than batch — only one symbol needed.
	 * Do NOT call getBatchQuotes() for a single asset lookup.
	 */
	public PositionView getAssetSummary(GetAssetQuery query) {
		Objects.requireNonNull(query, "GetAssetQuery cannot be null");

		Portfolio portfolio = loadUserPortfolio(query.portfolioId(), query.userId());
		Account account = portfolio.findAccount(query.accountId())
				.orElseThrow(() -> new AccountNotFoundException(query.accountId(), query.portfolioId()));

		// findPosition or similar — adjust to your actual Account API
		var position = account.getPosition(query.symbol())
				.orElseThrow(
						() -> new AssetNotFoundException("No position found for symbol: " + query.symbol().value()));

		// Single quote, not a batch — correct API hygiene for single-item lookups
		MarketAssetQuote quote = marketDataService.getCurrentQuote(query.symbol()).orElse(null);

		return portfolioViewMapper.toPositionView(position, quote);
	}

	private Portfolio loadUserPortfolio(PortfolioId portfolioId, UserId userId) {
		return portfolioRepository.findByIdAndUserId(portfolioId, userId)
				.orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
	}
}