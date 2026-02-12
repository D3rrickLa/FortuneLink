package com.laderrco.fortunelink.portfolio_management.application.queries.views;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public record PortfolioSummaryView(PortfolioId id, String name, Money totalValue, Instant lastUpdated) {}