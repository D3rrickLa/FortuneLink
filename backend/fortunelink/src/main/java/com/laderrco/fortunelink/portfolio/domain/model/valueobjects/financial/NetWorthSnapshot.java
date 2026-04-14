package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Instant;
import java.util.UUID;

public record NetWorthSnapshot(
    UUID id,
    UserId userId,
    Money totalAssets,
    Money totalLiabilities,
    Money netWorth,
    Currency displayCurrency,
    boolean hasStaleData,
    Instant snapshotDate) {

  public static NetWorthSnapshot create(UserId userId, Money totalAssets, Money totalLiabilities,
      Currency displayCurrency, boolean hasStaleData) {
    Money netWorth = totalAssets.subtract(totalLiabilities);
    return new NetWorthSnapshot(UUID.randomUUID(), userId, totalAssets, totalLiabilities, netWorth,
        displayCurrency, hasStaleData, Instant.now());
  }
}