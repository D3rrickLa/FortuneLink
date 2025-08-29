package com.laderrco.fortunelink.portfoliomanagement.domain.enums;

// track only cash movement, not the reason.
// 'How teh accoutn balance is affected'
public enum CashflowType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER,

    DIVIDEND,
    INTEREST,
    RENTAL_INCOME,
    OTHER_INCOME,
    
    // Outflows (not initiated by usesr)
    FEE,
    FOREIGN_TAX_WITHHELD,
    OTHER_OUTFLOW, // USED FOR TAXES/FEES THAT REDUCES THE ACCOUNT BUT ARE NOT WITHDRAWALS INITIATED BY THE USER

    TEST,

    UNKNOWN,
    
    ERROR, 
}
