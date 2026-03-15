package com.laderrco.fortunelink.portfolio.domain.model.enums;

// @formatter:off
public enum TransactionType {
  BUY(true, true, true, CashImpact.OUT),
  SELL(true, true, true, CashImpact.IN),
  
  DEPOSIT(false,false, false, CashImpact.IN),
  WITHDRAWAL(false, false, false, CashImpact.OUT), 
  INTEREST(false, true,false, CashImpact.IN), 
  FEE(false, false, false, CashImpact.OUT), 
  
  DIVIDEND(false, true, false,CashImpact.IN), 
  DIVIDEND_REINVEST(true, true, true, CashImpact.NONE), 
  SPLIT(true, false, true, CashImpact.NONE), 

  RETURN_OF_CAPITAL(true, false, true, CashImpact.IN),
  // Intentionally unimplemented stub - not used in MVP (Bug 8 / tech debt).
  // CashImpact.NONE is deliberately wrong; this whole entry needs a design
  // removed it so it won't be called
  // REINVESTED_CAPITAL_GAIN(false, true, false, CashImpact.NONE),
  
  // Bug 6 - transfer in/out are orphaned, exists in Enum only
  TRANSFER_IN(false, false, false, CashImpact.IN), 
  TRANSFER_OUT(false, false, false, CashImpact.OUT),
  
  OTHER(false, false, false, CashImpact.NONE);

  private final boolean affectsHoldings;
  private final boolean taxable;
  private final boolean requiresExecution;
  private final CashImpact cashImpact;

  TransactionType(boolean affectsHoldings, boolean taxable, boolean requiresExecution,
      CashImpact cashImpact) {
    this.affectsHoldings = affectsHoldings;
    this.taxable = taxable;
    this.requiresExecution = requiresExecution;
    this.cashImpact = cashImpact;
  }

  public CashImpact cashImpact() {
    return cashImpact;
  }

  public boolean affectsHoldings() {
    return affectsHoldings;
  }

  public boolean isTaxable() {
    return taxable;
  }

  public boolean requiresExecution() {
    return requiresExecution;
  }

  public boolean requiresSplitDetails() {
    return this.equals(SPLIT);
  }
}
// @formatter:on