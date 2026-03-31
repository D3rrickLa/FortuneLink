package com.laderrco.fortunelink.portfolio.application.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laderrco.fortunelink.portfolio.application.commands.ExcludeTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.commands.RestoreTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.*;
import com.laderrco.fortunelink.portfolio.domain.model.enums.FeeType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.*;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.*;

@ExtendWith(MockitoExtension.class)
class TransactionCommandValidatorTest {

  private TransactionCommandValidator validator;

  private static final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private static final UserId USER_ID = UserId.random();
  private static final AccountId ACCOUNT_ID = AccountId.newId();

  private static final Currency USD = Currency.of("USD");
  private static final Instant NOW = Instant.now();

  @BeforeEach
  void setUp() {
    validator = new TransactionCommandValidator();
  }

  private Money validMoney() {
    return new Money(BigDecimal.TEN, USD);
  }

  private List<Fee> validFees() {
    return List.of(Fee.of(FeeType.ACCOUNT_MAINTENANCE, new Money(BigDecimal.ONE, USD), NOW));
  }

  private void assertSuccess(ValidationResult result) {
    assertTrue(result.isValid());
  }

  private void assertFailure(ValidationResult result, String expectedMessage) {
    assertFalse(result.isValid());
    assertTrue(result.errors().contains(expectedMessage));
  }

  @Nested
  @DisplayName("validatePurchase: purchase validation")
  class PurchaseTests {
    @Test
    @DisplayName("shouldPassWithValidCommand")
    void shouldPassWithValidCommand() {
      RecordPurchaseCommand command = new RecordPurchaseCommand(
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          "AAPL",
          Quantity.of(1),
          new Price(validMoney()),
          validFees(),
          NOW,
          null);

      ValidationResult result = validator.validate(command);

      assertSuccess(result);
    }
  }

  // ------------------- SALE -------------------

  @Nested
  @DisplayName("validateSale: sale validation")
  class SaleTests {

    @Test
    @DisplayName("shouldPassWithValidCommand")
    void shouldPassWithValidCommand() {
      RecordSaleCommand command = new RecordSaleCommand(
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          "AAPL",
          Quantity.of(1),
          new Price(validMoney()),
          validFees(),
          NOW,
          null);

      assertSuccess(validator.validate(command));
    }
  }

  // ------------------- DEPOSIT -------------------

  @Nested
  @DisplayName("validateDeposit: deposit validation")
  class DepositTests {

    @Test
    @DisplayName("shouldFailWhenAmountInvalid")
    void shouldFailWhenAmountInvalid() {
      RecordDepositCommand command = new RecordDepositCommand(
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          new Money(BigDecimal.ZERO, USD),
          validFees(),
          NOW,
          null);

      ValidationResult result = validator.validate(command);

      assertFalse(result.isValid());
    }
  }

  @Nested
  @DisplayName("validateWithdrawal: withdrawal validation")
  class WithdrawalTests {

    @Test
    @DisplayName("shouldPassWithValidCommand")
    void shouldPassWithValidCommand() {
      RecordWithdrawalCommand command = new RecordWithdrawalCommand(
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          validMoney(),
          NOW,
          null);

      assertSuccess(validator.validate(command));
    }
  }

  @Nested
  @DisplayName("validateInterest: interest validation")
  class InterestTests {

    @Test
    @DisplayName("shouldValidateSymbolWhenAssetInterest")
    void shouldValidateSymbolWhenAssetInterest() {
      RecordInterestCommand command = new RecordInterestCommand(
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          "AAPL",
          validMoney(),
          NOW,
          null);

      assertSuccess(validator.validate(command));
    }
  }

  @Nested
  @DisplayName("validateDividend: dividend validation")
  class DividendTests {

    @Test
    @DisplayName("shouldPassWithValidCommand")
    void shouldPassWithValidCommand() {
      RecordDividendCommand command = new RecordDividendCommand(
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          "AAPL",
          validMoney(),
          NOW,
          null);

      assertSuccess(validator.validate(command));
    }
  }

  @Nested
  @DisplayName("validateDividendReinvestment: DRIP validation")
  class DripTests {

    @Test
    @DisplayName("shouldFailWhenExecutionMissing")
    void shouldFailWhenExecutionMissing() {
      RecordDividendReinvestmentCommand command = new RecordDividendReinvestmentCommand(
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          "AAPL",
          null,
          NOW,
          null);

      ValidationResult result = validator.validate(command);

      assertFailure(result, "Drip execution is required");
    }
  }

  @Nested
  @DisplayName("validateSplit: split validation")
  class SplitTests {

    @Test
    @DisplayName("shouldFailWhenRatioIsOneToOne")
    void shouldFailWhenRatioIsOneToOne() {
      RecordSplitCommand command = new RecordSplitCommand(
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          "AAPL",
          new Ratio(1, 1),
          NOW,
          null);

      ValidationResult result = validator.validate(command);

      assertFailure(result, "Ratio: A 1:1 split is a no-op and is not permitted");
    }

