package com.laderrco.fortunelink.portfolio.application.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

public class PortfolioLifecycleCommandValidatorTest {
  private static final String VALID_NAME = "Retirement Portfolio";
  private static final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private static final UserId USER_ID = UserId.random();
  private static final Currency USD = Currency.of("USD");
  private PortfolioLifecycleCommandValidator validator;

  @BeforeEach
  void setUp() {
    validator = new PortfolioLifecycleCommandValidator();
  }

  @Nested
  @DisplayName("validate(CreatePortfolioCommand)")
  class CreatePortfolioTests {
    @Test
    @DisplayName("validate: success on valid command")
    void validateSuccessOnValidCommand() {
      var command = new CreatePortfolioCommand(USER_ID, VALID_NAME, null, USD, false,
          AccountType.CHEQUING, PositionStrategy.ACB);
      ValidationResult result = validator.validate(command);
      assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("validate: failure when userId is missing")
    void validateFailureOnMissingUserId() {
      var command = new CreatePortfolioCommand(null, VALID_NAME, null, USD, false,
          AccountType.CHEQUING, PositionStrategy.ACB);
      ValidationResult result = validator.validate(command);
      assertThat(result.errors()).contains("UserId is required");
    }

    @Test
    @DisplayName("validate: failure when currency is missing")
    void validateFailureOnMissingCurrency() {
      var command = new CreatePortfolioCommand(USER_ID, VALID_NAME, null, null, false,
          AccountType.CHEQUING, PositionStrategy.ACB);
      ValidationResult result = validator.validate(command);
      assertThat(result.errors()).contains("Currency is required");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ",
        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
    @DisplayName("validate: failure on invalid portfolio names")
    void validateFailureOnInvalidNames(String invalidName) {
      var command = new CreatePortfolioCommand(USER_ID, invalidName, null, USD, false,
          AccountType.CHEQUING, PositionStrategy.ACB);
      ValidationResult result = validator.validate(command);
      assertThat(result.errors()).anyMatch(e -> e.contains("Portfolio name"));
    }

    @Test
    @DisplayName("validate: failure when strategy missing for default account")
    void validateFailureOnMissingStrategyWithDefaultAccount() {

      var command = new CreatePortfolioCommand(USER_ID, VALID_NAME, "DSEC", USD, true,
          AccountType.CHEQUING, null);
      ValidationResult result = validator.validate(command);
      assertThat(result.errors()).contains(
          "Position strategy is required when createDefaultAccount is true");
    }

    @Test
    @DisplayName("validate: success when strategy present for default account")
    void validateSuccessWithDefaultAccountAndStrategy() {
      var command = new CreatePortfolioCommand(USER_ID, VALID_NAME, null, USD, true,
          AccountType.CHEQUING, PositionStrategy.ACB);
      assertThat(validator.validate(command).isValid()).isTrue();
    }
  }

  @Nested
  @DisplayName("validate(UpdatePortfolioCommand)")
  class UpdatePortfolioTests {

    private static final int MAX_DESC = 500;
    private static final String VALID_DESC = "A standard investment portfolio.";
    private static final String LONG_DESC = "A".repeat(MAX_DESC + 1);
    private static final String BOUNDARY_DESC = "B".repeat(MAX_DESC);

    @Test
    @DisplayName("validate: success with all fields valid")
    void validateSuccessWithAllFields() {
      var command = new UpdatePortfolioCommand(PORTFOLIO_ID, USER_ID, VALID_NAME, VALID_DESC,
          Currency.of("USD"));

      ValidationResult result = validator.validate(command);

      assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("validate: success when optional fields are null")
    void validateSuccessWithNullOptionals() {
      var command = new UpdatePortfolioCommand(PORTFOLIO_ID, USER_ID, VALID_NAME, null, null);

      ValidationResult result = validator.validate(command);

      assertThat(result.isValid()).isTrue();
    }

    @Nested
    @DisplayName("Description Validation")
    class DescriptionTests {

      @Test
      @DisplayName("validate: success with description at maximum length")
      void validateSuccessAtMaxLength() {
        var command = new UpdatePortfolioCommand(PORTFOLIO_ID, USER_ID, VALID_NAME, BOUNDARY_DESC,
            null);
        assertThat(validator.validate(command).isValid()).isTrue();
      }

      @Test
      @DisplayName("validate: failure when description exceeds limit")
      void validateFailureOnTooLongDescription() {
        var command = new UpdatePortfolioCommand(PORTFOLIO_ID, USER_ID, VALID_NAME, LONG_DESC,
            null);

        ValidationResult result = validator.validate(command);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).contains("Description must be 500 characters or less");
      }
    }

    @Nested
    @DisplayName("Currency Validation")
    class CurrencyTests {

      @Test
      @DisplayName("validate: failure when currency code is invalid")
      void validateFailureOnInvalidCurrency() {

        Currency mockCurrency = mock(Currency.class);
        when(mockCurrency.getCode()).thenReturn("INVALID_XYZ");

        var command = new UpdatePortfolioCommand(PORTFOLIO_ID, USER_ID, VALID_NAME, VALID_DESC,
            mockCurrency);

        ValidationResult result = validator.validate(command);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).contains("Invalid currency code");
      }
    }
  }

  @Nested
  @DisplayName("validate(DeletePortfolioCommand)")
  class DeletePortfolioTests {
    @Test
    @DisplayName("validate: success")
    void validateSuccessNoErrors() {
      var command = new DeletePortfolioCommand(PORTFOLIO_ID, USER_ID, false, true);
      assertThat(validator.validate(command).isValid()).isTrue();
    }

    @Test
    @DisplayName("validate: errors when Id is null")
    void validateFailure() {
      var command = new DeletePortfolioCommand(null, USER_ID, false, true);
      assertThat(validator.validate(command).isValid()).isFalse();
    }
  }

  @Nested
  @DisplayName("Null Safety")
  class NullSafetyTests {
    @Test
    @DisplayName("validate: throws NullPointerException on null command")
    void validateThrowsNpeOnNullCommand() {
      assertThatThrownBy(() -> validator.validate((CreatePortfolioCommand) null)).isInstanceOf(
          NullPointerException.class);
    }
  }
}
