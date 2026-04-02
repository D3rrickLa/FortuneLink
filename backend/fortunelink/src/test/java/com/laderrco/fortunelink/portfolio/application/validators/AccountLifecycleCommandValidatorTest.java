package com.laderrco.fortunelink.portfolio.application.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.commands.CreateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeleteAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.ReopenAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdateAccountCommand;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AccountLifecycleCommandValidatorTest {

  // Constants for testing
  private static final String VALID_NAME = "Standard Investment Account";
  // 101 characters
  private static final String LONG_NAME = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
  // 100 characters
  private static final String NAME_100_CHARS = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB";
  private static final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private static final UserId USER_ID = UserId.random();
  private static final AccountId ACCOUNT_ID = AccountId.newId();
  private AccountLifecycleCommandValidator validator;

  @BeforeEach
  void setUp() {
    validator = new AccountLifecycleCommandValidator();
  }

  @Nested
  @DisplayName("validate(CreateAccountCommand)")
  class CreateAccountTests {
    @Test
    @DisplayName("validate: success with all valid fields")
    void validateSuccessWithValidFields() {
      var command = createValidCommand();
      ValidationResult result = validator.validate(command);
      assertThat(result.isValid()).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", LONG_NAME})
    @DisplayName("validate: failure on invalid account names")
    void validateFailureOnInvalidNames(String invalidName) {
      var command = new CreateAccountCommand(PORTFOLIO_ID, USER_ID, invalidName,
          AccountType.CHEQUING, PositionStrategy.ACB, Currency.of("USD"));

      ValidationResult result = validator.validate(command);

      assertThat(result.isValid()).isFalse();
      assertThat(result.errors()).anyMatch(e -> e.contains("Account name"));
    }

    @Test
    @DisplayName("validate: success with exactly 100 character name")
    void validateSuccessWithBoundaryNameLength() {
      var command = new CreateAccountCommand(PORTFOLIO_ID, USER_ID, NAME_100_CHARS,
          AccountType.CHEQUING, PositionStrategy.ACB, Currency.of("USD"));
      assertThat(validator.validate(command).isValid()).isTrue();
    }

    @Test
    @DisplayName("validate: failure when required fields are null")
    void validateFailureOnMissingRequiredFields() {
      var command = new CreateAccountCommand(PORTFOLIO_ID, USER_ID, VALID_NAME, null, null, null);

      ValidationResult result = validator.validate(command);

      assertThat(result.errors()).contains("Account type is required", "Strategy is required",
          "Base currency is required");
    }

    @Test
    @DisplayName("validate: should report only missing error when currency is null")
    void validateReportsOnlyMissingWhenNull() {
      var command = new CreateAccountCommand(PORTFOLIO_ID, USER_ID, VALID_NAME,
          AccountType.CHEQUING, PositionStrategy.ACB, null);

      ValidationResult result = validator.validate(command);

      assertThat(result.errors()).containsOnlyOnce("Base currency is required")
          .doesNotContain("Invalid currency code");
    }

    private CreateAccountCommand createValidCommand() {
      return new CreateAccountCommand(PORTFOLIO_ID, USER_ID, VALID_NAME, AccountType.MARGIN,
          PositionStrategy.ACB, Currency.of("USD"));
    }
  }

  @Nested
  @DisplayName("validate(UpdateAccountCommand)")
  class UpdateAccountTests {
    @Test
    @DisplayName("validate: success on valid update")
    void validateSuccessOnValidUpdate() {
      var command = new UpdateAccountCommand(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, "New Name");
      assertThat(validator.validate(command).isValid()).isTrue();
    }

    @Test
    @DisplayName("validate: failure when accountId is missing")
    void validateFailureOnMissingAccountId() {
      var command = new UpdateAccountCommand(PORTFOLIO_ID, USER_ID, null, VALID_NAME);

      ValidationResult result = validator.validate(command);

      assertThat(result.errors()).contains("AccountId is required");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("validate: failure on invalid names during update")
    void validateFailureOnInvalidNames(String invalidName) {
      var command = new UpdateAccountCommand(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, invalidName);
      assertThat(validator.validate(command).isValid()).isFalse();
    }
  }

  @Nested
  @DisplayName("validate(DeleteAccountCommand)")
  class DeleteAccountTests {
    @Test
    @DisplayName("validate: success on valid deletion command")
    void validateSuccessOnValidDelete() {
      var command = new DeleteAccountCommand(PORTFOLIO_ID, USER_ID, ACCOUNT_ID);
      assertThat(validator.validate(command).isValid()).isTrue();
    }

    @Test
    @DisplayName("validate: failure on null accountId")
    void validateFailureOnNullId() {
      var command = new DeleteAccountCommand(PORTFOLIO_ID, USER_ID, null);
      ValidationResult result = validator.validate(command);
      assertThat(result.errors()).contains("AccountId is required");
    }
  }

  @Nested
  @DisplayName("validate(ReopenAccountCommand)")
  class ReopenAccountTests {

    @Test
    @DisplayName("validate: success on valid reopen command")
    void validateSuccessOnValidReopen() {
      var command = createValidReopenCommand();
      ValidationResult result = validator.validate(command);

      assertThat(result.isValid()).isTrue();
      assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("validate: failure when accountId is missing")
    void validateFailureOnMissingAccountId() {
      var command = new ReopenAccountCommand(null, PORTFOLIO_ID, USER_ID);

      ValidationResult result = validator.validate(command);

      assertThat(result.isValid()).isFalse();
      assertThat(result.errors()).contains("AccountId is required");
    }

    @Test
    @DisplayName("validate: failure when portfolio or user IDs are invalid")
    void validateFailureOnInvalidContextIds() {
      // Here we assume ValidationUtils adds specific messages for these
      var command = new ReopenAccountCommand(ACCOUNT_ID, null, null);

      ValidationResult result = validator.validate(command);

      assertThat(result.isValid()).isFalse();
      // Checking that multiple errors are collected
      assertThat(result.errors()).hasSizeGreaterThanOrEqualTo(2);
    }

    private ReopenAccountCommand createValidReopenCommand() {
      return new ReopenAccountCommand(ACCOUNT_ID, PORTFOLIO_ID, USER_ID);
    }
  }

  @Nested
  @DisplayName("validate(CreateAccountCommand) - Currency Logic")
  class CurrencyValidationTests {
    @Test
    @DisplayName("validate: failure when currency code is invalid")
    void validateFailureOnInvalidCurrencyCode() {
      Currency invalidCurrency = mock(Currency.class);

      // When the validator calls .getCode(), return a junk string
      // This ensures ValidationUtils.isValidCurrency("JUNK_CODE") is called
      when(invalidCurrency.getCode()).thenReturn("JUNK_CODE");

      var command = new CreateAccountCommand(PORTFOLIO_ID, USER_ID, VALID_NAME, AccountType.RESP,
          PositionStrategy.ACB, invalidCurrency);

      ValidationResult result = validator.validate(command);

      assertThat(result.isValid()).isFalse();
      assertThat(result.errors()).contains("Invalid currency code");
    }
  }

  @Nested
  @DisplayName("Null Safety")
  class NullSafetyTests {
    @Test
    @DisplayName("validate: throws NullPointerException when command is null")
    void validateThrowsNpeOnNullCommand() {
      // Testing the Objects.requireNonNull(command)
      org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> {
        validator.validate((CreateAccountCommand) null);
      });
    }
  }
}