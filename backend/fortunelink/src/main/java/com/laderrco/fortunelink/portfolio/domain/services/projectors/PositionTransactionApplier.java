package com.laderrco.fortunelink.portfolio.domain.services.projectors;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.ApplyResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;

final class PositionTransactionApplier {

    private PositionTransactionApplier() {
    }

    static <P extends Position> P apply(P position, Transaction tx) {

        return switch (tx.transactionType()) {
            case BUY -> applyBuy(position, tx);
            case SELL -> applySell(position, tx);
            case SPLIT -> applySplit(position, tx);
            case DIVIDEND_REINVEST -> applyDividendReinvest(position, tx);

            default -> position; // dividends, interest, fees, etc.
        };
    }

    private static <P extends Position> P applyBuy(P position, Transaction tx) {
        Money totalCost = tx.cashDelta().abs();

        ApplyResult<? extends Position> r = position.buy(
                tx.execution().quantity(),
                totalCost,
                tx.occurredAt().timestamp());
        return cast(r);
    }

    private static <P extends Position> P applySell(P position, Transaction tx) {
        // cashDelta on SELL is net proceeds (positive); correct for realized gain calc
        ApplyResult<? extends Position> r = position.sell(
                tx.execution().quantity(),
                tx.cashDelta(), // proceeds (positive)
                tx.occurredAt().timestamp());
        return cast(r);
    }

    private static <P extends Position> P applySplit(P position, Transaction tx) {
        ApplyResult<? extends Position> r = position.split(tx.split().ratio());
        return cast(r);
    }

    private static <P extends Position> P applyDividendReinvest(P position, Transaction tx) {
        // No cash movement; position increases at grossValue cost
        Money totalCost = tx.execution().grossValue();
        ApplyResult<? extends Position> r = position.buy(
                tx.execution().quantity(),
                totalCost,
                tx.occurredAt().timestamp());
        return cast(r);
    }

    @SuppressWarnings("unchecked")
    private static <P extends Position> P cast(
            ApplyResult<? extends Position> r) {
        return (P) r.newPosition();
    }

}
