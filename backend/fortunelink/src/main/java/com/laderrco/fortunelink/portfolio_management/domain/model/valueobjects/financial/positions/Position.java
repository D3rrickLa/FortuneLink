package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.positions;

import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;

// meant for 'assets' where we can AVG cost, ACB, etc.
// cash events will need their own 'positon.java'
// TODO POSITION NEEDS TOT BE UPDATED TO NOT HAVE TRANSACTION INFOMATION IN IT
public sealed interface Position permits AcbPosition, FifoPosition {
    PositionResult apply(Transaction tx);
    AssetSymbol symbol();
    AssetType type();
    Currency accountCurrency();

    Quantity getTotalQuantity();
    Money getTotalCostBasis();
    Money calculateCurrentValue(Money currentPrice);    
}