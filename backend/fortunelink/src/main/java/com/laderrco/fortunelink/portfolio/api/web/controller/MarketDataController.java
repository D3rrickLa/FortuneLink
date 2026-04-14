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
 * Public-facing market data endpoints.ks
 * <p>
 * Rate-limit notes (enforced by RateLimitInterceptor): - /search : lenient -
 * lightweight
 * autocomplete, no external API call if cached - /quotes : moderate - single
 * symbol, Redis-cached,
 * may fan out to FMP - /batch : strict - each call may burn N FMP quota
 * requests; must be short -
 * /info : lenient - long TTL in DB, rarely hits FMP - /validate : moderate -
 * always hits FMP if
 * symbol is uncached; used pre-transaction
 * <p>
 * All endpoints are authenticated. Market data is never public - it would be
 * trivial to scrape the
 * full symbol list otherwise.
 * <p>
 * The controller deliberately stays thin. No business logic lives here. Shape
 * mapping (domain →
 * response DTO) is done inline for now; if it grows, extract a
 * MarketDataResponseMapper.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/market")
public class MarketDataController {
  private final MarketDataService marketDataService;

  // -------------------------------------------------------------------------
  // Symbol search - autocomplete for the UI "add transaction" flow
  // -------------------------------------------------------------------------

  /**
   * Searches for tradeable symbols matching a query string.
   * <p>
   * This is the UI autocomplete endpoint. It returns shallow results only - name,
   * exchange, trading
   * currency. Full asset info requires /info/{symbol}.
   * <p>
   * Do NOT use this to validate a symbol before recording a transaction. Use
   * /validate/{symbol} for
   * that - it has different caching semantics and guarantees the symbol is
   * actually known to the
   * backend.
   * <p>
   * Rate limit: lenient (30/min). Results are not cached by this layer; the
   * client should debounce
   * aggressively (300ms minimum).
   */
  @GetMapping("/search")
  public List<SymbolSearchResponse> searchSymbols(
      @RequestParam @Size(min = 1, max = 50) String query) {

    if (query.isBlank()) {
      return List.of();
    }

    List<SymbolSearchResult> results = marketDataService.searchSymbols(query.trim());
    return results.stream().map(SymbolSearchResponse::fromDomain).toList();
  }

  // -------------------------------------------------------------------------
  // Single quote - used when displaying a specific position's current value
  // -------------------------------------------------------------------------

  /**
   * Returns the current market quote for a single symbol.
   * <p>
   * This is Redis-cached with a 5-minute TTL. If the cache is cold, this will
   * fire one FMP API
   * call. Clients should prefer /batch when loading a full account or portfolio
   * view.
   * <p>
   * Returns 404 if the symbol has no known trading currency in the DB (i.e. no
   * transaction has ever
   * been recorded for it). This is intentional - you cannot display a meaningful
   * price without a
   * known account currency for conversion.
   */
  @GetMapping("/quotes/{symbol}")
  public MarketQuoteResponse getQuote(@PathVariable @NotBlank String symbol) {
    AssetSymbol assetSymbol;
    try {
      assetSymbol = new AssetSymbol(symbol);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid symbol format: " + symbol);
    }

    Map<AssetSymbol, MarketAssetQuote> quotes = marketDataService.getBatchQuotes(
        Set.of(assetSymbol));

    MarketAssetQuote quote = quotes.get(assetSymbol);
    if (quote == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No quote available for symbol: " + symbol
          + ". Record a transaction first to seed the asset cache.");
    }

