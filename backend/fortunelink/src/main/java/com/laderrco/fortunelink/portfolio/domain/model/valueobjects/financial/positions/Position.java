package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Ratio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.services.projectors.TransactionApplier;
import java.time.Instant;
import java.util.List;

/**
 * Internal domain interface — not part of the public API.
 * <p>
 * Position mutations are intentionally public to satisfy Java's sealed interface constraints, but
 * ALL callers MUST go through {@link TransactionApplier#apply(Position, Transaction)}.
 * <p>
 * Direct calls to buy(), sell(), split(), applyReturnOfCapital() outside of
 * {@link TransactionApplier} are a domain violation. Enforce this in code review.
 */
public sealed interface Position permits AcbPosition, FifoPosition {
  ApplyResult<? extends Position> buy(Quantity quantity, Money totalCost, Instant at);

  ApplyResult<? extends Position> sell(Quantity quantity, Money proceeds, Instant at);

  ApplyResult<? extends Position> split(Ratio ratio);

  ApplyResult<? extends Position> applyReturnOfCapital(Price distributionPerUnit,
      Quantity heldQuantity);

  AssetSymbol symbol();

  AssetType type();

  Currency accountCurrency();

  Quantity totalQuantity();

  Money totalCostBasis();

  Money costPerUnit();

  Money currentValue(Price currentPrice);

  Instant lastModifiedAt();

  default Position copy() {
    return switch (this) {
      case AcbPosition acb ->
          new AcbPosition(acb.symbol(), acb.type(), acb.accountCurrency(), acb.totalQuantity(),
              acb.totalCostBasis(), acb.firstAcquiredAt(), acb.lastModifiedAt());
      case FifoPosition fifo -> new FifoPosition(fifo.symbol(), fifo.type(), fifo.accountCurrency(),
          List.copyOf(fifo.lots()), fifo.lastModifiedAt());
    };
  }

  default boolean isEmpty() {
    return totalQuantity().isZero();
  }
}
