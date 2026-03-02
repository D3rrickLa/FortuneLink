package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

public final record AcbPosition(AssetSymbol symbol, AssetType type, Currency accountCurrency,
        Quantity totalQuantity, Money totalCostBasis) implements Position {

    public AcbPosition {
        notNull(symbol, "AssetSymbol");
        notNull(type, "type");
        notNull(accountCurrency, "accountCurrency");
        notNull(totalQuantity, "totalQuantity");
        notNull(totalCostBasis, "totalCostBasis");
    }

    public static AcbPosition empty(AssetSymbol symbol, AssetType type, Currency currency) {
        return new AcbPosition(symbol, type, currency, Quantity.ZERO, Money.ZERO(currency));
    }

    @Override
    public ApplyResult<? extends Position> buy(Quantity quantity, Money totalCost, Instant at) {
        AcbPosition updated = new AcbPosition(
                symbol,
                type,
                accountCurrency,
                totalQuantity.add(quantity), // accumulate quantity
                totalCostBasis.add(totalCost)); // accumulate cost basis

        return new ApplyResult.Purchase<>(updated);
    }

    @Override
    public ApplyResult<? extends Position> sell(Quantity quantity, Money proceeds, Instant at) {
        if (hasInSufficientQuantity(quantity)) {
            throw new IllegalStateException("Insufficient quantity");
        }
//        Money acbPerUnit = totalCostBasis.divide(totalQuantity.amount());
//        Money costBasisSold = acbPerUnit.multiply(quantity.amount());
//        Money realizedGain = proceeds.subtract(costBasisSold);

        // calculate the portion of hte cost basis being removed
        BigDecimal ratio = quantity.amount().divide(totalQuantity.amount(), MathContext.DECIMAL128);
        Money costBasisSold = totalCostBasis.multiply(ratio);
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
