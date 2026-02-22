package com.laderrco.fortunelink.portfolio.application.mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioSummaryView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio.application.views.PositionView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.PercentageChange;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.FifoPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PortfolioViewMapper {
    private final PortfolioValuationService portfolioValuationService;

    public PortfolioView toNewPortfolioView(Portfolio portfolio) {
        return new PortfolioView(
                portfolio.getPortfolioId(),
                portfolio.getUserId(),
                portfolio.getName(),
                portfolio.getDescription(),
                List.of(), // no accounts with positions yet
                Money.ZERO(portfolio.getDisplayCurrency()),
                portfolio.getCreatedAt(),
                portfolio.getLastUpdatedAt());
    }

    public PortfolioView toPortfolioView(Portfolio portfolio, Currency currency,
            Map<AssetSymbol, MarketAssetQuote> quoteCache) {
        Objects.requireNonNull(portfolio, "Portfolio cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");

        // Map accounts with cached quotes
        List<AccountView> accountViews = portfolio.getAccounts().stream()
                .map(account -> toAccountView(account, quoteCache))
                .toList();

        // Calculate portfolio-level totals in user's display currency
        // this technically can be called one level up to PortfolioValuatoinService
        Money totalValue = portfolioValuationService.calculateTotalValue(portfolio, currency, quoteCache);

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

    public PortfolioSummaryView toPortfolioSummaryView(Portfolio portfolio, Currency currency,
            Map<AssetSymbol, MarketAssetQuote> quoteCache) {
        Objects.requireNonNull(portfolio, "Portfolio cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");

        Money totalValue = portfolioValuationService.calculateTotalValue(portfolio, currency, quoteCache);

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

    public AccountView toAccountView(Account account, Map<AssetSymbol, MarketAssetQuote> quoteCache) {
        // Map positions to view DTOs
        List<PositionView> positionViews = account.getPositionEntries().stream()
                .map(entry -> toPositionView(entry.getValue(), quoteCache.get(entry.getKey())))
                .toList();

        // Calculate account totals in account's base currency
        Money totalValue = portfolioValuationService.calculateAccountValue(account, quoteCache);
        // Single source of truth: the field on the entity, not a position scan
        // NOTE: might be a unesscesary check
        Money cashBalance = Optional.ofNullable(account.getCashBalance())
                .orElse(Money.ZERO(account.getAccountCurrency()));

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
    public PositionView toPositionView(Position position, MarketAssetQuote quote) {
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
            case AcbPosition _ -> "ACB";
            case FifoPosition _ -> "FIFO";
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
            case AcbPosition _ -> null; // ACB doesn't track individual lot dates
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
            case AcbPosition _ -> null; // Would need to be added to AcbPosition
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