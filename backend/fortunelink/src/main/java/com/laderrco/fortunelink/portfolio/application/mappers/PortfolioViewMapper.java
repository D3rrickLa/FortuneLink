package com.laderrco.fortunelink.portfolio.application.mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;

@Component
public class PortfolioViewMapper {

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

    public PortfolioView toPortfolioView(Portfolio portfolio, List<AccountView> accountViews, Money totalValue) {
        Objects.requireNonNull(portfolio, "Portfolio cannot be null");

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

    public PortfolioSummaryView toPortfolioSummaryView(Portfolio portfolio, Money totalValue) {
        Objects.requireNonNull(portfolio, "Portfolio cannot be null");
        Objects.requireNonNull(totalValue, "Total value cannot be null");

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

    public AccountView toAccountView(Account account, List<PositionView> positionViews, Money totalValue,
            Money cashBalance) {
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
     * Maps a position to its view with no fee data.
     * Used for summary screens where tax data is not needed.
     * totalFeesIncurred will be Price.ZERO.
     */
    public PositionView toPositionView(Position position, MarketAssetQuote quote) {
        return toPositionView(position, quote, Money.ZERO(position.accountCurrency()));
    }

    /**
     * Maps a position to its view.
     * * Fee Display Note: Following Canadian Tax Law (CRA), fees ARE included
     * in the position's totalCostBasis. The feesForSymbol parameter is used
     * strictly for breakdown/display purposes (transparency).
     *
     * UI contract:
     * Holdings screen → totalCostBasis (already includes fees)
     * Tax / ACB screen → totalCostBasis (this IS the effective ACB)
     *
     * @param position      the position value object
     * @param quote         current market quote (nullable — falls back to cost
     *                      basis)
     * @param feesForSymbol cumulative BUY fees for this symbol in account currency
     */
    public PositionView toPositionView(Position position, MarketAssetQuote quote, Money feesForSymbol) {
        AssetSymbol symbol = position.symbol();
        Currency currency = position.accountCurrency();

        // Ensure fee currency matches — defensive check since this comes from external
        // computation
        Money fees = (feesForSymbol != null && feesForSymbol.currency().equals(currency))
                ? feesForSymbol
                : Money.ZERO(currency);

        Money totalAcb = position.totalCostBasis(); // $1,010
        Money feeComponent = fees; // $10
        Money grossCost = totalAcb.subtract(feeComponent); // $1,000

        if (quote == null || quote.currentPrice() == null || quote.currentPrice().pricePerUnit().isZero()) {
            return new PositionView(
                    symbol.symbol(),
                    position.type(),
                    position.totalQuantity(),
                    new Price(position.totalCostBasis()),
                    new Price(position.costPerUnit()),
                    new Price(fees), // fees even when quote unavailable
                    new Price(totalAcb), // The real ACB for taxes
                    new Price(grossCost), // The "sticker price" of the shares
                    new Price(feeComponent), // The "service charge"
                    PercentageChange.ZERO,
                    determineMethodology(position),
                    extractFirstAcquiredDate(position),
                    extractLastModifiedDate(position));
        }

        Price currentPrice = quote.currentPrice();
        Money marketValue = position.currentValue(currentPrice.pricePerUnit());
        Money unrealizedPnL = marketValue.subtract(position.totalCostBasis());
        PercentageChange returnPct = calculateReturnPercentage(unrealizedPnL, position.totalCostBasis());

        return new PositionView(
                symbol.symbol(),
                position.type(),
                position.totalQuantity(),
                new Price(position.totalCostBasis()),
                new Price(position.costPerUnit()),
                new Price(fees),
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
            case AcbPosition acb -> acb.firstAcquiredAt();
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