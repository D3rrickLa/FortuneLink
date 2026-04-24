package com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions;

import com.laderrco.fortunelink.portfolio.api.web.dto.requests.FeeRequest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Request to record an asset sale (Sell)")
public record RecordSaleRequest(
    @Schema(example = "NVDA") @NotNull String symbol,
    @Schema(example = "5.0") @NotNull BigDecimal quantity,
    @Schema(example = "900.00") @NotNull BigDecimal price,
    @Schema(example = "USD") @NotNull String currency,
    List<FeeRequest> fees,
    Instant transactionDate,
    String notes) {
}