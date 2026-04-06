package com.laderrco.fortunelink.portfolio.infrastructure.persistence.projections;

import java.util.UUID;

/**
 * Maps a position row to (accountId, symbol) pair.
 * Used for batch-fetching all open symbols across multiple accounts
 * without loading the full Account aggregate.
 */
public interface AccountSymbolProjection {
  UUID getAccountId();

  String getSymbol();
}
