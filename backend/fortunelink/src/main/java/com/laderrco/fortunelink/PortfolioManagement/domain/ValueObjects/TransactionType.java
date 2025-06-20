package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

public enum TransactionType {
    BUY, // -- stocks and trading
    VOID_BUY,
    DIVIDEND,
    SELL,
    VOID_SELL,
    DEPOSIT, // -- cash and regular
    WITHDRAWAL,
    INTEREST_INCOME,
    LOAN_PAYMENT,
    EXPENSE, // -- other
    FEE,
    TRANSFER_IN,
    TRANSFER_OUT
}
