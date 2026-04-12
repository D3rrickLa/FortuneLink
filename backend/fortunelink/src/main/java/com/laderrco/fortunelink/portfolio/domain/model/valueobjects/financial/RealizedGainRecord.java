package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

import java.nio.charset.StandardCharsets;
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
 * <b>ID contract:</b>
 * - New gains: use {@link #of} , generates a stable UUID once, persisted
 * immediately. - DB
 * hydration: use {@link #reconstitute} , passes the existing row UUID through,
 * ensuring the mapper
 * can detect which gains already exist and skip re-inserting them.
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
   * Generates a stable UUID based on the content of the gain.
   * This ensures that replaying the same transaction results in the same ID.
   */
  public static RealizedGainRecord of(AccountId accountId, AssetSymbol symbol, Money gain, Money cost, Instant at) {
    String source = String.format("%s-%s-%s-%s-%s",
        accountId.id(),
        symbol.symbol(),
        at.toEpochMilli(),
        gain.amount().toPlainString(),
        cost.amount().toPlainString());

    UUID deterministicId = UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));

    return new RealizedGainRecord(deterministicId, symbol, gain, cost, at);
  }

  /**
   * Reconstitutes a realized gain record from a persisted row. Passes the
   * existing DB row UUID
   * through so the mapper can perform ID-based diffing and avoid DELETE +
   * re-INSERT on every
   * portfolio save. Call this only from the infrastructure mapper.
   */
  public static RealizedGainRecord reconstitute(UUID id, AssetSymbol symbol, Money gain, Money cost,
      Instant at) {
    return new RealizedGainRecord(id, symbol, gain, cost, at);
  }

  public boolean isGain() {
    return realizedGainLoss.isPositive();
  }

  public boolean isLoss() {
    return realizedGainLoss.isNegative();
  }
}
