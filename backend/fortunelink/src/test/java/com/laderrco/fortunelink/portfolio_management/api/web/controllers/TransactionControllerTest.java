package com.laderrco.fortunelink.portfolio_management.api.web.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
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
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.TransactionResponse;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordIncomeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdateTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AccountView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionView;
import com.laderrco.fortunelink.portfolio_management.application.services.PortfolioApplicationService;
import com.laderrco.fortunelink.portfolio_management.application.services.PortfolioQueryService;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.DevSecurityConfig;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.RateLimitConfig;
import com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions.GlobalExceptionHandler;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

@AutoConfigureMockMvc
@Import({ DevSecurityConfig.class, RateLimitConfig.class })
@WebMvcTest({ TransactionController.class, GlobalExceptionHandler.class })
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

    private TransactionResponse responses;

    @BeforeEach
    void setup() {
        when(portfolioHttpMapper.toCommand(anyString(), any(GetAccountRequest.class)))
                .thenReturn(new GetAccountSummaryQuery(toPortfolioId(PORTFOLIO_ID), toAccountId(ACCOUNT_ID)));

        when(queryService.getAccountSummary(any(GetAccountSummaryQuery.class)))
                .thenReturn(accountView);

        when(accountView.baseCurrency()).thenReturn(ValidatedCurrency.of("USD"));
        responses = new TransactionResponse(
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
        TransactionResponse response = responses;

        when(transactionCommandAssembler.toPurchaseCommand(anyString(), anyString(), any(), any()))
                .thenReturn(mock(RecordPurchaseCommand.class));
        when(applicationService.recordAssetPurchase(any())).thenReturn(view);
        when(transactionDtoMapper.toResponse(anyString(), eq(view))).thenReturn(response);

        mockMvc.perform(post("/api/portfolios/{portfolioId}/accounts/{accountId}/buy", PORTFOLIO_ID, ACCOUNT_ID)
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
        TransactionResponse response = responses;

        when(transactionCommandAssembler.toSaleCommand(anyString(), anyString(), any(), any()))
                .thenReturn(mock(RecordSaleCommand.class));
        when(applicationService.recordAssetSale(any())).thenReturn(view);
        when(transactionDtoMapper.toResponse(anyString(), eq(view))).thenReturn(response);

        mockMvc.perform(post("/api/portfolios/{portfolioId}/accounts/{accountId}/sell", PORTFOLIO_ID, ACCOUNT_ID)
                .contentType(MediaType.APPLICATION_JSON)
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
        TransactionResponse response = responses;

        when(transactionCommandAssembler.toDividendCommand(anyString(), anyString(), any(), any()))
                .thenReturn(mock(RecordIncomeCommand.class));
        when(applicationService.recordDividendIncome(any())).thenReturn(view);
        when(transactionDtoMapper.toResponse(anyString(), eq(view))).thenReturn(response);

        mockMvc.perform(post("/api/portfolios/{portfolioId}/accounts/{accountId}/dividends", PORTFOLIO_ID, ACCOUNT_ID)
                .contentType(MediaType.APPLICATION_JSON)
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
        TransactionResponse response = responses;

        when(transactionCommandAssembler.toDepositCommand(anyString(), anyString(), any(), any()))
                .thenReturn(mock(RecordDepositCommand.class));
        when(applicationService.recordDeposit(any())).thenReturn(view);
        when(transactionDtoMapper.toResponse(anyString(), eq(view))).thenReturn(response);

        mockMvc.perform(post("/api/portfolios/{portfolioId}/accounts/{accountId}/deposit", PORTFOLIO_ID, ACCOUNT_ID)
                .contentType(MediaType.APPLICATION_JSON)
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
        TransactionResponse response = responses;

        when(transactionCommandAssembler.toWithdrawalCommand(anyString(), anyString(), any(), any()))
                .thenReturn(mock(RecordWithdrawalCommand.class));
        when(applicationService.recordWithdrawal(any())).thenReturn(view);
        when(transactionDtoMapper.toResponse(anyString(), eq(view))).thenReturn(response);

        mockMvc.perform(post("/api/portfolios/{portfolioId}/accounts/{accountId}/withdrawal", PORTFOLIO_ID, ACCOUNT_ID)
                .contentType(MediaType.APPLICATION_JSON)
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
        TransactionResponse response = responses;

        when(transactionCommandAssembler.toUpdateCommand(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(mock(UpdateTransactionCommand.class));
        when(applicationService.updateTransation(any())).thenReturn(view);
        when(transactionDtoMapper.toResponse(anyString(), eq(view))).thenReturn(response);

        mockMvc.perform(put("/api/portfolios/{portfolioId}/accounts/{accountId}/update/{transactionId}", PORTFOLIO_ID,
                ACCOUNT_ID, TRANSACTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }

    // ------------------- DELETE -------------------
    @SuppressWarnings("null")
    @Test
    void delete_returnsNoContent() throws Exception {
        DeleteTransactionRequest request = new DeleteTransactionRequest("some notes");
        request.setNotes("Some note");

        doNothing().when(applicationService).deleteTransaction(any());

        mockMvc.perform(delete("/api/portfolios/{portfolioId}/accounts/{accountId}/delete/{transactionId}",
                PORTFOLIO_ID, ACCOUNT_ID, TRANSACTION_ID)
                .param("softDelete", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @SuppressWarnings("null")
    @Test
    void delete_returnsNoContentNullRequestParam() throws Exception {
        doNothing().when(applicationService).deleteTransaction(any());

        mockMvc.perform(delete("/api/portfolios/{portfolioId}/accounts/{accountId}/delete/{transactionId}",
                PORTFOLIO_ID, ACCOUNT_ID, TRANSACTION_ID)
                .param("softDelete", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(null)))
                .andExpect(status().isNoContent());
    }

    private PortfolioId toPortfolioId(String id) {
        return new PortfolioId(UUID.fromString(id));
    }

    private AccountId toAccountId(String id) {
        return new AccountId(UUID.fromString(id));
    }
}
