package com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request body for recording a cash dividend payment.
 * <p>
 * Use this when cash lands in the account from a dividend distribution , the broker credits cash
 * and you receive it. The position count does not change. The cash balance increases by the full
 * amount.
 * <p>
 * DRIP CONFLICT WARNING: Do NOT record both a DIVIDEND and a DIVIDEND_REINVEST (DRIP) for the same
 * event. They are mutually exclusive: - Use POST /dividend when cash hits the account (manual
 * reinvestment) - Use POST /drip when the broker auto-reinvests (no cash event) Recording both will
 * double-count cash. The server logs a warning if it detects a DRIP transaction for the same symbol
 * within 24 hours of this request, but it will not block the save , you must resolve the conflict.
 * <p>
 * Foreign dividend: If the dividend was paid in a foreign currency (e.g. USD dividend in a CAD
 * account), the amount and currency here should reflect what was actually credited to your account
 * in its base currency after conversion. If you need to track the foreign withholding tax
 * separately, record a standalone POST /fee with feeType=WITHHOLDING_TAX.
 */
public record RecordDividendRequest(
    @NotBlank(message = "Asset symbol is required") @Pattern(regexp = "^[A-Z0-9.\\-]{1,20}$", message = "Symbol must be 1-20 uppercase letters, digits, dots, or hyphens") String assetSymbol,

    @NotNull(message = "Amount is required") @DecimalMin(value = "0.000001", message = "Dividend amount must be greater than zero") BigDecimal amount,

    @NotBlank(message = "Currency is required") @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO-4217 code") String currency,

    @NotNull(message = "Transaction date is required") Instant transactionDate,

    String notes) {
}