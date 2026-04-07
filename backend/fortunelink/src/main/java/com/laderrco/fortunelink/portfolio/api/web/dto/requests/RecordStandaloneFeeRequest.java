package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import com.laderrco.fortunelink.portfolio.domain.model.enums.FeeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request body for recording a standalone fee transaction.
 * <p>
 * This creates a FEE transaction type that reduces the account's cash balance. Use this for: -
 * Annual account maintenance fees - Wire transfer fees - Margin interest charges - Management fees
 * billed to the account
 * <p>
 * Do NOT use this for trading commissions on a BUY or SELL — those are attached to the trade via
 * the fees[] array on /buy or /sell. Trading fees attached to trades affect the cost basis (ACB).
 * Standalone fees do not.
 * <p>
 * IMPORTANT: This is intentionally named RecordStandaloneFeeRequest to distinguish it from
 * FeeRequest, which is the embedded fee line item within a buy/sell transaction body. They serve
 * very different purposes.
 */
public record RecordStandaloneFeeRequest(
    @NotBlank String idempotencyKey,

    @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Fee amount must be greater than zero") BigDecimal amount,

    @NotBlank(message = "Currency is required") @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO-4217 code") String currency,

    @NotNull(message = "Fee type is required") FeeType feeType,

    @NotNull(message = "Transaction date is required") Instant transactionDate,

    String notes) {
}
