package com.laderrco.fortunelink.portfolio_management.api.web.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers.PortfolioHttpMapper;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers.TransactionCommandAssembler;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers.TransactionDtoMapper;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.DeleteTransactionRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.GetAccountRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.RecordTransactionRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.PagedTransactionHttpResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.TransactionHttpResponse;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeleteTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordIncomeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdateTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetTransactionByIdQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetTransactionHistoryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AccountView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionHistoryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionView;
import com.laderrco.fortunelink.portfolio_management.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio_management.application.services.PortfolioApplicationService;
import com.laderrco.fortunelink.portfolio_management.application.services.PortfolioQueryService;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.DevSecurityConfig;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.RateLimitConfig;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.SecurityConfig;
import com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions.GlobalExceptionHandler;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

@AutoConfigureMockMvc
@Import({ SecurityConfig.class, RateLimitConfig.class })
@WebMvcTest({ TransactionController.class, GlobalExceptionHandler.class, AuthenticationUserService.class })
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PortfolioApplicationService applicationService;

    @MockitoBean
    private PortfolioQueryService queryService;

    @MockitoBean
    private PortfolioHttpMapper portfolioHttpMapper;

    @MockitoBean
    private TransactionDtoMapper transactionDtoMapper;

    @MockitoBean
    private TransactionCommandAssembler transactionCommandAssembler;

    @MockitoBean
    private AccountView accountView;

    private static final String PORTFOLIO_ID = UUID.randomUUID().toString();
    private static final String ACCOUNT_ID = UUID.randomUUID().toString();
    private static final String TRANSACTION_ID = UUID.randomUUID().toString();
    private static final UUID USER_ID = UUID.randomUUID();

    private TransactionHttpResponse responses;

    @BeforeEach
    void setup() {
        when(portfolioHttpMapper.toCommand(anyString(), any(), any(GetAccountRequest.class)))
                .thenReturn(new GetAccountSummaryQuery(toPortfolioId(PORTFOLIO_ID), toUserId(USER_ID),
                        toAccountId(ACCOUNT_ID)));

        when(queryService.getAccountSummary(any(GetAccountSummaryQuery.class)))
                .thenReturn(accountView);

        when(accountView.baseCurrency()).thenReturn(ValidatedCurrency.of("USD"));
        responses = new TransactionHttpResponse(
                "tx-1", // id
                ACCOUNT_ID, // accountId
                "BUY", // transactionType
                "AAPL", // symbol
                new BigDecimal("10"), // quantity
                new BigDecimal("150"), // price
                "USD", // priceCurrency
                new BigDecimal("0"), // fee
                new BigDecimal("1500"), // totalCost (price*quantity + fee)
                new BigDecimal("1500"), // netAmount
                LocalDateTime.now(), // transactionDate
                "Some note", // notes
                LocalDateTime.now() // recordedAt
        );
    }

    private RecordTransactionRequest sampleAssetRequest() {
        RecordTransactionRequest request = new RecordTransactionRequest();
        request.setTransactionType("BUY");
        request.setSymbol("AAPL");
        request.setQuantity(new BigDecimal("10"));
        request.setPrice(new BigDecimal("150"));
        request.setPriceCurrency("USD");
        request.setTransactionDate(LocalDateTime.now());
        return request;
    }

    private RecordTransactionRequest sampleCashRequest() {
        RecordTransactionRequest request = new RecordTransactionRequest();
        request.setTransactionType("DEPOSIT");
        request.setQuantity(new BigDecimal("1000"));
        request.setPriceCurrency("USD");
        request.setTransactionDate(LocalDateTime.now());
        return request;
    }

    // ------------------- BUY -------------------
    @SuppressWarnings("null")
    @Test
    void buy_returnsCreatedTransaction() throws Exception {
        RecordTransactionRequest request = sampleAssetRequest();
        TransactionView view = mock(TransactionView.class);
        TransactionHttpResponse response = responses;

        when(transactionCommandAssembler.toPurchaseCommand(anyString(), anyString(), any(), any(), any()))
                .thenReturn(mock(RecordPurchaseCommand.class));
        when(applicationService.recordAssetPurchase(any())).thenReturn(view);
        when(transactionDtoMapper.toResponse(anyString(), eq(view))).thenReturn(response);

        mockMvc.perform(
                post("/api/portfolios/{portfolioId}/accounts/{accountId}/transactions/buy", PORTFOLIO_ID, ACCOUNT_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }

    // ------------------- SELL -------------------
    @SuppressWarnings("null")
    @Test
    void sell_returnsCreatedTransaction() throws Exception {
        RecordTransactionRequest request = sampleAssetRequest();
        TransactionView view = mock(TransactionView.class);
        TransactionHttpResponse response = responses;

        when(transactionCommandAssembler.toSaleCommand(anyString(), anyString(), any(), any(), any()))
                .thenReturn(mock(RecordSaleCommand.class));
        when(applicationService.recordAssetSale(any())).thenReturn(view);
        when(transactionDtoMapper.toResponse(anyString(), eq(view))).thenReturn(response);

        mockMvc.perform(
                post("/api/portfolios/{portfolioId}/accounts/{accountId}/transactions/sell", PORTFOLIO_ID, ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.subject(USER_ID.toString())))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }

    // ------------------- DIVIDEND -------------------
    @SuppressWarnings("null")
    @Test
    void dividend_returnsCreatedTransaction() throws Exception {
        RecordTransactionRequest request = sampleAssetRequest();
        TransactionView view = mock(TransactionView.class);
        TransactionHttpResponse response = responses;

        when(transactionCommandAssembler.toDividendCommand(anyString(), anyString(), any(), any(), any()))
                .thenReturn(mock(RecordIncomeCommand.class));
        when(applicationService.recordDividendIncome(any())).thenReturn(view);
        when(transactionDtoMapper.toResponse(anyString(), eq(view))).thenReturn(response);

        mockMvc.perform(post("/api/portfolios/{portfolioId}/accounts/{accountId}/transactions/dividends", PORTFOLIO_ID,
                ACCOUNT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString())))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }

    // ------------------- DEPOSIT -------------------
    @SuppressWarnings("null")
    @Test
    void deposit_returnsCreatedTransaction() throws Exception {
        RecordTransactionRequest request = sampleCashRequest();
        TransactionView view = mock(TransactionView.class);
        TransactionHttpResponse response = responses;

        when(transactionCommandAssembler.toDepositCommand(anyString(), anyString(), any(), any(), any()))
                .thenReturn(mock(RecordDepositCommand.class));
        when(applicationService.recordDeposit(any())).thenReturn(view);
        when(transactionDtoMapper.toResponse(anyString(), eq(view))).thenReturn(response);

        mockMvc.perform(post("/api/portfolios/{portfolioId}/accounts/{accountId}/transactions/deposit", PORTFOLIO_ID,
                ACCOUNT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString())))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }

    // ------------------- WITHDRAWAL -------------------
    @SuppressWarnings("null")
    @Test
    void withdrawal_returnsCreatedTransaction() throws Exception {
        RecordTransactionRequest request = sampleCashRequest();
        TransactionView view = mock(TransactionView.class);
        TransactionHttpResponse response = responses;

        when(transactionCommandAssembler.toWithdrawalCommand(anyString(), anyString(), any(), any(), any()))
                .thenReturn(mock(RecordWithdrawalCommand.class));
        when(applicationService.recordWithdrawal(any())).thenReturn(view);
        when(transactionDtoMapper.toResponse(anyString(), eq(view))).thenReturn(response);

        mockMvc.perform(post("/api/portfolios/{portfolioId}/accounts/{accountId}/transactions/withdrawal", PORTFOLIO_ID,
                ACCOUNT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString())))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }

    // ------------------- UPDATE -------------------
    @SuppressWarnings("null")
    @Test
    void update_returnsOkTransaction() throws Exception {
        RecordTransactionRequest request = sampleAssetRequest();
        TransactionView view = mock(TransactionView.class);
        TransactionHttpResponse response = responses;

        when(transactionCommandAssembler.toUpdateCommand(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(mock(UpdateTransactionCommand.class));
        when(applicationService.updateTransaction(any())).thenReturn(view);
        when(transactionDtoMapper.toResponse(anyString(), eq(view))).thenReturn(response);

        mockMvc.perform(
                put("/api/portfolios/{portfolioId}/accounts/{accountId}/transactions/{transactionId}", PORTFOLIO_ID,
                        ACCOUNT_ID, TRANSACTION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.subject(USER_ID.toString())))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }

    // ------------------- DELETE -------------------
    @SuppressWarnings("null")
    @Test
    void delete_returnsNoContent() throws Exception {
        // 1. Arrange
        DeleteTransactionRequest request = new DeleteTransactionRequest("Some note");

        // Ensure the assembler returns a non-null command for the service
        when(transactionCommandAssembler.toDeleteCommand(any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(mock(DeleteTransactionCommand.class));

        doNothing().when(applicationService).deleteTransaction(any());

        // 2. Act: Corrected the URL to match the @DeleteMapping
        mockMvc.perform(delete("/api/portfolios/{portfolioId}/accounts/{accountId}/transactions/{transactionId}",
                PORTFOLIO_ID, ACCOUNT_ID, TRANSACTION_ID)
                .param("softDelete", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString())))
                .content(objectMapper.writeValueAsString(request)))
                // 3. Assert
                .andExpect(status().isNoContent());
    }

    @SuppressWarnings("null")
    @Test
    void delete_returnsNoContentNullRequestParam() throws Exception {
        // 1. Arrange
        when(transactionCommandAssembler.toDeleteCommand(any(), any(), any(), any(), anyBoolean(), isNull()))
                .thenReturn(mock(DeleteTransactionCommand.class));

        doNothing().when(applicationService).deleteTransaction(any());

        // 2. Act: Corrected the URL
        mockMvc.perform(delete("/api/portfolios/{portfolioId}/accounts/{accountId}/transactions/{transactionId}",
                PORTFOLIO_ID, ACCOUNT_ID, TRANSACTION_ID)
                .param("softDelete", "true")
                .with(jwt().jwt(j -> j.subject(USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)) // Removed .content(null) for cleaner test
                // 3. Assert
                .andExpect(status().isNoContent());
    }
    // ---------------- GET /{transactionId} ----------------

    @Test
    @DisplayName("GET transaction by id returns 200")
    void getTransaction_returnsOk() throws Exception {
        TransactionView transactionView = mock(TransactionView.class);
        TransactionHttpResponse response = mock(TransactionHttpResponse.class);
        GetTransactionByIdQuery query = mock(GetTransactionByIdQuery.class);

        when(transactionCommandAssembler.toTransactionQuery(any(), any(), any(), any()))
                .thenReturn(query);

        when(queryService.getTransactionDetails(query))
                .thenReturn(transactionView);

        when(transactionDtoMapper.toResponse(any(), any()))
                .thenReturn(response);

        mockMvc.perform(get(
                "/api/portfolios/{portfolioId}/accounts/{accountId}/transactions/{transactionId}",
                "portfolio-1",
                "account-1",
                "tx-1").with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isOk());
    }

    // ---------------- GET transaction history ----------------

    @Test
    @DisplayName("GET transaction history returns 200")
    void getTransactionHistory_returnsOk() throws Exception {
        GetTransactionHistoryQuery query = mock(GetTransactionHistoryQuery.class);
        TransactionHistoryView historyView = mock(TransactionHistoryView.class);
        PagedTransactionHttpResponse response = mock();

        // Use anyInt() for primitives
        when(transactionCommandAssembler.toHistoryQuery(
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(query);

        when(queryService.getTransactionHistory(query))
                .thenReturn(historyView);

        when(transactionDtoMapper.toPagedResponse(any(), any()))
                .thenReturn(response);

        mockMvc.perform(get(
                "/api/portfolios/{portfolioId}/accounts/{accountId}/transactions",
                "portfolio-1",
                "account-1")
                .param("type", "BUY")
                .param("page", "1")
                .param("size", "20")
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isOk());
    }

    private PortfolioId toPortfolioId(String id) {
        return new PortfolioId(UUID.fromString(id));
    }

    private AccountId toAccountId(String id) {
        return new AccountId(UUID.fromString(id));
    }

    private UserId toUserId(UUID id) {
        return new UserId(id);
    }
}
