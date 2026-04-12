package com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request body for recording a Return of Capital (ROC) distribution.
 * <p>
 * A Return of Capital is a distribution from a fund or REIT that is classified as a return of
 * invested capital rather than income. It is NOT immediately taxable as income , instead it reduces
 * the Adjusted Cost Base (ACB) of the position. If the ACB reaches zero, subsequent ROC
 * distributions become capital gains.
 * <p>
 * Accounting effect: - Cash balance: increases by (distributionPerUnit × heldQuantity) - ACB:
 * decreases by (distributionPerUnit × heldQuantity) - If ACB would go negative: the excess is
 * recorded as a capital gain - Share count: UNCHANGED
 * <p>
 * distributionPerUnit: The per-share ROC amount from the fund's distribution notice (T3/T5013). Not
 * the total distribution amount , the system multiplies by heldQuantity. Example: if a fund
 * distributes $0.05/unit ROC on 200 units: distributionPerUnit=0.05, heldQuantity=200 → total
 * reduction = $10.00
 * <p>
 * heldQuantity: MUST exactly match the current position quantity. The server validates this. If you
 * have 200.00000000 units, pass exactly that. This prevents silent partial-position ROC
 * calculations. If the quantities don't match, the server returns 422 UNPROCESSABLE_ENTITY.
 * <p>
 * This is a Canadian tax concept (CRA IT-434R). Not applicable to US positions under US GAAP , US
 * cost basis adjustments use different rules.
 */
public record RecordReturnOfCapitalRequest(
    @NotBlank(message = "Asset symbol is required") @Pattern(regexp = "^[A-Z0-9.\\-]{1,20}$", message = "Symbol must be 1-20 uppercase letters, digits, dots, or hyphens") String assetSymbol,

    @NotNull(message = "Distribution per unit is required") @DecimalMin(value = "0.000001", message = "Distribution per unit must be greater than zero") BigDecimal distributionPerUnit,

    @NotBlank(message = "Currency is required") @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO-4217 code") String currency,

    @NotNull(message = "Held quantity is required") @DecimalMin(value = "0.00000001", message = "Held quantity must be greater than zero") BigDecimal heldQuantity,

    @NotNull(message = "Transaction date is required") Instant transactionDate,

    String notes) {
}
