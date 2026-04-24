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
 * Request body for recording interest income.
 * <p>
 * Supports two distinct scenarios:
 * <p>
 * 1. Cash / account-level interest (HISA, savings account, GIC): Omit
 * assetSymbol (leave null or
 * absent). The interest is credited to the account's cash balance with no
 * position association.
 * Example: Monthly HISA interest payment.
 * <p>
 * 2. Asset-level interest (bond coupon, debenture): Provide assetSymbol. The
 * interest is credited
 * to cash but is associated with the holding for tax reporting purposes (T5 box
 * 13). Example:
 * Semi-annual coupon on a corporate bond.
 * <p>
 * In both cases the cash balance increases by the full amount. Interest is
 * taxable in the year
 * received (not deferred).
 * <p>
 * NOTE: Bond coupon support is marked TODO in TransactionCommandValidator, the
 * validator does not
 * currently confirm an open position exists for the symbol. The server will
 * accept the request even
 * if no bond position is held. This will be tightened when bonds are fully
 * supported.
 */
@Schema(description = "Request to record interest income (Cash or Asset-based)")
public record RecordInterestRequest(
    @Schema(description = "Optional ticker if interest is from a specific bond/GIC", example = "O") @Pattern(regexp = "^[A-Z0-9.\\-]{1,20}$") String assetSymbol,
    @Schema(example = "15.25") @NotNull @DecimalMin("0.000001") BigDecimal amount,
    @Schema(example = "CAD") @NotBlank @Size(min = 3, max = 3) String currency,
    @NotNull Instant transactionDate,
    String notes) {
  public boolean isAssetInterest() {
    return assetSymbol != null && !assetSymbol.isBlank();
  }
}