package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums;

public enum FeeType {
    BROKERAGE,       // Fee charged by a broker (traditional assets)
    GAS,             // Network/transaction fee (crypto)
    FOREIGN_EXCHANGE_CONVERSION,   // Fee/spread for currency conversion
    COMMISSION,      // Another term for brokerage/trading fee
    REGULATORY,      // SEC fees, etc.
    ACCOUNT_MAINTENANCE, // For standalone fees, if you reuse the Fee object
    WITHDRAWAL_FEE,
    DEPOSIT_FEE,
    OTHER
}
