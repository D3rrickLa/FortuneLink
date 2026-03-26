package com.laderrco.fortunelink.portfolio.application.commands;

import com.laderrco.fortunelink.portfolio.application.utils.valueobjects.HasPortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public record ReopenAccountCommand(
    AccountId accountId, PortfolioId portfolioId, UserId userId) implements HasPortfolioId {
}