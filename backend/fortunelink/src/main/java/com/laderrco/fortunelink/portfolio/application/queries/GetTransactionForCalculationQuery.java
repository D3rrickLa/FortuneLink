package com.laderrco.fortunelink.portfolio.application.queries;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public record GetTransactionForCalculationQuery(PortfolioId portfolioId, UserId userId, AccountId accountId,
    Instant start, Instant end) {
}