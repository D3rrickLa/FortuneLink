package com.laderrco.fortunelink.portfolio_management.application.commands;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.UserId;

// we are getting the currency form the web - locale by default, make an account for more fine tuning
public record CreatePortfolioCommand(UserId userId, String name, String description, Currency currency, boolean createDefaultAccount, PositionStrategy defaultStrategy) {
    
}
