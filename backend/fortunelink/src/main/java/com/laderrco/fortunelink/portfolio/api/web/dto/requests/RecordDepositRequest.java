package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record RecordDepositRequest(
    @NotBlank String idempotencyKey,
    @NotNull BigDecimal amount,
    @NotNull String currency,
    @NotNull Instant transactionDate,
    String notes) {

}
