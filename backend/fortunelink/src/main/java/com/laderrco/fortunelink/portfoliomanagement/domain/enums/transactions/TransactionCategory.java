package com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions;

// coarse-grained, stable partitioning, used for reporting, DB partitioning, routing
public enum TransactionCategory {
    TRADE,
    CORPORATE_ACTION,
    CASH,
    INCOME,
    EXPENSE,
    REVERSAL,
    OTHER
}
