package com.laderrco.fortunelink.portfolio_management.infrastructure.models.portfolio;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.infrastructure.models.portfolio.responses.AccountResponse;
import com.laderrco.fortunelink.portfolio_management.infrastructure.models.portfolio.responses.AssetHoldingResponse;
import com.laderrco.fortunelink.portfolio_management.infrastructure.models.portfolio.responses.PortfolioResponse;

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
    public PortfolioResponse toResponse(Portfolio portfolio) {
        return new PortfolioResponse(
            portfolio.getPortfolioId().toString(),
            portfolio.getUserId().toString(),
            null,
            null,
            portfolio.getAccounts().stream()
                .map(this::toAccountResponse)
                .collect(Collectors.toList()),
            LocalDateTime.ofInstant(portfolio.getSystemCreationDate(), ZoneId.systemDefault()),
            LocalDateTime.ofInstant(portfolio.getLastUpdatedAt(), ZoneId.systemDefault())
        );
    }
    
    /**
     * Convert domain Account to API response DTO.
     */
    public AccountResponse toAccountResponse(Account account) {
        return new AccountResponse(
            account.getAccountId().toString(),
            account.getName(),
            account.getAccountType().name(),
            account.getBaseCurrency().toString(),
            account.getAssets().stream()
                .map(this::toAssetResponse)
                .collect(Collectors.toList())
        );
    }
    
    /**
     * Convert domain Asset to API response DTO.
     */
    public AssetHoldingResponse toAssetResponse(Asset asset) {
        return new AssetHoldingResponse(
            asset.getAssetId().toString(),
            asset.getAssetIdentifier().getPrimaryId(),
            asset.getAssetIdentifier().getAssetType().toString(),
            asset.getQuantity(),
            asset.getCostBasis().amount(),
            LocalDateTime.ofInstant(asset.getAcquiredOn(), ZoneId.systemDefault())
        );
    }
}