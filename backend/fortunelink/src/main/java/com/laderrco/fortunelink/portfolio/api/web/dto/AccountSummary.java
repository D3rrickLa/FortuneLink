package com.laderrco.fortunelink.portfolio.api.web.dto;

import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import java.time.Instant;

public record AccountSummary(
    String id,
    String name,
    String type,
    String status,
    String currency,
    double cashBalance,
    double totalValue,
    int assetCount,
    Instant creationDate,
    boolean hasCashImbalance,
    int excludedTransactionCount) {

  public static AccountSummary fromView(AccountView view) {
    return new AccountSummary(view.accountId().toString(), view.name(), view.type().name(),
        view.status().name(), view.baseCurrency().getCode(),
        view.cashBalance().amount().doubleValue(), view.totalValue().amount().doubleValue(),
        view.assets().size(), view.creationDate(), view.hasCashImbalance(),
        view.excludedTransactionCount());
  }
}