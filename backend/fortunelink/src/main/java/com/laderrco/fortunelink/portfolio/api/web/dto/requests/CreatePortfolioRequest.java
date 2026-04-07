package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for portfolio creation.
 * <p>
 * createDefaultAccount defaults to false if omitted in JSON. When true, both defaultAccountType and
 * defaultStrategy are required.
 * <p>
 * The cross-field validation (if createDefaultAccount=true then type/strategy required) is handled
 * in PortfolioLifecycleCommandValidator rather than here, because Jakarta Bean Validation doesn't
 * support cross-field rules cleanly on records.
 */
public record CreatePortfolioRequest(
    @NotBlank @Size(max = 100, message = "Portfolio name must be 100 characters or less") String name,

    @Size(max = 500, message = "Description must be 500 characters or less") String description,

    @NotBlank @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO-4217 code") String currency,

    // Use primitive boolean or default to false, NEVER nullable Boolean
    // Nullable Boolean causes a NullPointerException when the field is
    // omitted from the JSON payload and the unboxing happens in the service layer.
    @NotNull Boolean createDefaultAccount,

    AccountType defaultAccountType,

    PositionStrategy defaultStrategy) {

  // Compact constructor applies a safe default for omitted field
  public CreatePortfolioRequest {
    createDefaultAccount = createDefaultAccount != null ? createDefaultAccount : Boolean.FALSE;
  }
}