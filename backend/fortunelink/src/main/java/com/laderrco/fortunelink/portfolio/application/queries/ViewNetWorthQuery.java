package com.laderrco.fortunelink.portfolio.application.queries;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;


// asOfDate Optional
public record ViewNetWorthQuery(PortfolioId portfolioId, UserId userId, Instant asOfDate) {

}
