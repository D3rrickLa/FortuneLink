package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

public enum TransactionType {
    BUY, // -- stocks and trading
    VOID_BUY,
    DIVIDEND,
    SELL,
    DEPOSIT, // -- cash and regular
    WITHDRAWAL,
    INTEREST_INCOME,
    EXPENSE, // -- other
    FEE
}