    @Test
    @DisplayName("shouldFailWhenRatioEqual")
    void shouldFailWhenRatioInvalid() {
      RecordSplitCommand command = new RecordSplitCommand(
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          "AAPL",
          new Ratio(2, 2),
          NOW,
          null);

      ValidationResult result = validator.validate(command);

      assertFailure(result, "Ratio: A 1:1 split is a no-op and is not permitted");
    }

    @Test
    @DisplayName("shouldFailWhenRatioNull")
    void shouldFailWhenRatioNull() {
      RecordSplitCommand command = new RecordSplitCommand(
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          "AAPL",
          null,
          NOW,
          null);

      ValidationResult result = validator.validate(command);

      assertFailure(result, "Split ratio is required");
    }
  }

  @Nested
  @DisplayName("validateReturnOfCapital: ROC validation")
  class RocTests {

    @Test
    @DisplayName("shouldPassWithValidCommand")
    void shouldPassWithValidCommand() {
      RecordReturnOfCaptialCommand command = new RecordReturnOfCaptialCommand(
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          "AAPL",
          new Price(validMoney()),
          Quantity.of(1),
          NOW,
          null);

      assertSuccess(validator.validate(command));
    }
  }

  @Nested
  @DisplayName("validateFee: fee validation")
  class FeeTests {

    @Test
    @DisplayName("shouldFailWhenFeeTypeMissing")
    void shouldFailWhenFeeTypeMissing() {
      RecordFeeCommand command = new RecordFeeCommand(
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          validMoney(),
          null,
          NOW,
          null);

      ValidationResult result = validator.validate(command);

      assertFailure(result, "Fee type is required");
    }
  }

  @Nested
  @DisplayName("validateExcludeTransaction: exclusion validation")
  class ExcludeTests {
    @Test
    @DisplayName("validate: passes")
    void success() {
      String longReason = "a".repeat(500);

      ExcludeTransactionCommand command = new ExcludeTransactionCommand(
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          TransactionId.newId(),
          longReason);

      ValidationResult result = validator.validate(command);

      assertSuccess(result);
    }

    @MethodSource("provideInvalidReasons")
    @DisplayName("validate: reason validation constraints")
    void shouldFailWhenReasonIsInvalid(String reason, String expectedErrorMessage) {
      ExcludeTransactionCommand command = new ExcludeTransactionCommand(
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          TransactionId.newId(),
          reason);

      ValidationResult result = validator.validate(command);

      assertFailure(result, expectedErrorMessage);
    }

    private static Stream<Arguments> provideInvalidReasons() {
      return Stream.of(
          // Case: Null
          Arguments.of(null, "Reason is required"),

          // Case: Empty string
          Arguments.of("", "Reason is required"),

          // Case: Whitespace only
          Arguments.of("   ", "Reason is required"),
          Arguments.of("\n\n", "Reason is required"),

          // Case: More than 500 characters
          Arguments.of("a".repeat(501), "Reason must be 500 characters or less"));
    }
  }

  @Nested
  @DisplayName("validateRestoreTransaction: restore validation")
  class RestoreTests {

    @Test
    @DisplayName("shouldFailWhenTransactionIdMissing")
    void shouldFailWhenTransactionIdMissing() {
      RestoreTransactionCommand command = new RestoreTransactionCommand(
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          null);

      ValidationResult result = validator.validate(command);

      assertFailure(result, "TransactionId is required");
    }
  }

  @Nested
  @DisplayName("validateTransferCommands: transfer in/out validation")
  class TransferTests {

    @Test
    @DisplayName("validateTransferIn: shouldValidateAmountAndDate")
    void shouldValidateTransferIn() {
      RecordTransferInCommand command = new RecordTransferInCommand(
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          new Money(BigDecimal.ZERO, USD),
          List.of(),
          NOW,
          null);

      ValidationResult result = validator.validate(command);

      assertFalse(result.isValid());
    }

    @Test
    @DisplayName("validateTransferOut: shouldValidateAmountAndDate")
    void shouldValidateTransferOut() {
      RecordTransferOutCommand command = new RecordTransferOutCommand(
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          new Money(BigDecimal.ZERO, USD),
          NOW,
          null);

      ValidationResult result = validator.validate(command);

      assertFalse(result.isValid());
    }
  }

  @Nested
  @DisplayName("validateCommonIds: shared validation")
  class CommonValidationTests {
    @Test
    @DisplayName("shouldFailWhenPortfolioIdMissing")
    void shouldFailWhenPortfolioIdMissing() {
      RecordDepositCommand command = new RecordDepositCommand(
          null, USER_ID, ACCOUNT_ID,
          validMoney(),
          validFees(),
          NOW,
          null);

      ValidationResult result = validator.validate(command);

      assertFailure(result, "PortfolioId is required");
    }

    @Test
    @DisplayName("shouldFailWhenAllCommonMissing")
    void shouldFailWhenAllMissing() {
      RecordDepositCommand command = new RecordDepositCommand(
          null, null, null,
          validMoney(),
          validFees(),
          NOW,
          null);

      ValidationResult result = validator.validate(command);

      assertThat(result.isValid()).isFalse();
    }
  }
}