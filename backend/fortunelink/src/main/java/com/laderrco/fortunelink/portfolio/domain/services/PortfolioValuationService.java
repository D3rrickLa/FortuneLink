package com.laderrco.fortunelink.portfolio.domain.services;

import java.util.Map;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

/**
 * Calculates monetary valuations for portfolio aggregates.
 *
 * DESIGN CONTRACT: All methods accept a pre-fetched quoteCache.
 * This service MUST NOT call MarketDataService internally.
 * Market data is fetched ONCE at the application service layer
 * and passed in — this prevents double API calls when both a
 * query service and this domain service need the same prices.
 *
 * The caller (application service) is responsible for:
 * 1. Extracting all required symbols
 * 2. Calling MarketDataService.getBatchQuotes() once
 * 3. Passing the resulting map into these methods
 */
public interface PortfolioValuationService {
    /**
     * Calculates the total value of a portfolio converted to the target currency.
     *
     * Sums all account values (each in their own base currency),
     * converting to targetCurrency via ExchangeRateService.
     *
     * @param portfolio      the portfolio aggregate
     * @param targetCurrency the currency to express the result in
     * @param quoteCache     pre-fetched quotes for all positions in the portfolio
     * @return total portfolio value in targetCurrency
     */
    Money calculateTotalValue(Portfolio portfolio, Currency targetCurrency,
            Map<AssetSymbol, MarketAssetQuote> quoteCache);

    /**
     * Calculates the total value of an account in the account's base currency.
     *
     * @param account    the account entity
     * @param quoteCache pre-fetched quotes (must include all symbols in this
     *                   account)
     * @return account value in the account's own currency
     */
    Money calculateAccountValue(Account account, Map<AssetSymbol, MarketAssetQuote> quoteCache);

    /**
     * Calculates the market value of all open positions in an account.
     * Excludes cash balance.
     *
     * @param account    the account entity
     * @param quoteCache pre-fetched quotes (must include all symbols in this
     *                   account)
     * @return total positions market value in the account's currency
     */
    Money calculatePositionsValue(Account account, Map<AssetSymbol, MarketAssetQuote> quoteCache);
}