    return MarketQuoteResponse.fromDomain(quote);
  }

  // -------------------------------------------------------------------------
  // Batch quotes - primary endpoint for portfolio / account page loads
  // -------------------------------------------------------------------------

  /**
   * Returns quotes for a set of symbols in a single round-trip.
   * <p>
   * This is the endpoint the frontend should call when rendering portfolio or
   * account views. The
   * backend batches Redis lookups (MGET) and fans out to FMP only for cache
   * misses.
   * <p>
   * Hard limit: 20 symbols per request. The FMP free tier burns 1 quota request
   * per symbol on a
   * cold cache. Enforced here, not just documented.
   * <p>
   * Symbols not found in the cache and not supported by FMP are silently excluded
   * from the
   * response. The caller must handle partial results.
   * <p>
   * Rate limit: strict (10/min). Use /quotes/{symbol} for single-symbol polling;
   * batch is for bulk
   * page loads only.
   */
  @PostMapping("/quotes/batch")
  public Map<String, MarketQuoteResponse> getBatchQuotes(@RequestBody BatchQuoteRequest request) {
    if (request.symbols().size() > 20) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Batch quote requests are limited to 20 symbols. Got: " + request.symbols().size());
    }

    Set<AssetSymbol> symbols = request.symbols().stream().map(s -> {
      try {
        return new AssetSymbol(s);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }).filter(Objects::nonNull).collect(Collectors.toSet());

    if (symbols.isEmpty()) {
      return Map.of();
    }

    return marketDataService.getBatchQuotes(symbols).entrySet().stream().collect(
        Collectors.toMap(e -> e.getKey().symbol(),
            e -> MarketQuoteResponse.fromDomain(e.getValue())));
  }

  // -------------------------------------------------------------------------
  // Asset info - slow-changing metadata, long TTL
  // -------------------------------------------------------------------------

  /**
   * Returns descriptive metadata for a symbol: name, exchange, sector, asset
   * type, trading
   * currency.
   * <p>
   * Cached in DB (market_asset_info) with a 7-day TTL. Very rarely hits the
   * external API. Use this
   * to populate the "asset details" panel in the UI after a user has recorded a
   * transaction for the
   * symbol.
   * <p>
   * Returns 404 if the symbol is completely unknown (has never been validated).
   */
  @GetMapping("/info/{symbol}")
  public AssetInfoResponse getAssetInfo(@PathVariable @NotBlank String symbol) {
    AssetSymbol assetSymbol;
    try {
      assetSymbol = new AssetSymbol(symbol);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid symbol format: " + symbol);
    }

    return marketDataService.getAssetInfo(assetSymbol).map(AssetInfoResponse::fromDomain)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No asset information found for symbol: " + symbol));
  }

  // -------------------------------------------------------------------------
  // Validate + seed - must be called before recording a BUY transaction
  // -------------------------------------------------------------------------

  /**
   * Validates that a symbol is real and seeds it into the asset info cache.
   * <p>
   * This endpoint MUST be called from the frontend before the user submits a BUY
   * transaction. It:
   * 1. Checks market_asset_info DB cache. 2. If not found, calls FMP
   * /profile/{symbol}. 3. If FMP
   * returns data, seeds the cache and returns the info. 4. If FMP returns
   * nothing, returns 404.
   * <p>
   * This is the contract that makes TransactionService.recordPurchase() safe to
   * call - by the time
   * the user submits the form, the symbol is known. The TransactionService still
   * calls
   * validateAndGet() internally but this pre-flight prevents the UI from
   * presenting an unresolvable
   * symbol to the user.
   * <p>
   * Rate limit: moderate (20/min). Each cold call burns one FMP quota request.
   * The frontend should
   * only call this once per symbol per session (cache client-side).
   */
  @GetMapping("/validate/{symbol}")
  public AssetInfoResponse validateSymbol(@PathVariable @NotBlank String symbol) {
    AssetSymbol assetSymbol;
    try {
      assetSymbol = new AssetSymbol(symbol);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid symbol format: " + symbol);
    }

    try {
      MarketAssetInfo info = marketDataService.validateAndGet(assetSymbol);
      return AssetInfoResponse.fromDomain(info);
    } catch (UnknownSymbolException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Symbol not found or not supported: " + symbol);
    }
  }
}
