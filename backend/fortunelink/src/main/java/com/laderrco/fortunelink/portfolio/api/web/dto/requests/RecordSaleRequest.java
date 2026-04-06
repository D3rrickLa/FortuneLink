package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;

public record RecordSaleRequest(
  @NotNull String symbol,
  @NotNull BigDecimal quantity,
  @NotNull BigDecimal price,
  @NotNull String currency,
  @NotNull List<FeeRequest> fees,
  @Nonnull Instant transactionDate,
  String notes
) {
  
}
