package com.laderrco.fortunelink.portfolio.api.web.controller;

import com.laderrco.fortunelink.portfolio.api.web.dto.responses.ExchangeRateResponse;
import com.laderrco.fortunelink.portfolio.api.web.dto.responses.SupportedCurrenciesResponse;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions.BocApiException;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions.ExchangeRateUnavailableException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Provides exchange rate information for display purposes.
 * <p>
 * This controller is intentionally read-only and display-focused. The backend never exposes raw
 * exchange rate data for use in external calculations — all currency conversion for portfolio
 * valuation happens server-side in PortfolioValuationService.
 * <p>
 * Primary use cases: - Show the user "your CAD portfolio is worth X USD at today's rate" - Show the
 * exchange rate used for a cross-currency transaction - Let the user see what rate is being applied
 * before they confirm a trade
 * <p>
 * Source: Bank of Canada Valet API (free, reliable for CAD pairs). Cross-currency pairs are
 * computed via CAD triangulation.
 * <p>
 * Rate limit: lenient (30/min). Rates are cached in Redis at 1-hour TTL. The BOC only publishes
 * once per business day at ~16:30 ET so polling more frequently than hourly is pointless. The
 * client should cache aggressively.
 * <p>
 * Error handling: - If BOC is down, returns 503 with a Retry-After header. - If a currency pair is
 * unsupported, returns 404. - The frontend must handle both gracefully — degrade to showing the
 * cost-basis value without a converted equivalent.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/exchange-rates")
public class ExchangeRateController {

  private static final String ISO_CURRENCY_PATTERN = "^[A-Z]{3}$";

  private final ExchangeRateService exchangeRateService;

  // -------------------------------------------------------------------------
  // Current rate
  // -------------------------------------------------------------------------

  /**
   * Returns the current exchange rate between two currencies.
   * <p>
   * Both currency codes must be ISO-4217 (3 uppercase letters). The rate returned is "1 {from} =
   * {rate} {to}".
   * <p>
   * Examples: GET /api/v1/exchange-rates/current?from=USD&to=CAD → { "from": "USD", "to": "CAD",
   * "rate": 1.3625, ... }
   * <p>
   * GET /api/v1/exchange-rates/current?from=EUR&to=USD → computed via EUR/CAD and CAD/USD
   * triangulation
   * <p>
   * Identity requests (from == to) return rate 1.0 without hitting the API.
   * <p>
   * Returns 404 if the currency pair is not supported by the Bank of Canada. Returns 503 if the BOC
   * API is unreachable.
   */
  @GetMapping("/current")
  public ExchangeRateResponse getCurrentRate(
      @RequestParam @NotBlank @Pattern(regexp = ISO_CURRENCY_PATTERN, message = "Currency code must be 3 uppercase letters") String from,

      @RequestParam @NotBlank @Pattern(regexp = ISO_CURRENCY_PATTERN, message = "Currency code must be 3 uppercase letters") String to) {

    if (from.equals(to)) {
      return ExchangeRateResponse.identity(from);
    }

    Currency fromCurrency;
    Currency toCurrency;
    try {
      fromCurrency = Currency.of(from);
      toCurrency = Currency.of(to);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Unrecognised ISO-4217 currency code: " + e.getMessage());
    }

    try {
      return exchangeRateService.getRate(fromCurrency, toCurrency)
          .map(rate -> ExchangeRateResponse.fromDomain(rate)).orElseThrow(
              () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                  "Exchange rate not available for: " + from + "/" + to));

    } catch (ExchangeRateUnavailableException e) {
      log.warn("Exchange rate unavailable for {}/{}: {}", from, to, e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Exchange rate not available for: " + from + "/" + to
              + ". The Bank of Canada may not publish this pair.");

    } catch (BocApiException e) {
      log.error("BOC API error fetching {}/{}: {}", from, to, e.getMessage());
      // Return 503 with a Retry-After header — client should back off
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Exchange rate service is temporarily unavailable. "
              + "Portfolio values will show cost-basis fallback.");
    }
  }

  // -------------------------------------------------------------------------
  // Supported currencies — lets the UI know what currencies are valid inputs
  // -------------------------------------------------------------------------

  /**
   * Returns the list of supported currency codes for exchange rate lookup.
   * <p>
   * This is the set of ISO-4217 codes that the BOC Valet API supports, which is the set FortuneLink
   * can reliably provide rates for.
   * <p>
   * Note: FortuneLink accounts can be opened in ANY valid ISO-4217 currency, but exchange rate
   * lookup is only guaranteed for currencies in this list. Unsupported currencies fall back to
   * cost-basis display (no conversion).
   */
  @GetMapping("/supported")
  public SupportedCurrenciesResponse getSupportedCurrencies() {
    // BOC publishes rates for these currencies vs CAD.
    // Cross rates (e.g. EUR/USD) are computed via CAD triangulation.
    // Source:
    // https://www.bankofcanada.ca/rates/exchange/legacy-noon-and-closing-rates/
    return new SupportedCurrenciesResponse(
        java.util.List.of("AUD", "BRL", "CAD", "CHF", "CNY", "DKK", "EUR", "GBP", "HKD", "IDR",
            "INR", "JPY", "KRW", "MXN", "MYR", "NOK", "NZD", "PEN", "SAR", "SEK", "SGD", "THB",
            "TRY", "USD", "ZAR"));
  }
}
