package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request body for recording a Dividend Reinvestment Plan (DRIP) transaction.
 * <p>
 * A DRIP is a corporate action where dividends are automatically reinvested as additional shares
 * rather than paid as cash. No cash enters or leaves the account. The share count increases at the
 * DRIP price. The cost basis increases by the total gross value (sharesPurchased × pricePerShare).
 * <p>
 * DRIP vs. DIVIDEND distinction (critical for correct accounting): - POST /drip → broker
 * auto-reinvests, ZERO cash movement - POST /dividend → cash credited to account, then manually buy
 * more shares
 * <p>
 * Never record both for the same event. If you record a DIVIDEND and then a BUY with the proceeds,
 * that is two separate transactions. A DRIP is ONE transaction — no cash, shares added, cost basis
 * increases.
 * <p>
 * pricePerShare: Use the actual DRIP price from your broker statement. DRIP prices are often
 * calculated differently from the market price (e.g. 5-day VWAP, ex-dividend NAV). Do not use the
 * market price as a substitute.
 * <p>
 * currency: Must match the account's base currency. The cost basis addition is denominated in the
 * account currency.
 */
public record RecordDRIPRequest(
    @NotBlank String idempotencyKey,

    @NotBlank(message = "Asset symbol is required") @Pattern(regexp = "^[A-Z0-9.\\-]{1,20}$", message = "Symbol must be 1-20 uppercase letters, digits, dots, or hyphens") String assetSymbol,

    @NotNull(message = "Shares purchased is required") @DecimalMin(value = "0.00000001", message = "Shares purchased must be greater than zero") BigDecimal sharesPurchased,

    @NotNull(message = "Price per share is required") @DecimalMin(value = "0.000001", message = "Price per share must be greater than zero") BigDecimal pricePerShare,

    @NotBlank(message = "Currency is required") @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO-4217 code") String currency,

    @NotNull(message = "Transaction date is required") Instant transactionDate,

    String notes) {
}