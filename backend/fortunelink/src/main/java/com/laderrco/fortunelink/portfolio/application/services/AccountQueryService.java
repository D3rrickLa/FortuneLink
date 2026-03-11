package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetAllAccountsQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetAssetQuery;
import com.laderrco.fortunelink.portfolio.application.utils.AccountViewBuilder;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioAccessUtils;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.PositionView;
import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
	private final TransactionRepository transactionRepository;
	private final MarketDataService marketDataService;
	private final PortfolioViewMapper portfolioViewMapper;
	private final AccountViewBuilder accountViewBuilder;

	public List<AccountView> getAllAccounts(GetAllAccountsQuery query) {
		Objects.requireNonNull(query, "GetAllAccountsQuery cannot be null");

		Portfolio portfolio = loadUserPortfolio(query.portfolioId(), query.userId());

		Set<AssetSymbol> allSymbols = PortfolioAccessUtils.extractSymbols(portfolio);
		Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(allSymbols);

		// NEW batch fee fetch
		List<AccountId> accountIds = portfolio.getAccounts().stream()
				.map(Account::getAccountId)
				.toList();

		Map<AccountId, Map<AssetSymbol, Money>> feeCache = transactionRepository
				.sumBuyFeesByAccountAndSymbol(accountIds);

		return portfolio.getAccounts().stream()
				.map(account -> accountViewBuilder.build(
						account,
						quoteCache,
						feeCache.getOrDefault(account.getAccountId(), Map.of())))
				.toList();
	}

	public AccountView getAccountSummary(GetAccountSummaryQuery query) {
		Objects.requireNonNull(query, "GetAccountSummaryQuery cannot be null");

		Portfolio portfolio = loadUserPortfolio(query.portfolioId(), query.userId());
		Account account = portfolio.findAccount(query.accountId())
				.orElseThrow(() -> new AccountNotFoundException(
						query.accountId(),
						query.portfolioId()));

		Set<AssetSymbol> symbols = PortfolioAccessUtils.extractSymbolsByAccount(account);
		Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(symbols);

		Map<AccountId, Map<AssetSymbol, Money>> feeCache = transactionRepository
				.sumBuyFeesByAccountAndSymbol(List.of(account.getAccountId()));

		Map<AssetSymbol, Money> feeBreakdown = feeCache.getOrDefault(account.getAccountId(), Map.of());

		return accountViewBuilder.build(account, quoteCache, feeBreakdown);
	}

	public List<PositionView> getAccountPositions(GetAccountSummaryQuery query) {
		Objects.requireNonNull(query, "GetAccountSummaryQuery cannot be null");

		Portfolio portfolio = loadUserPortfolio(query.portfolioId(), query.userId());
		Account account = portfolio.findAccount(query.accountId())
				.orElseThrow(() -> new AccountNotFoundException(query.accountId(), query.portfolioId()));

		Set<AssetSymbol> symbols = PortfolioAccessUtils.extractSymbolsByAccount(account);
		Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(symbols);

		return account.getPositionEntries().stream()
				.map(entry -> portfolioViewMapper
						.toPositionView(entry.getValue(), quoteCache.get(entry.getKey())))
				.toList();
	}

	public PositionView getAssetSummary(GetAssetQuery query) {
		Objects.requireNonNull(query, "GetAssetQuery cannot be null");

		Portfolio portfolio = loadUserPortfolio(query.portfolioId(), query.userId());
		Account account = portfolio.findAccount(query.accountId())
				.orElseThrow(() -> new AccountNotFoundException(query.accountId(), query.portfolioId()));

		var position = account.getPosition(query.symbol())
				.orElseThrow(() -> new AssetNotFoundException(query.symbol()));

		Map<AssetSymbol, MarketAssetQuote> quotes = marketDataService.getBatchQuotes(Set.of(query.symbol()));
		MarketAssetQuote quote = quotes.get(query.symbol()); // null if not found, same behavior

		return portfolioViewMapper.toPositionView(position, quote);
	}

	private Portfolio loadUserPortfolio(PortfolioId portfolioId, UserId userId) {
		Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
				.orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

		if (portfolio.isDeleted()) {
			throw new PortfolioNotFoundException(portfolioId);
		}
		return portfolio;
	}
}