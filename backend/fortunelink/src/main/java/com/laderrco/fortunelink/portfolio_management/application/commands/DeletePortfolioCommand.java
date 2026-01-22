package com.laderrco.fortunelink.portfolio_management.application.commands;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;

/**
 * PortfolioId -> what to del
 * UserId -> who is requesting it (for auth)
 */
public record DeletePortfolioCommand(PortfolioId portfolioId, UserId userId, boolean confirmed, boolean softDelete) {
 public DeletePortfolioCommand {}   
}
