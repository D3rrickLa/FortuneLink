package com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for recording a Dividend Reinvestment Plan (DRIP) transaction.
 * <p>
 * A DRIP is a corporate action where dividends are automatically reinvested as
 * additional shares
 * rather than paid as cash. No cash enters or leaves the account. The share
 * count increases at the
 * DRIP price. The cost basis increases by the total gross value
 * (sharesPurchased × pricePerShare).
 * <p>
 * DRIP vs. DIVIDEND distinction (critical for correct accounting): - POST /drip
 * → broker
 * auto-reinvests, ZERO cash movement - POST /dividend → cash credited to
 * account, then manually buy
 * more shares
 * <p>
 * Never record both for the same event. If you record a DIVIDEND and then a BUY
 * with the proceeds,
 * that is two separate transactions. A DRIP is ONE transaction , no cash,
 * shares added, cost basis
 * increases.
 * <p>
 * pricePerShare: Use the actual DRIP price from your broker statement. DRIP
 * prices are often
 * calculated differently from the market price (e.g. 5-day VWAP, ex-dividend
 * NAV). Do not use the
 * market price as a substitute.
 * <p>
 * currency: Must match the account's base currency. The cost basis addition is
 * denominated in the
 * account currency.
 */
@Schema(description = "Request to record a Dividend Reinvestment (DRIP)")
public record RecordDRIPRequest(
    @Schema(example = "MSFT") @NotBlank @Pattern(regexp = "^[A-Z0-9.\\-]{1,20}$") String assetSymbol,
    @Schema(description = "Number of new shares acquired", example = "0.4521") @NotNull @DecimalMin("0.00000001") BigDecimal sharesPurchased,
    @Schema(description = "Price per share at time of reinvestment", example = "420.10") @NotNull @DecimalMin("0.000001") BigDecimal pricePerShare,
    @Schema(example = "USD") @NotBlank @Size(min = 3, max = 3) String currency,
    @NotNull Instant transactionDate,
    String notes) {
}