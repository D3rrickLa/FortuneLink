package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.AccountHttpResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.AssetHoldingHttpResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.PortfolioHttpResponse;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AccountView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AssetView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PortfolioSummaryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PortfolioView;

/**
 * Maps between domain models and API DTOs.
 * 
 * NOTE These response types are DIFFERENT to the application layers
 * Those are meant for the DB, while these are for HTTP endpoints
 * 
 * Responsibilities:
 * - Convert Portfolio → PortfolioResponse
 * - Convert Account → AccountResponse
 * - Convert Asset → AssetHoldingResponse
 * - Extract primitive values from value objects
 */
@Component
public class PortfolioDtoMapper {
    
    /**
     * Convert domain Portfolio to API response DTO.
     */
    public PortfolioHttpResponse toPortfolioResponse(PortfolioView portfolio) {
        return new PortfolioHttpResponse(
            portfolio.portfolioId().toString(),
            portfolio.userId().toString(),
            portfolio.name(),
            portfolio.description(),
            portfolio.accounts().stream()
                .map(a -> toAccountResponse(portfolio.portfolioId().toString(), a))
                .collect(Collectors.toList()),
            portfolio.totalValue().amount(),
            portfolio.totalValue().currency().getCode(),
            LocalDateTime.ofInstant(portfolio.createDate(), ZoneId.systemDefault()),
            LocalDateTime.ofInstant(portfolio.lastUpdated(), ZoneId.systemDefault())
        );
    }

    public PortfolioHttpResponse toPortfolioResponse(PortfolioSummaryView portfolio) {
        return new PortfolioHttpResponse(
            portfolio.id().toString(),
            null,
            null,
            null,
            null,
            portfolio.totalValue().amount(),
            portfolio.totalValue().currency().getCode(), 
            null,
            LocalDateTime.ofInstant(portfolio.lastUpdated(), ZoneId.systemDefault())
        );
    }
    
    /**
     * Convert application AccountResponse to API response DTO.
     */
    public AccountHttpResponse toAccountResponse(String id, AccountView account) {
        return new AccountHttpResponse(
            account.accountId().toString(),
            id,
            account.name(),
            account.type().name(),
            account.baseCurrency().toString(),
            account.assets().stream()
                .map(this::toAssetResponse)
                .collect(Collectors.toList())
        );
    }
    
    /**
     * Convert domain Asset to API response DTO.
     */
    public AssetHoldingHttpResponse toAssetResponse(AssetView asset) {
        return new AssetHoldingHttpResponse(
            asset.assetId().toString(),
            asset.symbol(),
            asset.type().toString(),
            asset.quantity(),
            asset.costBasis().amount(),
            LocalDateTime.ofInstant(asset.acquiredDate(), ZoneId.systemDefault())
        );
    }
}