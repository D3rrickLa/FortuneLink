package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
    @NotNull String accountName,
    @NotNull AccountType accountType,
    @NotNull PositionStrategy strategy,
    @NotNull String currency) {
}