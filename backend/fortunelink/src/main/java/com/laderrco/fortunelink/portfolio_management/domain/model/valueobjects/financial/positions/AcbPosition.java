package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.positions;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

public final record AcbPosition(AssetSymbol symbol, AssetType type, Currency accountCurrency,
        Quantity quantity, Money totalAcb) implements Position, ClassValidation {

    public static AcbPosition empty(AssetSymbol symbol, AssetType type, Currency currency) {
        return new AcbPosition(symbol, type, currency, Quantity.ZERO, Money.ZERO(currency));
    }

    @Override
    public Position buy(Quantity quantity, Money totalCost, Instant at) {
        if(quantity.isZero() || quantity.isNegative()) {
            throw new IllegalArgumentException("Buy quantity must be positive");
        }

        return new AcbPosition(
            symbol,
            type,
            accountCurrency,
            totalQuantity().add(quantity),
            totalCostBasis().add(totalCost);
        );
    }

    @Override
    public Position sell(Quantity quantity, Instant at) {
        return null;
    }

    @Override
    public Position split(double ratio) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'split'");
    }

    @Override
    public Quantity totalQuantity() {
        return quantity;
    }

    @Override
    public Money totalCostBasis() {
        return totalAcb.divide(quantity.amount());
    }

    @Override
    public Money costPerUnit() {
        return null;
    }

    @Override
    public Money currentValue(Money currentPrice) {
        return currentPrice.multiply(quantity.amount());
    }

    public Money calculateUnrealizedGain(Money currentPrice) {
        return currentValue(currentPrice).subtract(totalAcb);
    }

    public sealed interface ApplyResult<P extends Position> extends PositionResult
            permits ApplyResult.Purchase, ApplyResult.Sale, ApplyResult.NoChange {

        P newPosition();

        @Override
        default Position getUpdatedPosition() {
            return newPosition();
        }

        record Purchase<P extends Position>(P newPosition) implements ApplyResult<P> {
        }

        record Sale<P extends Position>(P newPosition, Money costBasisSold, Money realizedGainLoss) implements ApplyResult<P> {
        }

        record NoChange<P extends Position>(P newPosition) implements ApplyResult<P> {
        }
    }
}
