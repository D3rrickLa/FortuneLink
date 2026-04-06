package com.laderrco.fortunelink.portfolio.infrastructure.persistence.projections;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface AccountSummaryProjection {
  UUID getId();

  String getName();

  String getAccountType();

  String getBaseCurrencyCode();

  String getPositionStrategy();

  String getHealthStatus();

  String getLifecycleState();

  BigDecimal getCashBalanceAmount();

  String getCashBalanceCurrency();

  Instant getCreatedDate();

  Instant getLastUpdatedOn();
}
