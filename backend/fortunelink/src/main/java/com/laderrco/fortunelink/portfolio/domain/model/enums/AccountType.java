package com.laderrco.fortunelink.portfolio.domain.model.enums;

public enum AccountType {
    // CAD SPECIFIC
    FHSA,
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
    NON_REGISTERED_INVESTMENT;

    public boolean requriesCaptialGainsTracking() {
        // some account don't need to be tracked
        return this == NON_REGISTERED_INVESTMENT;
    }
}