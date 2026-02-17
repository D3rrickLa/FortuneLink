package com.laderrco.fortunelink.portfolio_management.application.mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.application.views.AccountView;
import com.laderrco.fortunelink.portfolio_management.application.views.PortfolioSummaryView;
import com.laderrco.fortunelink.portfolio_management.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio_management.application.views.PositionView;
import com.laderrco.fortunelink.portfolio_management.application.views.TransactionView;
import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.PercentageChange;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.positions.FifoPosition;
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
     * @param currency    user's currency display preference
     */
    public PortfolioView toPortfolioView(Portfolio portfolio, Currency currency) {
        Objects.requireNonNull(portfolio, "Portfolio cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");

        // Step 1: Batch fetch all market quotes upfront
        Set<AssetSymbol> allSymbols = extractAllAssetSymbols(portfolio);
        Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(allSymbols);

        // Step 2: Map accounts with cached quotes
        List<AccountView> accountViews = portfolio.getAccounts().stream()
                .map(account -> toAccountView(account, quoteCache))
                .toList();

        // Step 3: Calculate portfolio-level totals in user's display currency
        Money totalValue = calculatePortfolioTotalValue(portfolio, quoteCache, currency);

        return new PortfolioView(
                portfolio.getPortfolioId(),
                portfolio.getUserId(),
                portfolio.getName(),
                portfolio.getDescription(),
                accountViews,
                totalValue,
                portfolio.getCreatedAt(),
                portfolio.getLastUpdatedAt());
    }

    public PortfolioSummaryView toPortfolioSummaryView(Portfolio portfolio, Currency currency) {
        Objects.requireNonNull(portfolio, "Portfolio cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");

        Set<AssetSymbol> allSymbols = extractAllAssetSymbols(portfolio);
        Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(allSymbols);

        Money totalValue = calculatePortfolioTotalValue(portfolio, quoteCache, currency);

        return new PortfolioSummaryView(
                portfolio.getPortfolioId(),
                portfolio.getName(),
                totalValue,
                portfolio.getLastUpdatedAt());
    }

    public AccountView toNewAccountView(Account account) {
        return new AccountView(
                account.getAccountId(),
                account.getName(),
                account.getAccountType(),
                Collections.emptyList(),
                account.getAccountCurrency(),
                Money.ZERO(account.getAccountCurrency()),
                Money.ZERO(account.getAccountCurrency()),
                account.getCreationDate());
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

    /**
     * Maps a Position to its view representation.
     * Handles both ACB and FIFO positions polymorphically via the sealed interface.
     */
    private PositionView toPositionView(Position position, MarketAssetQuote quote) {
        AssetSymbol symbol = position.symbol();
        Currency currency = position.accountCurrency();

        // Handle missing or stale quote data
        if (quote == null || quote.currentPrice() == null || quote.currentPrice().pricePerUnit().isZero()) {
            return new PositionView(
                    symbol.symbol(),
                    position.type(),
                    position.totalQuantity(),
                    new Price(position.totalCostBasis()),
                    new Price(position.costPerUnit()),
                    Price.ZERO(currency), // current price
                    Price.ZERO(currency), // market value
                    Price.ZERO(currency), // unrealized P&L
                    PercentageChange.ZERO, // gain/loss %
                    determineMethodology(position), // ACB or FIFO
                    extractFirstAcquiredDate(position),
                    extractLastModifiedDate(position));
        }

        Price currentPrice = quote.currentPrice();

        // Calculate derived values using Position interface methods
        Money marketValue = position.currentValue(currentPrice.pricePerUnit());
        Money unrealizedPnL = marketValue.subtract(position.totalCostBasis());
        PercentageChange returnPct = calculateReturnPercentage(unrealizedPnL, position.totalCostBasis());

        return new PositionView(
                symbol.symbol(),
                position.type(),
                position.totalQuantity(),
                new Price(position.totalCostBasis()),
                new Price(position.costPerUnit()),
                currentPrice,
                new Price(marketValue),
                new Price(unrealizedPnL),
                returnPct,
                determineMethodology(position),
                extractFirstAcquiredDate(position),
                extractLastModifiedDate(position));
    }

    public TransactionView toTransactionView(Transaction transaction) {
        Objects.requireNonNull(transaction, "Transaction cannot be null");

        return new TransactionView(
                transaction.transactionId(),
                transaction.transactionType(),
                transaction.execution().asset().symbol(),
                transaction.execution().quantity(),
                transaction.execution().pricePerUnit(),
                transaction.fees(),
                transaction.cashDelta(),
                transaction.metadata().additionalData(),
                transaction.occurredAt().timestamp(),
                transaction.notes());
    }

    // ===== Helper Methods =====

    private Set<AssetSymbol> extractAllAssetSymbols(Portfolio portfolio) {
        return portfolio.getAccounts().stream()
                .flatMap(account -> account.getPositionEntries().stream().map(a -> a.getKey()))
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
    private Money calculateAccountTotalValue(Account account, Map<AssetSymbol, MarketAssetQuote> quoteCache) {

        Currency accountCurrency = account.getAccountCurrency();

        Money positionsValue = account.getPositionEntries().stream()
                .map(entry -> {
                    Position position = entry.getValue();
                    MarketAssetQuote quote = quoteCache.get(entry.getKey());

                    Money valueInPositionCurrency;
                    if (quote == null || quote.currentPrice() == null || quote.currentPrice().pricePerUnit().isZero()) {
                        // No market data - use cost basis
                        valueInPositionCurrency = position.totalCostBasis();
                    } else {
                        // Calculate market value using Position's own method
                        valueInPositionCurrency = position.currentValue(quote.currentPrice().pricePerUnit());
                    }

                    // Convert to account's base currency if needed
                    // Position is already in account currency per the interface
                    return valueInPositionCurrency;
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
        return account.getPositionEntries().stream()
                .filter(entry -> entry.getValue().type() == AssetType.CASH)
                .map(entry -> entry.getValue().totalCostBasis())
                .reduce(Money::add)
                .orElse(Money.ZERO(account.getAccountCurrency()));
    }

    /**
     * Calculates return percentage: (gain / cost basis) * 100
     * Returns ZERO if cost basis is zero/null to avoid division by zero.
     */
    private static PercentageChange calculateReturnPercentage(Money gain, Money costBasis) {
        if (costBasis == null || costBasis.isZero()) {
            return new PercentageChange(BigDecimal.ZERO);
        }

        BigDecimal percentageValue = gain.amount()
                .divide(costBasis.amount(),
                        Precision.PERCENTAGE.getDecimalPlaces(),
                        Rounding.PERCENTAGE.getMode())
                .multiply(BigDecimal.valueOf(100));

        return new PercentageChange(percentageValue);
    }

    /**
     * Determines the cost basis methodology used by the position.
     * Returns "ACB" for Canadian tax method or "FIFO" for US tax method.
     */
    private static String determineMethodology(Position position) {
        return switch (position) {
            case AcbPosition ignored -> "ACB";
            case FifoPosition ignored -> "FIFO";
        };
    }

    /**
     * Extracts the earliest acquisition date from the position.
     * For ACB: would need to track separately if you want this
     * For FIFO: first lot's acquisition date
     * 
     * NOTE: Your Position interface doesn't expose this yet.
     * You may need to add this to the interface or track separately.
     */
    private static Instant extractFirstAcquiredDate(Position position) {
        return switch (position) {
            case AcbPosition acb -> null; // ACB doesn't track individual lot dates
            case FifoPosition fifo -> {
                var lots = fifo.lots();
                yield lots.isEmpty() ? null : lots.get(0).acquiredDate();
            }
        };
    }

    /**
     * Extracts the most recent modification date.
     * This would typically come from the aggregate root or event sourcing.
     * 
     * NOTE: Your Position interface doesn't expose this.
     * Consider adding lastModifiedAt to Position interface or tracking at Account
     * level.
     */
    private static Instant extractLastModifiedDate(Position position) {
        return switch (position) {
            case AcbPosition acb -> null; // Would need to be added to AcbPosition
            case FifoPosition fifo -> {
                var lots = fifo.lots();
                yield lots.isEmpty() ? null
                        : lots.stream()
                                .map(lot -> lot.acquiredDate())
                                .max(Instant::compareTo)
                                .orElse(null);
            }
        };
    }
}