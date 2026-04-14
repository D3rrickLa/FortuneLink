package com.laderrco.fortunelink.portfolio.api.web.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.laderrco.fortunelink.portfolio.api.web.dto.SymbolSearchResult;
import com.laderrco.fortunelink.portfolio.api.web.dto.requests.BatchQuoteRequest;
import com.laderrco.fortunelink.portfolio.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.PercentageChange;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.infrastructure.config.limiting.RateLimitInterceptor;
import com.laderrco.fortunelink.portfolio.infrastructure.exceptions.UnknownSymbolException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(controllers = MarketDataController.class)
@AutoConfigureMockMvc(addFilters = false)
class MarketDataControllerTest {

  private static final String BASE_URL = "/api/v1/market";

  @Autowired
  MockMvc mockMvc;

  @Autowired
  JsonMapper objectMapper;

  @MockitoBean
  MarketDataService marketDataService;

  @MockitoBean
  AuthenticationUserService authenticationUserService;

  @MockitoBean
  RateLimitInterceptor rateLimitInterceptor;

  @BeforeEach
  void setUp() throws Exception {
    when(rateLimitInterceptor.preHandle(any(HttpServletRequest.class),
        any(HttpServletResponse.class), any())).thenReturn(true);
  }

  @Test
  @DisplayName("200 with empty map when no valid symbols provided")
  void returnsEmptyMapWhenNoValidSymbols() throws Exception {

    BatchQuoteRequest request = new BatchQuoteRequest(List.of(" ", "invalid!"));

    mockMvc.perform(post(BASE_URL + "/quotes/batch").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());

    verifyNoInteractions(marketDataService);
  }

  private MarketAssetQuote buildQuote(String symbol, String price) {
    Currency usd = Currency.of("USD");
    Price p = Price.of(new BigDecimal(price), usd);
    return new MarketAssetQuote(new AssetSymbol(symbol), p, p, p, p, p,
        new PercentageChange(BigDecimal.ZERO), BigDecimal.ZERO, null, null, "FMP", Instant.now());
  }

  @Nested
  @DisplayName("GET /search")
  class Search {

    @Test
    @DisplayName("200 with matching results when query is valid")
    void returns200WithResults() throws Exception {
      when(marketDataService.searchSymbols("AAPL")).thenReturn(List.of(
          new SymbolSearchResult(new AssetSymbol("AAPL"), "Apple Inc.", "NASDAQ",
              Currency.of("USD"))));

      mockMvc.perform(get(BASE_URL + "/search").param("query", "AAPL")).andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].symbol").value("AAPL"));
    }

    @Test
    @DisplayName("200 with empty list when query is blank")
    void returns200WhenQueryIsBlank() throws Exception {
      mockMvc.perform(get(BASE_URL + "/search").param("query", "   ").with(jwt()))
          .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

      verifyNoInteractions(marketDataService);
    }

    @Test
    @DisplayName("400 when request exceeds 20 symbols")
    void returns400WhenTooManySymbols() throws Exception {
      List<String> tooMany = Collections.nCopies(21, "AAPL");
      BatchQuoteRequest request = new BatchQuoteRequest(tooMany);

      mockMvc.perform(post(BASE_URL + "/quotes/batch").contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message", containsString("limited to 20 symbols")));
    }

    @Test
    @DisplayName("200 but filters out invalid symbols via try-catch")
    void filtersOutInvalidSymbolsInBatch() throws Exception {
      BatchQuoteRequest request = new BatchQuoteRequest(List.of("!!", "MSFT"));

      AssetSymbol msft = new AssetSymbol("MSFT");
      when(marketDataService.getBatchQuotes(Set.of(msft))).thenReturn(
          Map.of(msft, buildQuote("MSFT", "400.00")));

      mockMvc.perform(post(BASE_URL + "/quotes/batch").contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))).andDo(print())
          .andExpect(status().isOk()).andExpect(jsonPath("$.MSFT").exists())
          .andExpect(jsonPath("$.length()").value(1));

