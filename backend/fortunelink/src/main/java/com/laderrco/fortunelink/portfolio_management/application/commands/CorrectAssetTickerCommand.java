package com.laderrco.fortunelink.portfolio_management.application.commands;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;

public record CorrectAssetTickerCommand(UserId userId, AccountId accountId, AssetIdentifier wrongAssetIdentifier, AssetIdentifier correctAssetIdentifier) {
    
}
