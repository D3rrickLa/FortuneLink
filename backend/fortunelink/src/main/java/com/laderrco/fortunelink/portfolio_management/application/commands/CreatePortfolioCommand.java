package com.laderrco.fortunelink.portfolio_management.application.commands;

import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.UserId;

public record CreatePortfolioCommand(UserId userId, String name, Currency defaultCurrency, String description, boolean createDefaultAccount) {
    
}
