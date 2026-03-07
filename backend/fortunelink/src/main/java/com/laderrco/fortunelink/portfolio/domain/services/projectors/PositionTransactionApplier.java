package com.laderrco.fortunelink.portfolio.domain.services.projectors;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.ApplyResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;

public final class PositionTransactionApplier {

	private PositionTransactionApplier() {
	}

	public static ApplyResult<? extends Position> apply(Position position, Transaction tx) {
		return switch (tx.transactionType()) {
			case BUY -> position.buy(
					tx.execution().quantity(),
					tx.cashDelta().abs(),
					tx.occurredAt().timestamp());
			case SELL -> position.sell(
					tx.execution().quantity(),
					tx.cashDelta(),
					tx.occurredAt().timestamp());
			case SPLIT -> position.split(tx.split().ratio());
			case DIVIDEND_REINVEST -> position.buy(
					tx.execution().quantity(),
					tx.execution().grossValue(),
					tx.occurredAt().timestamp());
			case RETURN_OF_CAPITAL -> position.applyReturnOfCapital(
					tx.execution().pricePerUnit(),
					tx.execution().quantity());
			default -> new ApplyResult.NoChange<>(position); // no-op, position unchanged
		};
	}
}
