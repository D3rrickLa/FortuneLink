package com.laderrco.fortunelink.portfolio.application.commands;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public record UpdatePortfolioCommand(
    PortfolioId portfolioId,
    UserId userId,
    String name,
    String description,
    Currency currency) implements PortfolioLifecycleCommand {

}
