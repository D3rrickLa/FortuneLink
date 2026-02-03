package com.laderrco.fortunelink.portfolio_management.domain.model.enums;

public enum AccountType {
    // CAD SPECIFIC
    HSFA,
    TFSA,
    RRSP,
    RESP,

    // USD SPECIFIC
    ROTH_IRA,
    SOLO_401K,

    // GENERIC
    CHEQUING, 
    SAVINGS,
    MARGIN, 
    REGISTERED_INVESTMENT,
    NON_REGISTERED_INVESTMENT,
}