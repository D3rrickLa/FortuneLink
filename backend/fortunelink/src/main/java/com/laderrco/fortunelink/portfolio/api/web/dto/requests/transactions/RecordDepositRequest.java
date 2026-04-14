package com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record RecordDepositRequest(
    @NotNull BigDecimal amount,
    @NotNull String currency,
    Instant transactionDate,
    String notes) {

}
