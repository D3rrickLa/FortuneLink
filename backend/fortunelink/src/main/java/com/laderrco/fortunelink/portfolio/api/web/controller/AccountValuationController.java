package com.laderrco.fortunelink.portfolio.api.web.controller;

import com.laderrco.fortunelink.portfolio.api.web.dto.responses.MoneyResponse;
import com.laderrco.fortunelink.portfolio.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio.application.services.AccountValuationApplicationService;
import com.laderrco.fortunelink.portfolio.application.views.AccountValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
      @ApiResponse(responseCode = "403", description = "Forbidden")})
  @GetMapping(value = "/{accountId}/valuation", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<AccountValuationResponse> getAccountValuation(
      @AuthenticatedUser UserId userId, @PathVariable String portfolioId,
      @PathVariable String accountId) {

    AccountValuationView view = accountValuationService.computeAccountValuation(
        new GetAccountSummaryQuery(PortfolioId.fromString(portfolioId), userId,
            AccountId.fromString(accountId)));

    return ResponseEntity.ok(AccountValuationResponse.from(view));
  }

  // ─────────────────────────────────────────────────────────────
  // DTO
  // ─────────────────────────────────────────────────────────────

  public record AccountValuationResponse(
      MoneyResponse totalValue,
      MoneyResponse totalCostBasis,
      MoneyResponse unrealizedGainLoss,
      BigDecimal gainLossPercent,
      MoneyResponse cashBalance,
      MoneyResponse investedValue,
      String currency) {

    public static AccountValuationResponse from(AccountValuationView v) {
      return new AccountValuationResponse(MoneyResponse.from(v.totalValue()),
          MoneyResponse.from(v.totalCostBasis()), MoneyResponse.from(v.unrealizedGainLoss()),
          v.gainLossPercent(), MoneyResponse.from(v.cashBalance()),
          MoneyResponse.from(v.investedValue()), v.currency().getCode());
    }
  }
}