      verify(marketDataService).getBatchQuotes(Set.of(msft));
    }

    @Test
    @DisplayName("200 with empty map when no valid symbols provided")
    void returnsEmptyMapWhenNoValidSymbols() throws Exception {

      BatchQuoteRequest request = new BatchQuoteRequest(List.of(" ", "invalid!"));

      mockMvc.perform(post(BASE_URL + "/quotes/batch").contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
          .andExpect(jsonPath("$").isEmpty());

      verifyNoInteractions(marketDataService);
    }
  }

  @Nested
  @DisplayName("POST /quotes/batch")
  class GetBatchQuotes {

    @Test
    @DisplayName("400 when more than 20 symbols requested")
    void returns400WhenTooManySymbols() throws Exception {

      List<String> tooManySymbols = Collections.nCopies(21, "AAPL");
      BatchQuoteRequest request = new BatchQuoteRequest(tooManySymbols);

      mockMvc.perform(post(BASE_URL + "/quotes/batch").contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())

          .andExpect(jsonPath("$.message", containsString("limited to 20 symbols")))
          .andExpect(jsonPath("$.message", containsString("Got: 21")));

      verifyNoInteractions(marketDataService);
    }

    @Test
    @DisplayName("200 but filters out invalid symbols via try-catch")
    void filtersOutInvalidSymbolsInBatch() throws Exception {
      BatchQuoteRequest request = new BatchQuoteRequest(List.of("!!INVALID!!", "MSFT"));
      AssetSymbol msft = new AssetSymbol("MSFT");

      when(marketDataService.getBatchQuotes(Set.of(msft))).thenReturn(
          Map.of(msft, buildQuote("MSFT", "400.00")));

      mockMvc.perform(post(BASE_URL + "/quotes/batch").contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
          .andExpect(jsonPath("$.MSFT").exists()).andExpect(jsonPath("$.length()").value(1));
    }
  }

  @Nested
  @DisplayName("GET /quotes/{symbol}")
  class GetQuote {

    @Test
    @DisplayName("200 with quote data when symbol exists")
    void returns200WithQuote() throws Exception {
      AssetSymbol symbol = new AssetSymbol("AAPL");
      when(marketDataService.getBatchQuotes(anySet())).thenReturn(
          Map.of(symbol, buildQuote("AAPL", "185.50")));

      mockMvc.perform(get(BASE_URL + "/quotes/AAPL")).andExpect(status().isOk())
          .andExpect(jsonPath("$.symbol").value("AAPL"));
    }

    @Test
    @DisplayName("400 for malformed symbol")
    void returns400ForMalformedSymbol() throws Exception {
      mockMvc.perform(get(BASE_URL + "/quotes/bad symbol!")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("404 when service returns empty map (quote == null logic)")
    void returns404WhenQuoteNotFound() throws Exception {
      AssetSymbol assetSymbol = new AssetSymbol("AAPL");

      when(marketDataService.getBatchQuotes(Set.of(assetSymbol))).thenReturn(Map.of());

      mockMvc.perform(get(BASE_URL + "/quotes/AAPL")).andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message", containsString("No quote available for symbol")));
    }
  }

  @Nested
  @DisplayName("GET /info/{symbol}")
  class GetAssetInfo {

    @Test
    @DisplayName("200 when info exists")
    void returns200WhenInfoExists() throws Exception {
      AssetSymbol symbol = new AssetSymbol("AAPL");
      MarketAssetInfo info = new MarketAssetInfo(symbol, "Apple Inc.", AssetType.STOCK, "NASDAQ",
          Currency.of("USD"), "Tech", "Desc");

      when(marketDataService.getAssetInfo(symbol)).thenReturn(Optional.of(info));

      mockMvc.perform(get(BASE_URL + "/info/AAPL")).andExpect(status().isOk())
          .andExpect(jsonPath("$.name").value("Apple Inc."));
    }

    @Test
    @DisplayName("404 when info is missing")
    void returns404WhenInfoMissing() throws Exception {
      when(marketDataService.getAssetInfo(any())).thenReturn(Optional.empty());

      mockMvc.perform(get(BASE_URL + "/info/UNKNOWN")).andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message", containsString("No asset information found")));
    }
  }

  @Nested
  @DisplayName("GET /validate/{symbol}")
  class ValidateSymbol {

    @Test
    @DisplayName("200 when validation succeeds")
    void returns200OnValidSymbol() throws Exception {
      AssetSymbol symbol = new AssetSymbol("AAPL");
      MarketAssetInfo info = new MarketAssetInfo(symbol, "Apple Inc.", AssetType.STOCK, "NASDAQ",
          Currency.of("USD"), "Tech", "Desc");

      when(marketDataService.validateAndGet(symbol)).thenReturn(info);

      mockMvc.perform(get(BASE_URL + "/validate/AAPL")).andExpect(status().isOk())
          .andExpect(jsonPath("$.symbol").value("AAPL"));
    }

    @Test
    @DisplayName("404 when UnknownSymbolException is thrown")
    void returns404OnUnknownSymbol() throws Exception {
      AssetSymbol symbol = new AssetSymbol("FAKE");
      when(marketDataService.validateAndGet(symbol)).thenThrow(
          new UnknownSymbolException("Symbol not found"));

      mockMvc.perform(get(BASE_URL + "/validate/FAKE")).andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message", containsString("Symbol not found or not supported")));
    }

    @Test
    @DisplayName("400 when symbol format is invalid (IllegalArgumentException)")
    void returns400OnMalformedSymbol() throws Exception {

      mockMvc.perform(get(BASE_URL + "/validate/bad sym!")).andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message", containsString("Invalid symbol format")));
    }
  }
}