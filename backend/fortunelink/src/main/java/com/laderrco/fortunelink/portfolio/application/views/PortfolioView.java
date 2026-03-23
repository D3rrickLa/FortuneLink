package com.laderrco.fortunelink.portfolio.application.views;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Builder
public record PortfolioView(
    PortfolioId portfolioId,
    UserId userId,
    String name,
    String description,
    List<AccountView> accounts,
    Money totalValue,
    boolean hasStaleData,
    Instant createDate,
    Instant lastUpdated) {
}