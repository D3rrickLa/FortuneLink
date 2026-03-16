package com.laderrco.fortunelink.portfolio.domain.services.projectors;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.ApplyResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.util.Comparator;
import java.util.List;

public final class AcbPositionProjector implements Projector<AcbPosition, Transaction> {
  private final AssetSymbol symbol;
  private final AssetType type;
  private final Currency accountCurrency;

  public AcbPositionProjector(AssetSymbol symbol, AssetType type, Currency accountCurrency) {
    this.symbol = symbol;
    this.type = type;
    this.accountCurrency = accountCurrency;
  }

  @Override
  public AcbPosition project(List<Transaction> transactions) {
    AcbPosition current = AcbPosition.empty(symbol, type, accountCurrency);

    List<Transaction> sorted = transactions.stream()
        .sorted(Comparator.comparing(tx -> tx.occurredAt().timestamp())).toList();

    for (Transaction tx : sorted) {
      ApplyResult<? extends Position> result = TransactionApplier.apply(current, tx);
      Position next = result.newPosition();

      // Safeguard against future changes to the Position type hierarchy.
      // Fail loudly if somehow a non-ACB position comes back.
      // This is intentionally unreachable with current implementation.
      if (!(next instanceof AcbPosition acb)) {
        throw new IllegalStateException(
            "AcbPositionProjector received non-AcbPosition result for tx type: "
                + tx.transactionType());
      }
      current = acb;
    }

    return current;
  }
}
