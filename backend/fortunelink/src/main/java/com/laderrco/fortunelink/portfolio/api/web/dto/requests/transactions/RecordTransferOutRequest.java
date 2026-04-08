package com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request body for recording an outbound cash transfer.
 * <p>
 * Use this when cash moves OUT of an account to another account. No fees are supported here — if
 * your broker charged an outbound transfer fee, record it as a separate POST /fee with
 * feeType=WIRE_TRANSFER_FEE or ACCOUNT_MAINTENANCE after this transfer-out.
 * <p>
 * See RecordTransferInRequest for the paired inbound side.
 * <p>
 * The account's cash balance must cover the full amount. The server returns 422 INSUFFICIENT_FUNDS
 * if it does not.
 */
public record RecordTransferOutRequest(
    @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Transfer amount must be greater than zero") BigDecimal amount,

    @NotBlank(message = "Currency is required") @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO-4217 code") String currency,

    @NotNull(message = "Transaction date is required") Instant transactionDate,

    String notes) {
}
