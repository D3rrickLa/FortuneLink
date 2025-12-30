package com.laderrco.fortunelink.portfolio_management.application.mappers;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.application.responses.AccountResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.AssetResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.PortfolioResponse;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
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
@RequiredArgsConstructor
@Component
public class PortfolioMapper {
    
    private final ExchangeRateService exchangeRateService;

    /**
     * Converts Portfolio domain object to PortfolioResponse DTO
     * 
     * @param portfolio The portfolio domain object
     * @param marketDataService Service to fetch current market prices
     * @return PortfolioResponse DTO or null if portfolio is null
     */
    public PortfolioResponse toResponse(Portfolio portfolio, MarketDataService marketDataService) {
        if (portfolio == null) {
            return null;
        }

        List<AccountResponse> accountResponses = portfolio.getAccounts().stream()
            .map(account -> toAccountResponse(account, marketDataService))
            .collect(Collectors.toList());

        Money totalAssetsValue = portfolio.getAssetsTotalValue(marketDataService, exchangeRateService);

        return new PortfolioResponse(
            portfolio.getPortfolioId(),
            portfolio.getUserId(),
            accountResponses,
            totalAssetsValue,
            portfolio.getTransactionCount(),
            portfolio.getSystemCreationDate(),
            portfolio.getLastUpdatedAt()
        );
    }

    /**
     * Converts Account domain object to AccountResponse DTO
     * 
     * @param account The account domain object
     * @param marketDataService Service to fetch current market prices
     * @return AccountResponse DTO or null if account is null
     */
    public AccountResponse toAccountResponse(Account account, MarketDataService marketDataService) {
        if (account == null) {
            return null;
        }
        
        Money totalValue = account.calculateTotalValue(marketDataService);
        Money cashBalance = calculateCashBalance(account);
        
        return new AccountResponse(
            account.getAccountId(),
            account.getName(),
            account.getAccountType(),
            account.getBaseCurrency(),
            cashBalance,
            totalValue,
            account.getSystemCreationDate()
        );
    }

    /**
     * Calculates total cash balance in an account by summing all CASH type assets
     * 
     * @param account The account to calculate cash balance for
     * @return Total cash balance in account's base currency
     */
    private Money calculateCashBalance(Account account) {
        return account.getAssets().stream()
            .filter(asset -> asset.getAssetIdentifier().getAssetType() == AssetType.CASH)
            .map(Asset::getCostBasis)
            .reduce(Money::add)
            .orElse(Money.ZERO(account.getBaseCurrency()));
    }

    /**
     * Converts Asset domain object to AssetResponse DTO
     * Static method as it doesn't require ExchangeRateService
     * 
     * @param asset The asset domain object
     * @param currentPrice Current market price for the asset
     * @return AssetResponse DTO or null if asset is null
     */
    public static AssetResponse toAssetResponse(Asset asset, Money currentPrice) {
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
            // Fallback to safe zero values
            ValidatedCurrency currency = asset.getCurrency();
            currentValue = Money.ZERO(currency);
            unrealizedGain = Money.ZERO(currency);
            unrealizedGainPercentage = Percentage.of(0);
        }
        
        return new AssetResponse(
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
            asset.getLastSystemInteraction()
        );
    }
    
    /**
     * Calculates gain/loss percentage
     * Handles edge cases for zero cost basis
     * 
     * @param gain The gain or loss amount
     * @param costBasis The original cost basis
     * @return Percentage gain/loss
     */
    private static Percentage calculateGainPercentage(Money gain, Money costBasis) {
        if (costBasis == null || costBasis.amount().compareTo(BigDecimal.ZERO) == 0) {
            return Percentage.of(0);
        }
        
        BigDecimal percentageValue = gain.amount()
            .divide(costBasis.amount(), Precision.PERCENTAGE.getDecimalPlaces(), Rounding.PERCENTAGE.getMode())
            .multiply(BigDecimal.valueOf(100));
        
        return new Percentage(percentageValue);
    }
}