package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;

// meant for 'assets' where we can AVG cost, ACB, etc.
// cash events will need their own 'positon.java'
public sealed interface Position permits AcbPosition, FifoPosition {
    AssetSymbol symbol();
    AssetType type();
    Currency accountCurrency();

    Quantity getTotalQuantity();
    Money getTotalCostBasis();
    Money calculateCurrentValue(Money currentPrice);    
}