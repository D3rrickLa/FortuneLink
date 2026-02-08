package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.positions;

import java.math.BigDecimal;
import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

public final record AcbPosition(AssetSymbol symbol, AssetType type, Currency accountCurrency,
        Quantity totalQuantity, Money totalCostBasis) implements Position, ClassValidation {

    public static AcbPosition empty(AssetSymbol symbol, AssetType type, Currency currency) {
        return new AcbPosition(symbol, type, currency, Quantity.ZERO, Money.ZERO(currency));
    }

    @Override
    public ApplyResult<AcbPosition> buy(Quantity quantity, Money totalCost, Instant at) {
        AcbPosition updated = new AcbPosition(symbol, type, accountCurrency, quantity, totalCost);
        return new ApplyResult.Purchase<AcbPosition>(updated);
    }

    @Override
    public ApplyResult<? extends Position> sell(Quantity quantity, Money proceeds, Instant at) {
        if (!hasSufficientQuantity(quantity)) {
            throw new IllegalStateException("Insufficient quantity");
        }
        Money acbPerUnit = totalCostBasis.divide(totalQuantity.amount());
        Money costBasisSold = acbPerUnit.multiply(quantity.amount());
        Money realizedGain = proceeds.subtract(costBasisSold);

        AcbPosition updated = new AcbPosition(
                symbol,
                type,
                accountCurrency,
                totalQuantity.subtract(quantity),
                totalCostBasis.subtract(costBasisSold));

        return new ApplyResult.Sale<>(updated, costBasisSold, realizedGain);

    }

    @Override
    public ApplyResult<? extends Position> split(double ratio) {
        if (ratio <= 0) {
            throw new IllegalArgumentException("Split ratio must be positive");
        }

        AcbPosition updated = new AcbPosition(
                symbol,
                type,
                accountCurrency,
                totalQuantity.multiply(BigDecimal.valueOf(ratio)),
                totalCostBasis // unchanged
        );

        return new ApplyResult.NoChange<>(updated);
    }

    @Override
    public Money costPerUnit() {
        return isEmpty() ? Money.ZERO(accountCurrency) : totalCostBasis.divide(totalQuantity.amount());
    }

    @Override
    public Money currentValue(Money currentPrice) {
        return currentPrice.multiply(totalQuantity.amount());

    }

    public Money calculateUnrealizedGain(Money currentPrice) {
        return currentValue(currentPrice).subtract(totalCostBasis);
    }
}
