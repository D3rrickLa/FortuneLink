package com.laderrco.fortunelink.portfolio.domain.services.projectors;

import java.util.Comparator;
import java.util.List;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.ApplyResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

public abstract class BasePositionProjector<P extends Position> implements Projector<P, Transaction> {
  private final AssetSymbol symbol;
  private final AssetType type;
  private final Currency accountCurrency;
  private final Class<P> positionClass;

  protected BasePositionProjector(AssetSymbol symbol, AssetType type, Currency accountCurrency,
      Class<P> positionClass) {
    this.symbol = symbol;
    this.type = type;
    this.accountCurrency = accountCurrency;
    this.positionClass = positionClass;
  }

  protected abstract P getEmptyPosition(AssetSymbol symbol, AssetType type, Currency accountCurrency);

  @Override
  public P project(List<Transaction> transactions) {
    P current = getEmptyPosition(symbol, type, accountCurrency);

    List<Transaction> sorted = transactions.stream()
        .sorted(Comparator.comparing(Transaction::occurredAt))
        .toList();

    for (Transaction tx : sorted) {
      ApplyResult<? extends Position> result = TransactionApplier.apply(current, tx);
      Position next = result.newPosition();

      if (!positionClass.isInstance(next)) {
        throw new IllegalStateException(
            String.format("%s received unexpected position type: %s",
                this.getClass().getSimpleName(), next.getClass().getSimpleName()));
      }
      current = positionClass.cast(next);
    }
    return current;
  }
}
