package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public record NetWorthSnapshot(UserId userId, Money totalAssets, Money totalLiabilities, Money netWorth,
    Currency displayCurrency, boolean hasStaleData, Instant snapshotDate) {
  public static NetWorthSnapshot create(UserId userId, Money totalAssets, Money totalLiabilities, Currency display,
      boolean hasStale) {
    Money netWorth = totalAssets.subtract(totalLiabilities);
    return new NetWorthSnapshot(userId, totalAssets, totalLiabilities, netWorth, display, hasStale, Instant.now());
  }
}
