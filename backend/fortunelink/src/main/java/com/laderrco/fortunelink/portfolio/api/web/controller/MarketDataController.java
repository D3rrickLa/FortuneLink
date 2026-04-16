package com.laderrco.fortunelink.portfolio.api.web.controller;

import com.laderrco.fortunelink.portfolio.api.web.dto.SymbolSearchResult;
import com.laderrco.fortunelink.portfolio.api.web.dto.requests.BatchQuoteRequest;
import com.laderrco.fortunelink.portfolio.api.web.dto.responses.AssetInfoResponse;
import com.laderrco.fortunelink.portfolio.api.web.dto.responses.MarketQuoteResponse;
import com.laderrco.fortunelink.portfolio.api.web.dto.responses.SymbolSearchResponse;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.infrastructure.exceptions.UnknownSymbolException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * @implNote: remember to do the routes properly because a GET of quotes/batch is
 * different to POST quotes/batch Public-facing market data endpoints
 * <p>
 * Rate-limit notes (enforced by RateLimitInterceptor): /search: lenient -
 * lightweight
 * autocomplete, no external API call if cached - /quotes : moderate - single
 * symbol, Redis-cached,
 * may fan out to FMP - /batch : strict - each call may burn N FMP quota
 * requests; must be short -
 * /info : lenient - long TTL in DB, rarely hits FMP - /validate : moderate -
 * always hits FMP if symbol is uncached; used pre-transaction
 * <p>
 * All endpoints are authenticated. Market data is never public - it would be
 * trivial to scrape the full symbol list otherwise.
 * <p>
 * The controller deliberately stays thin. No business logic lives here. Shape
 * mapping (domain → response DTO) is done inline for now; if it grows, extract a
 * MarketDataResponseMapper.
 */

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/market")
@Tag(name = "Market Data", description = "Endpoints for searching assets and retrieving real-time pricing.")
public class MarketDataController {
  private final MarketDataService marketDataService;

  @Operation(summary = "Autocomplete symbol search", description = "Searches for tradeable symbols. Returns shallow results (name, exchange). "
      + "Client should debounce at least 300ms. Rate limit: 30/min.")
  @GetMapping("/search")
  public List<SymbolSearchResponse> searchSymbols(
      @Parameter(description = "Search query (ticker or company name)") @RequestParam @Size(min = 1, max = 50) String query) {

    if (query.isBlank())
      return List.of();

    List<SymbolSearchResult> results = marketDataService.searchSymbols(query.trim());
    return results.stream().map(SymbolSearchResponse::fromDomain).toList();
  }

  @Operation(summary = "Validate and seed a symbol", description = "Must be called before a BUY transaction. Checks DB cache, then FMP API. "
      + "Seeds the internal database if found. Rate limit: 20/min.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Symbol validated"),
      @ApiResponse(responseCode = "404", description = "Symbol not found in external provider")
  })
  @GetMapping("/validate/{symbol}")
  public AssetInfoResponse validateSymbol(@PathVariable @NotBlank String symbol) {
    AssetSymbol assetSymbol = parseSymbol(symbol);
    try {
      MarketAssetInfo info = marketDataService.validateAndGet(assetSymbol);
      return AssetInfoResponse.fromDomain(info);
    } catch (UnknownSymbolException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Symbol not supported: " + symbol);
    }
  }

  @Operation(summary = "Get asset metadata", description = "Returns slow-changing data (sector, type, currency). DB-cached for 7 days.")
  @GetMapping("/info/{symbol}")
  public AssetInfoResponse getAssetInfo(@PathVariable @NotBlank String symbol) {
    AssetSymbol assetSymbol = parseSymbol(symbol);
    return marketDataService.getAssetInfo(assetSymbol)
        .map(AssetInfoResponse::fromDomain)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset info not found."));
  }

  @Operation(summary = "Get batch quotes", description = "Primary endpoint for portfolio views. 20 symbol limit. " +
      "Uses Redis MGET; fans out to FMP on cache miss. Rate limit: 10/min.")
  @PostMapping("/quotes/batch")
  public Map<String, MarketQuoteResponse> getBatchQuotes(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of symbols (max 20)") @Valid @RequestBody BatchQuoteRequest request) {

    Set<AssetSymbol> symbols = request.symbols().stream()
        .map(this::tryParseSymbol)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    if (symbols.isEmpty())
      return Map.of();

    return marketDataService.getBatchQuotes(symbols).entrySet().stream().collect(
        Collectors.toMap(e -> e.getKey().symbol(), e -> MarketQuoteResponse.fromDomain(e.getValue())));
  }

  @Operation(summary = "Get single quote", description = "Redis-cached (5m TTL). Returns 404 if symbol has no historical transaction.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Quote retrieved"),
      @ApiResponse(responseCode = "404", description = "No quote found or asset unseeded")
  })
  @GetMapping("/quotes/{symbol}")
  public MarketQuoteResponse getQuote(@PathVariable @NotBlank String symbol) {
    AssetSymbol assetSymbol = parseSymbol(symbol);
    Map<AssetSymbol, MarketAssetQuote> quotes = marketDataService.getBatchQuotes(Set.of(assetSymbol));

    MarketAssetQuote quote = quotes.get(assetSymbol);
    if (quote == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record a transaction first to seed the asset.");
    }

    return MarketQuoteResponse.fromDomain(quote);
  }

  private AssetSymbol parseSymbol(String symbol) {
    try {
      return new AssetSymbol(symbol);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid symbol format: " + symbol);
    }
  }

  private AssetSymbol tryParseSymbol(String symbol) {
    try {
      return new AssetSymbol(symbol);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}