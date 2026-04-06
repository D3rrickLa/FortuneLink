package com.laderrco.fortunelink.portfolio.application.queries;

import org.springframework.data.domain.Pageable;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public record GetAllAccountsQuery(PortfolioId portfolioId, UserId userId, Pageable pageable) {
}
