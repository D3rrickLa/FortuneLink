package com.laderrco.fortunelink.portfolio.api.web.controller;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ValuationSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.ValuationSnapshotRepository;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/valuations/history")
@Tag(name = "Valuations", description = "Historical portfolio valuation and performance tracking")
public class ValuationHistoryController {
  private final ValuationSnapshotRepository snapshotRepository;

  @GetMapping
  @Operation(summary = "Get historical net worth", description = "Returns a time-series list of net worth snapshots for the authenticated user. Maximum range is 5 years (1825 days).")
  public List<ValuationSnapshotResponse> getHistory(@AuthenticatedUser UserId userId,
      @Parameter(description = "Number of days of history to retrieve", example = "90") @RequestParam(defaultValue = "90") @Min(1) @Max(1825) int days) {

    // Note: The @Min/@Max annotations on @RequestParam handle the validation
    // and throw a 400 Bad Request automatically if out of bounds.
    // if (days < 1 || days > 1825) { // max 5 years
    // throw new IllegalArgumentException("days must be between 1 and 1825");
    // }

    Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
    return snapshotRepository.findByUserIdSince(userId, since).stream()
        .map(ValuationSnapshotResponse::from).toList();
  }

  @Schema(description = "Historical valuation snapshot")
  public record ValuationSnapshotResponse(

      @Schema(description = "Total portfolio value at the time of snapshot", example = "125000.50") double totalValue, // domain no longer guarantees “net worth = assets - liabilities”.

      @Schema(description = "Total cost basis across all positions", example = "100000.00") double totalCostBasis,

      @Schema(description = "Unrealized gain/loss", example = "25000.50") double unrealizedGainLoss,

      @Schema(description = "Percentage gain/loss", example = "25.00") double gainLossPercent,

      @Schema(description = "Total cash balance", example = "15000.00") double totalCashBalance,

      @Schema(description = "Market value of invested assets", example = "110000.50") double totalInvestedValue,

      @Schema(description = "ISO-4217 currency code", example = "USD") String currency,

      @Schema(description = "True if stale pricing data was used") boolean hasStaleData,

      @Schema(description = "UTC timestamp when snapshot was recorded") Instant snapshotDate

  ) {

    public static ValuationSnapshotResponse from(ValuationSnapshot s) {
      return new ValuationSnapshotResponse(
          s.totalValue().amount().doubleValue(),
          s.totalCostBasis().amount().doubleValue(),
          s.unrealizedGainLoss().amount().doubleValue(),
          s.gainLossPercent() != null ? s.gainLossPercent().doubleValue() : 0.0,
          s.totalCashBalance().amount().doubleValue(),
          s.totalInvestedValue().amount().doubleValue(),
          s.displayCurrency(),
          s.hasStaleData(),
          s.snapshotDate());
    }
  }
}