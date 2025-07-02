package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums;

public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    INTEREST,
    DIVIDEND,
    OTHER,

    BUY,
    SELL,

    PAYMENT,
    EXPENSE,
    FEE,

    VOID_BUY,
    VOID_SELL,
    VOID_WITHDRAWAL,

    TRANSFER_IN,
    TRANSFER_OUT,

    STOCK_SPLIT,
    REVERSE_STOCK_SPLIT,
    CORPORATE_ACTION,  // for things other than splits



    CASH_DEPOSIT,
    CASH_WITHDRAWAL,
    ASSET_ADDITION,
    ASSET_SALE,
    REVERSAL,
}
