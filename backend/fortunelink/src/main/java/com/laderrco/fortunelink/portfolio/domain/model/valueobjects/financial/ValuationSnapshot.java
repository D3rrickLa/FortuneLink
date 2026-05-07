package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import com.laderrco.fortunelink.portfolio.application.views.ValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ValuationSnapshot(
    UUID id,
    UserId userId,
    Money totalValue,
    Money totalCostBasis,
    Money unrealizedGainLoss,
    BigDecimal gainLossPercent,
    Money totalCashBalance,
    Money totalInvestedValue,
    String displayCurrency,
    boolean hasStaleData,
    Instant snapshotDate) {

  public static ValuationSnapshot fromView(UserId userId, ValuationView view) {
    return new ValuationSnapshot(UUID.randomUUID(), userId, view.totalValue(), 
    view.totalCostBasis(), view.unrealizedGainLoss(), view.gainLossPercent(), 
    view.totalCashBalance(), view.totalInvestedValue(), view.displayCurrency().getCode(), 
    view.hasStaleData(), view.asOfDate());
  }
}