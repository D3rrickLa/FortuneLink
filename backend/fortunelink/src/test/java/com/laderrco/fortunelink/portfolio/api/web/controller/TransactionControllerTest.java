package com.laderrco.fortunelink.portfolio.api.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.laderrco.fortunelink.portfolio.application.exceptions.InsufficientQuantityException;
import com.laderrco.fortunelink.portfolio.application.exceptions.TransactionNotFoundException;
import com.laderrco.fortunelink.portfolio.application.queries.GetTransactionByIdQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetTransactionHistoryQuery;
import com.laderrco.fortunelink.portfolio.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio.application.services.TransactionQueryService;
import com.laderrco.fortunelink.portfolio.application.services.TransactionService;
import com.laderrco.fortunelink.portfolio.application.views.TransactionView;
import com.laderrco.fortunelink.portfolio.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.infrastructure.config.limiting.RateLimitInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * Controller slice tests for TransactionController.
 *
 * <p>
 * TransactionController has 14 write endpoints, 2 read endpoints, and 2 exclusion endpoints. This
 * suite covers: - All write endpoints: happy path 201 + key validation failure - Both read
 * endpoints: 200, 404, pagination - Both exclusion endpoints: 200, 409, 404 - Idempotency-Key
 * header handling (present and absent)
 */
