package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses;

import java.math.BigDecimal;
import java.time.Instant;

public record PortfolioSummaryHttpResponse(
    String id,
    String name,
    String description,
    BigDecimal totalValue,
    String currency,
    int numberOfAccounts,
    Instant createdDate,
    Instant lastUpdated
) {}