package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums;

public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    INTEREST,
    DIVIDEND,
    OTHER,

    BUY,
    SELL,

    LIABILITY_INCURRENCE,
    INTEREST_INCOME,
    INTEREST_EXPENSE,

    PAYMENT,
    EXPENSE,
    FEE,

    REVERSAL_BUY,
    REVERSAL_SELL,
    REVERSAL_WITHDRAWAL,
    REVERSAL_DEPOSIT,
    REVERSAL, 

    SIMPLE

    // TRANSFER_IN,
    // TRANSFER_OUT,

    // STOCK_SPLIT,
    // REVERSE_STOCK_SPLIT,
    // CORPORATE_ACTION,  // for things other than splits 
}
