package com.laderrco.fortunelink.portfolio.api.web.controller;

import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.application.queries.GetRealizedGainsQuery;
import com.laderrco.fortunelink.portfolio.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio.application.services.RealizedGainsQueryService;
import com.laderrco.fortunelink.portfolio.application.views.RealizedGainsSummaryView;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.infrastructure.config.limiting.RateLimitInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Year;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(controllers = RealizedGainsController.class)
@AutoConfigureMockMvc(addFilters = false)
class RealizedGainsControllerTest {

  private static final UUID USER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final String PORTFOLIO_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
  private static final String ACCOUNT_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";
  private static final String BASE_URL = "/api/v1/portfolios/" + PORTFOLIO_ID + "/accounts/" + ACCOUNT_ID
      + "/realized-gains";

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  RealizedGainsQueryService realizedGainsQueryService;
  @MockitoBean
  AuthenticationUserService authenticationUserService;
  @MockitoBean
  RateLimitInterceptor rateLimitInterceptor;

  @BeforeEach
  void setUp() throws Exception {
    when(authenticationUserService.getCurrentUser()).thenReturn(USER_UUID);
    when(rateLimitInterceptor.preHandle(
        any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
        .thenReturn(true);
  }

  
  
  

  @Nested
  @DisplayName("GET / — getRealizedGains")
  class GetRealizedGains {

    @Test
    @DisplayName("200 with summary for all time when no filters provided")
    void returns200WithAllTimeGains() throws Exception {
      when(realizedGainsQueryService.getRealizedGains(any(GetRealizedGainsQuery.class)))
          .thenReturn(buildSummaryView(null));

      mockMvc.perform(get(BASE_URL))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items").isArray())
          .andExpect(jsonPath("$.currency").value("CAD"))
          .andExpect(jsonPath("$.taxYear").isEmpty());
    }

    @Test
    @DisplayName("200 with year-filtered gains when taxYear is provided")
    void returns200WithYearFilter() throws Exception {
      when(realizedGainsQueryService.getRealizedGains(any(GetRealizedGainsQuery.class)))
          .thenReturn(buildSummaryView(2024));

      mockMvc.perform(get(BASE_URL).param("taxYear", "2024"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.taxYear").value(2024));
    }

    @Test
    @DisplayName("200 with symbol-filtered gains when symbol is provided")
    void returns200WithSymbolFilter() throws Exception {
      when(realizedGainsQueryService.getRealizedGains(any(GetRealizedGainsQuery.class)))
          .thenReturn(buildSummaryView(null));

      mockMvc.perform(get(BASE_URL).param("symbol", "AAPL"))
          .andExpect(status().isOk());

      ArgumentCaptor<GetRealizedGainsQuery> captor = ArgumentCaptor.forClass(GetRealizedGainsQuery.class);
      verify(realizedGainsQueryService).getRealizedGains(captor.capture());
      assertThat(captor.getValue().symbol().symbol()).isEqualTo("AAPL");
    }

    @Test
    @DisplayName("200 with both taxYear and symbol filters combined")
    void returns200WithCombinedFilters() throws Exception {
      when(realizedGainsQueryService.getRealizedGains(any(GetRealizedGainsQuery.class)))
          .thenReturn(buildSummaryView(2024));

      mockMvc.perform(get(BASE_URL)
          .param("taxYear", "2024")
          .param("symbol", "AAPL"))
          .andExpect(status().isOk());

      ArgumentCaptor<GetRealizedGainsQuery> captor = ArgumentCaptor.forClass(GetRealizedGainsQuery.class);
      verify(realizedGainsQueryService).getRealizedGains(captor.capture());
      assertThat(captor.getValue().taxYear()).isEqualTo(2024);
      assertThat(captor.getValue().symbol().symbol()).isEqualTo("AAPL");
    }

    @Test
    @DisplayName("400 when taxYear is before 2000 (MIN_TAX_YEAR)")
    void returns400ForTaxYearTooEarly() throws Exception {
      mockMvc.perform(get(BASE_URL).param("taxYear", "1999"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(realizedGainsQueryService);
    }

    @Test
    @DisplayName("400 when taxYear is in the future")
    void returns400ForFutureTaxYear() throws Exception {
      int nextYear = Year.now().getValue() + 1;

      mockMvc.perform(get(BASE_URL).param("taxYear", String.valueOf(nextYear)))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(realizedGainsQueryService);
    }

    @Test
    @DisplayName("400 when symbol contains lowercase characters")
    void returns400ForLowercaseSymbol() throws Exception {
      
      mockMvc.perform(get(BASE_URL).param("symbol", "aapl"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(realizedGainsQueryService);
    }

    @Test
    @DisplayName("GetRealizedGainsQuery carries the correct IDs")
    void queryCarriesCorrectIds() throws Exception {
      when(realizedGainsQueryService.getRealizedGains(any(GetRealizedGainsQuery.class)))
          .thenReturn(buildSummaryView(null));

      ArgumentCaptor<GetRealizedGainsQuery> captor = ArgumentCaptor.forClass(GetRealizedGainsQuery.class);

      mockMvc.perform(get(BASE_URL))
          .andExpect(status().isOk());

      verify(realizedGainsQueryService).getRealizedGains(captor.capture());
      GetRealizedGainsQuery q = captor.getValue();
      assertThat(q.portfolioId()).isEqualTo(PortfolioId.fromString(PORTFOLIO_ID));
      assertThat(q.userId().id()).isEqualTo(USER_UUID);
    }

    @Test
    @DisplayName("404 when portfolio or account not found for this user")
    void returns404WhenNotFound() throws Exception {
      when(realizedGainsQueryService.getRealizedGains(any(GetRealizedGainsQuery.class)))
          .thenThrow(new PortfolioNotFoundException(PortfolioId.fromString(PORTFOLIO_ID)));

      mockMvc.perform(get(BASE_URL))
          .andExpect(status().isNotFound());
    }

    @Test
    void getRealizedGainsWithFutureTaxYearShouldReturnBadRequest() throws Exception {
      int futureYear = Year.now().getValue() + 1;

      mockMvc.perform(get(BASE_URL)
          .param("taxYear", String.valueOf(futureYear))
          .with(user("test")))
          .andExpect(status().isBadRequest());

      
      verifyNoInteractions(realizedGainsQueryService);
    }

    @Test
    void getRealizedGains_WithBlankSymbol_ShouldPassNullAssetSymbol() throws Exception {
      when(realizedGainsQueryService.getRealizedGains(any()))
          .thenReturn(buildSummaryView(2025));

      
      mockMvc.perform(get(BASE_URL)
          .with(user("test")))
          .andExpect(status().isOk());

      ArgumentCaptor<GetRealizedGainsQuery> captor = ArgumentCaptor.forClass(GetRealizedGainsQuery.class);
      verify(realizedGainsQueryService).getRealizedGains(captor.capture());

      assertNull(captor.getValue().symbol());
    }
  }

  
  
  

  private RealizedGainsSummaryView buildSummaryView(Integer taxYear) {
    Currency cad = Currency.of("CAD");
    Money zero = Money.zero(cad);
    return new RealizedGainsSummaryView(
        List.of(), 
        zero, 
        zero, 
        zero, 
        cad,
        taxYear,
        0L, 
        0); 
  }
}