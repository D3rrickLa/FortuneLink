package com.laderrco.fortunelink.portfolio_management.application.commands;

import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.UserId;

public record CreatePortfolioCommand(UserId userId, String name, String description, String locale, boolean createDefaultAccount) {
    
}
