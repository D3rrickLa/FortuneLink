package com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;

// Trade Transaction
public enum TradeType implements TransactionType {
    BUY,
    SELL,
    SHORT_SELL,
    COVER_SHORT,
    OPTIONS_EXERCISED,
    OPTIONS_ASSIGNED,
    OPTIONS_EXPIRED,
    CRYPTO_SWAP,
    
    OTHER_TRADE_TYPE_REVERSAL,
    TRADE_REVERSAL, 
    BUY_REVERSAL, 
    SELL_REVERSAL;

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public TransactionCategory getCategory() {
        return TransactionCategory.TRADE;
    }

    @Override
    public boolean isReversal() {
        return name().contains("REVERSAL");
    }
}
