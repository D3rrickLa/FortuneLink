package com.laderrco.fortunelink.portfolio.api.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.laderrco.fortunelink.portfolio.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio.application.services.AccountValuationApplicationService;
import com.laderrco.fortunelink.portfolio.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio.application.views.AccountValuationView;
import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUserResolver;
import com.laderrco.fortunelink.portfolio.infrastructure.config.limiting.RateLimitInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountValuationController.class)
@Import(AuthenticatedUserResolver.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountValuationControllerTest {

  private static final UUID USER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID PORTFOLIO_UUID = UUID.fromString(
      "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final UUID ACCOUNT_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

  @MockitoBean
  AuthenticationUserService authenticationUserService;
  @MockitoBean
  RateLimitInterceptor rateLimitInterceptor;
  @MockitoBean
  AccountValuationApplicationService valuationService;

  @Autowired
  MockMvc mockMvc;

  @BeforeEach
  void setUp() throws Exception {
    when(authenticationUserService.getCurrentUser()).thenReturn(USER_UUID);
    when(rateLimitInterceptor.preHandle(any(HttpServletRequest.class),
        any(HttpServletResponse.class), any())).thenReturn(true);
  }

  @Test
  void shouldReturnAccountValuation() throws Exception {
    AccountValuationView view = new AccountValuationView(Money.of(15000, "CAD"),
        Money.of(10000, "CAD"), Money.of(5000, "CAD"), BigDecimal.valueOf(50),
        Money.of(2000, "CAD"), Money.of(13000, "CAD"), Currency.of("CAD"));

    when(valuationService.computeAccountValuation(any())).thenReturn(view);

    mockMvc.perform(
            get("/api/v1/portfolios/{portfolioId}/accounts/{accountId}/valuation", PORTFOLIO_UUID,
                ACCOUNT_UUID).with(user(USER_UUID.toString()))).andDo(print())
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.currency").value("CAD"))
        .andExpect(jsonPath("$.gainLossPercent").value(50))
        .andExpect(jsonPath("$.totalValue.amount").value(15000))
        .andExpect(jsonPath("$.totalValue.currency").value("CAD"))
        .andExpect(jsonPath("$.totalCostBasis.amount").value(10000))
        .andExpect(jsonPath("$.unrealizedGainLoss.amount").value(5000))
        .andExpect(jsonPath("$.cashBalance.amount").value(2000))
        .andExpect(jsonPath("$.investedValue.amount").value(13000));
  }

  @Test
  void shouldReturn404WhenAccountNotFound() throws Exception {
    when(valuationService.computeAccountValuation(any())).thenThrow(
        new AccountNotFoundException(ACCOUNT_UUID.toString()));

    mockMvc.perform(
        get("/api/v1/portfolios/{portfolioId}/accounts/{accountId}/valuation", PORTFOLIO_UUID,
            ACCOUNT_UUID).with(user(USER_UUID.toString()))).andExpect(status().isNotFound());
  }

  @Test
  void shouldVerifyQueryIsConstructedCorrectly() throws Exception {
    AccountValuationView view = new AccountValuationView(Money.of(1000, "CAD"),
        Money.of(800, "CAD"), Money.of(200, "CAD"), BigDecimal.valueOf(25), Money.of(100, "CAD"),
        Money.of(900, "CAD"), Currency.of("CAD"));
    when(valuationService.computeAccountValuation(any())).thenReturn(view);

    mockMvc.perform(
        get("/api/v1/portfolios/{portfolioId}/accounts/{accountId}/valuation", PORTFOLIO_UUID,
            ACCOUNT_UUID).with(user(USER_UUID.toString()))).andExpect(status().isOk());

    ArgumentCaptor<GetAccountSummaryQuery> captor = ArgumentCaptor.forClass(
        GetAccountSummaryQuery.class);
    verify(valuationService).computeAccountValuation(captor.capture());

    GetAccountSummaryQuery query = captor.getValue();
    assertThat(query.portfolioId()).isEqualTo(PortfolioId.fromString(PORTFOLIO_UUID.toString()));
    assertThat(query.accountId()).isEqualTo(AccountId.fromString(ACCOUNT_UUID.toString()));
    assertThat(query.userId()).isEqualTo(new UserId(USER_UUID));
  }
}