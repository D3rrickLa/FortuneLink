package com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio.api.web.dto.requests.FeeRequest;

public record RecordSaleRequest(
    @NotNull String symbol,
    @NotNull BigDecimal quantity,
    @NotNull BigDecimal price,
    @NotNull String currency,
    @NotNull List<FeeRequest> fees,
    @NotNull Instant transactionDate,
    String notes) {

}
