package com.laderrco.fortunelink.portfolio_management.application.commands;

import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.UserId;

public record UpdateAccountCommand(PortfolioId portfolioId, UserId userId, AccountId accountId, String accountName) {
    
}
