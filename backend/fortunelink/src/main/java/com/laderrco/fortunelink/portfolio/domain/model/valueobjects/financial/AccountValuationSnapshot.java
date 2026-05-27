package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;

import com.laderrco.fortunelink.portfolio.application.views.ValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;

public record AccountValuationSnapshot(
    AccountId accountId,
    LocalDate snapshotDate,
    Money totalValue,
    Money totalCostBasis,
    Money unrealizedGainLoss,
    PercentageChange gainLossPercent,
    Money cashBalance,
    Money investedValue,
    boolean hasStaleData) {

  public AccountValuationSnapshot {
    Objects.requireNonNull(accountId, "Account ID cannot be null");
    Objects.requireNonNull(snapshotDate, "Snapshot date cannot be null");
    Objects.requireNonNull(totalValue, "Total value cannot be null");
    Objects.requireNonNull(cashBalance, "Cash balance cannot be null");
    Objects.requireNonNull(investedValue, "Invested value cannot be null");

    // Total Value MUST equal Cash + Invested Value
    // (Assuming Money has an .add() and .equals() method)
    if (!totalValue.equals(cashBalance.add(investedValue))) {
      throw new IllegalArgumentException("Total value must equal cash balance plus invested value");
    }
  }

  public static AccountValuationSnapshot create(AccountId accountId, Money cashBalance,
      Money investedValue, Money totalCostBasis) {

    Money totalValue = cashBalance.add(investedValue);
    Money unrealizedGL = totalValue.subtract(totalCostBasis);
    PercentageChange glPercent = PercentageChange.calculate(totalValue, totalCostBasis);

    return new AccountValuationSnapshot(
        accountId,
        LocalDate.now(),
        totalValue,
        totalCostBasis,
        unrealizedGL,
        glPercent,
        cashBalance,
        investedValue,
        false // initially fresh data
    );
  }

  public static AccountValuationSnapshot fromView(AccountId accountId, ValuationView view) {
    return new AccountValuationSnapshot(
        accountId,
        LocalDate.ofInstant(view.asOfDate(), ZoneOffset.UTC),
        view.totalValue(), // Money totalValue - construct appropriately when Money type/methods are
                           // available
        view.totalCostBasis(), // Money totalCostBasis
        view.unrealizedGainLoss(), // Money unrealizedGainLoss
        new PercentageChange(view.gainLossPercent()), // BigDecimal gainLossPercent
        view.totalCashBalance(), // Money totalCashBalance
        view.totalInvestedValue(), // Money totalInvestedValue
        view.hasStaleData());
  }

  public boolean isStale() {
    return this.hasStaleData;
  }
}