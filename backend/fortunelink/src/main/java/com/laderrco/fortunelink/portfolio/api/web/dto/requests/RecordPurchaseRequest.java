package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RecordPurchaseRequest(
    @NotBlank String idempotencyKey,
    @NotNull String accountId,
    @NotNull String symbol,
    @NotNull AssetType type,
    @NotNull @DecimalMin("0.00000001") BigDecimal quantity,
    @NotNull @DecimalMin("0.01") BigDecimal price,
    @NotNull String currency,
    List<FeeRequest> fees,
    @NotNull Instant transactionDate,
    String notes) {
}