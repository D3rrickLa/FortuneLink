package com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for recording an outbound cash transfer.
 * <p>
 * Use this when cash moves OUT of an account to another account. No fees are
 * supported here , if
 * your broker charged an outbound transfer fee, record it as a separate POST
 * /fee with
 * feeType=WIRE_TRANSFER_FEE or ACCOUNT_MAINTENANCE after this transfer-out.
 * <p>
 * See RecordTransferInRequest for the paired inbound side.
 * <p>
 * The account's cash balance must cover the full amount. The server returns 422
 * INSUFFICIENT_FUNDS
 * if it does not.
 */
@Schema(description = "Request to record a cash transfer out of the account")
public record RecordTransferOutRequest(
    @Schema(example = "250.00") @NotNull @DecimalMin("0.01") BigDecimal amount,
    @Schema(example = "USD") @NotBlank @Size(min = 3, max = 3) String currency,
    @NotNull Instant transactionDate,
    String notes) {
}