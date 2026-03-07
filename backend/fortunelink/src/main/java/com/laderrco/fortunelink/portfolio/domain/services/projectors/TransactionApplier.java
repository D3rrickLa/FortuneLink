package com.laderrco.fortunelink.portfolio.domain.services.projectors;

import org.springframework.stereotype.Component;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.ApplyResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.utils.TradeValueResolver;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class TransactionApplier {
	private final TradeValueResolver resolver;


	public ApplyResult<? extends Position> apply(Position position, Transaction tx) {
		return switch (tx.transactionType()) {
			case BUY -> applyBuy(position, tx);
			case SELL -> applySell(position, tx);
			case SPLIT -> applySplit(position, tx);
			case DIVIDEND_REINVEST -> applyDividendReinvest(position, tx);
			case RETURN_OF_CAPITAL -> applyReturnOfCapital(position, tx);
			default -> new ApplyResult.NoChange<>(position);
		};
	}

	private ApplyResult<? extends Position> applyBuy(Position position, Transaction tx) {
		Money totalCost = resolver.buyerCost(tx);
		return position.buy(tx.execution().quantity(), totalCost, tx.occurredAt().timestamp());
	}

	private ApplyResult<? extends Position> applySell(Position position, Transaction tx) {
		Money proceeds = resolver.sellerProceeds(tx);
		return position.sell(tx.execution().quantity(), proceeds, tx.occurredAt().timestamp());
	}

	private ApplyResult<? extends Position> applySplit(Position position, Transaction tx) {
		return position.split(tx.split().ratio());
	}

	private ApplyResult<? extends Position> applyDividendReinvest(Position position, Transaction tx) {
		// DRIP uses gross value (qty × price), not net-of-fees, because no cash
		// changes hands — there is nothing to subtract fees from.
		Money totalCost = tx.execution().grossValue();
		return position.buy(tx.execution().quantity(), totalCost, tx.occurredAt().timestamp());
	}

	private ApplyResult<? extends Position> applyReturnOfCapital(Position position, Transaction tx) {
		return position.applyReturnOfCapital(tx.execution().pricePerUnit(), tx.execution().quantity());
	}
}
