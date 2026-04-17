package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
    @NotNull @Schema(description = "The display name for the account", example = "Tech Growth 2026") String accountName,

    @NotNull @Schema(description = "The type of account", example = "TFSA") AccountType accountType,

    @NotNull @Schema(description = "The strategy used for cost-basis and tax lot selection", example = "FIFO") PositionStrategy strategy,

    @NotNull @Schema(description = "3-letter ISO currency code", example = "USD", minLength = 3, maxLength = 3) String currency) {
}