package com.laderrco.fortunelink.portfolio.api.web.controller;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.NetWorthSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.NetWorthSnapshotRepository;
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
@RequestMapping("/api/v1/net-worth/history")
@Tag(name = "Analytics", description = "Historical net worth and performance tracking")
public class NetWorthHistoryController {
  private final NetWorthSnapshotRepository snapshotRepository;

  @GetMapping
  @Operation(summary = "Get historical net worth", description = "Returns a time-series list of net worth snapshots for the authenticated user. Maximum range is 5 years (1825 days).")
  public List<NetWorthSnapshotResponse> getHistory(
      @AuthenticatedUser UserId userId,
      @Parameter(description = "Number of days of history to retrieve", example = "90") @RequestParam(defaultValue = "90") @Min(1) @Max(1825) int days) {

    // Note: The @Min/@Max annotations on @RequestParam handle the validation
    // and throw a 400 Bad Request automatically if out of bounds.
    // if (days < 1 || days > 1825) { // max 5 years
    // throw new IllegalArgumentException("days must be between 1 and 1825");
    // }

    Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
    return snapshotRepository.findByUserIdSince(userId, since).stream()
        .map(NetWorthSnapshotResponse::from).toList();
  }

  @Schema(description = "A point-in-time snapshot of the user's total net worth")
  public record NetWorthSnapshotResponse(
      @Schema(description = "Total net worth (Assets - Liabilities)", example = "125000.50") double netWorth,

      @Schema(description = "Sum of all active account balances", example = "150000.00") double totalAssets,

      @Schema(description = "Sum of all debts/margin (expressed as a positive number)", example = "25000.50") double totalLiabilities,

      @Schema(description = "ISO-4217 currency code used for the calculation", example = "USD") String currency,

      @Schema(description = "True if some prices were missing and cost-basis was used instead") boolean hasStaleData,

      @Schema(description = "UTC timestamp when the snapshot was recorded") Instant snapshotDate) {

    public static NetWorthSnapshotResponse from(NetWorthSnapshot s) {
      return new NetWorthSnapshotResponse(
          s.netWorth().amount().doubleValue(),
          s.totalAssets().amount().doubleValue(),
          s.totalLiabilities().amount().doubleValue(),
          s.displayCurrency().getCode(),
          s.hasStaleData(),
          s.snapshotDate());
    }
  }
}