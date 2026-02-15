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
import com.laderrco.fortunelink.portfolio_management.application.views.AssetView;
import com.laderrco.fortunelink.portfolio_management.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
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
     * @param locale user's locale for currency display preference
     */
    public PortfolioView toPortfolioView(Portfolio portfolio, Locale locale) {
        Objects.requireNonNull(portfolio, "Portfolio cannot be null");
        Objects.requireNonNull(locale, "Locale cannot be null");
        
        // Step 1: Batch fetch all market prices upfront
        Set<AssetSymbol> allAssetIds = extractAllAssetSymbols(portfolio);
        Map<AssetSymbol, MarketAssetQuote> priceCache = marketDataService.getBatchQuotes(allAssetIds);
        
        // Step 2: Map accounts with cached prices
        List<AccountView> accountViews = portfolio.getAccounts().stream()
            .map(account -> toAccountView(account, priceCache))
            .toList();
        
        // Step 3: Calculate portfolio-level totals in user's display currency
        Currency displayCurrency = Currency.of(locale.toString());
        Money totalValue = calculatePortfolioTotalValue(portfolio, priceCache, displayCurrency);

        return new PortfolioView(
            portfolio.getPortfolioId(),
            portfolio.getUserId(),
            portfolio.getName(),
            portfolio.getDescription(),
            accountViews,
            totalValue,
            portfolio.getTransactionCount(),
            portfolio.getSystemCreationDate(),
            portfolio.getLastUpdatedAt()
        );
    }

    public PortfolioSummaryView toPortfolioSummaryView(Portfolio portfolio, Locale locale) {
        Objects.requireNonNull(portfolio, "Portfolio cannot be null");
        Objects.requireNonNull(locale, "Locale cannot be null");
        
        Set<AssetSymbol> allAssetIds = extractAllAssetSymbols(portfolio);
        Map<AssetSymbol, Money> priceCache = marketDataService.getCurrentPrices(allAssetIds);
        
        Currency displayCurrency = Currency.fromLocale(locale);
        Money totalValue = calculatePortfolioTotalValue(portfolio, priceCache, displayCurrency);
        
        return new PortfolioSummaryView(
            portfolio.getPortfolioId(),
            portfolio.getName(),
            totalValue,
            portfolio.getLastUpdatedAt()
        );
    }

    private AccountView toAccountView(Account account, Map<AssetSymbol, MarketAssetQuote> priceCache) {
        List<AssetView> assetViews = account.().stream()
            .map(asset -> toAssetView(asset, priceCache.get(asset.getAssetSymbol())))
            .toList();

        Money totalValue = calculateAccountTotalValue(account, priceCache);
        Money cashBalance = calculateCashBalance(account);

        return new AccountView(
            account.getAccountId(),
            account.getName(),
            account.getAccountType(),
            assetViews,
            account.getBaseCurrency(),
            cashBalance,
            totalValue,
            account.getSystemCreationDate()
        );
    }

    private AssetView toAssetView(Asset asset, Money currentPrice) {
        Currency currency = asset.getCurrency();
        
        // Handle missing or zero price data
        if (currentPrice == null || currentPrice.isZero()) {
            return new AssetView(
                asset.getAssetId(),
                asset.getAssetSymbol().getPrimaryId(),
                asset.getAssetSymbol().getAssetType(),
                asset.getQuantity(),
                asset.getCostBasis(),
                asset.getCostPerUnit(),
                Money.ZERO(currency),
                Money.ZERO(currency),
                Money.ZERO(currency),
                Percentage.ZERO,
                asset.getAcquiredOn(),
                asset.getLastSystemInteraction()
            );
        }

        // Calculate derived values using domain methods
        Money currentValue = asset.calculateCurrentValue(currentPrice);
        Money unrealizedGain = asset.calculateUnrealizedGainLoss(currentPrice);
        Percentage gainPercentage = calculateGainPercentage(unrealizedGain, asset.getCostBasis());

        return new AssetView(
            asset.getAssetId(),
            asset.getAssetSymbol().getPrimaryId(),
            asset.getAssetSymbol().getAssetType(),
            asset.getQuantity(),
            asset.getCostBasis(),
            asset.getCostPerUnit(),
            currentPrice,
            currentValue,
            unrealizedGain,
            gainPercentage,
            asset.getAcquiredOn(),
            asset.getLastSystemInteraction()
        );
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
            transaction.getNotes()
        );
    }

    // ===== Helper Methods =====

    private Set<AssetSymbol> extractAllAssetSymbols(Portfolio portfolio) {
        return portfolio.getAccounts().stream()
            .flatMap(account -> account.getAssets().stream())
            .map(Asset::getAssetSymbol)
            .collect(Collectors.toSet());
    }

    /**
     * Calculates total portfolio value by summing all account values,
     * converting each to the display currency based on user locale.
     */
    private Money calculatePortfolioTotalValue(
            Portfolio portfolio, 
            Map<AssetSymbol, Money> priceCache,
            Currency displayCurrency) {
        
        return portfolio.getAccounts().stream()
            .map(account -> calculateAccountTotalValue(account, priceCache))
            .map(accountValue -> exchangeRateService.convert(accountValue, displayCurrency))
            .reduce(Money::add)
            .orElse(Money.ZERO(displayCurrency));
    }

    /**
     * Calculates total value of an account in the account's base currency.
     */
    private Money calculateAccountTotalValue(Account account, Map<AssetSymbol, MarketAssetQuote> priceCache) {
        Currency accountCurrency = account.getBaseCurrency();
        
        return account.getAssets().stream()
            .map(asset -> {
                Money price = priceCache.get(asset.getAssetSymbol());
                if (price == null || price.isZero()) {
                    // Fallback to cost basis if no market price available
                    return asset.getCostBasis();
                }
                Money currentValue = asset.calculateCurrentValue(price);
                // Convert to account's base currency if asset is in different currency
                return exchangeRateService.convert(currentValue, accountCurrency);
            })
            .reduce(Money::add)
            .orElse(Money.ZERO(accountCurrency));
    }

    private Money calculateCashBalance(Account account) {
        return account.getAssets().stream()
            .filter(asset -> asset.getAssetSymbol().getAssetType() == AssetType.CASH)
            .map(Asset::getCostBasis)
            .reduce(Money::add)
            .orElse(Money.ZERO(account.getBaseCurrency()));
    }

    private static Percentage calculateGainPercentage(Money gain, Money costBasis) {
        if (costBasis == null || costBasis.isZero()) {
            return Percentage.ZERO;
        }

        BigDecimal percentageValue = gain.amount()
            .divide(costBasis.amount(), Precision.PERCENTAGE.getDecimalPlaces(), Rounding.PERCENTAGE.getMode())
            .multiply(BigDecimal.valueOf(100));

        return new Percentage(percentageValue);
    }
}
