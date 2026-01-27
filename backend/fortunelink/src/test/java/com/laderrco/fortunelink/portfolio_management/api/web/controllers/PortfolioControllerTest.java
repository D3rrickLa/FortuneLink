package com.laderrco.fortunelink.portfolio_management.api.web.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers.PortfolioDtoMapper;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers.PortfolioHttpMapper;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.CreateAccountRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.CreatePortfolioRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.DeletePortfolioRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.GetUsersPortfolioRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.AccountHttpResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.AssetHoldingHttpResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.PortfolioHttpResponse;
import com.laderrco.fortunelink.portfolio_management.application.commands.AddAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RemoveAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.InvalidTransactionException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioAlreadyExistsException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioNotEmptyException;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetAssetQueryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetPortfolioByIdQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetPortfoliosByUserIdQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AccountView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AssetView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PortfolioSummaryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PortfolioView;
import com.laderrco.fortunelink.portfolio_management.application.services.PortfolioApplicationService;
import com.laderrco.fortunelink.portfolio_management.application.services.PortfolioQueryService;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.DevSecurityConfig;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.RateLimitConfig;
import com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions.GlobalExceptionHandler;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

@AutoConfigureMockMvc
@Import({ DevSecurityConfig.class, RateLimitConfig.class })
@WebMvcTest({ PortfolioController.class, GlobalExceptionHandler.class })
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortfolioApplicationService portfolioApplicationService;

    @MockitoBean
    private PortfolioQueryService portfolioQueryService;

    @MockitoBean
    private PortfolioDtoMapper portfolioDtoMapper;

    @MockitoBean
    private PortfolioHttpMapper requestMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PORTFOLIO_ID = UUID.randomUUID().toString();
    private static final String USER_ID = UUID.randomUUID().toString();

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    // ================= CREATE PORTFOLIO =================
    @SuppressWarnings("null")
    @Test
    void createPortfolio_ShouldReturnCreatedPortfolio() throws Exception {
        CreatePortfolioRequest request = new CreatePortfolioRequest(USER_ID, "Personal", "USD", "desc", false);
        CreatePortfolioCommand command = mock(CreatePortfolioCommand.class);
        PortfolioView view = mock(PortfolioView.class);
        PortfolioHttpResponse response = mock(PortfolioHttpResponse.class);

        when(requestMapper.toCommand(any(CreatePortfolioRequest.class))).thenReturn(command);
        when(portfolioApplicationService.createPortfolio(command)).thenReturn(view);
        when(portfolioDtoMapper.toPortfolioResponse(view)).thenReturn(response);

        mockMvc.perform(post("/api/portfolios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }

    // ================= GET PORTFOLIO BY ID =================
    @SuppressWarnings("null")
    @Test
    void getPortfolio_ShouldReturnPortfolio() throws Exception {
        GetPortfolioByIdQuery query = mock(GetPortfolioByIdQuery.class);
        PortfolioView view = mock(PortfolioView.class);
        PortfolioHttpResponse response = mock(PortfolioHttpResponse.class);

        when(requestMapper.toCommand(PORTFOLIO_ID)).thenReturn(query);
        when(portfolioQueryService.getPortfolioById(query)).thenReturn(view);
        when(portfolioDtoMapper.toPortfolioResponse(view)).thenReturn(response);

        mockMvc.perform(get("/api/portfolios/{portfolioId}", PORTFOLIO_ID))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }

    // ================= GET USER PORTFOLIOS =================
    @SuppressWarnings("null")
    @Test
    void getUserPortfolios_ShouldReturnList() throws Exception {
        GetUsersPortfolioRequest userRequest = new GetUsersPortfolioRequest(USER_ID);
        GetPortfoliosByUserIdQuery query = mock(GetPortfoliosByUserIdQuery.class);

        PortfolioSummaryView summary1 = mock(PortfolioSummaryView.class);
        PortfolioSummaryView summary2 = mock(PortfolioSummaryView.class);
        List<PortfolioSummaryView> summaries = List.of(summary1, summary2);

        PortfolioHttpResponse resp1 = mock(PortfolioHttpResponse.class);
        PortfolioHttpResponse resp2 = mock(PortfolioHttpResponse.class);

        when(requestMapper.toCommand(userRequest)).thenReturn(query);
        when(portfolioQueryService.getUserPortfolioSummaries(query)).thenReturn(summaries);
        when(portfolioDtoMapper.toPortfolioResponse(summary1)).thenReturn(resp1);
        when(portfolioDtoMapper.toPortfolioResponse(summary2)).thenReturn(resp2);

        mockMvc.perform(get("/api/portfolios/user/{userId}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(resp1, resp2))));
    }

    // ================= UPDATE PORTFOLIO =================
    @SuppressWarnings("null")
    @Test
    void updatePortfolio_ShouldReturnUpdatedPortfolio() throws Exception {

        // --- Request payload ---
        CreatePortfolioRequest request = new CreatePortfolioRequest(
                USER_ID, "Portfolio Name", "USD", "DESC", false);

        // --- Mock service & mapper ---
        PortfolioView view = new PortfolioView(
                new PortfolioId(UUID.fromString(PORTFOLIO_ID)),
                new UserId(UUID.fromString(USER_ID)),
                "Portfolio Name",
                "DESC",
                List.of(),
                Money.of(BigDecimal.TEN, "USD"),
                1L,
                Instant.now(),
                Instant.now());

        PortfolioHttpResponse response = new PortfolioHttpResponse(
                PORTFOLIO_ID,
                USER_ID,
                "Portfolio Name",
                "DESC",
                List.of(),
                BigDecimal.TEN,
                "USD",
                LocalDateTime.now(),
                LocalDateTime.now());

        when(requestMapper.toCommand(anyString(), any(CreatePortfolioRequest.class)))
                .thenReturn(mock(UpdatePortfolioCommand.class));
        when(portfolioApplicationService.updatePortfolio(any())).thenReturn(view);
        when(portfolioDtoMapper.toPortfolioResponse(any(PortfolioView.class))).thenReturn(response);
        mockMvc.perform(put("/api/portfolios/{portfolioId}", PORTFOLIO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PORTFOLIO_ID))
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.name").value("Portfolio Name"))
                .andExpect(jsonPath("$.description").value("DESC"))
                .andExpect(jsonPath("$.totalValue").value(10))
                .andExpect(jsonPath("$.totalValueCurrency").value("USD"));
    }

    // ================= DELETE PORTFOLIO =================
    @SuppressWarnings("null")
    @Test
    void deletePortfolio_ShouldReturnNoContent() throws Exception {
        DeletePortfolioRequest request = new DeletePortfolioRequest(
                PORTFOLIO_ID,
                USER_ID,
                true, // confirmed
                false // softDelete
        );

        DeletePortfolioCommand command = mock(DeletePortfolioCommand.class);

        when(requestMapper.toCommand(any(DeletePortfolioRequest.class))).thenReturn(command);
        doNothing().when(portfolioApplicationService).deletePortfolio(eq(command));

        mockMvc.perform(delete("/api/portfolios/{id}", PORTFOLIO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(portfolioApplicationService).deletePortfolio(command);
    }

    // ================= ADD ACCOUNT =================
    @SuppressWarnings("null")
    @Test
    void addAccount_ShouldReturnAccountResponse() throws Exception {
        // --- Arrange: request payload ---
        CreateAccountRequest request = new CreateAccountRequest(
                "My Account",
                "INVESTMENT",
                "USD");

        // --- Mock service return ---
        AccountView accountView = new AccountView(
                new AccountId(UUID.fromString(ACCOUNT_ID)),
                "My Account",
                AccountType.INVESTMENT,
                List.of(),
                ValidatedCurrency.USD,
                Money.of(BigDecimal.valueOf(500), "USD"),
                Money.of(BigDecimal.valueOf(500), "USD"),
                Instant.now());

        // --- Expected response DTO ---
        AccountHttpResponse response = new AccountHttpResponse(
                ACCOUNT_ID,
                PORTFOLIO_ID, // portfolioId
                accountView.name(),
                accountView.type().toString(),
                accountView.baseCurrency().getCode(),
                List.of());

        // --- Mocks ---
        when(requestMapper.toCommand(eq(PORTFOLIO_ID), any(CreateAccountRequest.class)))
                .thenReturn(mock(AddAccountCommand.class));

        when(portfolioApplicationService.addAccount(any(AddAccountCommand.class)))
                .thenReturn(accountView);

        when(portfolioDtoMapper.toAccountResponse(eq(PORTFOLIO_ID), any(AccountView.class)))
                .thenReturn(response);

        // --- Act & Assert ---
        mockMvc.perform(post("/api/portfolios/{id}/accounts", PORTFOLIO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value(PORTFOLIO_ID))
                .andExpect(jsonPath("$.id").value(accountView.accountId().accountId().toString()))
                .andExpect(jsonPath("$.name").value("My Account"))
                // .andExpect(jsonPath("$.totalValue").value(500))
                .andExpect(jsonPath("$.baseCurrency").value("USD"));
    }

    // ================= REMOVE ACCOUNT =================
    @Test
    void removeAccount_ShouldReturnNoContent() throws Exception {
        RemoveAccountCommand command = mock(RemoveAccountCommand.class);
        String accountId = UUID.randomUUID().toString();

        when(requestMapper.toCommand(PORTFOLIO_ID, accountId)).thenReturn(command);

        mockMvc.perform(delete("/api/portfolios/{id}/accounts/{accountId}", PORTFOLIO_ID, accountId))
                .andExpect(status().isNoContent());

        verify(portfolioApplicationService).removeAccount(command);
    }

    @SuppressWarnings("null")
    @Test
    void getPortfolio_ShouldReturn404_WhenNotFound() throws Exception {
        String portfolioId = "non-existent";
        // 1. Setup Mock to throw the exception
        given(portfolioQueryService.getPortfolioById(any()))
                .willThrow(new PortfolioNotFoundException("Portfolio not found: " + portfolioId));

        // 2. Perform request and verify ExceptionHandler response
        mockMvc.perform(get("/api/portfolios/" + portfolioId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(containsString(portfolioId)));
    }

    @SuppressWarnings("null")
    @Test
    void createPortfolio_ShouldReturn409_WhenUserAlreadyHasPortfolio() throws Exception {
        // Mock the mapping and service call
        given(requestMapper.toCommand(any(CreatePortfolioRequest.class)))
                .willReturn(mock(CreatePortfolioCommand.class));
        given(portfolioApplicationService.createPortfolio(any()))
                .willThrow(new PortfolioAlreadyExistsException("User already has a portfolio"));

        String jsonRequest = "{\"userId\": \"user123\", \"name\": \"My Portfolio\", \"defaultCurrency\": \"USD\"}";

        mockMvc.perform(post("/api/portfolios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("User already has a portfolio"));
    }

    @SuppressWarnings("null")
    @Test
    void deletePortfolio_ShouldReturn409_WhenPortfolioIsNotEmpty() throws Exception {
        given(requestMapper.toCommand(any(DeletePortfolioRequest.class)))
                .willReturn(mock(DeletePortfolioCommand.class));
        doThrow(new PortfolioNotEmptyException("Cannot delete portfolio with accounts"))
                .when(portfolioApplicationService).deletePortfolio(any());

        mockMvc.perform(delete("/api/portfolios/123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmed\": true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("accounts")));
    }

    @SuppressWarnings("null")
    @Test
    void addAccount_ShouldReturn400_WhenCommandIsInvalid() throws Exception {
        given(requestMapper.toCommand(anyString(), any(CreateAccountRequest.class)))
                .willReturn(mock(AddAccountCommand.class));
        given(portfolioApplicationService.addAccount(any()))
                .willThrow(new InvalidTransactionException("Invalid currency", List.of("Currency not supported")));

        String jsonRequest = "{\"accountName\": \"Savings\", \"accountType\": \"CASH\", \"baseCurrency\": \"XYZ\"}";

        mockMvc.perform(post("/api/portfolios/123/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @SuppressWarnings("null")
    @Test
    void testGetAsset_Success() throws Exception {
        // 1. Arrange: Use UUID strings for the web layer
        UUID portfolioIdStr = UUID.randomUUID();
        UUID accountIdStr = UUID.randomUUID();
        UUID assetIdStr = UUID.randomUUID();

        // Mock the Query Object creation (Internal to the controller)
        GetAssetQueryView mockQuery = new GetAssetQueryView(new PortfolioId(portfolioIdStr),
                new AccountId(accountIdStr), new AssetId(assetIdStr));
        when(requestMapper.toAssetQuery(portfolioIdStr.toString(), accountIdStr.toString(), assetIdStr.toString()))
                .thenReturn(mockQuery);

        // 2. Mock the View (Populating all fields to pass record validation)
        AssetView mockView = new AssetView(
                new AssetId(assetIdStr), // Domain ID type
                "AAPL",
                AssetType.STOCK,
                new BigDecimal("10.00"),
                Money.of(1500, "USD"), // costBasis
                Money.of(150, "USD"), // avgCost
                Money.of(175, "USD"), // currentPrice
                Money.of(1750, "USD"), // currentValue
                Money.of(250, "USD"), // unrealizedGain
                Percentage.of(16.67), // unrealizedGainPercentage
                Instant.now(), // acquiredDate
                Instant.now() // lastUpdated
        );

        when(portfolioQueryService.getAssetSummary(mockQuery)).thenReturn(mockView);

        // Mock the final DTO mapping
        AssetHoldingHttpResponse mockResponse = new AssetHoldingHttpResponse(assetIdStr.toString(), "AAPL",
                AssetType.STOCK.name(), BigDecimal.valueOf(10.00d), BigDecimal.valueOf(150), LocalDateTime.now());
        when(portfolioDtoMapper.toAssetResponse(mockView)).thenReturn(mockResponse);

        // 3. Act
        mockMvc.perform(get("/api/portfolios/{pId}/accounts/{aId}/assets/{asId}",
                portfolioIdStr.toString(), accountIdStr.toString(), assetIdStr.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assetIdStr.toString()))
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.quantity").value(10.00));
    }
}
