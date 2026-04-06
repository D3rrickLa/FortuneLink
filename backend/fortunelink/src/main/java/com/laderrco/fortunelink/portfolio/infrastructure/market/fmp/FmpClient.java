package com.laderrco.fortunelink.portfolio.infrastructure.market.fmp;

import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpProfileResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpQuoteResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpSearchResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.exceptions.FmpApiException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.type.CollectionType;
import tools.jackson.databind.type.TypeFactory;

/**
 * Low-level HTTP client for FMP API.
 * <p>
 * Responsibilities: - Construct FMP API URLs - Execute HTTP requests - Parse
 * JSON responses -
 * Handle HTTP errors
 *
 */
@Slf4j
@Component
public class FmpClient {
  private final FmpClientConfig config;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  FmpClient(FmpClientConfig config, @Qualifier("defaultObjectMapper") ObjectMapper objectMapper,
      @Qualifier("fmpHttpClient") HttpClient httpClient) {
    this.config = config;
    this.objectMapper = objectMapper;
    this.httpClient = httpClient;
    // Validation is handled via @PostConstruct in the config class usually,
    // but keeping it here as per your current setup.
    config.validate();
  }

  public FmpQuoteResponse getQuote(String symbol) {
    // FMP Quote endpoint is actually /quote/SYMBOL
    String url = buildUrl("/quote/" + symbol);
    return executeAndParseSingle(url, FmpQuoteResponse.class);
  }

  public List<FmpQuoteResponse> getBatchQuotes(List<String> symbols) {
    if (symbols == null || symbols.isEmpty()) {
      return List.of();
    }

    // Sequential lookups for Free Tier
    log.info("Fetching {} quotes sequentially (FMP Free Tier)", symbols.size());
    return symbols.stream().map(this::getQuote).filter(Objects::nonNull).toList();
  }

  public FmpProfileResponse getProfile(String symbol) {
    String url = buildUrl("/profile/" + symbol);
    return executeAndParseSingle(url, FmpProfileResponse.class);
  }

  public List<FmpProfileResponse> getBatchProfiles(List<String> symbols) {
    if (symbols == null || symbols.isEmpty()) {
      return List.of();
    }

    log.info("Fetching {} info sequentially (FMP Free Tier)", symbols.size());
    return symbols.stream().map(this::getProfile).filter(Objects::nonNull).toList();
  }

  public List<FmpSearchResponse> getSearch(String query) {
    String url = UriComponentsBuilder.fromUriString(config.getBaseUrl()).path("/search")
        .queryParam("query", query).queryParam("limit", 10).queryParam("apikey", config.getApiKey())
        .build().toUriString();

    return executeAndParseList(url, FmpSearchResponse.class);
  }

  /**
   * Specifically handles the FMP "Array Wrapper" quirk for single objects.
   */
  private <T> T executeAndParseSingle(String url, Class<T> responseType) {
    List<T> results = executeAndParseList(url, responseType);
    return (results == null || results.isEmpty()) ? null : results.get(0);
  }

  /**
   * The core execution logic for Jackson 3
   */
  private <T> List<T> executeAndParseList(String url, Class<T> responseType) {
    try {
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
          .timeout(Duration.ofSeconds(config.getTimeoutSeconds())).GET().build();

      if (config.isDebugLogging()) {
        log.debug("FMP Request: GET {}", url);
      }

      HttpResponse<String> response = httpClient.send(request,
          HttpResponse.BodyHandlers.ofString());
      handleErrorResponse(response, url);

      TypeFactory tf = objectMapper.getTypeFactory();
      CollectionType listType = tf.constructCollectionType(List.class, responseType);

      return objectMapper.readValue(response.body(), listType);

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new FmpApiException("Request interrupted", e);
    } catch (IOException e) {
      if (e instanceof java.net.SocketTimeoutException) {
        throw new FmpApiException("FMP API request timed out for: " + url, e);
      }
      throw new FmpApiException("Failed to communicate with FMP API: " + e.getMessage(), e);
    }
  }

  private void handleErrorResponse(HttpResponse<String> response, String url) {
    int code = response.statusCode();
    if (code == 200) {
      return;
    }

    throw switch (code) {
      case 401 -> new FmpApiException("Invalid FMP API key.");
      case 429 -> new FmpApiException("FMP Rate limit exceeded (250/day).");
      case 404 -> new FmpApiException("Endpoint not found: " + url);
      default -> new FmpApiException("FMP API Error: " + code + " - " + response.body());
    };
  }

  private String buildUrl(String path) {
    String sanitizedPath = path.startsWith("/") ? path.substring(1) : path;
    String sanitizedBase = config.getBaseUrl().endsWith("/") ? config.getBaseUrl() : config.getBaseUrl() + "/";

    return UriComponentsBuilder.fromUriString(sanitizedBase).path(sanitizedPath)
        .queryParam("apikey", config.getApiKey()).build().toUriString();
  }
}