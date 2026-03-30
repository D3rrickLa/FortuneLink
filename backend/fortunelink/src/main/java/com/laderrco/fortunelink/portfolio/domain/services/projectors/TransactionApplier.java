package com.laderrco.fortunelink.portfolio.domain.services.projectors;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.ApplyResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;

public final class TransactionApplier {
  private TransactionApplier() {
    // Utility class
  }

  /**
   * Routes a transaction to the correct position update logic. This is the single source of truth
   * for applying transactions to positions.
   */
  public static ApplyResult<? extends Position> apply(Position position, Transaction tx) {
    return switch (tx.transactionType()) {
      case BUY -> applyBuy(position, tx);
      case SELL -> applySell(position, tx);
      case SPLIT -> position.split(tx.split());
      case DIVIDEND_REINVEST -> applyDrip(position, tx);
      case RETURN_OF_CAPITAL -> applyReturnOfCapital(position, tx);
      default -> new ApplyResult.NoChange<>(position);
    };
  }

  private static ApplyResult<? extends Position> applyBuy(Position p, Transaction tx) {
    return p.buy(tx.execution().quantity(), tx.cashDelta().abs(), tx.occurredAt());
  }

  /**
   * Records a sale of assets from this position.
   *
   * @param quantity The amount of the asset to sell.
   * @param proceeds The NET proceeds received (Gross - Fees). MUST be a positive absolute value.
   * @param at       The timestamp of the sale.
   */
  private static ApplyResult<? extends Position> applySell(Position p, Transaction tx) {
    return p.sell(tx.execution().quantity(), tx.cashDelta(), tx.occurredAt());
  }

  private static ApplyResult<? extends Position> applyDrip(Position p, Transaction tx) {
    return p.buy(tx.execution().quantity(), tx.execution().grossValue(), tx.occurredAt());
  }

  private static ApplyResult<? extends Position> applyReturnOfCapital(Position p, Transaction tx) {
    // CRITICAL: ACB is reduced by the GROSS distribution amount (CRA IT-434R).
    // Broker fees on ROC are deductible investment expenses and must NOT
    // modify the ACB reduction. We intentionally use price/quantity (gross) here.
    return p.applyReturnOfCapital(tx.execution().pricePerUnit(), tx.execution().quantity());
  }
}