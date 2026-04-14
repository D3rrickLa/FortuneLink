package com.laderrco.fortunelink.portfolio.api.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.laderrco.fortunelink.portfolio.application.commands.CreateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeleteAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.ReopenAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.exceptions.AccountCannotBeClosedException;
import com.laderrco.fortunelink.portfolio.application.exceptions.AccountCannotBeReopenedException;
import com.laderrco.fortunelink.portfolio.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetAllAccountsQuery;
import com.laderrco.fortunelink.portfolio.application.services.AccountLifecycleService;
import com.laderrco.fortunelink.portfolio.application.services.AccountQueryService;
import com.laderrco.fortunelink.portfolio.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountLifecycleState;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
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
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * Slice test for AccountController.
 *
 * <p>
 * Security filters are disabled — we are testing controller routing and service delegation, not
 * auth. Security is covered by a separate integration test suite.
 *
 * <p>
 * RateLimitInterceptor is mocked and configured to always pass. Its Redis dependencies are not
 * available in a slice test and are irrelevant here.
 */
@WebMvcTest(controllers = AccountController.class)
@Import(AuthenticatedUserResolver.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {
  private static final UUID USER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final String PORTFOLIO_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
  private static final String ACCOUNT_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";
  private static final String BASE_URL = "/api/v1/portfolios/" + PORTFOLIO_ID + "/accounts";
  private static final String ACCOUNT_URL = BASE_URL + "/" + ACCOUNT_ID;

  @Autowired
  MockMvc mockMvc;
  @Autowired
  JsonMapper objectMapper;

  @MockitoBean
  AccountLifecycleService lifecycleService;
  @MockitoBean
  AccountQueryService accountQueryService;
  // Backing bean for AuthenticatedUserResolver
  @MockitoBean
  AuthenticationUserService authenticationUserService;
  // WebConfig wires this interceptor; mock it so Redis deps are not needed
  @MockitoBean
  RateLimitInterceptor rateLimitInterceptor;

  @BeforeEach
  void setUp() throws Exception {

    when(authenticationUserService.getCurrentUser()).thenReturn(USER_UUID);

    when(rateLimitInterceptor.preHandle(any(HttpServletRequest.class),
        any(HttpServletResponse.class), any())).thenReturn(true);
  }

  private String validCreateRequest() {
    return """
        {
            "accountName": "My TFSA",
            "accountType": "TFSA",
            "strategy": "ACB",
            "currency": "CAD"
        }
        """;
  }

  private AccountView buildAccountView(String accountId, String accountTypeName,
      AccountLifecycleState state) {
    Currency cad = Currency.of("CAD");
    Money zero = Money.zero(cad);

    return new AccountView(AccountId.fromString(accountId), "Test Account",
        AccountType.valueOf(accountTypeName), state, List.of(), cad, zero, zero, Instant.now(),
        false, 0);
  }

  @Nested
  @DisplayName("POST / — createAccount")
  class CreateAccount {

    @Test
    @DisplayName("201 and returns AccountView when request is valid")
    void returns201OnSuccess() throws Exception {
      AccountView view = buildAccountView(ACCOUNT_ID, "TFSA", AccountLifecycleState.ACTIVE);
      when(lifecycleService.createAccount(any(CreateAccountCommand.class))).thenReturn(view);

      mockMvc.perform(
              post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(validCreateRequest()))
          .andExpect(status().isCreated());

      verify(lifecycleService, times(1)).createAccount(any(CreateAccountCommand.class));
    }

    @Test
    @DisplayName("CreateAccountCommand carries the correct field values")
    void commandIsConstructedCorrectly() throws Exception {
      AccountView view = buildAccountView(ACCOUNT_ID, "TFSA", AccountLifecycleState.ACTIVE);
      when(lifecycleService.createAccount(any(CreateAccountCommand.class))).thenReturn(view);

      ArgumentCaptor<CreateAccountCommand> captor = ArgumentCaptor.forClass(
          CreateAccountCommand.class);

      mockMvc.perform(
              post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(validCreateRequest()))
          .andExpect(status().isCreated());

      verify(lifecycleService).createAccount(captor.capture());
      CreateAccountCommand cmd = captor.getValue();

      assertThat(cmd.portfolioId()).isEqualTo(PortfolioId.fromString(PORTFOLIO_ID));
      assertThat(cmd.userId().id()).isEqualTo(USER_UUID);
      assertThat(cmd.accountName()).isEqualTo("My TFSA");
      assertThat(cmd.accountType()).isEqualTo(AccountType.TFSA);
      assertThat(cmd.strategy()).isEqualTo(PositionStrategy.ACB);
      assertThat(cmd.baseCurrency()).isEqualTo(Currency.of("CAD"));
    }

    @Test
    @DisplayName("400 when accountName is missing")
    void returns400WhenNameMissing() throws Exception {
      String body = """
          {
              "accountType": "TFSA",
              "strategy": "ACB",
              "currency": "CAD"
          }
          """;

      mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(lifecycleService);
    }

    @Test
    @DisplayName("400 when accountType is missing")
    void returns400WhenAccountTypeMissing() throws Exception {
      String body = """
          {
              "accountName": "My TFSA",
              "strategy": "ACB",
              "currency": "CAD"
          }
          """;

      mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(lifecycleService);
    }

    @Test
    @DisplayName("400 when currency is missing")
    void returns400WhenCurrencyMissing() throws Exception {
      String body = """
          {
              "accountName": "My TFSA",
              "accountType": "TFSA",
              "strategy": "ACB"
          }
          """;

      mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(lifecycleService);
    }

    @Test
    @DisplayName("400 when strategy is missing")
    void returns400WhenStrategyMissing() throws Exception {
      String body = """
          {
              "accountName": "My TFSA",
              "accountType": "TFSA",
              "currency": "CAD"
          }
          """;

      mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(lifecycleService);
    }

    @Test
    @DisplayName("400 when request body is entirely absent")
    void returns400WhenBodyAbsent() throws Exception {
      mockMvc.perform(post(BASE_URL).with(jwt()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(lifecycleService);
    }
  }

  @Nested
  @DisplayName("PUT /{accountId} — updateAccount")
  class UpdateAccount {

    @Test
    @DisplayName("204 when update succeeds")
    void returns204OnSuccess() throws Exception {
      doNothing().when(lifecycleService).updateAccount(any(UpdateAccountCommand.class));

      mockMvc.perform(put(ACCOUNT_URL).contentType(MediaType.APPLICATION_JSON)
          .content("{\"accountName\": \"Renamed TFSA\"}")).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("UpdateAccountCommand carries the correct field values")
    void commandIsConstructedCorrectly() throws Exception {
      doNothing().when(lifecycleService).updateAccount(any(UpdateAccountCommand.class));

      ArgumentCaptor<UpdateAccountCommand> captor = ArgumentCaptor.forClass(
          UpdateAccountCommand.class);

      mockMvc.perform(put(ACCOUNT_URL).contentType(MediaType.APPLICATION_JSON)
          .content("{\"accountName\": \"Renamed TFSA\"}")).andExpect(status().isNoContent());

      verify(lifecycleService).updateAccount(captor.capture());
      UpdateAccountCommand cmd = captor.getValue();

      assertThat(cmd.portfolioId()).isEqualTo(PortfolioId.fromString(PORTFOLIO_ID));
      assertThat(cmd.userId().id()).isEqualTo(USER_UUID);
      assertThat(cmd.accountId()).isEqualTo(AccountId.fromString(ACCOUNT_ID));
      assertThat(cmd.accountName()).isEqualTo("Renamed TFSA");
    }

    @Test
    @DisplayName("400 when accountName is null")
    void returns400WhenNameNull() throws Exception {
      mockMvc.perform(put(ACCOUNT_URL).contentType(MediaType.APPLICATION_JSON)
          .content("{\"accountName\": null}")).andExpect(status().isBadRequest());

      verifyNoInteractions(lifecycleService);
    }

    @Test
    @DisplayName("400 when body is empty")
    void returns400WhenBodyEmpty() throws Exception {
      mockMvc.perform(put(ACCOUNT_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(lifecycleService);
    }
  }

  @Nested
  @DisplayName("DELETE /{accountId} — closeAccount")
  class CloseAccount {

    @Test
    @DisplayName("204 when close succeeds")
    void returns204OnSuccess() throws Exception {
      doNothing().when(lifecycleService).deleteAccount(any(DeleteAccountCommand.class));

      mockMvc.perform(delete(ACCOUNT_URL)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DeleteAccountCommand carries the correct IDs")
    void commandIsConstructedCorrectly() throws Exception {
      doNothing().when(lifecycleService).deleteAccount(any(DeleteAccountCommand.class));

      ArgumentCaptor<DeleteAccountCommand> captor = ArgumentCaptor.forClass(
          DeleteAccountCommand.class);

      mockMvc.perform(delete(ACCOUNT_URL)).andExpect(status().isNoContent());

      verify(lifecycleService).deleteAccount(captor.capture());
      DeleteAccountCommand cmd = captor.getValue();

      assertThat(cmd.portfolioId()).isEqualTo(PortfolioId.fromString(PORTFOLIO_ID));
      assertThat(cmd.userId().id()).isEqualTo(USER_UUID);
      assertThat(cmd.accountId()).isEqualTo(AccountId.fromString(ACCOUNT_ID));
    }

    @Test
    @DisplayName("409 when account has open positions or non-zero balance")
    void returns409WhenCannotClose() throws Exception {
      doThrow(new AccountCannotBeClosedException("Account has open positions")).when(
          lifecycleService).deleteAccount(any(DeleteAccountCommand.class));

      mockMvc.perform(delete(ACCOUNT_URL)).andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("ACCOUNT_CANNOT_BE_CLOSED"));
    }
  }

  @Nested
  @DisplayName("PATCH /{accountId}/reopen — reopenAccount")
  class ReopenAccount {

    private final String REOPEN_URL = ACCOUNT_URL + "/reopen";

    @Test
    @DisplayName("204 when reopen succeeds")
    void returns204OnSuccess() throws Exception {
      doNothing().when(lifecycleService).reopenAccount(any(ReopenAccountCommand.class));

      mockMvc.perform(patch(REOPEN_URL)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("ReopenAccountCommand carries the correct IDs")
    void commandIsConstructedCorrectly() throws Exception {
      doNothing().when(lifecycleService).reopenAccount(any(ReopenAccountCommand.class));

      ArgumentCaptor<ReopenAccountCommand> captor = ArgumentCaptor.forClass(
          ReopenAccountCommand.class);

      mockMvc.perform(patch(REOPEN_URL)).andExpect(status().isNoContent());

      verify(lifecycleService).reopenAccount(captor.capture());
      ReopenAccountCommand cmd = captor.getValue();

      assertThat(cmd.portfolioId()).isEqualTo(PortfolioId.fromString(PORTFOLIO_ID));
      assertThat(cmd.userId().id()).isEqualTo(USER_UUID);
      assertThat(cmd.accountId()).isEqualTo(AccountId.fromString(ACCOUNT_ID));
    }

    @Test
    @DisplayName("409 when account is not in CLOSED state")
    void returns409WhenCannotReopen() throws Exception {
      doThrow(new AccountCannotBeReopenedException("Account is not closed")).when(lifecycleService)
          .reopenAccount(any(ReopenAccountCommand.class));

      mockMvc.perform(patch(REOPEN_URL)).andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("ACCOUNT_CANNOT_BE_REOPENED"));
    }
  }

  @Nested
  @DisplayName("GET / — getAllAccounts")
  class GetAllAccounts {

    @Test
    @DisplayName("200 and returns paged accounts on success")
    void returns200WithPagedAccounts() throws Exception {
      AccountView view = buildAccountView(ACCOUNT_ID, "TFSA", AccountLifecycleState.ACTIVE);
      Page<AccountView> page = new PageImpl<>(List.of(view));
      when(accountQueryService.getAllAccounts(any(GetAllAccountsQuery.class))).thenReturn(page);

      mockMvc.perform(get(BASE_URL)).andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.length()").value(1))
          .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("200 and returns empty page when no accounts exist")
    void returns200WithEmptyPage() throws Exception {
      Page<AccountView> emptyPage = Page.empty();
      when(accountQueryService.getAllAccounts(any(GetAllAccountsQuery.class))).thenReturn(
          emptyPage);

      mockMvc.perform(get(BASE_URL)).andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @DisplayName("GetAllAccountsQuery uses page/size from Pageable query params")
    void queryUsesPageableParams() throws Exception {
      Page<AccountView> emptyPage = Page.empty();
      when(accountQueryService.getAllAccounts(any(GetAllAccountsQuery.class))).thenReturn(
          emptyPage);

      ArgumentCaptor<GetAllAccountsQuery> captor = ArgumentCaptor.forClass(
          GetAllAccountsQuery.class);

      mockMvc.perform(get(BASE_URL).param("page", "2").param("size", "10"))
          .andExpect(status().isOk());

      verify(accountQueryService).getAllAccounts(captor.capture());
      GetAllAccountsQuery query = captor.getValue();

      assertThat(query.page()).isEqualTo(2);
      assertThat(query.size()).isEqualTo(10);
      assertThat(query.portfolioId()).isEqualTo(PortfolioId.fromString(PORTFOLIO_ID));
      assertThat(query.userId().id()).isEqualTo(USER_UUID);
    }

    @Test
    @DisplayName("GetAllAccountsQuery defaults to page 0 when no params given")
    void queryDefaultsToPageZero() throws Exception {
      Page<AccountView> emptyPage = Page.empty();
      when(accountQueryService.getAllAccounts(any(GetAllAccountsQuery.class))).thenReturn(
          emptyPage);

      ArgumentCaptor<GetAllAccountsQuery> captor = ArgumentCaptor.forClass(
          GetAllAccountsQuery.class);

      mockMvc.perform(get(BASE_URL)).andExpect(status().isOk());

      verify(accountQueryService).getAllAccounts(captor.capture());
      assertThat(captor.getValue().page()).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("GET /{accountId} — getAccount")
  class GetAccount {

    @Test
    @DisplayName("200 and returns AccountView on success")
    void returns200OnSuccess() throws Exception {
      AccountView view = buildAccountView(ACCOUNT_ID, "TFSA", AccountLifecycleState.ACTIVE);
      when(accountQueryService.getAccountSummary(any(GetAccountSummaryQuery.class))).thenReturn(
          view);

      mockMvc.perform(get(ACCOUNT_URL)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GetAccountSummaryQuery carries the correct IDs")
    void queryIsConstructedCorrectly() throws Exception {
      AccountView view = buildAccountView(ACCOUNT_ID, "TFSA", AccountLifecycleState.ACTIVE);
      when(accountQueryService.getAccountSummary(any(GetAccountSummaryQuery.class))).thenReturn(
          view);

      ArgumentCaptor<GetAccountSummaryQuery> captor = ArgumentCaptor.forClass(
          GetAccountSummaryQuery.class);

      mockMvc.perform(get(ACCOUNT_URL)).andExpect(status().isOk());

      verify(accountQueryService).getAccountSummary(captor.capture());
      GetAccountSummaryQuery query = captor.getValue();

      assertThat(query.portfolioId()).isEqualTo(PortfolioId.fromString(PORTFOLIO_ID));
      assertThat(query.userId().id()).isEqualTo(USER_UUID);
      assertThat(query.accountId()).isEqualTo(AccountId.fromString(ACCOUNT_ID));
    }

    @Test
    @DisplayName("404 when account does not exist or belongs to another user")
    void returns404WhenNotFound() throws Exception {
      when(accountQueryService.getAccountSummary(any(GetAccountSummaryQuery.class))).thenThrow(
          new AccountNotFoundException(AccountId.fromString(ACCOUNT_ID),
              PortfolioId.fromString(PORTFOLIO_ID)));

      mockMvc.perform(get(ACCOUNT_URL)).andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }
  }
}