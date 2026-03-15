package com.laderrco.fortunelink.portfolio.domain.services;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
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
   * Calculates the total value of a portfolio in the specified currency.
   *
   * @param portfolio      the portfolio to value
   * @param targetCurrency the currency for the final result
   * @param quoteCache     pre-fetched quotes for all assets in the portfolio
   * @return total value in the target currency
   */
  Money calculateTotalValue(Portfolio portfolio, Currency targetCurrency,
      Map<AssetSymbol, MarketAssetQuote> quoteCache);

  /**
   * Calculates the total value of an account in its base currency.
   *
   * @param account    the account to value
   * @param quoteCache pre-fetched quotes for all assets in the account
   * @return total account value in base currency
   */
  Money calculateAccountValue(Account account, Map<AssetSymbol, MarketAssetQuote> quoteCache);

  /**
   * Calculates the market value of all holdings, excluding cash.
   *
   * @param account    the account to value
   * @param quoteCache pre-fetched quotes for all assets in the account
   * @return total market value of holdings
   */
  Money calculatePositionsValue(Account account, Map<AssetSymbol, MarketAssetQuote> quoteCache);
}