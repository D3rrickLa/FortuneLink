package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.positions;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;

// meant for 'assets' where we can AVG cost, ACB, etc.
// cash events will need their own 'positon.java'
// TODO POSITION NEEDS TOT BE UPDATED TO NOT HAVE TRANSACTION INFOMATION IN IT
public sealed interface Position permits AcbPosition, FifoPosition {
    Position buy(Quantity quantity, Money totalCost, Instant at);
    Position sell(Quantity quantity, Instant at);
    Position split(double ratio);

    AssetSymbol symbol();
    AssetType type();
    Currency accountCurrency();
    Quantity totalQuantity();
    Money totalCostBasis();
    Money costPerUnit();
    Money currentValue(Money currentPrice);

    default boolean isEmpty() {
        return totalQuantity().isZero();
    }

    default boolean hasSufficientQuantity(Quantity required) {
        return totalQuantity().compareTo(required) >= 0;
    }
}