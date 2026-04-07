package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

import com.laderrco.fortunelink.portfolio.api.web.dto.AccountSummary;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioView;
import java.time.Instant;
import java.util.List;

public record PortfolioResponse(
    String id,
    String name,
    String currency,
    double totalValue,
    boolean hasStaleData,
    List<AccountSummary> accounts,
    Instant creationDate,
    Instant lastUpdated) {

  public static PortfolioResponse fromView(PortfolioView view) {
    return new PortfolioResponse(view.portfolioId().toString(), view.name(),
        view.totalValue().currency().getCode(), view.totalValue().amount().doubleValue(),
        view.hasStaleData(), view.accounts().stream().map(AccountSummary::fromView).toList(),
        view.creationDate(), view.lastUpdated());
  }
}