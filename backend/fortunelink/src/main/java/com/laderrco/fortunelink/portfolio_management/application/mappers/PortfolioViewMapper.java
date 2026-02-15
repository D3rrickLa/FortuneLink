package com.laderrco.fortunelink.portfolio_management.application.mappers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.application.views.AccountView;
import com.laderrco.fortunelink.portfolio_management.application.views.PortfolioSummaryView;
import com.laderrco.fortunelink.portfolio_management.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio_management.application.views.PositionView;
import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.shared.enums.Precision;
import com.laderrco.fortunelink.portfolio_management.shared.enums.Rounding;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PortfolioViewMapper {
    private final MarketDataService marketDataService;
    private final ExchangeRateService exchangeRateService;

    /**
     * Maps Portfolio aggregate to PortfolioView DTO.
     * Orchestrates market data fetching and enrichment.
     * 
     * @param portfolio the portfolio aggregate
     * @param locale    user's locale for currency display preference
     */
    public PortfolioView toPortfolioView(Portfolio portfolio, Locale locale) {
        Objects.requireNonNull(portfolio, "Portfolio cannot be null");
        Objects.requireNonNull(locale, "Locale cannot be null");

        // Step 1: Batch fetch all market quotes upfront
        Set<AssetSymbol> allSymbols = extractAllAssetSymbols(portfolio);
        Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(allSymbols);

        // Step 2: Map accounts with cached quotes
        List<AccountView> accountViews = portfolio.getAccounts().stream()
                .map(account -> toAccountView(account, quoteCache))
                .toList();

        // Step 3: Calculate portfolio-level totals in user's display currency
        Currency displayCurrency = Currency.fromLocale(locale);
        Money totalValue = calculatePortfolioTotalValue(portfolio, quoteCache, displayCurrency);

        return new PortfolioView(
                portfolio.getPortfolioId(),
                portfolio.getUserId(),
                portfolio.getName(),
                portfolio.getDescription(),
                accountViews,
                totalValue,
                portfolio.getCreatedAt(),
                portfolio.getLastUpdatedOn());
    }

    public PortfolioSummaryView toPortfolioSummaryView(Portfolio portfolio, Locale locale) {
        Objects.requireNonNull(portfolio, "Portfolio cannot be null");
        Objects.requireNonNull(locale, "Locale cannot be null");

        Set<AssetSymbol> allSymbols = extractAllAssetSymbols(portfolio);
        Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(allSymbols);

        Currency displayCurrency = Currency.fromLocale(locale);
        Money totalValue = calculatePortfolioTotalValue(portfolio, quoteCache, displayCurrency);

        return new PortfolioSummaryView(
                portfolio.getPortfolioId(),
                portfolio.getName(),
                totalValue,
                portfolio.getLastUpdatedOn());
    }

    private AccountView toAccountView(Account account, Map<AssetSymbol, MarketAssetQuote> quoteCache) {
        // Map positions to view DTOs
        List<PositionView> positionViews = account.getPositionEntries().stream()
                .map(entry -> toPositionView(entry.getValue(), quoteCache.get(entry.getKey())))
                .toList();

        // Calculate account totals in account's base currency
        Money totalValue = calculateAccountTotalValue(account, quoteCache);
        Money cashBalance = calculateCashBalance(account);

        return new AccountView(
                account.getAccountId(),
                account.getName(),
                account.getAccountType(),
                positionViews,
                account.getAccountCurrency(),
                cashBalance,
                totalValue,
                account.getCreationDate());
    }

    private PositionView toPositionView(Position position, MarketAssetQuote quote) {
        AssetSymbol symbol = position.symbol();
        Currency currency = position.accountCurrency();

        // Handle missing or stale quote data
        if (quote == null || quote.currentPrice() == null || quote.getPrice().isZero()) {
            return new PositionView(
                    symbol.getPrimaryId(),
                    position.type(),
                    position.totalQuantity(),
                    position.totalCostBasis(),
                    position.costPerUnit(),
                    Money.ZERO(currency), // current price
                    Money.ZERO(currency), // market value
                    Money.ZERO(currency), // unrealized P&L
                    Percentage.ZERO, // gain/loss %
                    determineMethodology(position), // ACB or FIFO
                    extractFirstAcquiredDate(position),
                    extractLastModifiedDate(position));
        }

        Money currentPrice = quote.getPrice();

        // Calculate derived values using Position interface methods
        Money marketValue = position.currentValue(currentPrice);
        Money unrealizedPnL = marketValue.subtract(position.totalCostBasis());
        Percentage returnPct = calculateReturnPercentage(unrealizedPnL, position.totalCostBasis());

        return new PositionView(
                symbol.getPrimaryId(),
                position.type(),
                position.totalQuantity(),
                position.totalCostBasis(),
                position.costPerUnit(),
                currentPrice,
                marketValue,
                unrealizedPnL,
                returnPct,
                determineMethodology(position),
                extractFirstAcquiredDate(position),
                extractLastModifiedDate(position));
    }

    public TransactionView toTransactionView(Transaction transaction) {
        Objects.requireNonNull(transaction, "Transaction cannot be null");

        return new TransactionView(
                transaction.getTransactionId(),
                transaction.getTransactionType(),
                transaction.getAssetSymbol().getPrimaryId(),
                transaction.getQuantity(),
                transaction.getPricePerUnit(),
                transaction.getFees(),
                transaction.calculateTotalCost(),
                transaction.getTransactionDate(),
                transaction.getNotes());
    }

    // ===== Helper Methods =====

    private Set<AssetSymbol> extractAllAssetSymbols(Portfolio portfolio) {
        return portfolio.getAccounts().stream()
                .flatMap(account -> account.getPositions().keySet().stream())
                .collect(Collectors.toSet());
    }

    /**
     * Calculates total portfolio value by summing all account values,
     * converting each to the display currency.
     * 
     * More efficient than converting every position individually.
     */
    private Money calculatePortfolioTotalValue(
            Portfolio portfolio,
            Map<AssetSymbol, MarketAssetQuote> quoteCache,
            Currency displayCurrency) {

        return portfolio.getAccounts().stream()
                .map(account -> calculateAccountTotalValue(account, quoteCache))
                .map(accountValue -> exchangeRateService.convert(accountValue, displayCurrency))
                .reduce(Money::add)
                .orElse(Money.ZERO(displayCurrency));
    }

    /**
     * Calculates total value of an account in the account's base currency.
     * Sums position market values, falling back to cost basis when prices
     * unavailable.
     */
    private Money calculateAccountTotalValue(
            Account account,
            Map<AssetSymbol, MarketAssetQuote> quoteCache) {

        Currency accountCurrency = account.getBaseCurrency();

        Money positionsValue = account.getPositions().entrySet().stream()
                .map(entry -> {
                    Position position = entry.getValue();
                    MarketAssetQuote quote = quoteCache.get(entry.getKey());

                    Money valueInPositionCurrency;
                    if (quote == null || quote.getPrice() == null || quote.getPrice().isZero()) {
                        // No market data - use cost basis
                        valueInPositionCurrency = position.getTotalCostBasis();
                    } else {
                        // Calculate market value
                        valueInPositionCurrency = position.calculateMarketValue(quote.getPrice());
                    }

                    // Convert to account's base currency if needed
                    return exchangeRateService.convert(valueInPositionCurrency, accountCurrency);
                })
                .reduce(Money::add)
                .orElse(Money.ZERO(accountCurrency));

        // Add cash balance (already in account currency)
        Money cash = calculateCashBalance(account);
        return positionsValue.add(cash);
    }

    /**
     * Extracts total cash positions within an account.
     * Cash positions are stored as regular positions with AssetType.CASH.
     */
    private Money calculateCashBalance(Account account) {
        return account.getPositions().entrySet().stream()
                .filter(entry -> entry.getKey().getAssetType() == AssetType.CASH)
                .map(entry -> entry.getValue().getTotalCostBasis())
                .reduce(Money::add)
                .orElse(Money.ZERO(account.getBaseCurrency()));
    }

    /**
     * Calculates return percentage: (gain / cost basis) * 100
     * Returns ZERO if cost basis is zero/null to avoid division by zero.
     */
    private static Percentage calculateReturnPercentage(Money gain, Money costBasis) {
        if (costBasis == null || costBasis.isZero()) {
            return Percentage.ZERO;
        }

        BigDecimal percentageValue = gain.amount()
                .divide(costBasis.amount(),
                        Precision.PERCENTAGE.getDecimalPlaces(),
                        Rounding.PERCENTAGE.getMode())
                .multiply(BigDecimal.valueOf(100));

        return new Percentage(percentageValue);
    }
}
