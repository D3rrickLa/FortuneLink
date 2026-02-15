package com.laderrco.fortunelink.portfolio_management.application.views;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.PortfolioId;

// when we have multiple portfolios - we use this
public record PortfolioSummaryView(PortfolioId id, String name, Money totalValue, Instant lastUpdated) {}