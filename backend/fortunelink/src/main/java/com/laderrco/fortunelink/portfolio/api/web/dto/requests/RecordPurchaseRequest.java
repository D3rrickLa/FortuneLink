package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;

public record RecordPurchaseRequest(
    @NotBlank String accountId,
    @NotBlank String symbol,
    @NotBlank AssetType type,
    @NotNull @DecimalMin("0.00000001") BigDecimal quantity,
    @NotNull @DecimalMin("0.01") BigDecimal price,
    @NotBlank String currency,
    List<FeeRequest> fees,
    @NotNull Instant transactionDate,
    String notes) {
}