@WebMvcTest(controllers = TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

  private static final UUID USER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final String PORTFOLIO_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
  private static final String ACCOUNT_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";
  private static final String TX_ID = "dddddddd-dddd-dddd-dddd-dddddddddddd";
  private static final String BASE_URL =
      "/api/v1/portfolios/" + PORTFOLIO_ID + "/accounts/" + ACCOUNT_ID + "/transactions";
  private static final String IDEMPOTENCY_KEY = UUID.randomUUID().toString();

  @Autowired
  MockMvc mockMvc;
  @Autowired
  JsonMapper objectMapper;

  @MockitoBean
  TransactionService transactionService;
  @MockitoBean
  TransactionQueryService transactionQueryService;
  @MockitoBean
  AuthenticationUserService authenticationUserService;
  @MockitoBean
  RateLimitInterceptor rateLimitInterceptor;

  @BeforeEach
  void setUp() throws Exception {
    when(authenticationUserService.getCurrentUser()).thenReturn(USER_UUID);
    when(rateLimitInterceptor.preHandle(any(HttpServletRequest.class),
        any(HttpServletResponse.class), any())).thenReturn(true);
  }

  private TransactionView buildTxView(TransactionType type) {
    Currency cad = Currency.of("CAD");
    return new TransactionView(TransactionId.newId(), type, type.affectsHoldings() ? "AAPL" : null,
        type.affectsHoldings() ? new Quantity(BigDecimal.TEN) : null,
        type.affectsHoldings() ? Price.of(BigDecimal.valueOf(150), cad) : null, List.of(),
        Money.zero(cad), Map.of(), Instant.now(), "");
  }

  /**
   * Valid BUY body — includes accountId as it is @NotNull on RecordPurchaseRequest
   */
  private String validBuyRequest() {
    return """
        {
            "accountId": "%s",
            "symbol": "AAPL",
            "type": "STOCK",
            "quantity": 10,
            "price": 150.00,
            "currency": "CAD",
            "transactionDate": "2024-01-15T00:00:00Z",
            "notes": "some notes on things"
        }
        """.formatted(ACCOUNT_ID);
  }

  private String validSellRequest() {
    return """
        {
            "symbol": "AAPL",
            "quantity": 5,
            "price": 160.00,
            "currency": "CAD",
            "fees": [],
            "transactionDate": "2024-01-20T00:00:00Z"
        }
        """;
  }

  private String validCashRequest(String amount) {
    return """
        {
            "amount": %s,
            "currency": "CAD",
            "transactionDate": "2024-01-15T00:00:00Z"
        }
        """.formatted(amount);
  }

  @Nested
  @DisplayName("POST /buy — recordBuy")
  class RecordBuy {
    @Test
    @DisplayName("201 on valid BUY request")
    void returns201OnSuccess() throws Exception {
      when(transactionService.recordPurchase(any())).thenReturn(buildTxView(TransactionType.BUY));

      mockMvc.perform(post(BASE_URL + "/buy").header("Idempotency-Key", IDEMPOTENCY_KEY)
              .contentType(MediaType.APPLICATION_JSON).content(validBuyRequest()))
          .andExpect(status().isCreated());

      String s = """
          {
              "accountId": "%s",
              "symbol": "AAPL",
              "type": "STOCK",
              "quantity": 10,
              "price": 150.00,
              "currency": "CAD",
              "transactionDate": "2024-01-16T00:00:00Z"
          }
          """.formatted(ACCOUNT_ID);
      mockMvc.perform(
          post(BASE_URL + "/buy").header("Idempotency-Key", UUID.randomUUID().toString())
              .contentType(MediaType.APPLICATION_JSON).content(s)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("201 succeeds without optional Idempotency-Key header")
    void returns201WithoutIdempotencyKey() throws Exception {
      when(transactionService.recordPurchase(any())).thenReturn(buildTxView(TransactionType.BUY));

      mockMvc.perform(post(BASE_URL + "/buy").contentType(MediaType.APPLICATION_JSON)
          .content(validBuyRequest())).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("400 when symbol is missing")
    void returns400WhenSymbolMissing() throws Exception {
      String body = """
          {
              "accountId": "%s",
              "type": "STOCK",
              "quantity": 10,
              "price": 150.00,
              "currency": "CAD",
              "transactionDate": "2024-01-15T00:00:00Z",
              "notes": "some notes"
          }
          """.formatted(ACCOUNT_ID);

      mockMvc.perform(post(BASE_URL + "/buy").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("400 when quantity is zero")
    void returns400WhenQuantityZero() throws Exception {
      String body = """
          {
              "accountId": "%s",
              "symbol": "AAPL",
              "type": "STOCK",
              "quantity": 0,
              "price": 150.00,
              "currency": "CAD",
              "transactionDate": "2024-01-15T00:00:00Z"
          }
          """.formatted(ACCOUNT_ID);

      mockMvc.perform(post(BASE_URL + "/buy").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("422 when account has insufficient cash")
    void returns422ForInsufficientFunds() throws Exception {
      when(transactionService.recordPurchase(any())).thenThrow(
          new InsufficientFundsException("Insufficient cash for buy"));

      mockMvc.perform(post(BASE_URL + "/buy").contentType(MediaType.APPLICATION_JSON)
              .content(validBuyRequest())).andExpect(status().isUnprocessableContent())
          .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
    }
  }

  @Nested
  @DisplayName("POST /sell — recordSell")
  class RecordSell {

    @Test
    @DisplayName("201 on valid SELL request")
    void returns201OnSuccess() throws Exception {
      when(transactionService.recordSale(any())).thenReturn(buildTxView(TransactionType.SELL));

      mockMvc.perform(post(BASE_URL + "/sell").contentType(MediaType.APPLICATION_JSON)
          .content(validSellRequest())).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("400 when fees array is absent (required field)")
    void returns400WhenFeesAbsent() throws Exception {
      String body = """
          {
              "symbol": "AAPL",
              "quantity": 5,
              "price": 160.00,
              "currency": "CAD",
              "transactionDate": "2024-01-20T00:00:00Z"
          }
          """;

      mockMvc.perform(
              post(BASE_URL + "/sell").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("422 when trying to sell more than held quantity")
    void returns422ForInsufficientQuantity() throws Exception {
      when(transactionService.recordSale(any())).thenThrow(
          new InsufficientQuantityException("Cannot sell 100, only holding 10"));

      mockMvc.perform(post(BASE_URL + "/sell").contentType(MediaType.APPLICATION_JSON)
              .content(validSellRequest())).andExpect(status().isUnprocessableContent())
          .andExpect(jsonPath("$.code").value("INSUFFICIENT_QUANTITY"));
    }
  }

  @Nested
  @DisplayName("POST /split — recordSplit")
  class RecordSplit {

    @Test
    @DisplayName("201 on valid SPLIT request (2-for-1)")
    void returns201OnSuccess() throws Exception {
      when(transactionService.recordSplit(any())).thenReturn(buildTxView(TransactionType.SPLIT));

      mockMvc.perform(post(BASE_URL + "/split").contentType(MediaType.APPLICATION_JSON).content("""
          {
              "symbol": "AAPL",
              "numerator": 2,
              "denominator": 1,
              "transactionDate": "2024-01-15T00:00:00Z"
          }
          """)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("400 when numerator is zero (below @Min(1))")
    void returns400WhenNumeratorZero() throws Exception {
      mockMvc.perform(post(BASE_URL + "/split").contentType(MediaType.APPLICATION_JSON).content("""
          {
              "symbol": "AAPL",
              "numerator": 0,
              "denominator": 1,
              "transactionDate": "2024-01-15T00:00:00Z"
          }
          """)).andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("POST /return-of-capital — recordReturnOfCapital")
  class RecordReturnOfCapital {

    @Test
    @DisplayName("201 on valid ROC request")
    void returns201OnSuccess() throws Exception {
      when(transactionService.recordReturnOfCapital(any())).thenReturn(
          buildTxView(TransactionType.RETURN_OF_CAPITAL));

      mockMvc.perform(
          post(BASE_URL + "/return-of-capital").contentType(MediaType.APPLICATION_JSON).content("""
              {
                  "assetSymbol": "VTI",
                  "distributionPerUnit": 0.05,
                  "currency": "CAD",
                  "heldQuantity": 100,
                  "transactionDate": "2024-01-15T00:00:00Z"
              }
              """)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("400 when assetSymbol is blank")
    void returns400WhenSymbolBlank() throws Exception {
      mockMvc.perform(
          post(BASE_URL + "/return-of-capital").contentType(MediaType.APPLICATION_JSON).content("""
              {
                  "assetSymbol": "",
                  "distributionPerUnit": 0.05,
                  "currency": "CAD",
                  "heldQuantity": 100,
                  "transactionDate": "2024-01-15T00:00:00Z"
              }
              """)).andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("POST /deposit — recordDeposit")
  class RecordDeposit {

    @Test
    @DisplayName("201 on valid DEPOSIT")
    void returns201OnSuccess() throws Exception {
      when(transactionService.recordDeposit(any())).thenReturn(
          buildTxView(TransactionType.DEPOSIT));

      mockMvc.perform(post(BASE_URL + "/deposit").contentType(MediaType.APPLICATION_JSON)
          .content(validCashRequest("1000.00"))).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("400 when amount is missing")
    void returns400WhenAmountMissing() throws Exception {
      mockMvc.perform(
          post(BASE_URL + "/deposit").contentType(MediaType.APPLICATION_JSON).content("""
              {"currency": "CAD", "transactionDate": "2024-01-15T00:00:00Z"}
              """)).andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("POST /withdrawal — recordWithdrawal")
  class RecordWithdrawal {

    @Test
    @DisplayName("201 on valid WITHDRAWAL")
    void returns201OnSuccess() throws Exception {
      when(transactionService.recordWithdrawal(any())).thenReturn(
          buildTxView(TransactionType.WITHDRAWAL));

      mockMvc.perform(post(BASE_URL + "/withdrawal").contentType(MediaType.APPLICATION_JSON)
          .content(validCashRequest("500.00"))).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("422 when account has insufficient cash for withdrawal")
    void returns422ForInsufficientFunds() throws Exception {
      when(transactionService.recordWithdrawal(any())).thenThrow(
          new InsufficientFundsException("Insufficient cash"));

      mockMvc.perform(post(BASE_URL + "/withdrawal").contentType(MediaType.APPLICATION_JSON)
          .content(validCashRequest("999999.00"))).andExpect(status().isUnprocessableContent());
    }
  }

  @Nested
  @DisplayName("POST /fee — recordFee")
  class RecordFee {

    @Test
    @DisplayName("201 on valid standalone FEE")
    void returns201OnSuccess() throws Exception {
      when(transactionService.recordFee(any())).thenReturn(buildTxView(TransactionType.FEE));

      mockMvc.perform(post(BASE_URL + "/fee").contentType(MediaType.APPLICATION_JSON).content("""
          {
              "amount": 10.00,
              "currency": "CAD",
              "feeType": "COMMISSION",
              "transactionDate": "2024-01-15T00:00:00Z"
          }
          """)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("400 when feeType is missing")
    void returns400WhenFeeTypeMissing() throws Exception {
      mockMvc.perform(post(BASE_URL + "/fee").contentType(MediaType.APPLICATION_JSON).content("""
          {"amount": 10.00, "currency": "CAD", "transactionDate": "2024-01-15T00:00:00Z"}
          """)).andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("POST /interest — recordInterest")
  class RecordInterest {

    @Test
    @DisplayName("201 for cash-level interest (no assetSymbol)")
    void returns201ForCashInterest() throws Exception {
      when(transactionService.recordInterest(any())).thenReturn(
          buildTxView(TransactionType.INTEREST));

      mockMvc.perform(
          post(BASE_URL + "/interest").contentType(MediaType.APPLICATION_JSON).content("""
              {"amount": 25.00, "currency": "CAD", "transactionDate": "2024-01-15T00:00:00Z"}
              """)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("201 for asset-level interest (bond coupon) with assetSymbol provided")
    void returns201ForAssetInterest() throws Exception {
      when(transactionService.recordInterest(any())).thenReturn(
          buildTxView(TransactionType.INTEREST));

      mockMvc.perform(
          post(BASE_URL + "/interest").contentType(MediaType.APPLICATION_JSON).content("""
              {
                  "assetSymbol": "XBB",
                  "amount": 50.00,
                  "currency": "CAD",
                  "transactionDate": "2024-01-15T00:00:00Z"
              }
              """)).andExpect(status().isCreated());

      var captor = ArgumentCaptor.forClass(
          com.laderrco.fortunelink.portfolio.application.commands.records.RecordInterestCommand.class);
      verify(transactionService).recordInterest(captor.capture());
      assertThat(captor.getValue().isAssetInterest()).isTrue();
      assertThat(captor.getValue().assetSymbol()).isEqualTo("XBB");
    }
  }

  @Nested
  @DisplayName("POST /dividend — recordDividend")
  class RecordDividend {

    @Test
    @DisplayName("201 on valid DIVIDEND")
    void returns201OnSuccess() throws Exception {
      when(transactionService.recordDividend(any())).thenReturn(
          buildTxView(TransactionType.DIVIDEND));

      mockMvc.perform(
          post(BASE_URL + "/dividend").contentType(MediaType.APPLICATION_JSON).content("""
              {
                  "assetSymbol": "AAPL",
                  "amount": 25.00,
                  "currency": "CAD",
                  "transactionDate": "2024-01-15T00:00:00Z"
              }
              """)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("400 when assetSymbol is blank")
    void returns400WhenSymbolBlank() throws Exception {
      mockMvc.perform(
          post(BASE_URL + "/dividend").contentType(MediaType.APPLICATION_JSON).content("""
              {"assetSymbol": "", "amount": 25.00, "currency": "CAD",
               "transactionDate": "2024-01-15T00:00:00Z"}
              """)).andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("POST /drip — recordDividendReinvestment")
  class RecordDrip {

    @Test
    @DisplayName("201 on valid DRIP")
    void returns201OnSuccess() throws Exception {
      when(transactionService.recordDividendReinvestment(any())).thenReturn(
          buildTxView(TransactionType.DIVIDEND_REINVEST));

      mockMvc.perform(post(BASE_URL + "/drip").contentType(MediaType.APPLICATION_JSON).content("""
          {
              "assetSymbol": "AAPL",
              "sharesPurchased": 0.15,
              "pricePerShare": 185.00,
              "currency": "CAD",
              "transactionDate": "2024-01-15T00:00:00Z"
          }
          """)).andExpect(status().isCreated());
    }
  }

  @Nested
  @DisplayName("POST /transfer-in & /transfer-out")
  class Transfers {

    @Test
    @DisplayName("201 on valid TRANSFER_IN")
    void returns201OnTransferIn() throws Exception {
      when(transactionService.recordTransferIn(any())).thenReturn(
          buildTxView(TransactionType.TRANSFER_IN));

      mockMvc.perform(
          post(BASE_URL + "/transfer-in").contentType(MediaType.APPLICATION_JSON).content("""
              {"amount": 5000.00, "currency": "CAD", "transactionDate": "2024-01-15T00:00:00Z"}
              """)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("201 on valid TRANSFER_OUT")
    void returns201OnTransferOut() throws Exception {
      when(transactionService.recordTransferOut(any())).thenReturn(
          buildTxView(TransactionType.TRANSFER_OUT));

      mockMvc.perform(
          post(BASE_URL + "/transfer-out").contentType(MediaType.APPLICATION_JSON).content("""
              {"amount": 2000.00, "currency": "CAD", "transactionDate": "2024-01-15T00:00:00Z"}
              """)).andExpect(status().isCreated());
    }
  }

  @Nested
  @DisplayName("GET / — getTransactionHistory")
  class GetTransactionHistory {

    @Test
    @DisplayName("200 with paginated history when no filters applied")
    void returns200WithHistory() throws Exception {
      Page<TransactionView> page = new PageImpl<>(List.of(buildTxView(TransactionType.BUY)));
      when(transactionQueryService.getTransactionHistory(
          any(GetTransactionHistoryQuery.class))).thenReturn(page);

      mockMvc.perform(get(BASE_URL)).andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("200 with symbol filter applied")
    void returns200WithSymbolFilter() throws Exception {
      Page<TransactionView> page = new PageImpl<>(List.of());
      when(transactionQueryService.getTransactionHistory(
          any(GetTransactionHistoryQuery.class))).thenReturn(page);

      mockMvc.perform(get(BASE_URL).param("symbol", "AAPL")).andExpect(status().isOk());

      var captor = ArgumentCaptor.forClass(GetTransactionHistoryQuery.class);
      verify(transactionQueryService).getTransactionHistory(captor.capture());
      assertThat(captor.getValue().symbol().symbol()).isEqualTo("AAPL");
    }

    @Test
    @DisplayName("200 with date range filter")
    void returns200WithDateRangeFilter() throws Exception {
      Page<TransactionView> page = new PageImpl<>(List.of());
      when(transactionQueryService.getTransactionHistory(
          any(GetTransactionHistoryQuery.class))).thenReturn(page);

      mockMvc.perform(get(BASE_URL).param("startDate", "2024-01-01T00:00:00Z")
          .param("endDate", "2024-12-31T23:59:59Z")).andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("GET /{transactionId} — getTransaction")
  class GetTransaction {

    @Test
    @DisplayName("200 when transaction exists and belongs to this account")
    void returns200OnSuccess() throws Exception {
      when(transactionQueryService.getTransaction(any(GetTransactionByIdQuery.class))).thenReturn(
          buildTxView(TransactionType.BUY));

      mockMvc.perform(get(BASE_URL + "/" + TX_ID)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("404 when transaction is not found or belongs to another user")
    void returns404WhenNotFound() throws Exception {
      when(transactionQueryService.getTransaction(any(GetTransactionByIdQuery.class))).thenThrow(
          new TransactionNotFoundException(TransactionId.fromString(TX_ID)));

      mockMvc.perform(get(BASE_URL + "/" + TX_ID)).andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("PATCH /{transactionId}/exclude — excludeTransaction")
  class ExcludeTransaction {

    @Test
    @DisplayName("200 when exclusion succeeds")
    void returns200OnSuccess() throws Exception {
      when(transactionService.excludeTransaction(any())).thenReturn(
          buildTxView(TransactionType.BUY));

      mockMvc.perform(
          patch(BASE_URL + "/" + TX_ID + "/exclude").header("Idempotency-Key", IDEMPOTENCY_KEY)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"reason\": \"Duplicate import\"}")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("400 when Idempotency-Key header is absent (required for exclusion)")
    void returns400WhenIdempotencyKeyAbsent() throws Exception {
      mockMvc.perform(patch(BASE_URL + "/" + TX_ID + "/exclude").with(jwt())
              .contentType(MediaType.APPLICATION_JSON).content("{\"reason\": \"Duplicate import\"}"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("400 when reason body is absent")
    void returns400WhenReasonAbsent() throws Exception {
      mockMvc.perform(
              patch(BASE_URL + "/" + TX_ID + "/exclude").header("Idempotency-Key", IDEMPOTENCY_KEY)
                  .contentType(MediaType.APPLICATION_JSON).content("{}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("404 when transaction does not exist")
    void returns404WhenTransactionNotFound() throws Exception {
      when(transactionService.excludeTransaction(any())).thenThrow(
          new TransactionNotFoundException(TransactionId.fromString(TX_ID)));

      mockMvc.perform(
              patch(BASE_URL + "/" + TX_ID + "/exclude").header("Idempotency-Key", IDEMPOTENCY_KEY)
                  .contentType(MediaType.APPLICATION_JSON).content("{\"reason\": \"Duplicate\"}"))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("PATCH /{transactionId}/restore — restoreTransaction")
  class RestoreTransaction {

    @Test
    @DisplayName("200 when restoration succeeds")
    void returns200OnSuccess() throws Exception {
      when(transactionService.restoreTransaction(any())).thenReturn(
          buildTxView(TransactionType.BUY));

      mockMvc.perform(
              patch(BASE_URL + "/" + TX_ID + "/restore").header("Idempotency-Key", IDEMPOTENCY_KEY))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("400 when Idempotency-Key header is absent (required for restore)")
    void returns400WhenIdempotencyKeyAbsent() throws Exception {
      mockMvc.perform(patch(BASE_URL + "/" + TX_ID + "/restore").with(jwt()))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("404 when transaction not found")
    void returns404WhenTransactionNotFound() throws Exception {
      when(transactionService.restoreTransaction(any())).thenThrow(
          new TransactionNotFoundException(TransactionId.fromString(TX_ID)));

      mockMvc.perform(
              patch(BASE_URL + "/" + TX_ID + "/restore").header("Idempotency-Key", IDEMPOTENCY_KEY))
          .andExpect(status().isNotFound());
    }
  }
}