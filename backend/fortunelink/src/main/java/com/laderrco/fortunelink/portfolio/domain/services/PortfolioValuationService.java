package com.laderrco.fortunelink.portfolio.domain.services;

import com.laderrco.fortunelink.portfolio.application.views.ValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

import java.util.List;
import java.util.Map;

/**
 * Calculates portfolio valuations.
 * <p>
 * Implementation Note: This service is stateless and does not fetch market data. All required
 * quotes must be pre-fetched and passed via the {@code quoteCache} parameter to ensure efficient
 * API usage.
 */
public interface PortfolioValuationService {
 /**
   * Calculates a full valuation summary for a single account.
   */
  ValuationView calculateAccountValuation(Account account, Map<AssetSymbol, MarketAssetQuote> quoteCache);

  /**
   * Calculates a full valuation summary for a portfolio.
   */
  ValuationView calculatePortfolioValuation(Portfolio portfolio, Currency targetCurrency,
      Map<AssetSymbol, MarketAssetQuote> quoteCache);

  /**
   * Calculates a full aggregated valuation across multiple portfolios.
   */
  ValuationView calculateUserValuation(List<Portfolio> portfolios, Currency targetCurrency,
      Map<AssetSymbol, MarketAssetQuote> quoteCache);
}