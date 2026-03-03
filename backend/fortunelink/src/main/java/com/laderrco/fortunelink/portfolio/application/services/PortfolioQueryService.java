package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.queries.GetNetWorthQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetPortfolioByIdQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetPortfoliosByUserIdQuery;
import com.laderrco.fortunelink.portfolio.application.utils.AccountViewBuilder;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioServiceUtils;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.NetWorthView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioSummaryView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles portfolio-level read operations only.
 *
 * Responsibilities: portfolio aggregate identity, total valuation,
 * net worth, performance, and allocation.
 * 
 * Account/position detail lives in AccountQueryService.
 *
 * API call discipline: ONE getBatchQuotes() call per request, resolved here
 * at the service layer and passed down into mappers and domain services.
 * Domain services must NOT independently call MarketDataService.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioQueryService {
    private final PortfolioRepository portfolioRepository;

    private final MarketDataService marketDataService;
    private final PortfolioValuationService portfolioValuationService;

    private final PortfolioViewMapper portfolioViewMapper;
    private final AccountViewBuilder accountViewBuilder;

    public PortfolioView getPortfolioById(GetPortfolioByIdQuery query) {
        Objects.requireNonNull(query, "GetPortfolioByIdQuery cannot be null");

        Portfolio portfolio = loadUserPortfolio(query.portfolioId(), query.userId());
        Currency displayCurrency = portfolio.getDisplayCurrency(); // owned by the aggregate

        // Single batch call for the entire request
        Map<AssetSymbol, MarketAssetQuote> quoteCache = fetchQuotes(portfolio);

        List<AccountView> accountViews = portfolio.getAccounts().stream()
                .map(account -> accountViewBuilder.build(account, quoteCache))
                .toList();

        Money totalValue = portfolioValuationService.calculateTotalValue(portfolio, displayCurrency, quoteCache);

        return portfolioViewMapper.toPortfolioView(portfolio, accountViews, totalValue);
    }

    public List<PortfolioSummaryView> getPortfolioSummaries(GetPortfoliosByUserIdQuery query) {
        Objects.requireNonNull(query, "GetPortfoliosByUserIdQuery cannot be null");

        List<Portfolio> portfolios = portfolioRepository.findAllByUserId(query.userId());
        if (portfolios.isEmpty()) {
            return List.of();
        }

        // One batch call across ALL portfolios - critical for multi-portfolio future
        Set<AssetSymbol> allSymbols = portfolios.stream()
                .flatMap(p -> PortfolioServiceUtils.extractSymbols(p).stream())
                .collect(Collectors.toSet());

        Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(allSymbols);

        return portfolios.stream()
                .map(p -> {
                    Money totalValue = portfolioValuationService
                            .calculateTotalValue(p, p.getDisplayCurrency(), quoteCache);
                    return portfolioViewMapper.toPortfolioSummaryView(p, totalValue);
                })
                .toList();
    }

    /**
     * Calculates net worth for a user's portfolio.
     *
     * Net Worth = Total Assets - Total Liabilities are currently zero (future: ACL into Loan/Debt context).
     */
    public NetWorthView getNetWorth(GetNetWorthQuery query) {
        Objects.requireNonNull(query, "ViewNetWorthQuery cannot be null");

        Portfolio portfolio = loadUserPortfolio(query.portfolioId(), query.userId());
        Instant asOf = query.asOfDate() != null ? query.asOfDate() : Instant.now();
        Currency displayCurrency = portfolio.getDisplayCurrency();

        // One batch call - passed into valuation service, not re-fetched inside it
        Map<AssetSymbol, MarketAssetQuote> quoteCache = fetchQuotes(portfolio);

        Money totalAssets = portfolioValuationService.calculateTotalValue(portfolio, displayCurrency, quoteCache);

        // TODO: integrate liabilities via ACL (Loan / Debt context)
        Money totalLiabilities = Money.ZERO(displayCurrency);

        Money netWorth = totalAssets.subtract(totalLiabilities);

        return new NetWorthView(
                totalAssets,
                totalLiabilities,
                netWorth,
                displayCurrency,
                asOf);
    }

    private Portfolio loadUserPortfolio(PortfolioId portfolioId, UserId userId) {
        return portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
    }

    /**
     * Fetches all market quotes for positions in a portfolio.
     * This is the ONLY place getBatchQuotes() should be called for portfolio
     * queries.
     */
    private Map<AssetSymbol, MarketAssetQuote> fetchQuotes(Portfolio portfolio) {
        Set<AssetSymbol> symbols = PortfolioServiceUtils.extractSymbols(portfolio);
        return marketDataService.getBatchQuotes(symbols);
    }

}
