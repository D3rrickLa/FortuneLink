package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

import com.laderrco.fortunelink.portfolio.application.views.NetWorthView;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current valuation of the entire portfolio")
public record NetWorthResponse(double totalNetWorth, String currency) {
  public static NetWorthResponse fromView(NetWorthView view) {
    return new NetWorthResponse(view.netWorth().amount().doubleValue(),
        view.netWorth().currency().getCode());
  }
}