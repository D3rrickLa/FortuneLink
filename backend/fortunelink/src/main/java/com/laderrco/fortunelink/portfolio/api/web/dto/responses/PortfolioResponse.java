package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

import com.laderrco.fortunelink.portfolio.application.views.PortfolioView;

public record PortfolioResponse(String id, String name, String currency) {
  public static PortfolioResponse fromView(PortfolioView view) {
    return new PortfolioResponse(view.portfolioId().toString(), view.name(),
        view.totalValue().currency().getCode());
  }
}