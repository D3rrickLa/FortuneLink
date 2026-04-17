package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for portfolio creation.
 * <p>
 * createDefaultAccount defaults to false if omitted in JSON. When true, both
 * defaultAccountType and
 * defaultStrategy are required.
 * <p>
 * The cross-field validation (if createDefaultAccount=true then type/strategy
 * required) is handled
 * in PortfolioLifecycleCommandValidator rather than here, because Jakarta Bean
 * Validation doesn't
 * support cross-field rules cleanly on records.
 */
@Schema(description = "Request body for initializing a new investment portfolio")
public record CreatePortfolioRequest(
    @Schema(description = "Display name for the portfolio", example = "Long-Term Retirement") @NotBlank @Size(max = 100) String name,

    @Schema(description = "Optional notes or goals for this portfolio", example = "Tax-advantaged accounts only") @Size(max = 500) String description,

    @Schema(description = "Base currency for net worth calculation (ISO-4217)", example = "USD") @NotBlank @Size(min = 3, max = 3) String currency,

    @Schema(description = "If true, initializes one default account immediately", example = "true") @NotNull Boolean createDefaultAccount,

    @Schema(description = "Type of the default account to be created") AccountType defaultAccountType,

    @Schema(description = "Strategy for position tracking in the default account") PositionStrategy defaultStrategy) {

  public CreatePortfolioRequest {
    createDefaultAccount = createDefaultAccount != null ? createDefaultAccount : Boolean.FALSE;
  }
}