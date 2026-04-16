package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountRequest(
    @NotNull @Schema(description = "The new display name for the account", example = "Main Savings") String accountName) {
}
