package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects;

import java.math.BigDecimal;
import java.util.List;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.MonetaryAmount;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.AssetHoldingId;

public class TradeTransactionDetails extends TransactionDetails {
    private final AssetHoldingId assetHoldingId;
    private final AssetIdentifier assetIdentifier;
    
    private final BigDecimal quantity;
    private final MonetaryAmount pricePerUnit;

    // sell specific fields
    private final MonetaryAmount realizedGainLoss;
    private final MonetaryAmount acbPerUnitAtSale;



    public MonetaryAmount getTotalFees() {
        return getFees().stream()
            .map(Fee::amount)
            .reduce(MonetaryAmount.ZERO, MonetaryAmount::add);
    }
    
}
