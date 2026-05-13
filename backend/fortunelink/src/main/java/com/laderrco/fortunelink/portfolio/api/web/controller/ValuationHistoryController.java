package com.laderrco.fortunelink.portfolio.api.web.controller;

import com.laderrco.fortunelink.portfolio.api.web.dto.responses.ValuationResponse;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioAccessUtils;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.views.ValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ValuationSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.ValuationSnapshotRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
  private final ValuationSnapshotRepository snapshotRepository;
  private final PortfolioValuationService portfolioValuationService;
  private final MarketDataService marketDataService;
  private final PortfolioLoader portfolioLoader;

  // Add this to ValuationHistoryController.java

  @GetMapping("/{portfolioId}")
  @Operation(summary = "Get portfolio valuation", description = "Computes live valuation for a specific portfolio.")
  public ResponseEntity<ValuationResponse> getPortfolioValuation(
      @AuthenticatedUser UserId userId,
      @PathVariable String portfolioId // This matches your frontend call
  ) {
    // Load specifically by ID
    Portfolio portfolio = portfolioLoader.loadUserPortfolio(PortfolioId.fromString(portfolioId), userId);

    if (portfolio == null) {
      return ResponseEntity.notFound().build();
    }

    Set<AssetSymbol> symbols = PortfolioAccessUtils.extractSymbols(portfolio);
    Map<AssetSymbol, MarketAssetQuote> quoteCache = symbols.isEmpty() ? Map.of()
        : marketDataService.getBatchQuotes(symbols);

    ValuationView view = portfolioValuationService.calculatePortfolioValuation(
        portfolio,
        portfolio.getDisplayCurrency(),
        quoteCache);

    return ResponseEntity.ok(ValuationResponse.from(view));
  }

  @GetMapping("/summary")
  @Operation(summary = "Get current valuation", description = "Computes live valuation across all active portfolios for the authenticated user.")
  public ResponseEntity<ValuationResponse> getSummary(@AuthenticatedUser UserId userId) {
    List<Portfolio> portfolios = portfolioLoader.loadAllUserPortfolios(userId);

    if (portfolios.isEmpty()) {
      return ResponseEntity.noContent().build();
    }

    Currency displayCurrency = portfolios.getFirst().getDisplayCurrency();

    Set<AssetSymbol> symbols = portfolios.stream()
        .flatMap(p -> PortfolioAccessUtils.extractSymbols(p).stream()).collect(Collectors.toSet());

    Map<AssetSymbol, MarketAssetQuote> quoteCache = symbols.isEmpty() ? Map.of()
        : marketDataService.getBatchQuotes(symbols);

    ValuationView view = portfolioValuationService.calculateUserValuation(portfolios,
        displayCurrency, quoteCache);

    return ResponseEntity.ok(ValuationResponse.from(view));
  }

  @GetMapping("/history")
  @Operation(summary = "Get historical net worth", description = "Returns a time-series list of net worth snapshots for the authenticated user. Maximum range is 5 years (1825 days).")
  public List<ValuationSnapshotResponse> getHistory(@AuthenticatedUser UserId userId,
      @Parameter(description = "Number of days of history to retrieve", example = "90") @RequestParam(defaultValue = "90") @Min(1) @Max(1825) int days) {

    Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
    return snapshotRepository.findByUserIdSince(userId, since).stream()
        .map(ValuationSnapshotResponse::from).toList();
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