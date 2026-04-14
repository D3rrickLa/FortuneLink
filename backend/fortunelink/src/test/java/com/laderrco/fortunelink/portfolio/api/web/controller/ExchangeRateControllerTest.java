package com.laderrco.fortunelink.portfolio.api.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.laderrco.fortunelink.portfolio.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ExchangeRate;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio.infrastructure.config.limiting.RateLimitInterceptor;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions.BocApiException;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions.ExchangeRateUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ExchangeRateController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExchangeRateControllerTest {

  private static final String BASE_URL = "/api/v1/exchange-rates";

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  ExchangeRateService exchangeRateService;

  @MockitoBean
  AuthenticationUserService authenticationUserService;
  @MockitoBean
  RateLimitInterceptor rateLimitInterceptor;

  @BeforeEach
  void setUp() throws Exception {
    when(rateLimitInterceptor.preHandle(any(HttpServletRequest.class),
        any(HttpServletResponse.class), any())).thenReturn(true);
  }

  private ExchangeRate buildRate(String from, String to, String rate) {
    return new ExchangeRate(Currency.of(from), Currency.of(to), new BigDecimal(rate),
        Instant.now());
  }

  @Nested
  @DisplayName("GET /current — getCurrentRate")
  class GetCurrentRate {

    @Test
    @DisplayName("200 with exchange rate when currencies are different")
    void returns200WithRate() throws Exception {
      when(exchangeRateService.getRate(any(Currency.class), any(Currency.class))).thenReturn(
          Optional.of(buildRate("USD", "CAD", "1.36")));

      mockMvc.perform(get(BASE_URL + "/current").param("from", "USD").param("to", "CAD"))
          .andExpect(status().isOk()).andExpect(jsonPath("$.from").value("USD"))
          .andExpect(jsonPath("$.to").value("CAD")).andExpect(jsonPath("$.rate").value(1.36))
          .andExpect(jsonPath("$.isIdentity").value(false));
    }

    @Test
    @DisplayName("200 with identity rate (rate=1, isIdentity=true) when from == to")
    void returns200WithIdentityRateForSameCurrency() throws Exception {
      mockMvc.perform(get(BASE_URL + "/current").param("from", "CAD").param("to", "CAD"))
          .andExpect(status().isOk()).andExpect(jsonPath("$.from").value("CAD"))
          .andExpect(jsonPath("$.to").value("CAD")).andExpect(jsonPath("$.rate").value(1))
          .andExpect(jsonPath("$.isIdentity").value(true));

      verifyNoInteractions(exchangeRateService);
    }

    @Test
    @DisplayName("404 when service returns empty Optional (pair unsupported by BOC)")
    void returns404WhenRateUnavailable() throws Exception {
      when(exchangeRateService.getRate(any(Currency.class), any(Currency.class))).thenReturn(
          Optional.empty());

      mockMvc.perform(get(BASE_URL + "/current").param("from", "USD").param("to", "CAD"))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("404 when ExchangeRateUnavailableException is thrown")
    void returns404OnExchangeRateUnavailableException() throws Exception {
      when(exchangeRateService.getRate(any(Currency.class), any(Currency.class))).thenThrow(
          new ExchangeRateUnavailableException("USD", "AED", Instant.now()));

      mockMvc.perform(get(BASE_URL + "/current").param("from", "USD").param("to", "AED"))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("503 when BOC API is unreachable")
    void returns503WhenBocApiDown() throws Exception {
      when(exchangeRateService.getRate(any(Currency.class), any(Currency.class))).thenThrow(
          new BocApiException("Connection refused"));

      mockMvc.perform(get(BASE_URL + "/current").param("from", "USD").param("to", "CAD"))
          .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("400 when 'from' currency code is unrecognised by Currency.of()")
    void returns400ForUnrecognisedCurrency() throws Exception {
      // Currency.of() will throw IllegalArgumentException for an unknown code,
      // which GlobalExceptionHandler maps to 400.
      mockMvc.perform(get(BASE_URL + "/current").param("from", "XYZ").param("to", "CAD"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cross-currency triangulation (EUR→USD) returns 200")
    void returns200ForCrossCurrencyPair() throws Exception {
      when(exchangeRateService.getRate(any(Currency.class), any(Currency.class))).thenReturn(
          Optional.of(buildRate("EUR", "USD", "1.08")));

      mockMvc.perform(get(BASE_URL + "/current").param("from", "EUR").param("to", "USD"))
          .andExpect(status().isOk()).andExpect(jsonPath("$.from").value("EUR"))
          .andExpect(jsonPath("$.to").value("USD"));
    }
  }

  @Nested
  @DisplayName("GET /supported — getSupportedCurrencies")
  class GetSupportedCurrencies {

    @Test
    @DisplayName("200 with a non-empty list of currency codes")
    void returns200WithCurrencyList() throws Exception {
      mockMvc.perform(get(BASE_URL + "/supported")).andExpect(status().isOk())
          .andExpect(jsonPath("$.currencies").isArray())
          .andExpect(jsonPath("$.currencies.length()").value(25));
    }

    @Test
    @DisplayName("Response includes CAD and USD")
    void listIncludesMajorCurrencies() throws Exception {
      mockMvc.perform(get(BASE_URL + "/supported")).andExpect(status().isOk())
          .andExpect(jsonPath("$.currencies[?(@ == 'CAD')]").exists())
          .andExpect(jsonPath("$.currencies[?(@ == 'USD')]").exists());
    }
  }
}