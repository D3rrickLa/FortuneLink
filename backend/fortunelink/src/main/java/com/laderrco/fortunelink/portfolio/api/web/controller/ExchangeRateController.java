package com.laderrco.fortunelink.portfolio.api.web.controller;

import com.laderrco.fortunelink.portfolio.api.web.dto.responses.ExchangeRateResponse;
import com.laderrco.fortunelink.portfolio.api.web.dto.responses.SupportedCurrenciesResponse;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions.BocApiException;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions.ExchangeRateUnavailableException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/exchange-rates")
@Tag(name = "Exchange Rates", description = "Read-only currency conversion data for display purposes")
public class ExchangeRateController {
  private static final String ISO_CURRENCY_PATTERN = "^[A-Z]{3}$";
  private final ExchangeRateService exchangeRateService;

  @GetMapping("/current")
  @Operation(summary = "Get current exchange rate", description = "Returns the rate for '1 {from} = X {to}'. Rates are sourced from Bank of Canada and cached for 1 hour. Cross-pairs (e.g. EUR/USD) are computed via CAD triangulation.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Successful rate lookup"),
    @ApiResponse(responseCode = "400", description = "Invalid ISO-4217 currency code format"),
    @ApiResponse(responseCode = "404", description = "Currency pair not supported by source"),
    @ApiResponse(responseCode = "503", description = "Source API (BOC) unreachable - client should use cost-basis fallback")
  })
  public ExchangeRateResponse getCurrentRate(
      @RequestParam @Parameter(description = "Base currency code (e.g., USD)", example = "USD") @NotBlank @Pattern(regexp = ISO_CURRENCY_PATTERN) String from,

      @RequestParam @Parameter(description = "Target currency code (e.g., CAD)", example = "CAD") @NotBlank @Pattern(regexp = ISO_CURRENCY_PATTERN) String to) {

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
          .map(ExchangeRateResponse::fromDomain).orElseThrow(
              () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                  "Exchange rate not available for: " + from + "/" + to));

    } catch (ExchangeRateUnavailableException e) {
      log.warn("Exchange rate unavailable for {}/{}: {}", from, to, e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Exchange rate not available for: " + from + "/" + to
              + ". The Bank of Canada may not publish this pair.");

    } catch (BocApiException e) {
      log.error("BOC API error fetching {}/{}: {}", from, to, e.getMessage());
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Exchange rate service is temporarily unavailable.");
    }
  }

  @GetMapping("/supported")
  @Operation(summary = "List supported currencies", description = "Returns all ISO-4217 codes available for exchange rate lookup via the Bank of Canada.")
  public SupportedCurrenciesResponse getSupportedCurrencies() {
    return new SupportedCurrenciesResponse(
        java.util.List.of("AUD", "BRL", "CAD", "CHF", "CNY", "DKK", "EUR", "GBP", "HKD", "IDR",
            "INR", "JPY", "KRW", "MXN", "MYR", "NOK", "NZD", "PEN", "SAR", "SEK", "SGD", "THB",
            "TRY", "USD", "ZAR"));
  }
}