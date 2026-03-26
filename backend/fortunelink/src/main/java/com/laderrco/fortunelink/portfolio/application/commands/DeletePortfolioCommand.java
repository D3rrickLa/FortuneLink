package com.laderrco.fortunelink.portfolio.application.commands;

import com.laderrco.fortunelink.portfolio.application.utils.valueobjects.HasPortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

/**
 * PortfolioId -> what to del UserId -> who is requesting it (for auth)
 */
public record DeletePortfolioCommand(
		PortfolioId portfolioId, UserId userId, boolean confirmed, boolean softDelete, boolean recursive) 
		implements HasPortfolioId {

}
