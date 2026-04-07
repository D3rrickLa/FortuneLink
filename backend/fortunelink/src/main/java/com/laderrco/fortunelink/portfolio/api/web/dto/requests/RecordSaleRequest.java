package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RecordSaleRequest(
    @NotBlank String idempotencyKey,
    @NotNull String symbol,
    @NotNull BigDecimal quantity,
    @NotNull BigDecimal price,
    @NotNull String currency,
    @NotNull List<FeeRequest> fees,
    @Nonnull Instant transactionDate,
    String notes) {

}
