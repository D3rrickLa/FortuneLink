package com.laderrco.fortunelink.portfolio_management.application.queries.views.assemblers;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.application.queries.views.AccountView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AssetView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PortfolioSummaryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PortfolioView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionView;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

import lombok.RequiredArgsConstructor;

/**
 * Mapper for converting Portfolio domain objects to response DTOs
 * Uses ExchangeRateService for currency conversions
 */
@Component
@RequiredArgsConstructor
public class PortfolioViewAssembler {

    private final ExchangeRateService exchangeRateService;
    private final MarketDataService marketDataService;

    /**
     * Assembles a detailed {@link PortfolioView} from a {@link Portfolio}
     * aggregate.
     *
     * This method computes all derived financial values such as total asset value
     * and per-account valuations using current market data and exchange rates.
     *
     * @param portfolio the portfolio aggregate to project
     * @return a fully populated {@link PortfolioView}, or {@code null} if portfolio
     *         is null
     */
    public PortfolioView assemblePortfolioView(Portfolio portfolio) {
        if (portfolio == null) {
            return null;
        }

        List<AccountView> accounts = portfolio.getAccounts().stream()
                .map(this::assembleAccountView)
                .collect(Collectors.toList());

        Money totalAssetsValue = portfolio.getAssetsTotalValue(marketDataService, exchangeRateService);

        return new PortfolioView(
                portfolio.getPortfolioId(),
                portfolio.getUserId(),
                portfolio.getName(),
                portfolio.getDescription(),
                accounts,
                totalAssetsValue,
                portfolio.getTransactionCount(),
                portfolio.getSystemCreationDate(),
                portfolio.getLastUpdatedAt());
    }

    /**
     * Assembles an {@link AccountView} from an {@link Account} entity.
     *
     * Calculates account-level financial summaries such as total value and
     * available cash balance.
     *
     * @param account the account entity
     * @return an {@link AccountView}, or {@code null} if account is null
     */
    public AccountView assembleAccountView(Account account) {
        if (account == null) {
            return null;
        }

        Money totalValue = account.calculateTotalValue(marketDataService);
        Money cashBalance = calculateCashBalance(account);

        List<AssetView> assets = account.getAssets().stream()
                .map(asset -> assembleAssetView(
                        asset,
                        marketDataService.getCurrentPrice(asset.getAssetIdentifier())))
                .collect(Collectors.toList());

        return new AccountView(
                account.getAccountId(),
                account.getName(),
                account.getAccountType(),
                assets,
                account.getBaseCurrency(),
                cashBalance,
                totalValue,
                account.getSystemCreationDate());
    }

    /**
     * Assembles a lightweight summary view of a portfolio.
     *
     * <p>
     * Intended for list views and overviews where full portfolio details
     * are not required.
     * </p>
     *
     * @param portfolio the portfolio aggregate
     * @return a {@link PortfolioSummaryView}
     */
    public PortfolioSummaryView assemblePortfolioSummaryView(Portfolio portfolio) {
        return new PortfolioSummaryView(
                portfolio.getPortfolioId(),
                portfolio.getName(),
                portfolio.getAssetsTotalValue(marketDataService, exchangeRateService),
                portfolio.getLastUpdatedAt());
    }

    /**
     * Calculates the total cash balance of an account by summing all
     * CASH-type assets.
     *
     * @param account the account to evaluate
     * @return total cash balance in the account's base currency
     */
    private Money calculateCashBalance(Account account) {
        return account.getAssets().stream()
                .filter(asset -> asset.getAssetIdentifier().getAssetType() == AssetType.CASH)
                .map(Asset::getCostBasis)
                .reduce(Money::add)
                .orElse(Money.ZERO(account.getBaseCurrency()));
    }

    /**
     * Assembles an {@link AssetView} from an {@link Asset} entity.
     *
     * Computes current valuation, unrealized gain/loss, and percentage change
     * using the provided market price.
     *
     * @param asset the asset entity
     * @param currentPrice current market price for the asset (may be null)
     * @return an {@link AssetView}, or {@code null} if asset is null
     */
    public AssetView assembleAssetView(Asset asset, Money currentPrice) {
        if (asset == null) {
            return null;
        }

        Money currentValue;
        Money unrealizedGain;
        Percentage unrealizedGainPercentage;

        if (currentPrice != null) {
            currentValue = asset.calculateCurrentValue(currentPrice);
            unrealizedGain = asset.calculateUnrealizedGainLoss(currentPrice);
            unrealizedGainPercentage = calculateGainPercentage(unrealizedGain, asset.getCostBasis());
        } else {
            ValidatedCurrency currency = asset.getCurrency();
            currentPrice = Money.ZERO(currency);
            currentValue = Money.ZERO(currency);
            unrealizedGain = Money.ZERO(currency);
            unrealizedGainPercentage = Percentage.of(0);
        }

        return new AssetView(
                asset.getAssetId(),
                asset.getAssetIdentifier().getPrimaryId(),
                asset.getAssetIdentifier().getAssetType(),
                asset.getQuantity(),
                asset.getCostBasis(),
                asset.getCostPerUnit(),
                currentPrice,
                currentValue,
                unrealizedGain,
                unrealizedGainPercentage,
                asset.getAcquiredOn(),
                asset.getLastSystemInteraction());
    }

    public TransactionView assembleTransactionView(Transaction transaction) {
        return new TransactionView(
            transaction.getTransactionId(),
            transaction.getTransactionType(),
            transaction.getAssetIdentifier().getPrimaryId(),
            transaction.getQuantity(),
            transaction.getPricePerUnit(),
            transaction.getFees(),
            transaction.calculateTotalCost(),
            transaction.getTransactionDate(),
            transaction.getNotes()
        );
    }

    /**
     * Calculates the percentage gain or loss relative to the original cost basis.
     *
     * @param gain the absolute gain or loss
     * @param costBasis the original investment amount
     * @return percentage gain/loss, or zero if cost basis is zero
     */
    private static Percentage calculateGainPercentage(Money gain, Money costBasis) {
        if (costBasis == null || costBasis.amount().compareTo(BigDecimal.ZERO) == 0) {
            return Percentage.of(0);
        }

        BigDecimal percentageValue = gain.amount()
                .divide(
                        costBasis.amount(),
                        Precision.PERCENTAGE.getDecimalPlaces(),
                        Rounding.PERCENTAGE.getMode())
                .multiply(BigDecimal.valueOf(100));

        return new Percentage(percentageValue);
    }
}