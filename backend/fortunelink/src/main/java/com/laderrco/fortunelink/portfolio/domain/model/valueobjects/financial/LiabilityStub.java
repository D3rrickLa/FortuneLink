package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

/**
 * TECH DEBT , Loan Management Bounded Context not implemented in MVP.
 * <p>
 * When implemented, this context will introduce: - Liability aggregate (mortgage, student loan,
 * credit card debt) - LiabilityPayment transaction type - Integration point into
 * NetWorthView.totalLiabilities
 * <p>
 * NetWorthView.includesLiabilities is intentionally false until this ships. Do not remove this file
 * until the Loan Management context is complete.
 * <p>
 * Tracked: https://github.com/D3rrickLa/FortuneLink/issues/157
 */
public final class LiabilityStub {
  private LiabilityStub() {
  }
}