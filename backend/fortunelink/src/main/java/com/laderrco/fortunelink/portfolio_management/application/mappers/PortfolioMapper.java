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
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

import lombok.AllArgsConstructor;

// convertes between portfolio domain entity and DTOs, handles ocmplex nested conversions
@AllArgsConstructor
@Component
public class PortfolioMapper {
    private ExchangeRateService exchangeRateService;

    public PortfolioResponse toResponse(Portfolio portfolio, MarketDataService marketDataService) {
        if (portfolio == null) {
            return null;
        }

        // convert all accounts with market data
        List<AccountResponse> accountResponses = portfolio.getAccounts().stream()
            .map(account -> toAccountResponse(account, marketDataService))
            .collect(Collectors.toList());

        // Money netWorth = portfolio.calculateNetWorth(marketDataService, this.exchangeRateService);

        Money totalAssetsValue = portfolio.getAssetsTotalValue(marketDataService, this.exchangeRateService);

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

    public AccountResponse toAccountResponse(Account account, MarketDataService marketDataService) {
        if (account == null) {
            return null;
        }
        
       // Calculate total account value using domain method
        Money totalValue = account.calculateTotalValue(marketDataService);
        
        // Calculate cash balance by finding all CASH type assets
        Money cashBalance = account.getAssets().stream()
                .filter(asset -> asset.getAssetIdentifier().getAssetType() == AssetType.CASH)
                .map(asset -> {
                    // For cash assets, the cost basis represents the cash amount
                    return asset.getCostBasis();
                })
                .reduce(Money::add)
                .orElse(new Money(BigDecimal.ZERO, account.getBaseCurrency()));
        
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

    public static AssetResponse toAssetResponse (Asset asset, Money currentPrice) {
        if (asset == null) {
            return null;
        }
        
        // Calculate current value if price is available
        Money currentValue = null;
        Money unrealizedGain = null;
        
        if (currentPrice != null) {
            // Convert Money to Price value object for calculation
            // Price price = new Price(currentPrice);
            currentValue = asset.calculateCurrentValue(currentPrice);
            unrealizedGain = asset.calculateUnrealizedGainLoss(currentPrice);
        }
        else {
            // fail safe
            currentValue = Money.ZERO(asset.getCurrency());
            unrealizedGain = Money.ZERO(asset.getCurrency());
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
            Percentage.of(unrealizedGain.amount()),
            asset.getAcquiredOn(),
            asset.getLastSystemInteraction()
        );
    }
    
}