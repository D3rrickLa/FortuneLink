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
 * gains for the account.**Primary use cases:*1. Tax reporting page—pass taxYear
 * to get a year'sschedule 3 data*2. Per-position P&L panel—pass symbol to see
 * full gain/loss history*3. Account overview—omit both to see lifetime
 * totals**Performance note:*Realized gains are stored in the DB as an
 * append-only ledger and are never*recomputed on read.This endpoint is always
 * O(n)on the filtered row count*with no market data calls.It is safe to call on
 * every page load.**Authorization:*The user must own the portfolio that owns
 * the account.The service layer*performs a three-way ownership
 * check(portfolioId+userId+accountId).*Passing an accountId that belongs to a
 * different user'sportfolio returns*404(not 403)—we intentionally do not
 * confirm that the account exists.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/portfolios/{portfolioId}/accounts/{accountId}/realized-gains")
public class RealizedGainsController {

  // Earliest reasonable tax year — prevents accidental full-history queries
  // being disguised as year queries with obviously wrong inputs.
  private static final int MIN_TAX_YEAR = 2000;

  private final RealizedGainsQueryService realizedGainsQueryService;

  /**
   * Returns realized capital gains and losses for an account.
   * <p>
   * Optionally filtered by tax year and/or symbol.
   * <p>
   * Response includes: - Line-item breakdown (items) sorted by occurredAt descending - Pre-computed
   * totals: totalGains, totalLosses, netGainLoss - The account's base currency (all amounts are in
   * this currency) - The taxYear filter that was applied (null = no year filter)
   *
   * @param taxYear Optional. Calendar year (UTC) to filter by. Min 2000.
   * @param symbol  Optional. ISO symbol to filter by (e.g. "AAPL", "BTC-USD"). Must match
   *                AssetSymbol validation rules: [A-Z0-9.-], max 20 chars.
   *                <p>
   *                Examples: GET .../realized-gains → all time, all symbols GET
   *                .../realized-gains?taxYear=2024 → 2024 only, all symbols GET
   *                .../realized-gains?symbol=AAPL → all time, AAPL only GET
   *                .../realized-gains?taxYear=2024&symbol=AAPL → 2024 AAPL only
   */
  @GetMapping
  public RealizedGainsSummaryResponse getRealizedGains(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,

      @RequestParam(required = false) @Min(value = MIN_TAX_YEAR, message = "Tax year must be "
          + MIN_TAX_YEAR
          + " or later") @Max(value = 9999, message = "Tax year must be a 4-digit year") Integer taxYear,

      @RequestParam(required = false) @Pattern(regexp = "^[A-Z0-9.\\-]{1,20}$", message = "Symbol must be 1-20 uppercase letters, digits, dots, or hyphens") String symbol,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

    // Reject future tax years — no trades can have settled in a future year
    if (taxYear != null && taxYear > Year.now().getValue()) {
      throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
          "Tax year cannot be in the future: " + taxYear);
    }

    AssetSymbol assetSymbol = null;
    if (symbol != null && !symbol.isBlank()) {
      try {
        assetSymbol = new AssetSymbol(symbol.trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Invalid symbol format: " + symbol);
      }
    }

    GetRealizedGainsQuery query = new GetRealizedGainsQuery(PortfolioId.fromString(portfolioId),
        userId, AccountId.fromString(accountId), taxYear, assetSymbol, page, size);

    RealizedGainsSummaryView view = realizedGainsQueryService.getRealizedGains(query);
    return RealizedGainsSummaryResponse.from(view);
  }
}