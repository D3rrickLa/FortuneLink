package com.laderrco.fortunelink.portfolio.domain.model.enums;

public enum AccountType {
  // CAD registered accounts
  FHSA(false),
  TFSA(false),
  RRSP(false),
  RESP(false),

  // US registered accounts
  ROTH_IRA(false),
  SOLO_401K(false),

  // Generic / unregistered accounts
  CHEQUING(false),
  SAVINGS(false),
  MARGIN(true),
  TAXABLE_INVESTMENT(true),
  NON_REGISTERED_INVESTMENT(true);

  private final boolean requiresCapitalGainsTracking;

  AccountType(boolean requiresCapitalGainsTracking) {
    this.requiresCapitalGainsTracking = requiresCapitalGainsTracking;
  }

  public boolean requiresCapitalGainsTracking() {
    return requiresCapitalGainsTracking;
  }
}