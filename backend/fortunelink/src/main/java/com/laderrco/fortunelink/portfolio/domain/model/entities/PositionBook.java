package com.laderrco.fortunelink.portfolio.domain.model.entities;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Encapculsates the position ledger for an Account is private pacakte, only Accoutn should
 * construct or mutate this directly.
 */
final class PositionBook {
  private final Map<AssetSymbol, Position> positions;
  private final Currency currency;
  private final PositionStrategy strategy;

  PositionBook(Currency currency, PositionStrategy strategy) {
    this.currency = currency;
    this.strategy = strategy;
    this.positions = new HashMap<>();
  }

  // Used in JPA reconstitution ONLY
  PositionBook(Map<AssetSymbol, Position> existing, Currency currency, PositionStrategy strategy) {
    this.positions = new HashMap<>(existing);
    this.currency = currency;
    this.strategy = strategy;
  }

  Position ensurePosition(AssetSymbol symbol, AssetType type) {
    return positions.computeIfAbsent(symbol, s -> createEmpty(s, type));
  }

  /**
   * Apply the result from a TransactionApplier. If the pos closes out, it's auto removed. Callers
   * never manipulate the map directly
   *
   * @param symbol
   * @param updated
   */
  void applyResult(AssetSymbol symbol, Position updated) {
    notNull(symbol, "symbol");
    notNull(updated, "updated");

    if (!updated.symbol().equals(symbol)) {
      throw new IllegalArgumentException(
          "Position symbol mismatch: expected " + symbol + ", got " + updated.symbol());
    }

    if (updated.totalQuantity().isZero()) {
      positions.remove(symbol);
    } else {
      positions.put(symbol, updated);
    }
  }

  void clearSymbol(AssetSymbol symbol) {
    positions.remove(symbol);
  }

  void clearAll() {
    positions.clear();
  }

  Optional<Position> get(AssetSymbol symbol) {
    Position p = positions.get(symbol);
    return Optional.ofNullable(p != null ? p.copy() : null);
  }

  boolean has(AssetSymbol symbol) {
    return positions.containsKey(symbol);
  }

  boolean isEmpty() {
    return positions.isEmpty();
  }

  int size() {
    return positions.size();
  }

  Collection<Map.Entry<AssetSymbol, Position>> entries() {
    return positions.entrySet().stream().map(e -> Map.entry(e.getKey(), e.getValue().copy()))
        .toList();
  }

  private Position createEmpty(AssetSymbol symbol, AssetType type) {
    return switch (strategy) {
      case ACB -> AcbPosition.empty(symbol, type, currency);
      case FIFO, LIFO, SPECIFIC_ID ->
          throw new IllegalArgumentException(strategy + " not supported");
    };
  }
}
