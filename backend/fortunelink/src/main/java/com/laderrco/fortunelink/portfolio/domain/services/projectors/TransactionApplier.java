package com.laderrco.fortunelink.portfolio.domain.services.projectors;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.ApplyResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.utils.TradeValueResolver;

public final class TransactionApplier {
	private final TradeValueResolver resolver;

	public TransactionApplier(TradeValueResolver resolver) {
		this.resolver = resolver;
	}

	<P extends Position> P apply(P position, Transaction tx) {
		return switch (tx.transactionType()) {
			case BUY -> applyBuy(position, tx);
			case SELL -> applySell(position, tx);
			case SPLIT -> applySplit(position, tx);
			case DIVIDEND_REINVEST -> applyDividendReinvest(position, tx);
			case RETURN_OF_CAPITAL -> applyReturnOfCapital(position, tx);
			default -> position;
		};
	}

	private <P extends Position> P applyBuy(P position, Transaction tx) {

		Money totalCost = resolver.buyerCost(tx);

		ApplyResult<? extends Position> r =
				position.buy(tx.execution().quantity(), totalCost, tx.occurredAt().timestamp());

		return cast(r);
	}

	private <P extends Position> P applySell(P position, Transaction tx) {

		Money proceeds = resolver.sellerProceeds(tx);

		ApplyResult<? extends Position> r =
				position.sell(tx.execution().quantity(), proceeds, tx.occurredAt().timestamp());

		return cast(r);
	}

	private <P extends Position> P applySplit(P position, Transaction tx) {

		ApplyResult<? extends Position> r = position.split(tx.split().ratio());

		return cast(r);
	}

	private <P extends Position> P applyDividendReinvest(P position, Transaction tx) {

		Money totalCost = tx.execution().grossValue();

		ApplyResult<? extends Position> r =
				position.buy(tx.execution().quantity(), totalCost, tx.occurredAt().timestamp());

		return cast(r);
	}

	private <P extends Position> P applyReturnOfCapital(P position, Transaction tx) {

		ApplyResult<? extends Position> r =
				position.applyReturnOfCapital(tx.execution().pricePerUnit(), tx.execution().quantity());

		return cast(r);
	}

	@SuppressWarnings("unchecked")
	private static <P extends Position> P cast(ApplyResult<? extends Position> r) {
		return (P) r.newPosition();
	}
}
