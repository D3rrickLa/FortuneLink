package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record RecordDepositRequest(@NotNull BigDecimal amount, @NotNull String currency,
    @NotNull Instant transactionDate, String notes) {

}
