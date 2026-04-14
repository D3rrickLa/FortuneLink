package com.laderrco.fortunelink.portfolio.api.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.laderrco.fortunelink.portfolio.api.web.dto.requests.UpdatePortfolioRequest;
import com.laderrco.fortunelink.portfolio.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioLimitReachedException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.application.queries.GetNetWorthQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetPortfolioByIdQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetPortfoliosByUserIdQuery;
import com.laderrco.fortunelink.portfolio.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio.application.services.PortfolioLifecycleService;
import com.laderrco.fortunelink.portfolio.application.services.PortfolioQueryService;
import com.laderrco.fortunelink.portfolio.application.views.NetWorthView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioSummaryView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUserResolver;
import com.laderrco.fortunelink.portfolio.infrastructure.config.limiting.RateLimitInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;
// import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(controllers = PortfolioController.class)
@AutoConfigureMockMvc(addFilters = false)
class PortfolioControllerTest {

  private static final UUID USER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final String PORTFOLIO_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
  private static final String BASE_URL = "/api/v1/portfolios";
  private static final String PORTFOLIO_URL = BASE_URL + "/" + PORTFOLIO_ID;

  @Autowired
  MockMvc mockMvc;
  @Autowired
  JsonMapper objectMapper;

  @MockitoBean
  PortfolioLifecycleService lifecycleService;
  @MockitoBean
  PortfolioQueryService queryService;
  @MockitoBean
  AuthenticationUserService authenticationUserService;
  @MockitoBean
  RateLimitInterceptor rateLimitInterceptor;
  @MockitoBean
  AuthenticatedUserResolver authenticatedUserResolver;

  @BeforeEach
  void setUp() throws Exception {
    when(authenticationUserService.getCurrentUser()).thenReturn(USER_UUID);
    when(rateLimitInterceptor.preHandle(any(HttpServletRequest.class),
        any(HttpServletResponse.class), any())).thenReturn(true);

    when(authenticatedUserResolver.supportsParameter(any())).thenAnswer(invocation -> {
      MethodParameter parameter = invocation.getArgument(0);
      return parameter.getParameterType().equals(UserId.class);
    });

    when(authenticatedUserResolver.resolveArgument(any(), any(), any(), any())).thenReturn(
        UserId.fromString(USER_UUID.toString()));
  }

  private String validCreateRequest() {
    return """
        {
            "name": "My Portfolio",
            "currency": "CAD",
            "createDefaultAccount": false
        }
        """;
  }

  private PortfolioView buildPortfolioView() {
    Currency cad = Currency.of("CAD");
    return new PortfolioView(PortfolioId.fromString(PORTFOLIO_ID),
        UserId.fromString(USER_UUID.toString()), "My Portfolio", "A test portfolio", List.of(),
        Money.zero(cad), false, Instant.now(), Instant.now());
  }

  private PortfolioSummaryView buildSummaryView() {
    Currency cad = Currency.of("CAD");
    return new PortfolioSummaryView(PortfolioId.fromString(PORTFOLIO_ID), "My Portfolio",
        Money.zero(cad), Instant.now());
  }

  private NetWorthView buildNetWorthView() {
    Currency cad = Currency.of("CAD");
    Money zero = Money.zero(cad);
    return new NetWorthView(zero, zero, zero, cad, false, false, Instant.now());
  }

  @Nested
  @DisplayName("POST / — createPortfolio")
  class CreatePortfolio {

    @Test
    @DisplayName("201 and returns PortfolioResponse on success")
    void returns201OnSuccess() throws Exception {
      when(lifecycleService.createPortfolio(any())).thenReturn(buildPortfolioView());

      mockMvc.perform(
              post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(validCreateRequest()))
          .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("My Portfolio"))
          .andExpect(jsonPath("$.currency").value("CAD"))
          .andExpect(jsonPath("$.hasStaleData").value(false));
    }

    @Test
    @DisplayName("CreatePortfolioCommand carries userId from JWT and fields from body")
    void commandCarriesCorrectFields() throws Exception {
      when(lifecycleService.createPortfolio(any())).thenReturn(buildPortfolioView());

      mockMvc.perform(
              post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(validCreateRequest()))
          .andExpect(status().isCreated());

      var captor = ArgumentCaptor.forClass(
          com.laderrco.fortunelink.portfolio.application.commands.CreatePortfolioCommand.class);
      verify(lifecycleService).createPortfolio(captor.capture());

      var cmd = captor.getValue();
      assertThat(cmd.userId().id()).isEqualTo(USER_UUID);
      assertThat(cmd.name()).isEqualTo("My Portfolio");
      assertThat(cmd.currency()).isEqualTo(Currency.of("CAD"));
      assertThat(cmd.createDefaultAccount()).isFalse();
    }

