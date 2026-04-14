package com.laderrco.fortunelink.portfolio.application.utils.valueobjects;

import java.math.BigDecimal;

public record GainsAggregation(BigDecimal sumGains, BigDecimal sumLosses) {
  public GainsAggregation {
    // Handle nulls from the DB (e.g., if there are no records, SUM returns null)
    sumGains = (sumGains == null) ? BigDecimal.ZERO : sumGains;
    sumLosses = (sumLosses == null) ? BigDecimal.ZERO : sumLosses;
  }
}
