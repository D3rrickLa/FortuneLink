package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable record representing the realized gain or loss from closing a
 * position.
 * <p>
 * Stored directly on the account to enable efficient reporting of capital gains
 * history without
 * replaying the full transaction ledger.
 * <p>
 * <b>Note:</b> {@code realizedGainLoss} is signed: a positive value represents
 * a
 * capital gain, while a negative value represents a capital loss.
 */
public record RealizedGainRecord(
    UUID id, AssetSymbol symbol, Money realizedGainLoss, Money costBasisSold, Instant occurredAt) {
  public RealizedGainRecord {
    notNull(symbol, "symbol");
    notNull(realizedGainLoss, "realizedGainLoss");
    notNull(costBasisSold, "costBasisSold");
    notNull(occurredAt, "occurredAt");
  }

  public static RealizedGainRecord of(AssetSymbol symbol, Money gain, Money cost, Instant at) {
    return new RealizedGainRecord(UUID.randomUUID(), symbol, gain, cost, at);
  }

  public boolean isGain() {
    return realizedGainLoss.isPositive();
  }

  public boolean isLoss() {
    return realizedGainLoss.isNegative();
  }
}
