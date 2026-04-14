package com.laderrco.fortunelink.portfolio.application.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.exceptions.InvalidCommandException;
import com.laderrco.fortunelink.portfolio.application.utils.annotations.HasPortfolioId;
import com.laderrco.fortunelink.portfolio.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ValidationUtilsTest {
  @Nested
  @DisplayName("validate: Functional Wrapper Logic")
  class GenericValidateTests {

    @Test
    @DisplayName("validate: should throw InvalidCommandException when validation fails")
    void validateshouldThrowExceptionOnFailure() {
      Object command = new Object();
      List<String> errorList = List.of("Error 1");
      ValidationResult failure = new ValidationResult(false, errorList);
      Function<Object, ValidationResult> fn = (c) -> failure;

      InvalidCommandException ex = assertThrows(InvalidCommandException.class,
          () -> ValidationUtils.validate(command, fn, "CreateAccount"));

      assertTrue(ex.getMessage().contains("CreateAccount"));

      
      assertEquals(errorList, ex.getErrors());
    }

    @Test
    @DisplayName("validate: should complete normally when validation passes")
    void validateshouldPassWhenValid() {
      Object command = new Object();
      ValidationResult success = new ValidationResult(true, List.of());
      Function<Object, ValidationResult> fn = (c) -> success;

      assertDoesNotThrow(() -> ValidationUtils.validate(command, fn, "CreateAccount"));
    }
  }

  @Nested
  @DisplayName("validatePortfolioAndUserIds: Identity Validation")
  class IdentityValidationTests {
    @Test
    @DisplayName("validatePortfolioAndUserIds: should add errors when IDs are missing")
    void validatePortfolioAndUserIdsShouldDetectNulls() {
      List<String> errors = new ArrayList<>();
      HasPortfolioId command = mock(HasPortfolioId.class);
      when(command.portfolioId()).thenReturn(null);
      when(command.userId()).thenReturn(null);

      ValidationUtils.validatePortfolioAndUserIds(command, errors);

      assertEquals(2, errors.size());
      assertTrue(errors.contains("PortfolioId is required"));
      assertTrue(errors.contains("UserId is required"));
    }

    @Test
    @DisplayName("validatePortfolioAndUserIds: passes")
    void validatePortfolioAndUserIdsPasses() {
      List<String> errors = new ArrayList<>();
      HasPortfolioId command = mock(HasPortfolioId.class);
      when(command.portfolioId()).thenReturn(PortfolioId.newId());
      when(command.userId()).thenReturn(UserId.random());

      ValidationUtils.validatePortfolioAndUserIds(command, errors);

      assertEquals(0, errors.size());
    }
  }

  @Nested
  @DisplayName("validateAmount: Numeric Validation")
  class AmountValidationTests {

    @ParameterizedTest
    @ValueSource(strings = {"-1.0", "0.0"})
    @DisplayName("validateAmount: should add error for non-positive amounts")
    void validateAmountshouldRejectNonPositive(String amountStr) {
      List<String> errors = new ArrayList<>();
      ValidationUtils.validateAmount(new BigDecimal(amountStr), errors);
      ValidationUtils.validateAmount(null, errors);

      assertTrue(errors.contains("Amount must be positive"));
      assertTrue(errors.contains("Amount is required"));
    }

    @Test
    @DisplayName("validateAmount: should pass for positive amount")
    void validateAmountshouldAcceptPositive() {
      List<String> errors = new ArrayList<>();
      ValidationUtils.validateAmount(BigDecimal.TEN, errors);
      assertTrue(errors.isEmpty());
    }
  }

  @Nested
  @DisplayName("validateQuantity: Precision and Scale Validation")
  class QuantityValidationTests {

    @Test
    @DisplayName("validateQuantity: should reject excessive decimal places")
    void validateQuantityshouldRejectTooMuchScale() {
      List<String> errors = new ArrayList<>();
      Quantity invalid = mock(Quantity.class);
      when(invalid.amount()).thenReturn(BigDecimal.TEN.setScale(20));

      ValidationUtils.validateQuantity(null, errors);
      ValidationUtils.validateQuantity(Quantity.ZERO, errors);
      ValidationUtils.validateQuantity(invalid, errors);

      assertTrue(errors.contains("Quantity is required"));
      assertTrue(errors.contains("Quantity must be greater than zero"));
      assertTrue(errors.contains("Quantity can have at most 8 decimal places"));
    }
  }

  @Nested
  @DisplayName("validateSymbol: String Format Validation")
  class SymbolValidationTests {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  ", "invalidsymbol!", "THISSYMBOLISTOOLONGFORTHEREGEX"})
    @DisplayName("validateSymbol: should reject null, empty, or malformed symbols")
    void validateSymbolshouldRejectInvalidFormats(String symbol) {
      List<String> errors = new ArrayList<>();
      ValidationUtils.validateSymbol(symbol, errors);
      assertFalse(errors.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"AAPL", "BTC.USD", "GOOG-123", "005930.KS"})
    @DisplayName("validateSymbol: should accept valid alphanumeric and allowed character formats")
    void validateSymbolshouldAcceptValidFormats(String symbol) {
      List<String> errors = new ArrayList<>();
      ValidationUtils.validateSymbol(symbol, errors);
      assertTrue(errors.isEmpty());
    }

    @Test
    void normalizedSymbolSuccess() {
      String symbol = "aapl";
      String normalized = ValidationUtils.normalizeSymbol(symbol);
      assertThat(normalized).isUpperCase();
      assertThat(ValidationUtils.normalizeSymbol(null)).isNull();
    }
  }

  @Nested
  @DisplayName("isValidCurrency: Boolean Check")
  class CurrencyValidationTests {
    @ParameterizedTest
    @CsvSource(value = {"USD, true", "EUR, true", "INVALID, false", "null, false", "' ', false"
        
    }, nullValues = "null")
    @DisplayName("isValidCurrency: should return correct boolean for currency codes including nulls")
    void isValidCurrencyshouldReturnExpectedResult(String code, boolean expected) {
      
      
      assertEquals(expected, ValidationUtils.isValidCurrency(code));
    }
  }
}