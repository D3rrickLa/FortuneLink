package com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for recording an inbound cash transfer.
 * <p>
 * Use this when cash moves INTO an account from an external source or from
 * another account at the
 * same broker. This is distinct from a DEPOSIT , a deposit is a new injection
 * of capital (bank
 * wire, payroll). A transfer-in is a movement between existing accounts.
 * <p>
 * For portfolio consolidation purposes: If you are moving from Questrade →
 * Wealthsimple, record: 1.
 * RecordTransferOut on the Questrade account (source) 2. RecordTransferIn on
 * the Wealthsimple
 * account (destination) The net effect on your total portfolio value is zero.
 * <p>
 * fees: Transfer fees are optional. If your broker charged a transfer-out fee
 * (common for
 * registered accounts), attach it here. These fees are NOT added to any
 * position's cost basis ,
 * they are an account-level expense. If fees were deducted from the transferred
 * amount (net
 * transfer), do not include them separately , just record the net amount and
 * note the gross.
 */
@Schema(description = "Request to record a cash transfer into the account")
public record RecordTransferInRequest(
    @Schema(example = "5000.00") @NotNull @DecimalMin("0.01") BigDecimal amount,
    @Schema(example = "USD") @NotBlank @Size(min = 3, max = 3) String currency,
    @NotNull Instant transactionDate,
    String notes) {
}