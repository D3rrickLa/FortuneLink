package com.laderrco.fortunelink.portfoliomanagement.domain.enums;

// track only cash movement, not the reason.
// this has taken over the role of IncomeType from v3
public enum CashflowType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER,

    DIVIDEND,
    INTEREST,
    RENTAL_INCOME,
    OTHER_INCOME,
    
    // Outflows (not initiated by usesr)
    OTHER_OUTFLOW, // USED FOR TAXES/FEES THAT REDUCES THE ACCOUNT BUT ARE NOT WITHDRAWALS INITIATED BY THE USER



    UNKNOWN
}
