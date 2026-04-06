package com.laderrco.fortunelink.portfolio.api.web.dto;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio.application.views.AccountView;

public record AccountSummary(
    String id,
    String name,
    String type,
    String currency,
    double cashBalance,
    double totalValue,
    int assetCount, // Derived from PositionView list size
    Instant creationDate) {
  public static AccountSummary fromView(AccountView view) {
    return new AccountSummary(
        view.accountId().toString(),
        view.name(),
        view.type().name(),
        view.baseCurrency().getCode(),
        view.cashBalance().amount().doubleValue(),
        view.totalValue().amount().doubleValue(),
        view.assets().size(),
        view.creationDate());
  }
}