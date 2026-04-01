package com.laderrco.fortunelink.portfolio.infrastructure.persistence.valueobjects;

import java.math.BigDecimal;
import java.util.UUID;

public interface FeeAggregationResult {
  UUID getAccountId();

  String getSymbol();

  BigDecimal getTotalFees();

  String getCurrency();
}