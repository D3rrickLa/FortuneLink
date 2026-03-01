package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

import java.time.Instant;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

/**
 * An immutable record of realized gain/loss produced when a position is (partially) closed.
 * <p>
 * Two records with the same symbol, amount, and timestamp are considered equal.
 * <p>
 * Stored on Account so that capital gains history is queryable without
 * replaying the full transaction log.
 * <p>
 * realizedGainLoss is signed:
 * positive → capital gain
 * negative → capital loss
 */
public record RealizedGainRecord(AssetSymbol symbol, Money realizedGainLoss, Money costBasisSold, Instant occurredAt) {
    public RealizedGainRecord {
        notNull(symbol, "symbol");
        notNull(realizedGainLoss, "realizedGainLoss");
        notNull(costBasisSold, "costBasisSold");
        notNull(occurredAt, "occurredAt");
    }

    public boolean isGain() {
        return realizedGainLoss.isPositive();
    }

    public boolean isLoss() {
        return realizedGainLoss.isNegative();
    }
}