    @Test
    @DisplayName("201 with default account when createDefaultAccount=true")
    void creates201WithDefaultAccount() throws Exception {
      when(lifecycleService.createPortfolio(any())).thenReturn(buildPortfolioView());

      String body = """
          {
              "name": "My Portfolio",
              "currency": "CAD",
              "createDefaultAccount": true,
              "defaultAccountType": "TFSA",
              "defaultStrategy": "ACB"
          }
          """;

      mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("400 when name is blank")
    void returns400WhenNameBlank() throws Exception {
      String body = """
          {"name": "", "currency": "CAD", "createDefaultAccount": false}
          """;

      mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(lifecycleService);
    }

    @Test
    @DisplayName("400 when currency is missing")
    void returns400WhenCurrencyMissing() throws Exception {
      String body = """
          {"name": "My Portfolio", "createDefaultAccount": false}
          """;

      mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(lifecycleService);
    }

    @Test
    @DisplayName("409 when user already has an active portfolio (MVP limit)")
    void returns409WhenLimitReached() throws Exception {
      when(lifecycleService.createPortfolio(any())).thenThrow(
          new PortfolioLimitReachedException("User already has an active portfolio"));

      mockMvc.perform(
              post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(validCreateRequest()))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("PORTFOLIO_LIMIT_REACHED"));
    }
  }

  @Nested
  @DisplayName("PATCH /{portfolioId} — updatePortfolio")
  class UpdatePortfolio {

    @Test
    @DisplayName("200 when update succeeds")
    void returns200OnSuccess() throws Exception {
      when(lifecycleService.updatePortfolio(any())).thenReturn(buildPortfolioView());

      mockMvc.perform(patch(PORTFOLIO_URL).contentType(MediaType.APPLICATION_JSON)
              .content("{\"name\": \"Renamed\", \"description\": \"New desc\"}"))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("404 when portfolio does not belong to user")
    void returns404WhenNotFound() throws Exception {
      when(lifecycleService.updatePortfolio(any())).thenThrow(
          new PortfolioNotFoundException(PortfolioId.fromString(PORTFOLIO_ID)));

      mockMvc.perform(patch(PORTFOLIO_URL).contentType(MediaType.APPLICATION_JSON)
              .content("{\"name\": \"Renamed\"}")).andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("PORTFOLIO_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("DELETE /{portfolioId} — deletePortfolio")
  class DeletePortfolio {

    @Test
    @DisplayName("204 on successful soft delete")
    void returns204OnSoftDelete() throws Exception {
      doNothing().when(lifecycleService).deletePortfolio(any());

      mockMvc.perform(delete(PORTFOLIO_URL)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DeletePortfolioCommand defaults to softDelete=true")
    void commandDefaultsToSoftDelete() throws Exception {
      doNothing().when(lifecycleService).deletePortfolio(any());

      var captor = ArgumentCaptor.forClass(
          com.laderrco.fortunelink.portfolio.application.commands.DeletePortfolioCommand.class);

      mockMvc.perform(delete(PORTFOLIO_URL)).andExpect(status().isNoContent());

      verify(lifecycleService).deletePortfolio(captor.capture());

      assertThat(captor.getValue().softDelete()).isTrue();
    }

    @Test
    void updatePortfolioShouldHandleNullCurrency() throws Exception {

      var mockView = new PortfolioView(PortfolioId.newId(), UserId.fromString(USER_UUID.toString()),
          "Name", "desc", List.of(), Money.of(0, Currency.CAD), false, Instant.now(),
          Instant.now());
      when(lifecycleService.updatePortfolio(any())).thenReturn(mockView);

      var request = new UpdatePortfolioRequest("New Name", "Desc", null);

      mockMvc.perform(patch(PORTFOLIO_URL).contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());

      ArgumentCaptor<UpdatePortfolioCommand> captor = ArgumentCaptor.forClass(
          UpdatePortfolioCommand.class);
      verify(lifecycleService).updatePortfolio(captor.capture());

      assertNull(captor.getValue().currency());
    }

    @Test
    @DisplayName("404 when portfolio not found")
    void returns404WhenNotFound() throws Exception {
      doThrow(new PortfolioNotFoundException(PortfolioId.fromString(PORTFOLIO_ID))).when(
          lifecycleService).deletePortfolio(any());

      mockMvc.perform(delete(PORTFOLIO_URL)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deletePortfolioAsUserForcesSoftDelete() throws Exception {
      mockMvc.perform(delete(PORTFOLIO_URL).param("softDelete", "false"))
          .andExpect(status().isNoContent());

      ArgumentCaptor<DeletePortfolioCommand> captor = ArgumentCaptor.forClass(
          DeletePortfolioCommand.class);
      verify(lifecycleService).deletePortfolio(captor.capture());

      assertTrue(captor.getValue().softDelete());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletePortfolioAsAdminRespectsHardDelete() throws Exception {
      mockMvc.perform(delete(PORTFOLIO_URL).param("softDelete", "false"))
          .andExpect(status().isNoContent());

      ArgumentCaptor<DeletePortfolioCommand> captor = ArgumentCaptor.forClass(
          DeletePortfolioCommand.class);
      verify(lifecycleService).deletePortfolio(captor.capture());

      assertFalse(captor.getValue().softDelete());
    }
  }

  @Nested
  @DisplayName("GET / — getPortfolios")
  class GetPortfolios {

    @Test
    @DisplayName("200 with list of portfolio summaries")
    void returns200WithSummaries() throws Exception {
      when(queryService.getPortfolioSummaries(any(GetPortfoliosByUserIdQuery.class))).thenReturn(
          List.of(buildSummaryView()));

      mockMvc.perform(get(BASE_URL)).andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].name").value("My Portfolio"));
    }

    @Test
    @DisplayName("200 with empty list when user has no portfolios")
    void returns200WithEmptyList() throws Exception {
      when(queryService.getPortfolioSummaries(any(GetPortfoliosByUserIdQuery.class))).thenReturn(
          List.of());

      mockMvc.perform(get(BASE_URL)).andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$.length()").value(0));
    }
  }

  @Nested
  @DisplayName("GET /{portfolioId} — getPortfolio")
  class GetPortfolio {

    @Test
    @DisplayName("200 with portfolio detail view")
    void returns200OnSuccess() throws Exception {
      when(queryService.getPortfolioById(any(GetPortfolioByIdQuery.class))).thenReturn(
          buildPortfolioView());

      mockMvc.perform(get(PORTFOLIO_URL)).andExpect(status().isOk())
          .andExpect(jsonPath("$.name").value("My Portfolio"))
          .andExpect(jsonPath("$.currency").value("CAD"));
    }

    @Test
    @DisplayName("404 when portfolio is not found or belongs to another user")
    void returns404WhenNotFound() throws Exception {
      when(queryService.getPortfolioById(any(GetPortfolioByIdQuery.class))).thenThrow(
          new PortfolioNotFoundException(PortfolioId.fromString(PORTFOLIO_ID)));

      mockMvc.perform(get(PORTFOLIO_URL)).andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("PORTFOLIO_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("GET /{portfolioId}/net-worth — getNetWorth")
  class GetNetWorth {

    @Test
    @DisplayName("200 with net worth breakdown")
    void returns200OnSuccess() throws Exception {
      when(queryService.getNetWorth(any(GetNetWorthQuery.class))).thenReturn(buildNetWorthView());

      mockMvc.perform(get(PORTFOLIO_URL + "/net-worth")).andExpect(status().isOk())
          .andExpect(jsonPath("$.currency").value("CAD"));
    }

    @Test
    @DisplayName("GetNetWorthQuery carries the correct IDs")
    void queryCarriesCorrectIds() throws Exception {
      when(queryService.getNetWorth(any(GetNetWorthQuery.class))).thenReturn(buildNetWorthView());

      var captor = ArgumentCaptor.forClass(GetNetWorthQuery.class);

      mockMvc.perform(get(PORTFOLIO_URL + "/net-worth")).andExpect(status().isOk());

      verify(queryService).getNetWorth(captor.capture());
      assertThat(captor.getValue().portfolioId()).isEqualTo(PortfolioId.fromString(PORTFOLIO_ID));
      assertThat(captor.getValue().userId().id()).isEqualTo(USER_UUID);
    }

    @Test
    @DisplayName("404 when portfolio not found")
    void returns404WhenNotFound() throws Exception {
      when(queryService.getNetWorth(any(GetNetWorthQuery.class))).thenThrow(
          new PortfolioNotFoundException(PortfolioId.fromString(PORTFOLIO_ID)));

      mockMvc.perform(get(PORTFOLIO_URL + "/net-worth")).andExpect(status().isNotFound());
    }
  }
}