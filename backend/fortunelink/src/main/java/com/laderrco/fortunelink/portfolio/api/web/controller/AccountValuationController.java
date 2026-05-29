
package com.laderrco.fortunelink.portfolio.api.web.controller;

import com.laderrco.fortunelink.portfolio.api.web.dto.responses.MoneyResponse;
import com.laderrco.fortunelink.portfolio.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio.application.services.AccountValuationApplicationService;
import com.laderrco.fortunelink.portfolio.application.views.ValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/portfolios/{portfolioId}/accounts")
@Tag(name = "Account Valuations", description = "Account-level valuation and performance tracking")
public class AccountValuationController {
  private final AccountValuationApplicationService accountValuationService;

  @Operation(summary = "Get account valuation", description = """
      Returns the current valuation snapshot for an account,
      including invested market value, cash balance,
      unrealized gain/loss, and performance metrics.
      """)
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Account valuation retrieved successfully"),
      @ApiResponse(responseCode = "404", description = "Account not found"),
      @ApiResponse(responseCode = "403", description = "Forbidden") })
  @GetMapping(value = "/{accountId}/valuation", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<AccountValuationResponse> getAccountValuation(
      @AuthenticatedUser UserId userId, @PathVariable String portfolioId,
      @PathVariable String accountId) {

    ValuationView view = accountValuationService.computeAccountValuation(
        new GetAccountSummaryQuery(PortfolioId.fromString(portfolioId), userId,
            AccountId.fromString(accountId)));

    return ResponseEntity.ok(AccountValuationResponse.from(view));
  }

  @Operation(summary = "Get account valuation history", description = """
      Returns a historical timeline list of valuation snapshots for an account over a given period.
      """)
  @GetMapping(value = "/{accountId}/valuation/history", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<AccountHistorySnapshotResponse>> getAccountValuationHistory(
      @AuthenticatedUser UserId userId,
      @PathVariable String portfolioId,
      @PathVariable String accountId,
      @RequestParam(defaultValue = "90") int days) {

    List<ValuationView> historyViews = accountValuationService.getAccountValuationHistory(
        AccountId.fromString(accountId), days);

    List<AccountHistorySnapshotResponse> historyResponse = historyViews.stream()
        .map(v -> {
          // Convert Instant to LocalDate, defaulting to today's date in UTC if null
          LocalDate snapshotDate = v.asOfDate() != null
              ? v.asOfDate().atZone(ZoneOffset.UTC).toLocalDate()
              : LocalDate.now(ZoneOffset.UTC);

          return new AccountHistorySnapshotResponse(
              snapshotDate,
              MoneyResponse.from(v.totalValue()),
              MoneyResponse.from(v.totalCostBasis()),
              MoneyResponse.from(v.unrealizedGainLoss()),
              v.gainLossPercent(),
              MoneyResponse.from(v.totalCashBalance()),
              MoneyResponse.from(v.totalInvestedValue()));
        })
        .toList();

    return ResponseEntity.ok(historyResponse);
  }

  // ─────────────────────────────────────────────────────────────
  // DTO
  // ─────────────────────────────────────────────────────────────
  public record AccountHistorySnapshotResponse(
      LocalDate snapshotDate,
      MoneyResponse totalValue,
      MoneyResponse totalCostBasis,
      MoneyResponse unrealizedGainLoss,
      BigDecimal gainLossPercent,
      MoneyResponse cashBalance,
      MoneyResponse investedValue) {
  }

  public record AccountValuationResponse(
      MoneyResponse totalValue,
      MoneyResponse totalCostBasis,
      MoneyResponse unrealizedGainLoss,
      BigDecimal gainLossPercent,
      MoneyResponse cashBalance,
      MoneyResponse investedValue,
      String currency) {

    public static AccountValuationResponse from(ValuationView v) {
      return new AccountValuationResponse(MoneyResponse.from(v.totalValue()),
          MoneyResponse.from(v.totalCostBasis()), MoneyResponse.from(v.unrealizedGainLoss()),
          v.gainLossPercent(), MoneyResponse.from(v.totalCashBalance()),
          MoneyResponse.from(v.totalInvestedValue()), v.displayCurrency().getCode());
    }
  }
}