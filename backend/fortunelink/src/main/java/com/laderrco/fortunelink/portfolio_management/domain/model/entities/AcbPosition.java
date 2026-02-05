package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

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

    public ApplyResult apply(Transaction tx) {

        // No execution = no position impact (dividends, deposits, etc.)
        if (tx.execution() == null || tx.execution().quantity().isZero()) {
            return new ApplyResult.NoChange(this);
        }

        Quantity delta = tx.execution().quantity();

        // BUY
        if (delta.isPositive()) {
            Money purchaseCost = tx.cashDelta().abs(); // fees included

            AcbPosition updated = new AcbPosition(
                    symbol,
                    type,
                    accountCurrency,
                    quantity.add(delta),
                    totalAcb.add(purchaseCost));

            return new ApplyResult.Purchase(updated);
        }

        // SELL
        Quantity sellQty = delta.abs();

        if (sellQty.compareTo(quantity) > 0) {
            throw new IllegalStateException(
                    "Cannot sell more than held quantity for " + symbol);
        }

        Money acbPerUnit = totalAcb.divide(quantity.amount());
        Money costOfSharesSold = acbPerUnit.multiply(sellQty.amount());

        Money proceeds = tx.cashDelta(); // already net, positive
        Money realizedGainLoss = proceeds.subtract(costOfSharesSold);

        AcbPosition updated = new AcbPosition(
                symbol,
                type,
                accountCurrency,
                quantity.subtract(sellQty),
                totalAcb.subtract(costOfSharesSold));

        return new ApplyResult.Sale(
                updated,
                costOfSharesSold,
                realizedGainLoss);
    }

    @Override
    public Quantity getTotalQuantity() {
        return quantity;
    }

    @Override
    public Money getTotalCostBasis() {
        return totalAcb.divide(quantity.amount());
    }

    @Override
    public Money calculateCurrentValue(Money currentPrice) {
        return currentPrice.multiply(quantity.amount());
    }

    public Money calculateUnrealizedGain(Money currentPrice) {
        return calculateCurrentValue(currentPrice).subtract(totalAcb);
    }

    public sealed interface ApplyResult {

        AcbPosition newPosition();

        record Purchase(AcbPosition newPosition) implements ApplyResult {
        }

        record Sale(AcbPosition newPosition, Money costBasisSold, Money realizedGainLoss) implements ApplyResult {
        }

        record NoChange(AcbPosition newPosition) implements ApplyResult {
        }
    }
}
