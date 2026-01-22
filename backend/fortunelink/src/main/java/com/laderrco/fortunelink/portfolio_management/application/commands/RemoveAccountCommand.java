package com.laderrco.fortunelink.portfolio_management.application.commands;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;

public record RemoveAccountCommand(PortfolioId portfolioId, AccountId accountId) {
}