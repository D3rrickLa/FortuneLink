package com.laderrco.fortunelink.portfolio.application.commands;

import com.laderrco.fortunelink.portfolio.application.utils.annotations.HasPortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

// DeletePortfolioCommand.java
// Remove 'confirmed' entirely, the act of calling the endpoint IS confirmation.
// If you ever need two-phase confirmation, implement it as a separate endpoint
// that issues a short-lived token, not a boolean field on the command.
public record DeletePortfolioCommand(
    PortfolioId portfolioId,
    UserId userId,
    boolean softDelete,
    boolean recursive) implements HasPortfolioId {

}
