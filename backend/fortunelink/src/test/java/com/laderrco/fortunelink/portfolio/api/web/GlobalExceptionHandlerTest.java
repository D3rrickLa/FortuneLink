package com.laderrco.fortunelink.portfolio.api.web;

import com.laderrco.fortunelink.portfolio.application.exceptions.*;
import com.laderrco.fortunelink.portfolio.domain.exceptions.*;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("GlobalExceptionHandler Integration Tests")
class GlobalExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionTriggerController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Nested
  @DisplayName("400 Bad Request Mappings")
  class BadRequestTests {
    @Test
    void shouldHandleInvalidCommand() throws Exception {
      mockMvc.perform(get("/throw/invalid-command"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.errors[0]").value("field: error"));
    }

    @Test
    void shouldHandleMethodArgumentNotValid() throws Exception {
      mockMvc.perform(get("/throw/bean-validation"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.errors[0]").value("amount: must be positive"));
    }

    @Test
    void shouldHandleInvalidDateRange() throws Exception {
      mockMvc.perform(get("/throw/invalid-date"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }
  }

  @Nested
  @DisplayName("404 Not Found Mappings")
  class NotFoundTests {
    @Test
    void shouldHandleTransactionNotFound() throws Exception {
      mockMvc.perform(get("/throw/tx-not-found"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"));
    }

    @Test
    void shouldHandleAssetNotFound() throws Exception {
      mockMvc.perform(get("/throw/asset-not-found"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("ASSET_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("409 Conflict Mappings")
  class ConflictTests {
    @Test
    void shouldHandlePortfolioAlreadyDeleted() throws Exception {
      mockMvc.perform(get("/throw/already-deleted"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("PORTFOLIO_STATE_ERROR"));
    }

    @Test
    @DisplayName("409: PortfolioDeletionException should return PORTFOLIO_DELETION_ERROR")
    void handlePortfolioDeletion() throws Exception {
      mockMvc.perform(get("/throw/portfolio-deletion-error"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("PORTFOLIO_DELETION_ERROR"))
          .andExpect(jsonPath("$.message").value("Cannot delete portfolio with active subscriptions"));
    }

    @Test
    void shouldHandleAccountCannotBeReopened() throws Exception {
      mockMvc.perform(get("/throw/reopen-fail"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("ACCOUNT_CANNOT_BE_REOPENED"));
    }

    @Test
    void shouldHandleIllegalState() throws Exception {
      mockMvc.perform(get("/throw/illegal-state"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("CONFLICT"));
    }
  }

  @Nested
  @DisplayName("422 Unprocessable Content Mappings")
  class UnprocessableTests {
    @Test
    void shouldHandleInsufficientQuantity() throws Exception {
      mockMvc.perform(get("/throw/no-qty"))
          .andExpect(status().isUnprocessableContent())
          .andExpect(jsonPath("$.code").value("INSUFFICIENT_QUANTITY"));
    }
  }

  @Nested
  @DisplayName("Security & Utility Mappings")
  class MiscTests {
    @Test
    void shouldHandleAccessDenied() throws Exception {
      mockMvc.perform(get("/throw/access-denied"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("FORBIDDEN"))
          .andExpect(jsonPath("$.message").value("Access denied"));
    }
  }

  // -------------------------------------------------------------------------
  // Helper Controller
  // -------------------------------------------------------------------------
  @RestController
  static class ExceptionTriggerController {

    @GetMapping("/throw/invalid-command")
    void t1() {
      throw new InvalidCommandException("msg", List.of("field: error"));
    }

    @GetMapping("/throw/bean-validation")
    void t2() throws Exception {
      BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "req");
      result.addError(new FieldError("req", "amount", "must be positive"));
      MethodParameter param = new MethodParameter(this.getClass().getDeclaredMethod("t2"), -1);
      throw new MethodArgumentNotValidException(param, result);
    }

    @GetMapping("/throw/invalid-date")
    void t3() {
      throw new InvalidDateRangeException("msg");
    }

    @GetMapping("/throw/tx-not-found")
    void t4() {
      throw new TransactionNotFoundException(TransactionId.newId());
    }

    @GetMapping("/throw/asset-not-found")
    void t5() {
      throw new AssetNotFoundException("msg");
    }

    @GetMapping("/throw/already-deleted")
    void t6() {
      throw new PortfolioAlreadyDeletedException("msg");
    }

    @GetMapping("/throw/reopen-fail")
    void t7() {
      throw new AccountCannotBeReopenedException("msg");
    }

    @GetMapping("/throw/illegal-state")
    void t8() {
      throw new IllegalStateException("msg");
    }

    @GetMapping("/throw/no-qty")
    void t9() {
      throw new InsufficientQuantityException("msg");
    }

    @GetMapping("/throw/access-denied")
    void t10() {
      throw new AccessDeniedException("Forbidden");
    }

    @GetMapping("/throw/portfolio-deletion-error")
    void tPortfolioDeletion() {
      throw new PortfolioDeletionException("Cannot delete portfolio with active subscriptions");
    }
  }
}