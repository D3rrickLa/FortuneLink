package com.laderrco.fortunelink.portfolio.application.views;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import java.time.Instant;

// when we have multiple portfolios - we use this
public record PortfolioSummaryView(
    PortfolioId id, String name, Money totalValue, Instant lastUpdated) {
}