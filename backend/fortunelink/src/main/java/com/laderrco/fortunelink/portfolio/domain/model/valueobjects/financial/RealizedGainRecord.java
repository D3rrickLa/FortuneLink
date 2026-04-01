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
 * history without replaying the full transaction ledger.
 * <p>
 * <b>ID contract:</b>
 * - New gains: use {@link #of} — generates a stable UUID once, persisted
 * immediately.
 * - DB hydration: use {@link #reconstitute} — passes the existing row UUID
 * through,
 * ensuring the mapper can detect which gains already exist and skip
 * re-inserting them.
 * <p>
 * <b>Note:</b> {@code realizedGainLoss} is signed; positive = capital gain,
 * negative = capital loss.
 */
public record RealizedGainRecord(
    UUID id, AssetSymbol symbol, Money realizedGainLoss, Money costBasisSold, Instant occurredAt) {
  public RealizedGainRecord {
    notNull(id, "id");
    notNull(symbol, "symbol");
    notNull(realizedGainLoss, "realizedGainLoss");
    notNull(costBasisSold, "costBasisSold");
    notNull(occurredAt, "occurredAt");
  }

  /**
   * Creates a new realized gain record at the moment of a sale or ROC excess
   * event.
   * Generates a stable UUID that will be used as the primary key when persisted.
   * Call this only from domain logic (Account.recordRealizedGain).
   */
  public static RealizedGainRecord of(AssetSymbol symbol, Money gain, Money cost, Instant at) {
    return new RealizedGainRecord(UUID.randomUUID(), symbol, gain, cost, at);
  }

  /**
   * Reconstitutes a realized gain record from a persisted row.
   * Passes the existing DB row UUID through so the mapper can perform
   * ID-based diffing and avoid DELETE + re-INSERT on every portfolio save.
   * Call this only from the infrastructure mapper.
   */
  public static RealizedGainRecord reconstitute(
      UUID id, AssetSymbol symbol, Money gain, Money cost, Instant at) {
    return new RealizedGainRecord(id, symbol, gain, cost, at);
  }

  public boolean isGain() {
    return realizedGainLoss.isPositive();
  }

  public boolean isLoss() {
    return realizedGainLoss.isNegative();
  }
}
