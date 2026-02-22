package com.laderrco.fortunelink.portfolio.application.services;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.queries.GetPortfolioByIdQuery;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;

import lombok.RequiredArgsConstructor;

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
    private final ExchangeRateService exchangeRateService;
    private final PortfolioValuationService portfolioValuationService;

    private final PortfolioViewMapper portfolioViewMapper;

    /**
     * Retrieves a portfolio by its unique identifier.
     *
     * Intended for explicit portfolio access (admin, deep links, future
     * multi-portfolio support).
     */
    public PortfolioView getPortfolioById(GetPortfolioByIdQuery query) {
        Objects.requireNonNull(query, "GetPortfolioByIdQuery cannot be null");

        Portfolio portfolio = portfolioRepository.findByIdAndUserId(query.portfolioId(), query.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(query.portfolioId()));

        Currency displayCurrency = portfolio.getDisplayCurrency(); // owned by the aggregate
        
        // Single batch call for the entire request
        Map<AssetSymbol, MarketAssetQuote> quoteCache = fetchQuotes(portfolio);

        return portfolioViewMapper.toPortfolioView(portfolio, quoteCache);
    }

    /**
     * Loads the portfolio owned by the given user.
     *
     * Centralizes the current "one portfolio per user" policy and
     * provides a single seam for future multi-portfolio evolution.
     */
    private Portfolio loadUserPortfolio(
            com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId portfolioId,
            com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId userId) {

        return portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
    }

    /**
     * Fetches all market quotes for positions in a portfolio.
     * This is the ONLY place getBatchQuotes() should be called for portfolio
     * queries.
     */
    private Map<AssetSymbol, MarketAssetQuote> fetchQuotes(Portfolio portfolio) {
        Set<AssetSymbol> symbols = extractSymbols(portfolio);
        return marketDataService.getBatchQuotes(symbols);
    }

    private Set<AssetSymbol> extractSymbols(Portfolio portfolio) {
        return portfolio.getAccounts().stream()
                .flatMap(account -> account.getPositionEntries().stream().map(Map.Entry::getKey))
                .collect(Collectors.toSet());
    }

}
