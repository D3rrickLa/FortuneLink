package com.laderrco.fortunelink.portfolio.domain.services.projectors;

import java.math.BigDecimal;
import java.util.UUID;

public interface AssetBalanceProjection {
  UUID getAccountId();

  String getSymbol();

  BigDecimal getQuantity();
}