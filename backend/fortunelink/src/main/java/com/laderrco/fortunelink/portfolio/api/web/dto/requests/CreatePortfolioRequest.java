package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePortfolioRequest(
    @NotBlank String name,
    String description,
    @NotBlank String currency,
    @NotNull Boolean createDefaultAccount, // still boxed but validated non-null
    @NotNull(message = "Account type required when creating default account") AccountType defaultAccountType,
    PositionStrategy defaultStrategy) {
}