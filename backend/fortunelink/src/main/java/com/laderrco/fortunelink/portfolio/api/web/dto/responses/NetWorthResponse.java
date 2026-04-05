package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

import com.laderrco.fortunelink.portfolio.application.views.NetWorthView;

public record NetWorthResponse(
    double totalNetWorth, String currency) {
  public static NetWorthResponse fromView(NetWorthView view) {
    return new NetWorthResponse(view.netWorth().amount().doubleValue(),
        view.netWorth().currency().getCode());
  }
}