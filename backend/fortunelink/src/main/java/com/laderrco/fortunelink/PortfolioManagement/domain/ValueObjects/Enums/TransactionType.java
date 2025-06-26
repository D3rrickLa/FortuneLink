package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums;

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
    CORPORATE_ACTION  // for things like splits or reverse splits, or other things
}
