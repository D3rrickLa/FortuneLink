package com.laderrco.fortunelink.portfolio_management.application.validators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.laderrco.fortunelink.portfolio_management.application.commands.AddAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CorrectAssetTickerCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeleteTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordFeeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordIncomeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RemoveAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdateTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.FeeType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CashIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@DisplayName("CommandValidator Tests")
class CommandValidatorTest {

    private CommandValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CommandValidator();
    }

    @Nested
    @DisplayName("RecordPurchaseCommand Validation Tests")
    class RecordPurchaseCommandTests {
        
        @Test
        @DisplayName("Should pass validation for valid purchase command")
        void shouldPassValidationForValidCommand() {
            RecordPurchaseCommand command = createValidPurchaseCommand();
            
            ValidationResult result = validator.validate(command);
            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }
        
        @Test
        @DisplayName("Should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            RecordPurchaseCommand command = new RecordPurchaseCommand(
                null,
                AccountId.randomId(),
                "AAPL",
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.of("USD")),
                Collections.emptyList(),
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("UserId is required");
        }
        
        @Test
        @DisplayName("Should fail when accountId is null")
        void shouldFailWhenAccountIdIsNull() {
            RecordPurchaseCommand command = new RecordPurchaseCommand(
                UserId.randomId(),
                null,
                "AAPL",
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.of("USD")),
                Collections.emptyList(),
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("AccountId is required");
        }
        
        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should fail when symbol is null or empty")
        void shouldFailWhenSymbolIsNullOrEmpty(String symbol) {
            RecordPurchaseCommand command = new RecordPurchaseCommand(
                UserId.randomId(),
                AccountId.randomId(),
                symbol,
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.of("USD")),
                Collections.emptyList(),
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Asset symbol is required");
        }
        
        @ParameterizedTest
        @ValueSource(strings = {"aapl", "AAPL!", "&", "TOOLONGSYMBOL123"})
        @DisplayName("Should fail for invalid symbol format")
        void shouldFailForInvalidSymbolFormat(String symbol) {
            RecordPurchaseCommand command = new RecordPurchaseCommand(
                UserId.randomId(),
                AccountId.randomId(),
                symbol,
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.of("USD")),
                Collections.emptyList(),
                Instant.now().minus(Duration.ofHours(36)),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("symbol"));
            IO.println(result.errors());
        }
        
        @Test
        @DisplayName("Should fail when quantity is invalid")
        void shouldFailWhenQuantityIsInvalid() {
            RecordPurchaseCommand command = new RecordPurchaseCommand(
                UserId.randomId(),
                AccountId.randomId(),
                "AAPL",
                BigDecimal.ZERO,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.of("USD")),
                Collections.emptyList(),
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("Quantity"));
        }
        
        @Test
        @DisplayName("Should fail when price amount is invalid")
        void shouldFailWhenPriceAmountIsInvalid() {
            RecordPurchaseCommand command = new RecordPurchaseCommand(
                UserId.randomId(),
                AccountId.randomId(),
                "AAPL",
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(-10), ValidatedCurrency.of("USD")),
                Collections.emptyList(),
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("Amount"));
        }
        
        // this test can never happen technically
        // but we're doing it via mock
        @Test
        @DisplayName("Should fail when currency is null")
        void shouldFailWhenCurrencyIsNull() {
            Money money = mock(Money.class);
            RecordPurchaseCommand command = new RecordPurchaseCommand(
                UserId.randomId(),
                AccountId.randomId(),
                "AAPL",
                BigDecimal.TEN,
                money,
                Collections.emptyList(),
                Instant.now().minus(Duration.ofHours(35)),
                "notes"
            );
            
            when(money.currency()).thenReturn(null);
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Currency is required");
            
        }

        @Test
        @DisplayName("Should fail when currency code is invalid")
        void shouldFailWhenCurrencyCodeIsInvalid() {
            Money money = mock(Money.class);
            RecordPurchaseCommand command = new RecordPurchaseCommand(
                UserId.randomId(),
                AccountId.randomId(),
                "AAPL",
                BigDecimal.TEN,
                money,
                Collections.emptyList(),
                Instant.now().minus(Duration.ofHours(35)),
                "notes"
            );
            
            when(money.currency()).thenReturn(mock(ValidatedCurrency.class));
            when(money.currency().getCode()).thenReturn("null");
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Invalid currency code");
            
        }
        
        // this also can't happen as fees itself has a way to protect this
        @Test
        @DisplayName("Should validate fees correctly")
        void shouldValidateFeesCorrectly() {    
            Fee invalidFee = mock(Fee.class);
            Money money = mock(Money.class);
            RecordPurchaseCommand command = new RecordPurchaseCommand(
                UserId.randomId(),
                AccountId.randomId(),
                "AAPL",
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.of("USD")),
                List.of(invalidFee, invalidFee),
                Instant.now(),
                "notes"
            );
            
            when(invalidFee.amountInNativeCurrency()).thenReturn(money);
            when(invalidFee.amountInNativeCurrency().amount()).thenReturn(null);

            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("fee"));
            
        }
        
        @Test
        @DisplayName("Should fail when transaction date is in future")
        void shouldFailWhenDateIsInFuture() {
            RecordPurchaseCommand command = new RecordPurchaseCommand(
                UserId.randomId(),
                AccountId.randomId(),
                "AAPL",
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.of("USD")),
                Collections.emptyList(),
                Instant.now().plusSeconds(86400),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("future"));
        }
        
        private RecordPurchaseCommand createValidPurchaseCommand() {
            Fee validFee = new Fee(
                FeeType.ACCOUNT_MAINTENANCE,
                new Money(BigDecimal.valueOf(5), ValidatedCurrency.USD), 
                ExchangeRate.createSingle(ValidatedCurrency.USD, "test"),
                null,
                Instant.now()
            );
            return new RecordPurchaseCommand(
                UserId.randomId(),
                AccountId.randomId(),
                "AAPL",
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                List.of(validFee),
                Instant.now().minus(Duration.ofHours(20)),
                "Test purchase"
            );
        }
    }

    @Nested
    @DisplayName("RecordSaleCommand Validation Tests")
    class RecordSaleCommandTests {
        
        @Test
        @DisplayName("Should pass validation for valid sale command")
        void shouldPassValidationForValidCommand() {
            RecordSaleCommand command = createValidSaleCommand();
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }
        
        @Test
        @DisplayName("Should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            RecordSaleCommand command = new RecordSaleCommand(
                null,
                AccountId.randomId(),
                "AAPL",
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.of("USD")),
                null,
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("UserId is required");
        }
        
        @Test
        @DisplayName("Should fail when accountId is null")
        void shouldFailWhenAccountIdIsNull() {
            RecordSaleCommand command = new RecordSaleCommand(
                UserId.randomId(),
                null,
                "AAPL",
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.of("USD")),
                null,
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("AccountId is required");
        }
        
        @Test
        @DisplayName("Should fail when symbol is invalid")
        void shouldFailWhenSymbolIsInvalid() {
            RecordSaleCommand command = new RecordSaleCommand(
                UserId.randomId(),
                AccountId.randomId(),
                "",
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.of("USD")),
                null,
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Asset symbol is required");
        }

        @Test
        @DisplayName("Should fail when symbol is invalid Null")
        void shouldFailWhenSymbolIsInvalidNull() {
            RecordSaleCommand command = new RecordSaleCommand(
                UserId.randomId(),
                AccountId.randomId(),
                null,
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.of("USD")),
                null,
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Asset symbol is required");
        }

        @Test
        @DisplayName("Should fail when currency is null")
        void shouldFailWhenCurrencyIsNull() {
            Money money = mock(Money.class);
            RecordSaleCommand command = new RecordSaleCommand(
                UserId.randomId(),
                AccountId.randomId(),
                null,
                BigDecimal.TEN,
                money,
                null,
                Instant.now(),
                "notes"
            );
            
            when(money.currency()).thenReturn(null);
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Currency is required");
            
        }

        @Test
        @DisplayName("Should fail when symbol is invalid Wrong Format")
        void shouldFailWhenSymbolIsInvalidTooLong() {
            RecordSaleCommand command = new RecordSaleCommand(
                UserId.randomId(),
                AccountId.randomId(),
                "ApPL",
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.of("USD")),
                null,
                Instant.now().minus(Duration.ofDays(1)),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Invalid asset symbol format");
        }

        @Test
        @DisplayName("Should fail when Money is negative")
        void shouldFailWhenMoneyIsNegative() {
            RecordSaleCommand command = new RecordSaleCommand(
                UserId.randomId(),
                AccountId.randomId(),
                "AAPL",
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(-160), ValidatedCurrency.of("USD")),
                null,
                Instant.now().minus(Duration.ofDays(1)),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Amount cannot be negative");
        }

        @Test
        @DisplayName("Should fail when Quantity is negative")
        void shouldFailWhenQuantityIsNegative() {
            RecordSaleCommand command = new RecordSaleCommand(
                UserId.randomId(),
                AccountId.randomId(),
                "AAPL",
                BigDecimal.valueOf(-1),
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.of("USD")),
                null,
                Instant.now().minus(Duration.ofDays(1)),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Quantity must be greater than zero");
        }

        private RecordSaleCommand createValidSaleCommand() {
            Fee validFee = new Fee(
                FeeType.ACCOUNT_MAINTENANCE,
                new Money(BigDecimal.valueOf(5), ValidatedCurrency.USD), 
                ExchangeRate.createSingle(ValidatedCurrency.USD, "test"),
                null,
                Instant.now()
            );
            return new RecordSaleCommand(
                UserId.randomId(),
                AccountId.randomId(),
                "AAPL",
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.of("USD")),
                List.of(validFee),
                Instant.now().minus(Duration.ofHours(30)),
                "Test sale"
            );
        }
    }

    @Nested
    @DisplayName("RecordDepositCommand Validation Tests")
    class RecordDepositCommandTests {
        
        @Test
        @DisplayName("Should pass validation for valid deposit command")
        void shouldPassValidationForValidCommand() {
            RecordDepositCommand command = createValidDepositCommand();
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }
        
        @Test
        @DisplayName("Should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            RecordDepositCommand command = new RecordDepositCommand(
                null,
                AccountId.randomId(),
                new Money(BigDecimal.valueOf(1000), ValidatedCurrency.of("USD")),
                ValidatedCurrency.of("USD"),
                null,
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("UserId is required");
        }

        @Test
        @DisplayName("Should fail when accountId is null")
        void shouldFailWhenAccountIdIsNull() {
            RecordDepositCommand command = new RecordDepositCommand(
                UserId.randomId(),
                null,
                new Money(BigDecimal.valueOf(1000), ValidatedCurrency.of("USD")),
                ValidatedCurrency.of("USD"),
                null,
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("AccountId is required");
        }
        
        @Test
        @DisplayName("Should fail when amount is invalid")
        void shouldFailWhenAmountIsInvalid() {
            RecordDepositCommand command = new RecordDepositCommand(
                UserId.randomId(),
                AccountId.randomId(),
                new Money(BigDecimal.valueOf(-1000), ValidatedCurrency.of("USD")),
                ValidatedCurrency.of("USD"),
                null,
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("Amount"));
        }
        
        @Test
        @DisplayName("Should fail when currency is null")
        void shouldFailWhenCurrencyIsNull() {
            RecordDepositCommand command = new RecordDepositCommand(
                UserId.randomId(),
                AccountId.randomId(),
                new Money(BigDecimal.valueOf(1000), ValidatedCurrency.of("USD")),
                null,
                null,
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Currency is required");
        }

        @Test
        @DisplayName("Should fail when Money is negative")
        void shouldFailWhenMoneyIsNegative() {
            RecordDepositCommand command = new RecordDepositCommand(
                UserId.randomId(),
                AccountId.randomId(),
                new Money(BigDecimal.valueOf(-1000), ValidatedCurrency.of("USD")),
                ValidatedCurrency.USD,
                null,
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Amount cannot be negative");
        }
        
        private RecordDepositCommand createValidDepositCommand() {
            Fee validFee = new Fee(
                FeeType.ACCOUNT_MAINTENANCE,
                new Money(BigDecimal.valueOf(5), ValidatedCurrency.USD), 
                ExchangeRate.createSingle(ValidatedCurrency.USD, "test"),
                null,
                Instant.now()
            );
            return new RecordDepositCommand(
                UserId.randomId(),
                AccountId.randomId(),
                new Money(BigDecimal.valueOf(1000), ValidatedCurrency.of("USD")),
                ValidatedCurrency.of("USD"),
                List.of(validFee),
                Instant.now().minus(Duration.ofHours(36)),
                "Test deposit"
            );
        }
    }

    @Nested
    @DisplayName("RecordWithdrawalCommand Validation Tests")
    class RecordWithdrawalCommandTests {
        
        @Test
        @DisplayName("Should pass validation for valid withdrawal command")
        void shouldPassValidationForValidCommand() {
            RecordWithdrawalCommand command = createValidWithdrawalCommand();
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }
        
        @Test
        @DisplayName("Should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            RecordWithdrawalCommand command = new RecordWithdrawalCommand(
                null,
                AccountId.randomId(),
                new Money(BigDecimal.valueOf(500), ValidatedCurrency.of("USD")),
                null,
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("UserId is required");
        }
        
        @Test
        @DisplayName("Should fail when accountId is null")
        void shouldFailWhenAccountIdIsNull() {
            RecordWithdrawalCommand command = new RecordWithdrawalCommand(
                UserId.randomId(),
                null,
                new Money(BigDecimal.valueOf(500), ValidatedCurrency.of("USD")),
                null,
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("AccountId is required");
        }

        @Test
        @DisplayName("Should fail when Money is negative")
        void shouldFailWhenMoneyIsNegative() {
            RecordWithdrawalCommand command = new RecordWithdrawalCommand(
                UserId.randomId(),
                AccountId.randomId(),
                new Money(BigDecimal.valueOf(-500), ValidatedCurrency.of("USD")),
                null,
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Amount cannot be negative");
        }
        
        private RecordWithdrawalCommand createValidWithdrawalCommand() {
            Fee validFee = new Fee(
                FeeType.ACCOUNT_MAINTENANCE,
                new Money(BigDecimal.valueOf(5), ValidatedCurrency.USD), 
                ExchangeRate.createSingle(ValidatedCurrency.USD, "test"),
                null,
                Instant.now()
            );
            return new RecordWithdrawalCommand(
                UserId.randomId(),
                AccountId.randomId(),
                new Money(BigDecimal.valueOf(500), ValidatedCurrency.of("USD")),
                List.of(validFee),
                Instant.now().minus(Duration.ofHours(36)),
                "Test withdrawal"
            );
        }
    }

    @Nested
    @DisplayName("RecordIncomeCommand Validation Tests")
    class RecordIncomeCommandTests {
        
        @Test
        @DisplayName("Should pass validation for valid dividend command")
        void shouldPassValidationForValidDividendCommand() {
            RecordIncomeCommand command = createValidIncomeCommand(TransactionType.DIVIDEND);
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }
        
        @Test
        @DisplayName("Should pass validation for valid interest command")
        void shouldPassValidationForValidInterestCommand() {
            RecordIncomeCommand command = createValidIncomeCommand(TransactionType.INTEREST);
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }
        
        @Test
        @DisplayName("Should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            RecordIncomeCommand command = new RecordIncomeCommand(
                null,
                AccountId.randomId(),
                "AAPL",
                new Money(BigDecimal.TEN, ValidatedCurrency.of("USD")),
                TransactionType.DIVIDEND,
                false,
                BigDecimal.TEN,
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("UserId is required");
        }

        @Test
        @DisplayName("Should fail when accountId is null")
        void shouldFailWhenAccountIdIsNull() {
            RecordIncomeCommand command = new RecordIncomeCommand(
                UserId.randomId(),
                null,
                "AAPL",
                new Money(BigDecimal.TEN, ValidatedCurrency.of("USD")),
                TransactionType.DIVIDEND,
                false,
                BigDecimal.TEN,
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("AccountId is required");
        }
        
        @Test
        @DisplayName("Should fail when symbol is empty")
        void shouldFailWhenSymbolIsEmpty() {
            RecordIncomeCommand command = new RecordIncomeCommand(
                UserId.randomId(),
                AccountId.randomId(),
                "",
                new Money(BigDecimal.TEN, ValidatedCurrency.of("USD")),
                TransactionType.DIVIDEND,
                false,
                BigDecimal.TEN,
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Asset symbol is required");
        }

        @Test
        @DisplayName("Should fail when symbol is null")
        void shouldFailWhenSymbolIsNull() {
            RecordIncomeCommand command = new RecordIncomeCommand(
                UserId.randomId(),
                AccountId.randomId(),
                null,
                new Money(BigDecimal.TEN, ValidatedCurrency.of("USD")),
                TransactionType.DIVIDEND,
                false,
                BigDecimal.TEN,
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Asset symbol is required");
        }
        
        @Test
        @DisplayName("Should fail when income type is invalid")
        void shouldFailWhenIncomeTypeIsInvalid() {
            RecordIncomeCommand command = new RecordIncomeCommand(
                UserId.randomId(),
                AccountId.randomId(),
                "AAPL",
                new Money(BigDecimal.TEN, ValidatedCurrency.of("USD")),
                TransactionType.BUY, // Invalid type
                false,
                BigDecimal.TEN,
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("DIVIDEND") && e.contains("INTEREST"));
        }
        
        @Test
        @DisplayName("Should fail when type is null")
        void shouldFailWhenTypeIsNull() {
            RecordIncomeCommand command = new RecordIncomeCommand(
                UserId.randomId(),
                AccountId.randomId(),
                "AAPL",
                new Money(BigDecimal.TEN, ValidatedCurrency.of("USD")),
                null,
                false,
                BigDecimal.TEN,
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("DIVIDEND") && e.contains("INTEREST"));
        }

        @Test
        @DisplayName("Should fail when shares is null but isDrip is true")
        void shouldFailWhenSharesRecievedIsNullAndIsDripIsTrue() {
            assertThrows(IllegalArgumentException.class, () ->new RecordIncomeCommand(
                UserId.randomId(),
                AccountId.randomId(),
                "AAPL",
                new Money(BigDecimal.TEN, ValidatedCurrency.of("USD")),
                TransactionType.DIVIDEND,
                true,
                null,
                Instant.now(),
                "notes"
            ));
            
        }
        @Test
        @DisplayName("Should fail when amount is invalid")
        void shouldFailWhenAmountIsInvalid() {
            RecordIncomeCommand command = new RecordIncomeCommand(
                UserId.randomId(),
                AccountId.randomId(),
                "AAPL",
                new Money(BigDecimal.valueOf(-10), ValidatedCurrency.of("USD")),
                TransactionType.DIVIDEND,
                false,
                BigDecimal.TEN,
                Instant.now().minus(Duration.ofDays(1)),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("Amount"));
        }
        
        private RecordIncomeCommand createValidIncomeCommand(TransactionType type) {
            return new RecordIncomeCommand(
                UserId.randomId(),
                AccountId.randomId(),
                "AAPL",
                new Money(BigDecimal.TEN, ValidatedCurrency.of("USD")),
                type,
                false,
                BigDecimal.TEN,
                Instant.now().minus(Duration.ofHours(36)),
                "Test income"
            );
        }
    }

    @Nested
    @DisplayName("RecordFeeCommand Validation Tests")
    class RecordFeeCommandTests {
        
        @Test
        @DisplayName("Should pass validation for valid fee command")
        void shouldPassValidationForValidCommand() {
            RecordFeeCommand command = createValidFeeCommand();
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }
        
        @Test
        @DisplayName("Should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            Fee validFee = new Fee(
                FeeType.ACCOUNT_MAINTENANCE,
                new Money(BigDecimal.valueOf(5), ValidatedCurrency.USD), 
                ExchangeRate.createSingle(ValidatedCurrency.USD, "test"),
                null,
                Instant.now()
            );
            RecordFeeCommand command = new RecordFeeCommand(
                null,
                AccountId.randomId(),
                new Money(BigDecimal.TEN, ValidatedCurrency.of("USD")),
                ValidatedCurrency.of("USD"),
                List.of(validFee),
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("UserId is required");
        }

        @Test
        @DisplayName("Should fail when accountId is null")
        void shouldFailWhenAccountIdIsNull() {
            Fee validFee = new Fee(
                FeeType.ACCOUNT_MAINTENANCE,
                new Money(BigDecimal.valueOf(5), ValidatedCurrency.USD), 
                ExchangeRate.createSingle(ValidatedCurrency.USD, "test"),
                null,
                Instant.now()
            );
            RecordFeeCommand command = new RecordFeeCommand(
                UserId.randomId(),
                null,
                new Money(BigDecimal.TEN, ValidatedCurrency.of("USD")),
                ValidatedCurrency.of("USD"),
                List.of(validFee),
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("AccountId is required");
        }

        @Test
        @DisplayName("Should fail when currency is null")
        void shouldFailWhenCurrencyIsNull() {
            Fee validFee = new Fee(
                FeeType.ACCOUNT_MAINTENANCE,
                new Money(BigDecimal.valueOf(5), ValidatedCurrency.USD), 
                ExchangeRate.createSingle(ValidatedCurrency.USD, "test"),
                null,
                Instant.now()
            );
            RecordFeeCommand command = new RecordFeeCommand(
                UserId.randomId(),
                AccountId.randomId(),
                new Money(BigDecimal.TEN, ValidatedCurrency.of("USD")),
                null,
                List.of(validFee),
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Currency is required");
        }
        
        @Test
        @DisplayName("Should fail when amount is invalid")
        void shouldFailWhenAmountIsInvalid() {
            Fee invalidFee = new Fee(
                FeeType.ACCOUNT_MAINTENANCE,
                new Money(BigDecimal.valueOf(10), ValidatedCurrency.USD), 
                ExchangeRate.createSingle(ValidatedCurrency.USD, "test"),
                null,
                Instant.now()
            );
            RecordFeeCommand command = new RecordFeeCommand(
                UserId.randomId(),
                AccountId.randomId(),
                new Money(BigDecimal.valueOf(-10), ValidatedCurrency.of("USD")),
                ValidatedCurrency.of("USD"),
                List.of(invalidFee),
                Instant.now(),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("Amount"));
        }
        
        private RecordFeeCommand createValidFeeCommand() {
            Fee validFee = new Fee(
                FeeType.ACCOUNT_MAINTENANCE,
                new Money(BigDecimal.valueOf(5), ValidatedCurrency.USD), 
                ExchangeRate.createSingle(ValidatedCurrency.USD, "test"),
                null,
                Instant.now()
            );
            return new RecordFeeCommand(
                UserId.randomId(),
                AccountId.randomId(),
                new Money(BigDecimal.TEN, ValidatedCurrency.of("USD")),
                ValidatedCurrency.of("USD"),
                List.of(validFee, validFee, validFee),
                Instant.now().minus(Duration.ofHours(36)),
                "Test fee"
            );
        }
    }

    @Nested
    @DisplayName("UpdateTransactionCommand Validation Tests")
    class UpdateTransactionCommandTests {
        
        @Test
        @DisplayName("Should pass validation for valid update command")
        void shouldPassValidationForValidCommand() {
            UpdateTransactionCommand command = createValidUpdateCommand();
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }
        
        @Test
        @DisplayName("Should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                null,
                AccountId.randomId(),
                TransactionId.randomId(),
                TransactionType.BUY,
                mock(AssetIdentifier.class),
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.of("USD")),
                List.of(),
                Instant.now().minusSeconds(3600),
                "notess"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("UserId is requried");
        }
        
        @Test
        @DisplayName("Should fail when date is in future")
        void shouldFailWhenDateIsInFuture() {
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                UserId.randomId(),
                AccountId.randomId(),
                TransactionId.randomId(),
                TransactionType.BUY,
                mock(AssetIdentifier.class),
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.of("USD")),
                List.of(),
                Instant.now().plusSeconds(86400),
                "Notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Date cannot be in the future");
        }
        
        @Test
        @DisplayName("Should fail when quantity is zero or negative")
        void shouldFailWhenQuantityIsZeroOrNegative() {
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                UserId.randomId(),
                AccountId.randomId(),
                TransactionId.randomId(),
                TransactionType.BUY,
                mock(AssetIdentifier.class),
                BigDecimal.ZERO,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.of("USD")),
                List.of(),
                Instant.now().minusSeconds(3600),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Quantity must be positive");
        }
        
        @Test
        @DisplayName("Should fail when price is zero or negative")
        void shouldFailWhenPriceIsZeroOrNegative() {
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                UserId.randomId(),
                AccountId.randomId(),
                TransactionId.randomId(),
                TransactionType.BUY,
                mock(AssetIdentifier.class),
                BigDecimal.TEN,
                new Money(BigDecimal.ZERO, ValidatedCurrency.of("USD")),
                List.of(),
                Instant.now().minusSeconds(3600),
                "notes"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Price must be positive");
        }
        
        @Test
        @DisplayName("Should fail when required fields are null")
        void shouldFailWhenRequiredFieldsAreNull() {
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                UserId.randomId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).hasSizeGreaterThan(0);
        }
        
        private UpdateTransactionCommand createValidUpdateCommand() {
            Fee validFee = new Fee(
                FeeType.ACCOUNT_MAINTENANCE,
                new Money(BigDecimal.valueOf(5), ValidatedCurrency.USD), 
                ExchangeRate.createSingle(ValidatedCurrency.USD, "test"),
                null,
                Instant.now()
            );
            AssetIdentifier cAssetIdentifier = new CashIdentifier("USD");
            return new UpdateTransactionCommand(
                UserId.randomId(),
                AccountId.randomId(),
                TransactionId.randomId(),
                TransactionType.BUY,
                cAssetIdentifier,
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.of("USD")),
                List.of(validFee),
                Instant.now().minus(Duration.ofHours(36)),
                "NOTES"
            );
        }
    }

    @Nested
    @DisplayName("AddAccountCommand Validation Tests")
    class AddAccountCommandTests {
        
        @Test
        @DisplayName("Should pass validation for valid account command")
        void shouldPassValidationForValidCommand() {
            AddAccountCommand command = createValidAddAccountCommand();
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }
        
        @Test
        @DisplayName("Should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            AddAccountCommand command = new AddAccountCommand(
                null,
                "My Account",
                AccountType.TFSA,
                ValidatedCurrency.of("USD")
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("UserId is required");
        }

        @Test
        @DisplayName("Should fail when account name is null")
        void shouldFailWhenAccountNameIsNull() {
            AddAccountCommand command = new AddAccountCommand(
                UserId.randomId(),
                null,
                AccountType.TFSA,
                ValidatedCurrency.of("USD")
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Account name is required");
        }
        
        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should fail when account name is null or empty")
        void shouldFailWhenAccountNameIsNullOrEmpty(String accountName) {
            AddAccountCommand command = new AddAccountCommand(
                UserId.randomId(),
                accountName,
                AccountType.TFSA,
                ValidatedCurrency.of("USD")
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Account name is required");
        }
        
        @Test
        @DisplayName("Should fail when account name exceeds 100 characters")
        void shouldFailWhenAccountNameExceedsMaxLength() {
            String longName = "A".repeat(101);
            AddAccountCommand command = new AddAccountCommand(
                UserId.randomId(),
                longName,
                AccountType.TFSA,
                ValidatedCurrency.of("USD")
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Account name must be 100 characters or less");
        }
        
        @Test
        @DisplayName("Should fail when account type is null")
        void shouldFailWhenAccountTypeIsNull() {
            AddAccountCommand command = new AddAccountCommand(
                UserId.randomId(),
                "My Account",
                null,
                ValidatedCurrency.of("USD")
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Account type is required");
        }
        
        @Test
        @DisplayName("Should fail when base currency is null")
        void shouldFailWhenBaseCurrencyIsNull() {
            AddAccountCommand command = new AddAccountCommand(
                UserId.randomId(),
                "My Account",
                AccountType.TFSA,
                null
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Base currency is required");
        }

        @Test
        @DisplayName("Should fail when account type is invalid")
        void shouldFailWhenAccountTypeIsInvalid() {
            AccountType accountType = mock(AccountType.class);
            AddAccountCommand command = new AddAccountCommand(
                UserId.randomId(),
                "My Account",
                accountType,
                ValidatedCurrency.USD
            );
            
            when(accountType.name()).thenReturn("TESTINGLOL");
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Invalid account type");
        }

        @Test
        @DisplayName("Should fail when base currency is invalid")
        void shouldFailWhenBaseCurrencyIsInvalid() {
            ValidatedCurrency currency = mock(ValidatedCurrency.class);
            AddAccountCommand command = new AddAccountCommand(
                UserId.randomId(),
                "My Account",
                AccountType.TFSA,
                currency
            );
            
            when(currency.getCode()).thenReturn("TESTINGLOL");
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Invalid currency code");
        }
        
        private AddAccountCommand createValidAddAccountCommand() {
            return new AddAccountCommand(
                UserId.randomId(),
                "My TFSA Account",
                AccountType.TFSA,
                ValidatedCurrency.of("USD")
            );
        }
    }

    
    @Nested
    @DisplayName("RemoveAccountCommand Validation Tests")
    class RemoveAccountCommandTests {
        @Test
        @DisplayName("Should pass validation for valid delete command")
        void shouldPassValidationForValidCommand() {
            RemoveAccountCommand command = createValidRemoveAccountCommand();
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("Should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            RemoveAccountCommand command = new RemoveAccountCommand(null, AccountId.randomId());
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("UserId is required");
        }
        
        @Test
        @DisplayName("Should fail when accountId is null")
        void shouldFailWhenAccountIdIsNull() {
                        RemoveAccountCommand command = new RemoveAccountCommand(UserId.randomId(), null);

            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("AccountId is required");
        }
        

        private RemoveAccountCommand createValidRemoveAccountCommand() {
            return new RemoveAccountCommand(UserId.randomId(), AccountId.randomId());
        }
    }

    @Nested
    @DisplayName("DeleteTransactionCommand Validation Tests")
    class DeleteTransactionCommandTests {
        
        @Test
        @DisplayName("Should pass validation for valid delete command")
        void shouldPassValidationForValidCommand() {
            DeleteTransactionCommand command = createValidDeleteCommand();
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }
        
        @Test
        @DisplayName("Should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            DeleteTransactionCommand command = new DeleteTransactionCommand(
                null,
                AccountId.randomId(),
                TransactionId.randomId(),
                false,
                null
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("UserId is required");
        }
        
        @Test
        @DisplayName("Should fail when accountId is null")
        void shouldFailWhenAccountIdIsNull() {
            DeleteTransactionCommand command = new DeleteTransactionCommand(
                UserId.randomId(),
                null,
                TransactionId.randomId(),
                false,
                "test"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("AccountId is required");
        }
        
        @Test
        @DisplayName("Should fail when transactionId is null")
        void shouldFailWhenTransactionIdIsNull() {
            DeleteTransactionCommand command = new DeleteTransactionCommand(
                UserId.randomId(),
                AccountId.randomId(),
                null,
                true,
                "reason"
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("TransactionId is required");
        }
        
        @Test
        @DisplayName("Should accumulate all validation errors")
        void shouldAccumulateAllErrors() {
            DeleteTransactionCommand command = new DeleteTransactionCommand(
                null,
                null,
                null,
                false,
                null
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).hasSize(3);
            assertThat(result.errors()).containsExactlyInAnyOrder(
                "UserId is required",
                "AccountId is required",
                "TransactionId is required"
            );
        }
        
        private DeleteTransactionCommand createValidDeleteCommand() {
            return new DeleteTransactionCommand(
                UserId.randomId(),
                AccountId.randomId(),
                TransactionId.randomId(),
                true,
                "some reason"
            );
        }
    }

    @Nested
    @DisplayName("CreatePortfolioCommand Validation Tests")
    class CreatePortfolioCommandTests {
        
        @Test
        @DisplayName("Should pass validation for valid portfolio command")
        void shouldPassValidationForValidCommand() {
            CreatePortfolioCommand command = createValidPortfolioCommand();
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }
        
        @Test
        @DisplayName("Should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            CreatePortfolioCommand command = new CreatePortfolioCommand(
                null,
                ValidatedCurrency.of("USD"),
                false
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("UserId is required");
        }
        
        @Test
        @DisplayName("Should fail when default currency is null")
        void shouldFailWhenDefaultCurrencyIsNull() {
            CreatePortfolioCommand command = new CreatePortfolioCommand(
                UserId.randomId(),
                null,
                false
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Curency is required");
        }
        
        private CreatePortfolioCommand createValidPortfolioCommand() {
            return new CreatePortfolioCommand(
                UserId.randomId(),
                ValidatedCurrency.of("USD"),
                true
            );
        }
    }

    @Nested
    @DisplayName("DeletePortfolioCommand Validation Tests")
    class DeletePortfolioCommandTests {
        @Test
        @DisplayName("Should pass validation for valid portfolio command")
        void shouldPassValidationForValidCommand() {
            DeletePortfolioCommand command =  new DeletePortfolioCommand(
                UserId.randomId(),
                true,
                false
            );
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("Should pass validation for valid portfolio command")
        void shouldPassValidationForValidCommandPart2() {
            DeletePortfolioCommand command = createValidDeletePortfolioCommand();
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("Should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            DeletePortfolioCommand command = new DeletePortfolioCommand(
                null,
                false,
                false
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("UserId is required");
        }

        @Test
        @DisplayName("Should fail when not confirmed but soft delete is null")
        void shouldFailWhenSoftDeleteConfirmAndConfirmedNot() {
            DeletePortfolioCommand command = new DeletePortfolioCommand(
                UserId.randomId(),
                false,
                true
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Cannot soft delete without confirming");
        }

        private DeletePortfolioCommand createValidDeletePortfolioCommand() {
            return new DeletePortfolioCommand(
                UserId.randomId(),
                true,
                true
            );
        }
    }

    @Nested
    @DisplayName("CorrectAssetTickerCommand Validation Tests")
    public class CorrectAssetTickerCommandTests {
        @Test
        @DisplayName("Should pass validation for valid portfolio command")
        void shouldPassValidationForValidCommand() {
            CorrectAssetTickerCommand command = createValidCorrectAssetTickerCommand();
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("Should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            CorrectAssetTickerCommand command = new CorrectAssetTickerCommand(
                null,
                AccountId.randomId(),
                new CashIdentifier("USD"),
                new CashIdentifier("CAD")
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("UserId is required");
        }

        @Test
        @DisplayName("Should fail when accountId is null")
        void shouldFailWhenAccountIdIsNull() {
            CorrectAssetTickerCommand command = new CorrectAssetTickerCommand(
                UserId.randomId(),
                null,
                new CashIdentifier("USD"),
                new CashIdentifier("CAD")
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("AccountId is required");
        }

        @Test
        @DisplayName("Should fail when wrong id is null")
        void shouldFailWhenWrongIdIsNull() {
            CorrectAssetTickerCommand command = new CorrectAssetTickerCommand(
                UserId.randomId(),
                AccountId.randomId(),
                null,
                new CashIdentifier("CAD")
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Wrong AssetIdentifier is required");
        }

        @Test
        @DisplayName("Should fail when rightId is null")
        void shouldFailWhenRightIdIsNull() {
            CorrectAssetTickerCommand command = new CorrectAssetTickerCommand(
                UserId.randomId(),
                AccountId.randomId(),
                new CashIdentifier("USD"),
                null
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains("Correct AssetIdentifier is required");
        }

        private CorrectAssetTickerCommand createValidCorrectAssetTickerCommand() {
            return new CorrectAssetTickerCommand(
                UserId.randomId(),
                AccountId.randomId(),
                new CashIdentifier("USD"),
                new CashIdentifier("CAD")
            );
        }
    }

    @Nested
    @DisplayName("Helper Method Validation Tests")
    class HelperMethodTests {
        
        @Nested
        @DisplayName("validateAmount() Tests")
        class ValidateAmountTests {
            
            @Test
            @DisplayName("Should pass for valid amount")
            void shouldPassForValidAmount() {
                ValidationResult result = validator.validateAmount(BigDecimal.valueOf(100.50));
                
                assertThat(result.isValid()).isTrue();
            }
            
            @Test
            @DisplayName("Should fail when amount is null")
            void shouldFailWhenAmountIsNull() {
                ValidationResult result = validator.validateAmount(null);
                
                assertThat(result.isValid()).isFalse();
                assertThat(result.errors()).contains("Amount is required");
            }
            
            @Test
            @DisplayName("Should fail when amount is negative")
            void shouldFailWhenAmountIsNegative() {
                ValidationResult result = validator.validateAmount(BigDecimal.valueOf(-10));
                
                assertThat(result.isValid()).isFalse();
                assertThat(result.errors()).contains("Amount cannot be negative");
            }
            
            @Test
            @DisplayName("Should fail when amount has more than 2 decimal places")
            void shouldFailWhenAmountHasTooManyDecimals() {
                ValidationResult result = validator.validateAmount(new BigDecimal("10.12399999999999999999999999999999999999999999999999"));
                
                assertThat(result.isValid()).isFalse();
                assertThat(result.errors()).contains("Amount can have at most 34 decimal places. Scale is 50");
            }
            
            @Test
            @DisplayName("Should pass for zero amount")
            void shouldPassForZeroAmount() {
                ValidationResult result = validator.validateAmount(BigDecimal.ZERO);
                
                assertThat(result.isValid()).isTrue();
            }
        }
        
        @Nested
        @DisplayName("validateQuantity() Tests")
        class ValidateQuantityTests {
            
            @Test
            @DisplayName("Should pass for valid quantity")
            void shouldPassForValidQuantity() {
                ValidationResult result = validator.validateQuantity(BigDecimal.TEN);
                
                assertThat(result.isValid()).isTrue();
            }
            
            @Test
            @DisplayName("Should fail when quantity is null")
            void shouldFailWhenQuantityIsNull() {
                ValidationResult result = validator.validateQuantity(null);
                
                assertThat(result.isValid()).isFalse();
                assertThat(result.errors()).contains("Quantity is required");
            }
            
            @Test
            @DisplayName("Should fail when quantity is zero")
            void shouldFailWhenQuantityIsZero() {
                ValidationResult result = validator.validateQuantity(BigDecimal.ZERO);
                
                assertThat(result.isValid()).isFalse();
                assertThat(result.errors()).contains("Quantity must be greater than zero");
            }
            
            @Test
            @DisplayName("Should fail when quantity is negative")
            void shouldFailWhenQuantityIsNegative() {
                ValidationResult result = validator.validateQuantity(BigDecimal.valueOf(-5));
                
                assertThat(result.isValid()).isFalse();
                assertThat(result.errors()).contains("Quantity must be greater than zero");
            }
            
            @Test
            @DisplayName("Should fail when quantity has more than 8 decimal places")
            void shouldFailWhenQuantityHasTooManyDecimals() {
                ValidationResult result = validator.validateQuantity(new BigDecimal("0.123456789"));
                
                assertThat(result.isValid()).isFalse();
                assertThat(result.errors()).contains("Quantity can have at most 8 decimal places");
            }
            
            @Test
            @DisplayName("Should pass for quantity with 8 decimal places")
            void shouldPassForQuantityWith8Decimals() {
                ValidationResult result = validator.validateQuantity(new BigDecimal("0.12345678"));
                
                assertThat(result.isValid()).isTrue();
            }
        }
        
        @Nested
        @DisplayName("validateDate() Tests")
        class ValidateDateTests {
            
            @Test
            @DisplayName("Should pass for valid past date")
            void shouldPassForValidPastDate() {
                LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
                ValidationResult result = validator.validateDate(pastDate);
                
                assertThat(result.isValid()).isTrue();
            }
            
            @Test
            @DisplayName("Should fail when date is null")
            void shouldFailWhenDateIsNull() {
                ValidationResult result = validator.validateDate(null);
                
                assertThat(result.isValid()).isFalse();
                assertThat(result.errors()).contains("Transaction date is required");
            }
            
            @Test
            @DisplayName("Should fail when date is in future")
            void shouldFailWhenDateIsInFuture() {
                LocalDateTime futureDate = LocalDateTime.now().plusDays(1);
                ValidationResult result = validator.validateDate(futureDate);
                
                assertThat(result.isValid()).isFalse();
                assertThat(result.errors()).contains("Transaction date cannot be in the future");
            }
            
            @Test
            @DisplayName("Should fail when date is too far in the past")
            void shouldFailWhenDateIsTooFarInPast() {
                LocalDateTime ancientDate = LocalDateTime.now().minusYears(51);
                ValidationResult result = validator.validateDate(ancientDate);
                
                assertThat(result.isValid()).isFalse();
                assertThat(result.errors()).contains("Transaction date is too far in the past");
            }
            
            @Test
            @DisplayName("Should pass for date exactly 50 years ago")
            void shouldPassForDateExactly50YearsAgo() {
                LocalDateTime date50YearsAgo = LocalDateTime.now().minusYears(50).plusDays(1);
                ValidationResult result = validator.validateDate(date50YearsAgo);
                
                assertThat(result.isValid()).isTrue();
            }
            
            @Test
            @DisplayName("Should pass for current time")
            void shouldPassForCurrentTime() {
                LocalDateTime now = LocalDateTime.now();
                ValidationResult result = validator.validateDate(now);
                
                assertThat(result.isValid()).isTrue();
            }
        }
        
        @Nested
        @DisplayName("validateSymbol() Tests")
        class ValidateSymbolTests {
            
            @ParameterizedTest
            @ValueSource(strings = {"AAPL", "MSFT", "BTC", "ETH", "SPY", "QQQ", "GOOGL"})
            @DisplayName("Should pass for valid symbols")
            void shouldPassForValidSymbols(String symbol) {
                ValidationResult result = validator.validateSymbol(symbol);
                
                assertThat(result.isValid()).isTrue();
            }
            
            @Test
            @DisplayName("Should fail when symbol is null")
            void shouldFailWhenSymbolIsNull() {
                ValidationResult result = validator.validateSymbol(null);
                
                assertThat(result.isValid()).isFalse();
                assertThat(result.errors()).contains("Symbol is required");
            }
            
            @Test
            @DisplayName("Should fail when symbol is empty")
            void shouldFailWhenSymbolIsEmpty() {
                ValidationResult result = validator.validateSymbol("");
                
                assertThat(result.isValid()).isFalse();
                assertThat(result.errors()).contains("Symbol is required");
            }
            
            @ParameterizedTest
            @ValueSource(strings = {"aapl", "Aapl", "AAPL!", "AA PL", "TOOLONGSYMLAA"})
            @DisplayName("Should fail for invalid symbol formats")
            void shouldFailForInvalidSymbolFormats(String symbol) {
                ValidationResult result = validator.validateSymbol(symbol);
                
                assertThat(result.isValid()).isFalse();
                assertThat(result.errors()).contains("Invalid symbol format");
            }
            
            @Test
            @DisplayName("Should pass for symbols with dots and dashes")
            void shouldPassForSymbolsWithDotsAndDashes() {
                ValidationResult result1 = validator.validateSymbol("BRK.B");
                ValidationResult result2 = validator.validateSymbol("BTC-USD");
                
                assertThat(result1.isValid()).isTrue();
                assertThat(result2.isValid()).isTrue();
            }

            @Test
            void isValidSymbolPaths() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
                
                CommandValidator validator = new CommandValidator();
                Method method = CommandValidator.class.getDeclaredMethod("isValidSymbol", String.class);
                method.setAccessible(true);

                // Path: Null
                assertFalse((boolean) method.invoke(validator, (Object) null));

                // Path: Too Long (> 10 chars)
                assertFalse((boolean) method.invoke(validator, "LONGSTOCKSYMBOL"));

                // Path: Invalid Characters (Lower case)
                assertFalse((boolean) method.invoke(validator, "aapl"));

                // Path: Valid Symbols
                assertTrue((boolean) method.invoke(validator, "AAPL"));
                assertTrue((boolean) method.invoke(validator, "BRK.B"));
                assertTrue((boolean) method.invoke(validator, "123-A"));
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases and Integration Tests")
    class EdgeCasesTests {
        
        // not possible with Money checks built in
        // @Test
        // @DisplayName("Should accumulate multiple validation errors")
        // void shouldAccumulateMultipleErrors() {
        //     RecordPurchaseCommand command = new RecordPurchaseCommand(
        //         null,
        //         null,
        //         "",
        //         BigDecimal.ZERO,
        //         new Money(BigDecimal.valueOf(-10), null),
        //         Collections.emptyList(),
        //         Instant.now().plusSeconds(86400),
        //         "notes"
        //     );
            
        //     ValidationResult result = validator.validate(command);
            
        //     assertThat(result.isValid()).isFalse();
        //     assertThat(result.errors()).hasSizeGreaterThan(3);
        // }
        
        @Test
        @DisplayName("Should handle command with all valid edge values")
        void shouldHandleCommandWithValidEdgeValues() {
            RecordPurchaseCommand command = new RecordPurchaseCommand(
                UserId.randomId(),
                AccountId.randomId(),
                "A",
                new BigDecimal("0.00000001"),
                new Money(new BigDecimal("0.01"), ValidatedCurrency.of("USD")),
                Collections.emptyList(),
                Instant.now().minus(Duration.ofMinutes(560)),
                ""
            );
            
            ValidationResult result = validator.validate(command);
            
            assertThat(result.isValid()).isTrue();
        }
        
        @Test
        @DisplayName("Should validate currency codes correctly")
        void shouldValidateCurrencyCodesCorrectly() {
            // Valid currencies
            assertThatNoException().isThrownBy(() -> ValidatedCurrency.of("USD"));
            assertThatNoException().isThrownBy(() -> ValidatedCurrency.of("EUR"));
            assertThatNoException().isThrownBy(() -> ValidatedCurrency.of("GBP"));
            assertThatNoException().isThrownBy(() -> ValidatedCurrency.of("CAD"));
        }
        
        @Test
        @DisplayName("Should validate account types correctly")
        void shouldValidateAccountTypesCorrectly() {
            // All enum values should be valid
            for (AccountType type : AccountType.values()) {
                AddAccountCommand command = new AddAccountCommand(
                    UserId.randomId(),
                    "Test Account",
                    type,
                    ValidatedCurrency.of("USD")
                );
                
                ValidationResult result = validator.validate(command);
                assertThat(result.isValid()).isTrue();
            }
        }
    }
}