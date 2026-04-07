package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request body for recording interest income.
 * <p>
 * Supports two distinct scenarios:
 * <p>
 * 1. Cash / account-level interest (HISA, savings account, GIC): Omit assetSymbol (leave null or
 * absent). The interest is credited to the account's cash balance with no position association.
 * Example: Monthly HISA interest payment.
 * <p>
 * 2. Asset-level interest (bond coupon, debenture): Provide assetSymbol. The interest is credited
 * to cash but is associated with the holding for tax reporting purposes (T5 box 13). Example:
 * Semi-annual coupon on a corporate bond.
 * <p>
 * In both cases the cash balance increases by the full amount. Interest is taxable in the year
 * received (not deferred).
 * <p>
 * NOTE: Bond coupon support is marked TODO in TransactionCommandValidator — the validator does not
 * currently confirm an open position exists for the symbol. The server will accept the request even
 * if no bond position is held. This will be tightened when bonds are fully supported.
 */
public record RecordInterestRequest(
    @NotBlank String idempotencyKey,

    @Pattern(regexp = "^[A-Z0-9.\\-]{1,20}$", message = "Symbol must be 1-20 uppercase letters, digits, dots, or hyphens") String assetSymbol,

    @NotNull(message = "Amount is required") @DecimalMin(value = "0.000001", message = "Interest amount must be greater than zero") BigDecimal amount,

    @NotBlank(message = "Currency is required") @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO-4217 code") String currency,

    @NotNull(message = "Transaction date is required") Instant transactionDate,

    String notes) {

  public boolean isAssetInterest() {
    return assetSymbol != null && !assetSymbol.isBlank();
  }
}
