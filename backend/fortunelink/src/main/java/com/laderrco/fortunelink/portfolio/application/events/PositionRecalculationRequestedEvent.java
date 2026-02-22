package com.laderrco.fortunelink.portfolio.application.events;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public record PositionRecalculationRequestedEvent(
		PortfolioId portfolioId, UserId userId, AccountId accountId, AssetSymbol symbol) {
}