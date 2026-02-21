package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

// meant for 'assets' where we can AVG cost, ACB, etc.
// cash events will need their own 'positon.java'
public sealed interface Position permits AcbPosition, FifoPosition {
    ApplyResult<? extends Position> buy(Quantity quantity, Money totalCost, Instant at);

    ApplyResult<? extends Position> sell(Quantity quantity, Money proceeds, Instant at);

    ApplyResult<? extends Position> split(double ratio);

    AssetSymbol symbol();

    AssetType type();

    Currency accountCurrency();

    Quantity totalQuantity();

    Money totalCostBasis();

    Money costPerUnit();

    Money currentValue(Money currentPrice);

    default Position copy() {
        return switch (this) {
            case AcbPosition acb -> new AcbPosition(acb.symbol(), acb.type(), acb.accountCurrency(), acb.totalQuantity(), acb.totalCostBasis());
            case FifoPosition fifo -> new FifoPosition(fifo.symbol(), fifo.type(), fifo.accountCurrency(), List.copyOf(fifo.lots()));
        };
    }

    default boolean isEmpty() {
        return totalQuantity().isZero();
    }

    default boolean hasSufficientQuantity(Quantity required) {
        return totalQuantity().compareTo(required) >= 0;
    }

}