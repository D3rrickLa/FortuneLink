package com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions;

import com.laderrco.fortunelink.portfolio.api.web.dto.requests.FeeRequest;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Request to record an asset purchase (Buy)")
public record RecordPurchaseRequest(
    @Schema(example = "TSLA") @NotNull String symbol,
    @NotNull AssetType type,
    @Schema(description = "Quantity bought (supports fractional)", example = "10.0") @NotNull @DecimalMin("0.00000001") BigDecimal quantity,
    @Schema(description = "Price per unit", example = "175.50") @NotNull @DecimalMin("0.01") BigDecimal price,
    @Schema(example = "USD") @NotNull String currency,
    @Schema(description = "Associated transaction fees (e.g., commissions)") List<FeeRequest> fees,
    Instant transactionDate,
    String notes) {
}