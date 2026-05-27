package com.laderrco.fortunelink.portfolio.infrastructure.persistence.mappers;

import java.util.UUID;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.AccountValuationSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.PercentageChange;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.AccountValuationSnapshotJpaEntity;

public class AccountValuationSnapshotDomainMapper {
  public static AccountValuationSnapshot toDomain(AccountValuationSnapshotJpaEntity jpa) {
    Currency currency = Currency.of(jpa.getCurrency());

    return new AccountValuationSnapshot(
      new AccountId(jpa.getAccountId()),
      jpa.getSnapshotDate(),
      new Money(jpa.getTotalValue(), currency),
      new Money(jpa.getTotalCostBasis(), currency),
      new Money(jpa.getUnrealizedGainLoss(), currency),
      new PercentageChange(jpa.getGainLossPercent()),
      new Money(jpa.getCashBalance(), currency),
      new Money(jpa.getInvestedValue(), currency),
      jpa.isHasStaleData());
  }

  public static AccountValuationSnapshotJpaEntity toJpa(UUID id, AccountValuationSnapshot domain) {
    return new AccountValuationSnapshotJpaEntity(
      id,
      domain.accountId().id(),
      domain.snapshotDate(),
      domain.totalValue().amount(),
      domain.totalCostBasis().amount(),
      domain.unrealizedGainLoss().amount(),
      domain.gainLossPercent().change(),
      domain.cashBalance().amount(),
      domain.investedValue().amount(),
      domain.cashBalance().currency().getCode(),
      domain.hasStaleData()
    );
  }
}
