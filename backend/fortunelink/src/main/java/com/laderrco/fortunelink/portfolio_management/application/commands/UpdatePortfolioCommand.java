package com.laderrco.fortunelink.portfolio_management.application.commands;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

public record UpdatePortfolioCommand(PortfolioId id, UserId userId, String name, ValidatedCurrency defaultCurrency, String description) {
    
}
