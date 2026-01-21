package com.laderrco.fortunelink.portfolio_management.infrastructure.models.portfolio.mappers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.application.views.AccountView;
import com.laderrco.fortunelink.portfolio_management.application.views.AssetView;
import com.laderrco.fortunelink.portfolio_management.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio_management.infrastructure.models.portfolio.responses.AccountHttpResponse;
import com.laderrco.fortunelink.portfolio_management.infrastructure.models.portfolio.responses.AssetHoldingHttpResponse;
import com.laderrco.fortunelink.portfolio_management.infrastructure.models.portfolio.responses.PortfolioHttpResponse;

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
    public PortfolioHttpResponse toResponse(PortfolioView portfolio) {
        return new PortfolioHttpResponse(
            portfolio.portfolioId().toString(),
            portfolio.userId().toString(),
            portfolio.name(),
            portfolio.description(),
            portfolio.accounts().stream()
                .map(this::toAccountResponse)
                .collect(Collectors.toList()),
            LocalDateTime.ofInstant(portfolio.createDate(), ZoneId.systemDefault()),
            LocalDateTime.ofInstant(portfolio.lastUpdated(), ZoneId.systemDefault())
        );
    }
    
    /**
     * Convert application AccountResponse to API response DTO.
     */
    public AccountHttpResponse toAccountResponse(AccountView account) {
        return new AccountHttpResponse(
            account.accountId().toString(),
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