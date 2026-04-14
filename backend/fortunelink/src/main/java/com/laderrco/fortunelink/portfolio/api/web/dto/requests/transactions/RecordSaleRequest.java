package com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions;

import com.laderrco.fortunelink.portfolio.api.web.dto.requests.FeeRequest;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RecordSaleRequest(
    @NotNull String symbol,
    @NotNull BigDecimal quantity,
    @NotNull BigDecimal price,
    @NotNull String currency,
    List<FeeRequest> fees,
    Instant transactionDate,
    String notes) {

}
