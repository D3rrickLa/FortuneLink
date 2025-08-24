package com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;

public enum TradeType implements TransactionType {
    BUY,
    SELL,
    SHORT_SELL,
    COVER_SHORT,
    OPTIONS_EXERCISED,
    OPTIONS_ASSIGNED,
    OPTIONS_EXPIRED,
    CRYPTO_SWAP;

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public TransactionCategory getCategory() {
        return TransactionCategory.TRADE;
    }
}
