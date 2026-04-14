package com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions;

import com.laderrco.fortunelink.portfolio.api.web.dto.requests.FeeRequest;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RecordPurchaseRequest(
    // @NotNull String accountId, // this is not needed as it should be in the part var
    @NotNull String symbol,
    @NotNull AssetType type,
    @NotNull @DecimalMin("0.00000001") BigDecimal quantity,
    @NotNull @DecimalMin("0.01") BigDecimal price,
    @NotNull String currency,
    List<FeeRequest> fees,
    Instant transactionDate,
    String notes) {
}