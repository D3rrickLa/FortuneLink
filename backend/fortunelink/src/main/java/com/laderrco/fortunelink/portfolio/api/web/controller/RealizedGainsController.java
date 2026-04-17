package com.laderrco.fortunelink.portfolio.api.web.controller;

import com.laderrco.fortunelink.portfolio.api.web.dto.responses.RealizedGainsSummaryResponse;
import com.laderrco.fortunelink.portfolio.application.queries.GetRealizedGainsQuery;
import com.laderrco.fortunelink.portfolio.application.services.RealizedGainsQueryService;
import com.laderrco.fortunelink.portfolio.application.views.RealizedGainsSummaryView;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.Year;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/***
 * Capital gains and losses query for a specific
 * account.**Path:/api/v1/portfolios/{portfolioId}/accounts/{accountId}/realized-gains**This
 * is a read-only endpoint.Realized gains are computed and persisted
 * by*PositionRecalculationService when a SELL or excess ROC transaction
 * is*processed.They cannot be created or modified directly via the
 * API.**Filtering:*-taxYear:filters events by UTC year.See timezone caveat in
 * the response DTO.*-symbol:filters to a single asset,e.g.?symbol=AAPL*-Both
 * can be combined:?taxYear=2024&symbol=AAPL*-Omit both to get all realized
 * gains for the account.**Primary use cases:*1. Tax reporting page,pass taxYear
 * to get a year'sschedule 3 data*2. Per-position P&L panel,pass symbol to see
 * full gain/loss history*3. Account overview,omit both to see lifetime
 * totals**Performance note:*Realized gains are stored in the DB as an
 * append-only ledger and are never*recomputed on read.This endpoint is always
 * O(n)on the filtered row count*with no market data calls.It is safe to call on
 * every page load.**Authorization:*The user must own the portfolio that owns
 * the account.The service layer*performs a three-way ownership
 * check(portfolioId+userId+accountId).*Passing an accountId that belongs to a
 * different user'sportfolio returns*404(not 403),we intentionally do not
 * confirm that the account exists.
 */

/**
 * Read-only ledger for realized capital gains and losses. Gains are computed during SELL or Return
 * of Capital (ROC) events and persisted as an immutable ledger. This endpoint provides
 * high-performance access for tax reporting and P&L analysis. Authorization: Requires a three-way
 * ownership check (User -> Portfolio -> Account). Invalid combinations return 404 to prevent
 * account enumeration.
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/portfolios/{portfolioId}/accounts/{accountId}/realized-gains")
@Tag(name = "Tax & Realized Gains", description = "Endpoints for capital gains reporting and historical P&L.")
public class RealizedGainsController {
  private static final int MIN_TAX_YEAR = 2000;
  private final RealizedGainsQueryService realizedGainsQueryService;


  @Operation(summary = "Get realized gains ledger", description =
      "Returns a breakdown of capital gains and losses, including pre-computed totals. "
          + "Results are sorted by occurrence date (descending). Database-backed and safe for high-frequency polling.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "Ledger retrieved successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid tax year or symbol format"),
      @ApiResponse(responseCode = "404", description = "Account not found or access denied")})
  @GetMapping
  public RealizedGainsSummaryResponse getRealizedGains(
      @Parameter(description = "The unique ID of the portfolio") @PathVariable String portfolioId,
      @Parameter(hidden = true) @AuthenticatedUser UserId userId,
      @Parameter(description = "The unique ID of the account") @PathVariable String accountId,
      @Parameter(description = "Filter by UTC tax year (e.g., 2024). Cannot be in the future.") @RequestParam(required = false) @Min(value = MIN_TAX_YEAR, message =
          "Tax year must be " + MIN_TAX_YEAR
              + " or later") @Max(value = 9999, message = "Tax year must be a 4-digit year") Integer taxYear,
      @Parameter(description = "Filter by specific ticker (e.g., AAPL).") @RequestParam(required = false) @Pattern(regexp = "^[A-Z0-9.\\-]{1,20}$", message = "Invalid symbol format") String symbol,
      @Parameter(description = "Page number for pagination") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Items per page") @RequestParam(defaultValue = "20") int size) {

    // Validate Year
    if (taxYear != null && taxYear > Year.now().getValue()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Tax year cannot be in the future.");
    }

    // Parse Symbol
    AssetSymbol assetSymbol = parseAssetSymbol(symbol);

    // Execute Query
    GetRealizedGainsQuery query = new GetRealizedGainsQuery(PortfolioId.fromString(portfolioId),
        userId, AccountId.fromString(accountId), taxYear, assetSymbol, page, size);

    RealizedGainsSummaryView view = realizedGainsQueryService.getRealizedGains(query);
    return RealizedGainsSummaryResponse.from(view);
  }

  private AssetSymbol parseAssetSymbol(String symbol) {
    if (symbol == null || symbol.isBlank()) {
      return null;
    }
    try {
      return new AssetSymbol(symbol.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid symbol format: " + symbol);
    }
  }
}