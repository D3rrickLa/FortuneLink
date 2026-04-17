package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to exclude a transaction from performance calculations")
public record ExcludeTransactionRequest(
    @Schema(description = "Reason for exclusion (e.g., duplicate, internal transfer error)", example = "Duplicate entry") @NotNull String reason) {
}