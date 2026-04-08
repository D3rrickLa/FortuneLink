package com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

/**
 * Request body for recording a stock split or reverse split.
 * <p>
 * The split ratio is expressed as numerator:denominator. - 2-for-1 split (doubles your shares):
 * numerator=2, denominator=1 - 3-for-1 split (triples your shares): numerator=3, denominator=1 -
 * 1-for-10 reverse split (consolidates): numerator=1, denominator=10 - 4-for-1 forward split:
 * numerator=4, denominator=1
 * <p>
 * Effect on position: - Share count: multiplied by (numerator / denominator) - Cost basis per
 * share: divided by (numerator / denominator) - Total cost basis: UNCHANGED (no capital event, no
 * cash movement) - Cash balance: UNCHANGED (no cash event)
 * <p>
 * ACB impact: Under CRA guidance, stock splits do not trigger an ACB recalculation event. The total
 * ACB remains the same; the per-share ACB adjusts proportionally. This is handled correctly by the
 * domain.
 * <p>
 * A 1:1 ratio is rejected (no-op, server returns 400). A split for a symbol with no open position
 * is rejected (server returns 422).
 * <p>
 * If the split has already been partially applied by your broker (i.e. your brokerage already shows
 * the post-split quantity), do NOT record it again — recording a split replays the ratio against
 * the current position quantity.
 */
public record RecordSplitRequest(
    @NotBlank(message = "Symbol is required") @Pattern(regexp = "^[A-Z0-9.\\-]{1,20}$", message = "Symbol must be 1-20 uppercase letters, digits, dots, or hyphens") String symbol,

    @Min(value = 1, message = "Numerator must be at least 1") int numerator,

    @Min(value = 1, message = "Denominator must be at least 1") int denominator,

    @NotNull(message = "Transaction date is required") Instant transactionDate,

    String notes) {
}
