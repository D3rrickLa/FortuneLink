package com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Request to record a manual cash deposit")
public record RecordDepositRequest(
    @Schema(example = "1000.00") @NotNull BigDecimal amount,
    @Schema(example = "USD") @NotNull String currency,
    @Schema(description = "UTC timestamp of the deposit") Instant transactionDate,
    @Schema(example = "Monthly savings contribution") String notes) {
}