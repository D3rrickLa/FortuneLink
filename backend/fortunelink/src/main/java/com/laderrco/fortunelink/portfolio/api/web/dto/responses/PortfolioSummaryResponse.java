package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

import com.laderrco.fortunelink.portfolio.application.views.PortfolioSummaryView;

public record PortfolioSummaryResponse(
    String id, String name, String currency, double totalValue // Aggregated value for the list view
) {
  public static PortfolioSummaryResponse fromView(PortfolioSummaryView view) {
    return new PortfolioSummaryResponse(view.id().toString(), view.name(),
        view.totalValue().currency().getCode(), view.totalValue().amount().doubleValue());
  }
}