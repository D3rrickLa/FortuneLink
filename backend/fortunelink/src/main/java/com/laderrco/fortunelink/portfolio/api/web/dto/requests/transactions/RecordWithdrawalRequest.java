package com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for recording a cash withdrawal.
 * <p>
 * Withdrawals do not carry fees. If your broker charged a withdrawal fee, record it as a separate
 * POST /fee transaction after this one.
 * <p>
 * The account's cash balance must cover the withdrawal amount. The server will return 422
 * INSUFFICIENT_FUNDS if it does not.
 * <p>
 * currency must match the account's base currency. Cross-currency withdrawals are not supported ,
 * withdraw in the account's base currency only.
 */
@Schema(description = "Request to record a manual cash withdrawal")
public record RecordWithdrawalRequest(
    @Schema(example = "100.00") @NotNull @DecimalMin("0.01") BigDecimal amount,
    @Schema(example = "USD") @NotBlank @Size(min = 3, max = 3) String currency,
    Instant transactionDate,
    String notes) {}