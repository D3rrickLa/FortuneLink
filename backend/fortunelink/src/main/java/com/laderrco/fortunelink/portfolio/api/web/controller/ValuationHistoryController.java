package com.laderrco.fortunelink.portfolio.api.web.controller;

import com.laderrco.fortunelink.portfolio.api.web.dto.responses.ValuationResponse;
import com.laderrco.fortunelink.portfolio.application.services.ValuationApplicationService;
import com.laderrco.fortunelink.portfolio.application.views.ValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ValuationSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.ValuationSnapshotRepository;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUser;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/valuations")
@Tag(name = "Valuations", description = "Historical portfolio valuation and performance tracking")
public class ValuationHistoryController {
  private final ValuationApplicationService valuationService;
  private final ValuationSnapshotRepository snapshotRepository;

  @GetMapping("/{portfolioId}")
  public ResponseEntity<ValuationResponse> getPortfolioValuation(
      @AuthenticatedUser UserId userId,
      @PathVariable String portfolioId) {

    ValuationView view = valuationService.computePortfolioValuation(
        PortfolioId.fromString(portfolioId), userId);

    return ResponseEntity.ok(ValuationResponse.from(view));
  }

  @GetMapping("/summary")
  public ResponseEntity<ValuationResponse> getSummary(@AuthenticatedUser UserId userId) {
    ValuationView view = valuationService.computeSummaryValuation(userId);
    return ResponseEntity.ok(ValuationResponse.from(view));
  }

  @GetMapping("/history")
  public List<ValuationSnapshotResponse> getHistory(
      @AuthenticatedUser UserId userId,
      @RequestParam(defaultValue = "90") @Min(1) @Max(1825) int days) {

    Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
    return snapshotRepository.findByUserIdSince(userId, since).stream()
        .map(ValuationSnapshotResponse::from)
        .toList();
  }

  @Schema(description = "Historical valuation snapshot")
  public record ValuationSnapshotResponse(

      @Schema(description = "Total portfolio value at the time of snapshot", example = "125000.50") BigDecimal totalValue,
      // domain no longer guarantees “net worth = assets - liabilities”.

      @Schema(description = "Total cost basis across all positions", example = "100000.00") BigDecimal totalCostBasis,

      @Schema(description = "Unrealized gain/loss", example = "25000.50") BigDecimal unrealizedGainLoss,

      @Schema(description = "Percentage gain/loss", example = "25.00") BigDecimal gainLossPercent,

      @Schema(description = "Total cash balance", example = "15000.00") BigDecimal totalCashBalance,

      @Schema(description = "Market value of invested assets", example = "110000.50") BigDecimal totalInvestedValue,

      @Schema(description = "ISO-4217 currency code", example = "USD") String currency,

      @Schema(description = "True if stale pricing data was used") boolean hasStaleData,

      @Schema(description = "UTC timestamp when snapshot was recorded") Instant snapshotDate

  ) {

    public static ValuationSnapshotResponse from(ValuationSnapshot s) {
      if (s == null) {
        return null;
      }

      return new ValuationSnapshotResponse(
          safeAmount(s.totalValue()),
          safeAmount(s.totalCostBasis()),
          safeAmount(s.unrealizedGainLoss()),
          s.gainLossPercent(),
          safeAmount(s.totalCashBalance()),
          safeAmount(s.totalInvestedValue()),
          s.displayCurrency(),
          s.hasStaleData(),
          s.snapshotDate());
    }

    // Helper to prevent NullPointerExceptions from Money/Monetary objects
    private static BigDecimal safeAmount(Money money) {
      return (money != null) ? money.amount() : BigDecimal.ZERO;
    }
  }
}