package com.laderrco.fortunelink.portfolio_management.application.commands;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;

public record DeletePortfolioCommand(UserId userId, boolean confirmed, boolean softDelete) {
    
}
