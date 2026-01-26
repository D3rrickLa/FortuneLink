package com.laderrco.fortunelink.portfolio_management.application.commands;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;

public record CorrectAssetTickerCommand(PortfolioId portfolioId, AccountId accountId, AssetIdentifier wrongAssetIdentifier, AssetIdentifier correctAssetIdentifier) {
    
}
