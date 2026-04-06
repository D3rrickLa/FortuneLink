package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;

public record RecordDepositRequest(@Nonnull BigDecimal amount, @Nonnull String currency,
    @NotNull Instant transactionDate, String notes) {

}
