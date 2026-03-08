package com.laderrco.fortunelink.portfolio.domain.services.projectors;

import org.springframework.stereotype.Component;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.ApplyResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class TransactionApplier {

	public static ApplyResult<? extends Position> apply(Position position, Transaction tx) {
		// cost paid (stored on tx at record time)
		return switch (tx.transactionType()) {
			case BUY -> position.buy(
					tx.execution().quantity(),
					tx.cashDelta().abs(),
					tx.occurredAt().timestamp());
			// net proceeds (stored on tx at record time)
			case SELL -> position.sell(
					tx.execution().quantity(),
					tx.cashDelta(),
					tx.occurredAt().timestamp());

			case SPLIT -> position.split(tx.split().ratio());
			// DRIP has no cashDelta use gross value
			case DIVIDEND_REINVEST -> position.buy(
					tx.execution().quantity(),
					tx.execution().grossValue(),
					tx.occurredAt().timestamp());

			case RETURN_OF_CAPITAL -> position.applyReturnOfCapital(
					tx.execution().pricePerUnit(),
					tx.execution().quantity());

			default -> new ApplyResult.NoChange<>(position);
		};
	}
}
