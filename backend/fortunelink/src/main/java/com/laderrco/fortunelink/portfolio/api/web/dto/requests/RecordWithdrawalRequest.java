package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for recording a cash withdrawal.
 *
 * Withdrawals do not carry fees. If your broker charged a withdrawal fee,
 * record it as a separate POST /fee transaction after this one.
 *
 * The account's cash balance must cover the withdrawal amount. The server
 * will return 422 INSUFFICIENT_FUNDS if it does not.
 *
 * currency must match the account's base currency. Cross-currency withdrawals
 * are not supported — withdraw in the account's base currency only.
 */
public record RecordWithdrawalRequest(
    @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Withdrawal amount must be greater than zero") BigDecimal amount,

    @NotBlank(message = "Currency is required") @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO-4217 code") String currency,

    @NotNull(message = "Transaction date is required") Instant transactionDate,

    String notes) {
}
