package com.laderrco.fortunelink.portfolio.application.services;

import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;

import lombok.RequiredArgsConstructor;


/**
 * Pure math implementation of PortfolioValuationService.
 *
 * CONTRACT: Never calls MarketDataService. All quotes are pre-fetched
 * by the calling application service and passed in via quoteCache.
 * This service only does arithmetic.
 */
@Service
@RequiredArgsConstructor
public final class PortfolioValuationServiceImpl implements PortfolioValuationService {

    private final ExchangeRateService exchangeRateService;

    /**
     * Sums all account values, converting each to the target display currency.
     * Each account may trade in a different base currency (e.g. CAD TFSA + USD
     * brokerage).
     */
    @Override
    public Money calculateTotalValue(Portfolio portfolio, Currency targetCurrency,
            Map<AssetSymbol, MarketAssetQuote> quoteCache) {
        Objects.requireNonNull(portfolio, "Portfolio cannot be null");
        Objects.requireNonNull(targetCurrency, "Target currency cannot be null");
        Objects.requireNonNull(quoteCache, "Quote cache cannot be null");

        return portfolio.getAccounts().stream()
                .map(account -> calculateAccountValue(account, quoteCache))
                .map(accountValue -> exchangeRateService.convert(accountValue, targetCurrency))
                .reduce(Money::add)
                .orElse(Money.ZERO(targetCurrency));
    }

    /**
     * Calculates total account value in the account's own base currency.
     * = positions market value + cash balance
     *
     * Falls back to cost basis when no quote is available for a position.
     * This is intentional: stale/unavailable data shows cost basis rather
     * than zero, which is less misleading for the user.
     */
    @Override
    public Money calculateAccountValue(Account account, Map<AssetSymbol, MarketAssetQuote> quoteCache) {
        Objects.requireNonNull(account, "Account cannot be null");
        Objects.requireNonNull(quoteCache, "Quote cache cannot be null");

        Money positionsValue = calculatePositionsValue(account, quoteCache);
        Money cashBalance = account.getCashBalance();

        return positionsValue.add(cashBalance);
    }

    /**
     * Calculates the market value of all non-cash positions in an account.
     * Cash positions (AssetType.CASH) are excluded — they're captured via
     * account.getCashBalance() in calculateAccountValue().
     */
    @Override
    public Money calculatePositionsValue(Account account, Map<AssetSymbol, MarketAssetQuote> quoteCache) {
        Objects.requireNonNull(account, "Account cannot be null");
        Objects.requireNonNull(quoteCache, "Quote cache cannot be null");

        Currency accountCurrency = account.getAccountCurrency();

        return account.getPositionEntries().stream()
                .filter(entry -> entry.getValue().type() != AssetType.CASH) // cash tracked separately
                .map(entry -> resolvePositionValue(entry.getValue(), quoteCache.get(entry.getKey()), accountCurrency))
                .reduce(Money::add)
                .orElse(Money.ZERO(accountCurrency));
    }

    /**
     * Resolves the current market value of a single position.
     *
     * If no quote is available (symbol not in cache, API was down, etc.),
     * falls back to cost basis. This is a deliberate trade-off: cost basis
     * is a known real number, whereas showing $0 would be actively wrong.
     */
    private Money resolvePositionValue(Position position, MarketAssetQuote quote, Currency accountCurrency) {
        if (quote == null || quote.currentPrice() == null || quote.currentPrice().pricePerUnit().isZero()) {
            // Fallback: use cost basis, not zero
            return position.totalCostBasis();
        }

        return position.currentValue(quote.currentPrice().pricePerUnit());
    }

}
