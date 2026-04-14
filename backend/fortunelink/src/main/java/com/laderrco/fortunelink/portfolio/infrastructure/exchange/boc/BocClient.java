package com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc;

import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.dtos.BocExchangeResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions.BocApiException;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions.BocParsingException;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions.ExchangeRateUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class BocClient {

  private final BankOfCanadaClientConfig config;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public BocClient(BankOfCanadaClientConfig config,
      @Qualifier("defaultObjectMapper") ObjectMapper objectMapper,
      @Qualifier("bocHttpClient") HttpClient httpClient) {
    this.config = config;
    this.objectMapper = objectMapper;
    this.httpClient = httpClient;

    log.info("Bank of Canada API Client initialized with base URL: {}", config.getBaseUrl());
    config.validate();
  }

  @Retry(name = "boc-api")
  @CircuitBreaker(name = "boc-api", fallbackMethod = "getLatestRateFallback")
  public BocExchangeResponse getLatestExchangeRate(String to, String from) {
    List<String> series = BocCurrencyPairResolver.resolveSeries(from, to);

    String url = new BocUrlBuilder(config.getBaseUrl()).observations(series.toArray(new String[0]))
        .format("json").recent(1).build();

    try {
      String jsonResponse = executeGetRequest(url);
      if (config.isDebugLogging()) {
        log.debug("BOC Latest Rate Response: {}", jsonResponse);
      }
      return objectMapper.readValue(jsonResponse, BocExchangeResponse.class);
    } catch (JacksonException e) {
      log.error("Network or Parsing error fetching latest rate from BOC for {}/{}", from, to, e);
      throw new BocApiException("Failed to process BOC latest rate request", e);
    }
  }

  @Retry(name = "boc-api")
  @CircuitBreaker(name = "boc-api")
  public BocExchangeResponse getHistoricalExchangeRate(String to, String from, Instant startDate,
      Instant endDate) {
    List<String> series = BocCurrencyPairResolver.resolveSeries(from, to);

    String url = new BocUrlBuilder(config.getBaseUrl()).observations(series.toArray(new String[0]))
        .format("json").startDate(startDate).endDate(endDate).build();

    try {
      String jsonResponse = executeGetRequest(url);
      return objectMapper.readValue(jsonResponse, BocExchangeResponse.class);
    } catch (JacksonException e) {
      log.error("Failed to fetch/parse historical BOC data for series {}", series, e);
      throw new BocParsingException("Malformed or inaccessible data from BOC", e);
    }
  }

  private String executeGetRequest(String url) {
    try {
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
          .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
          .header("Accept", "application/json") // Good practice
          .GET().build();

      HttpResponse<String> response = httpClient.send(request,
          HttpResponse.BodyHandlers.ofString());

      return handleResponse(response, url);

    } catch (IOException e) {
      throw new BocApiException("Connection failed while calling BOC API: " + url, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new BocApiException("BOC API request was interrupted", e);
    }
  }

  private String handleResponse(HttpResponse<String> response, String url) {
    int status = response.statusCode();

    if (status >= 200 && status < 300) {
      return response.body();
    }

    String errorMessage = switch (status) {
      case 400 -> "Invalid inputs for BOC request. Check series name or date format.";
      case 404 -> "BOC API endpoint or series not found: " + url;
      case 500 -> "Bank of Canada server encountered an internal error.";
      default ->
          String.format("BOC API returned unexpected status: %d - %s", status, response.body());
    };

    log.error(errorMessage);
    throw new BocApiException(errorMessage);
  }

  BocExchangeResponse getLatestRateFallback(String to, String from, Throwable t) {
    log.error("BOC circuit open for {}/{}. Exchange rate unavailable.", from, to);
    throw new ExchangeRateUnavailableException(from, to, Instant.now());
  }
}