package com.laderrco.fortunelink.portfolio_management.application.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
import com.laderrco.fortunelink.portfolio_management.application.exceptions.AccountNotEmptyException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.InvalidCommandException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.InvalidTransactionException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioAlreadyExistsException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioDeletionRequiresConfirmationException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioNotEmptyException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio_management.application.mappers.PortfolioMapper;
import com.laderrco.fortunelink.portfolio_management.application.responses.AccountResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.PortfolioResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.TransactionResponse;
import com.laderrco.fortunelink.portfolio_management.application.validators.CommandValidator;
import com.laderrco.fortunelink.portfolio_management.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.TransactionNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.FeeType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CashIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.TransactionQueryRepository;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PortfolioValuationService;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Portfolio Application Service Tests")
class PortfolioApplicationServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private TransactionQueryRepository transactionQueryRepository;

    @Mock
    private PortfolioValuationService portfolioValuationService;

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private CommandValidator commandValidator;

    @Mock
    private PortfolioMapper portfolioMapper;

    @InjectMocks
    private PortfolioApplicationService service;

    private UserId userId;
    private AccountId accountId;
    private Portfolio portfolio;
    private Account account;
    private MarketAssetInfo assetInfo;
    private MarketIdentifier identifier;

    @BeforeEach
    void setUp() {
        userId = UserId.randomId();
        accountId = AccountId.randomId();
        
        // Create portfolio with account
        portfolio = new Portfolio(userId, ValidatedCurrency.USD);
        account = new Account(accountId, "Test Account", AccountType.NON_REGISTERED, ValidatedCurrency.USD);
        portfolio.addAccount(account);
        
        // Add initial cash
        Transaction depositTx = new Transaction(
            TransactionId.randomId(),
            accountId,
            TransactionType.DEPOSIT,
            new CashIdentifier("USD"),
            BigDecimal.ONE,
            new Money(BigDecimal.valueOf(10000), ValidatedCurrency.USD),
            null,
            Instant.now(),
            "Initial deposit"
        );
        portfolio.recordTransaction(accountId, depositTx);
        
        // Setup asset info
        assetInfo = new MarketAssetInfo(
            "AAPL",
            "Apple Inc.",
            AssetType.STOCK,
            "NASDAQ",
            ValidatedCurrency.USD,
            "Technology",
            Money.of(215, "USD")
        );

        identifier = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);
    }

    @Nested
    @DisplayName("Record Asset Purchase Tests")
    class RecordAssetPurchaseTests {

        private RecordPurchaseCommand command;

        @BeforeEach
        void setUp() {
            command = new RecordPurchaseCommand(
                userId,
                accountId,
                "AAPL",
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now(),
                "Test purchase"
            );
        }

        @Test
        @DisplayName("Should successfully record asset purchase")
        void shouldRecordAssetPurchase() {
            // Given
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(marketDataService.getAssetInfo("AAPL")).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionResponse response = service.recordAssetPurchase(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
            verify(marketDataService).getAssetInfo("AAPL");
            
            // Verify portfolio state
            ArgumentCaptor<Portfolio> captor = ArgumentCaptor.forClass(Portfolio.class);
            verify(portfolioRepository).save(captor.capture());
            Portfolio savedPortfolio = captor.getValue();
            
            Account savedAccount = savedPortfolio.getAccount(accountId);
            assertThat(savedAccount.getAssets()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw exception when validation fails")
        void shouldThrowExceptionWhenValidationFails() {
            // Given
            when(commandValidator.validate(command))
                .thenReturn(ValidationResult.failure(List.of("Invalid quantity")));

            // When/Then
            assertThatThrownBy(() -> service.recordAssetPurchase(command))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Invalid purchase command");
            
            verify(portfolioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when asset not found")
        void shouldThrowExceptionWhenAssetNotFound() {
            // Given
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(marketDataService.getAssetInfo("AAPL")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.recordAssetPurchase(command))
                .isInstanceOf(AssetNotFoundException.class)
                .hasMessageContaining("Asset not found: AAPL");
        }

        @Test
        @DisplayName("Should throw exception when portfolio not found")
        void shouldThrowExceptionWhenPortfolioNotFound() {
            // Given
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(marketDataService.getAssetInfo("AAPL")).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.recordAssetPurchase(command))
                .isInstanceOf(PortfolioNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when insufficient funds")
        void shouldThrowExceptionWhenInsufficientFunds() {
            // Given
            RecordPurchaseCommand largeCommand = new RecordPurchaseCommand(
                userId,
                accountId,
                "AAPL",
                BigDecimal.valueOf(1000), // Too many shares
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now(),
                "Large purchase"
            );
            
            when(commandValidator.validate(largeCommand)).thenReturn(ValidationResult.success());
            when(marketDataService.getAssetInfo("AAPL")).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.recordAssetPurchase(largeCommand))
                .isInstanceOf(InsufficientFundsException.class);
        }
    }

    @Nested
    @DisplayName("Record Asset Sale Tests")
    class RecordAssetSaleTests {

        private RecordSaleCommand command;

      @BeforeEach
        void setUp() {
            // Create a fresh portfolio for this test to ensure clean state
            portfolio = new Portfolio(userId, ValidatedCurrency.USD);
            account = new Account(accountId, "Test Account", AccountType.NON_REGISTERED, ValidatedCurrency.USD);
            portfolio.addAccount(account);
            
            // Add initial cash
            Transaction depositTx = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.DEPOSIT,
                new CashIdentifier("USD"),
                BigDecimal.ONE,
                new Money(BigDecimal.valueOf(10000), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(7200),
                "Initial deposit"
            );
            portfolio.recordTransaction(accountId, depositTx);
            
            // Buy assets that we'll later sell - using the ACTUAL domain logic
            Transaction buyTx = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.BUY,
                assetInfo.toIdentifier(), // Use the same identifier that will be used in the sale
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600),
                "Buy for test"
            );
            portfolio.recordTransaction(accountId, buyTx);
            
            command = new RecordSaleCommand(
                userId,
                accountId,
                "AAPL",
                BigDecimal.valueOf(5),
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                null,
                Instant.now(),
                "Test sale"
            );
        }

        @Test
        @DisplayName("Should successfully record asset sale")
        void shouldRecordAssetSale() {
            // Given
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(marketDataService.getAssetInfo("AAPL")).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);



            // Verify the asset exists before selling (for debugging)
            Account accountBeforeSale = portfolio.getAccount(accountId);
            Asset assetBeforeSale = accountBeforeSale.getAsset(assetInfo.toIdentifier());
            assertThat(assetBeforeSale).isNotNull();
            assertThat(assetBeforeSale.getQuantity()).isGreaterThanOrEqualTo(command.quantity());

            IO.println(assetBeforeSale);

            // When
            TransactionResponse response = service.recordAssetSale(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
            
            // Verify the sale reduced the asset quantity
            ArgumentCaptor<Portfolio> captor = ArgumentCaptor.forClass(Portfolio.class);
            verify(portfolioRepository).save(captor.capture());
            Portfolio savedPortfolio = captor.getValue();
            Account savedAccount = savedPortfolio.getAccount(accountId);
            Asset soldAsset = savedAccount.getAsset(assetInfo.toIdentifier());

            IO.println(soldAsset);

            assertThat(soldAsset.getQuantity()).isEqualTo(BigDecimal.valueOf(5)); // 10 - 5 = 5 remaining
        }

        @Test
        @DisplayName("Should throw exception when selling more than owned")
        void shouldThrowExceptionWhenSellingMoreThanOwned() {
            // Given - we only own 10 shares from setUp
            RecordSaleCommand largeCommand = new RecordSaleCommand(
                userId,
                accountId,
                "AAPL",
                BigDecimal.valueOf(100), // Trying to sell 100, but only own 10
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                null,
                Instant.now(),
                "Large sale"
            );
            
            when(commandValidator.validate(largeCommand)).thenReturn(ValidationResult.success());
            when(marketDataService.getAssetInfo("AAPL")).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.recordAssetSale(largeCommand))
                .isInstanceOf(InsufficientFundsException.class);
            
            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when validation result is not valid")
        void shouldThrowExceptionsWhenValidationResultIsNotValid() {
            RecordSaleCommand command = mock(RecordSaleCommand.class);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("ERROR"));

            assertThatThrownBy(() -> service.recordAssetSale(command))
                .isInstanceOf(InvalidTransactionException.class);
            
            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }
    }

    @Nested
    @DisplayName("Record Deposit Tests")
    class RecordDepositTests {

        @Test
        @DisplayName("Should successfully record deposit")
        void shouldRecordDeposit() {
            // Given
            RecordDepositCommand command = new RecordDepositCommand(
                userId,
                accountId,
                new Money(BigDecimal.valueOf(1000), ValidatedCurrency.USD),
                ValidatedCurrency.USD,
                null,
                Instant.now(),
                "Deposit"
            );
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionResponse response = service.recordDeposit(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when validation result is not valid")
        void shouldThrowExceptionsWhenValidationResultIsNotValid() {
            RecordDepositCommand command = mock(RecordDepositCommand.class);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("ERROR"));

            assertThatThrownBy(() -> service.recordDeposit(command))
                .isInstanceOf(InvalidTransactionException.class);
            
            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }
    }

    @Nested
    @DisplayName("Record Withdrawal Tests")
    class RecordWithdrawalTests {

        @Test
        @DisplayName("Should successfully record withdrawal")
        void shouldRecordWithdrawal() {
            // Given
            RecordWithdrawalCommand command = new RecordWithdrawalCommand(
                userId,
                accountId,
                new Money(BigDecimal.valueOf(500), ValidatedCurrency.USD),
                ValidatedCurrency.USD,
                null,
                Instant.now(),
                "Withdrawal"
            );
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionResponse response = service.recordWithdrawal(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when insufficient cash")
        void shouldThrowExceptionWhenInsufficientCash() {
            // Given
            RecordWithdrawalCommand command = new RecordWithdrawalCommand(
                userId,
                accountId,
                new Money(BigDecimal.valueOf(50000), ValidatedCurrency.USD), // More than available
                ValidatedCurrency.USD,
                null,
                Instant.now(),
                "Large withdrawal"
            );
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.recordWithdrawal(command))
                .isInstanceOf(InsufficientFundsException.class);
        }

        @Test
        @DisplayName("Should throw exception when validation result is not valid")
        void shouldThrowExceptionsWhenValidationResultIsNotValid() {
            RecordWithdrawalCommand command = mock(RecordWithdrawalCommand.class);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("ERROR"));

            assertThatThrownBy(() -> service.recordWithdrawal(command))
                .isInstanceOf(InvalidTransactionException.class);
            
            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }
    }

    @Nested
    @DisplayName("Record Dividend Income Tests")
    class RecordDividendIncomeTests {

        @BeforeEach
        void setUp() {
            // Add stock position first
            Transaction buyTx = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.BUY,
                identifier,
                BigDecimal.valueOf(100),
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600),
                "Buy for dividend test"
            );
            portfolio.recordTransaction(accountId, buyTx);
        }

        @Test
        @DisplayName("Should successfully record dividend income without DRIP")
        void shouldRecordDividendWithoutDrip() {
            // Given
            RecordIncomeCommand command = new RecordIncomeCommand(
                userId,
                accountId,
                "AAPL",
                new Money(BigDecimal.valueOf(50), ValidatedCurrency.USD),
                TransactionType.DIVIDEND,
                false,
                BigDecimal.ONE,
                Instant.now(),
                "Dividend"
            );
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(marketDataService.getAssetInfo("AAPL")).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionResponse response = service.recordDividendIncome(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should successfully record dividend income with DRIP")
        void shouldRecordDividendWithDrip() {
            // Given
            RecordIncomeCommand command = new RecordIncomeCommand(
                userId,
                accountId,
                "AAPL",
                new Money(BigDecimal.valueOf(75), ValidatedCurrency.USD),
                TransactionType.DIVIDEND,
                true,
                BigDecimal.valueOf(0.5), // Bought 0.5 shares
                Instant.now(),
                "DRIP Dividend"
            );
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(marketDataService.getAssetInfo("AAPL")).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionResponse response = service.recordDividendIncome(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when validation result is not valid")
        void shouldThrowExceptionsWhenValidationResultIsNotValid() {
            RecordIncomeCommand command = mock(RecordIncomeCommand.class);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("ERROR"));

            assertThatThrownBy(() -> service.recordDividendIncome(command))
                .isInstanceOf(InvalidTransactionException.class);
            
            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when asset info not found")
        void shouldThrowExceptionsWhenAssetInfoIsNotFound() {
            // Given
            RecordIncomeCommand command = new RecordIncomeCommand(
                userId,
                accountId,
                null,
                new Money(BigDecimal.valueOf(75), ValidatedCurrency.USD),
                TransactionType.DIVIDEND,
                true,
                BigDecimal.valueOf(0.5), // Bought 0.5 shares
                Instant.now(),
                "DRIP Dividend"
            );
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));

            assertThatThrownBy(() -> service.recordDividendIncome(command))
                .isInstanceOf(AssetNotFoundException.class);
            
            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }
    }

    @Nested
    @DisplayName("Record Fee Tests")
    class RecordFeeTests {

        @Test
        @DisplayName("Should successfully record fee transaction")
        void shouldRecordFee() {

            // Given
            RecordFeeCommand command = new RecordFeeCommand(
                userId,
                accountId,
                new Money(BigDecimal.valueOf(25.50), ValidatedCurrency.USD),
                ValidatedCurrency.USD,
                null,
                Instant.now(),
                "Account maintenance fee"
            );
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            Money cashBeforeFee = portfolio.getAccount(accountId).getCashBalance();

            // When
            TransactionResponse response = service.recordFee(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
            
            // Verify fee reduced cash balance
            ArgumentCaptor<Portfolio> captor = ArgumentCaptor.forClass(Portfolio.class);
            verify(portfolioRepository).save(captor.capture());
            Portfolio savedPortfolio = captor.getValue();
            Account savedAccount = savedPortfolio.getAccount(accountId);
            Money cashAfterFee = savedAccount.getCashBalance();
            
            assertThat(cashAfterFee.amount()).isLessThan(cashBeforeFee.amount());
        }

        @Test
        @DisplayName("Should throw exception when validation fails")
        void shouldThrowExceptionWhenValidationFails() {
            // Given
            RecordFeeCommand command = new RecordFeeCommand(
                userId,
                accountId,
                new Money(BigDecimal.valueOf(-10), ValidatedCurrency.USD), // Invalid negative amount
                ValidatedCurrency.USD,
                null,
                Instant.now(),
                "Invalid fee"
            );
            
            when(commandValidator.validate(command))
                .thenReturn(ValidationResult.failure(List.of("Fee amount must be positive")));

            // When/Then
            assertThatThrownBy(() -> service.recordFee(command))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Invalid fee command");
            
            verify(portfolioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when portfolio not found")
        void shouldThrowExceptionWhenPortfolioNotFound() {
            // Given
            RecordFeeCommand command = new RecordFeeCommand(
                userId,
                accountId,
                new Money(BigDecimal.valueOf(25), ValidatedCurrency.USD),
                ValidatedCurrency.USD,
                null,
                Instant.now(),
                "Fee"
            );
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.recordFee(command))
                .isInstanceOf(PortfolioNotFoundException.class);
            
            verify(portfolioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when account not found")
        void shouldThrowExceptionWhenAccountNotFound() {
            // Given
            AccountId nonExistentAccountId = AccountId.randomId();
            RecordFeeCommand command = new RecordFeeCommand(
                userId,
                nonExistentAccountId,
                new Money(BigDecimal.valueOf(25), ValidatedCurrency.USD),
                ValidatedCurrency.USD,
                null,
                Instant.now(),
                "Fee"
            );
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.recordFee(command))
                .isInstanceOf(AccountNotFoundException.class);
            
            verify(portfolioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should record fee with different currencies")
        void shouldRecordFeeWithDifferentCurrencies() {
            // Given - create account with CAD currency
            AccountId cadAccountId = AccountId.randomId();
            Account cadAccount = new Account(
                cadAccountId, 
                "CAD Account", 
                AccountType.NON_REGISTERED, 
                ValidatedCurrency.CAD
            );
            portfolio.addAccount(cadAccount);
            
            // Add CAD cash
            Transaction cadDeposit = new Transaction(
                TransactionId.randomId(),
                cadAccountId,
                TransactionType.DEPOSIT,
                new CashIdentifier("CAD"),
                BigDecimal.ONE,
                new Money(BigDecimal.valueOf(5000), ValidatedCurrency.CAD),
                null,
                Instant.now().minusSeconds(3600),
                "CAD deposit"
            );
            portfolio.recordTransaction(cadAccountId, cadDeposit);
            
            RecordFeeCommand command = new RecordFeeCommand(
                userId,
                cadAccountId,
                new Money(BigDecimal.valueOf(15.75), ValidatedCurrency.CAD),
                ValidatedCurrency.CAD,
                null,
                Instant.now(),
                "Foreign exchange fee"
            );
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionResponse response = service.recordFee(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should record fee with additional fees")
        void shouldRecordFeeWithAdditionalFees() {
            // Given
            Fee additionalFees = new Fee(FeeType.ACCOUNT_MAINTENANCE, new Money(BigDecimal.valueOf(2.50), ValidatedCurrency.USD), ExchangeRate.createSingle(ValidatedCurrency.USD, "null"), null, Instant.now());
            RecordFeeCommand command = new RecordFeeCommand(
                userId,
                accountId,
                new Money(BigDecimal.valueOf(25), ValidatedCurrency.USD),
                ValidatedCurrency.USD,
                List.of(additionalFees),
                Instant.now(),
                "Transfer fee with processing charge"
            );
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionResponse response = service.recordFee(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
            
            // Verify both total amount and additional fees were recorded
            ArgumentCaptor<Portfolio> captor = ArgumentCaptor.forClass(Portfolio.class);
            verify(portfolioRepository).save(captor.capture());
        }

        @Test
        @DisplayName("Should record multiple fees in sequence")
        void shouldRecordMultipleFeesInSequence() {
            // Given
            RecordFeeCommand fee1 = new RecordFeeCommand(
                userId,
                accountId,
                new Money(BigDecimal.valueOf(10), ValidatedCurrency.USD),
                ValidatedCurrency.USD,
                null,
                Instant.now(),
                "Monthly fee"
            );
            
            RecordFeeCommand fee2 = new RecordFeeCommand(
                userId,
                accountId,
                new Money(BigDecimal.valueOf(5), ValidatedCurrency.USD),
                ValidatedCurrency.USD,
                null,
                Instant.now().plusSeconds(60),
                "Transaction fee"
            );
            
            when(commandValidator.validate(any(RecordFeeCommand.class))).thenReturn(ValidationResult.success());
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionResponse response1 = service.recordFee(fee1);
            TransactionResponse response2 = service.recordFee(fee2);

            // Then
            assertThat(response1).isNotNull();
            assertThat(response2).isNotNull();
            verify(portfolioRepository, times(2)).save(any(Portfolio.class));
        }
    }

    @Nested
    @DisplayName("Update Transaction Tests")
    class UpdateTransactionTests {

        private Transaction existingTransaction;
        private TransactionId transactionId;

        @BeforeEach
        void setUp() {
            transactionId = TransactionId.randomId();
            
            // Create existing transaction
            existingTransaction = new Transaction(
                transactionId,
                accountId,
                TransactionType.BUY,
                identifier,
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600),
                "Original purchase"
            );
            
            portfolio.recordTransaction(accountId, existingTransaction);
        }

        @Test
        @DisplayName("Should successfully update transaction")
        void shouldUpdateTransaction() {
            // Given
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                userId,
                accountId,
                transactionId,
                TransactionType.BUY,
                identifier,
                BigDecimal.valueOf(15), // Changed quantity
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600),
                "Updated purchase"
            );
            
            List<Transaction> transactions = List.of(existingTransaction);
            if (transactions.isEmpty()) {
                fail("Something wrong, transactions are empty here");
            }
            Page<Transaction> page = new PageImpl<>(transactions);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryRepository.findByAccountId(accountId, Pageable.unpaged()))
                .thenReturn(page);
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionResponse response = service.updateTransation(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }


        @Test
        @DisplayName("Should throw exception when updating transaction date to future")
        void shouldThrowExceptionWhenDateInFuture() {
            // Given
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                userId,
                accountId,
                transactionId,
                TransactionType.BUY,
                new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null),
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now().plusSeconds(86400), // Future date
                "Future purchase"
            );
            
            List<Transaction> transactions = List.of(existingTransaction);
            if (transactions.isEmpty()) {
                fail("Something went wrong");
            }

            Page<Transaction> page = new PageImpl<>(transactions);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryRepository.findByAccountId(accountId, Pageable.unpaged()))
                .thenReturn(page);

            // When/Then
            assertThatThrownBy(() -> service.updateTransation(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transaction date cannot be in the future");
        }

        @Test
        @DisplayName("Should throw exception when updating with negative quantity")
        void shouldThrowExceptionWhenNegativeQuantity() {
            // Given
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                userId,
                accountId,
                transactionId,
                TransactionType.BUY,
                new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null),
                BigDecimal.valueOf(-5), // Negative quantity
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600),
                "Invalid quantity"
            );
            List<Transaction> transactions = List.of(existingTransaction);
            if (transactions.isEmpty()) {
                fail("Something went wrong");
            }

            Page<Transaction> page = new PageImpl<>(transactions);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryRepository.findByAccountId(accountId, Pageable.unpaged()))
                .thenReturn(page);

            // When/Then
            assertThatThrownBy(() -> service.updateTransation(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be positive");
        }
        @Test
        @DisplayName("Should successfully update SELL transaction with sufficient holdings")
        void shouldUpdateSellTransactionWithSufficientHoldings() {
            // Given - Setup: Buy 100 shares, then sell 50
            Transaction buyTx = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.BUY,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(100),
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(7200), // 2 hours ago
                "Initial buy"
            );
            portfolio.recordTransaction(accountId, buyTx);
            
            TransactionId sellTxId = TransactionId.randomId();
            Transaction sellTx = new Transaction(
                sellTxId,
                accountId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(50),
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600), // 1 hour ago
                "Original sell"
            );
            portfolio.recordTransaction(accountId, sellTx);
            
            // Now update the sell to 60 shares (still within the 100 we own)
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                userId,
                accountId,
                sellTxId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(60), // Increase sell from 50 to 60
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600),
                "Updated sell"
            );
            
            List<Transaction> transactions = List.of(buyTx, sellTx);
            if (transactions.isEmpty()) {
                fail("something went wrong");
            }
            Page<Transaction> page = new PageImpl<>(transactions);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryRepository.findByAccountId(accountId, Pageable.unpaged()))
                .thenReturn(page);
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionResponse response = service.updateTransation(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when updating SELL transaction exceeds holdings")
        void shouldThrowExceptionWhenUpdatingSellExceedsHoldings() {
            // Given - Setup: Buy 100 shares, then sell 50
            Transaction buyTx = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.BUY,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(100),
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(7200), // 2 hours ago
                "Initial buy"
            );
            portfolio.recordTransaction(accountId, buyTx);
            
            TransactionId sellTxId = TransactionId.randomId();
            Transaction sellTx = new Transaction(
                sellTxId,
                accountId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(50),
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600), // 1 hour ago
                "Original sell"
            );
            portfolio.recordTransaction(accountId, sellTx);
            
            // Try to update sell to 150 shares (more than the 100 we own)
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                userId,
                accountId,
                sellTxId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(150), // Try to sell 150, but only own 100
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600),
                "Invalid updated sell"
            );
            
            List<Transaction> transactions = List.of(buyTx, sellTx);
            if (transactions.isEmpty()) {
                fail();
            }
            Page<Transaction> page = new PageImpl<>(transactions);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryRepository.findByAccountId(accountId, Pageable.unpaged()))
                .thenReturn(page);

            // When/Then
            assertThatThrownBy(() -> service.updateTransation(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient holdings");
            
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should validate SELL with multiple prior transactions")
        void shouldValidateSellWithMultiplePriorTransactions() {
            // Given - Complex scenario: Multiple buys and sells
            TransactionId buy1Id = TransactionId.randomId();
            Transaction buy1 = new Transaction(
                buy1Id,
                accountId,
                TransactionType.BUY,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(50),
                new Money(BigDecimal.valueOf(140), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(10800), // 3 hours ago
                "First buy"
            );
            portfolio.recordTransaction(accountId, buy1);
            
            TransactionId buy2Id = TransactionId.randomId();
            Transaction buy2 = new Transaction(
                buy2Id,
                accountId,
                TransactionType.BUY,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(50),
                new Money(BigDecimal.valueOf(145), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(7200), // 2 hours ago
                "Second buy"
            );
            portfolio.recordTransaction(accountId, buy2);
            
            TransactionId sell1Id = TransactionId.randomId();
            Transaction sell1 = new Transaction(
                sell1Id,
                accountId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(30),
                new Money(BigDecimal.valueOf(155), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600), // 1 hour ago
                "First sell"
            );
            portfolio.recordTransaction(accountId, sell1);
            
            // Now update the sell to 70 shares
            // Holdings: 50 + 50 - 30 = 70 shares at time of sale
            // So selling 70 should be exactly at the limit
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                userId,
                accountId,
                sell1Id,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(70), // Update to sell exactly all holdings
                new Money(BigDecimal.valueOf(155), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600),
                "Updated sell - all shares"
            );
            
            List<Transaction> transactions = List.of(buy1, buy2, sell1);
            if (transactions.isEmpty()) {
                fail();
            }
            Page<Transaction> page = new PageImpl<>(transactions);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryRepository.findByAccountId(accountId, Pageable.unpaged()))
                .thenReturn(page);
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionResponse response = service.updateTransation(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should correctly exclude the transaction being updated from validation")
        void shouldExcludeUpdatedTransactionFromValidation() {
            // Given - Buy 100 shares, then sell 100 shares
            Transaction buyTx = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.BUY,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(100),
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(7200),
                "Buy 100"
            );
            portfolio.recordTransaction(accountId, buyTx);
            
            TransactionId sellTxId = TransactionId.randomId();
            Transaction sellTx = new Transaction(
                sellTxId,
                accountId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(100),
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600),
                "Sell all 100"
            );
            portfolio.recordTransaction(accountId, sellTx);
            
            // Update the sell to 90 shares (should work because we exclude the original 100 sell)
            // Without exclusion: holdings would be 100 (buy) - 100 (old sell) = 0, can't sell 90
            // With exclusion: holdings = 100 (buy), can sell 90
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                userId,
                accountId,
                sellTxId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(90), // Reduce sell from 100 to 90
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600),
                "Updated sell to 90"
            );
            
            List<Transaction> transactions = List.of(buyTx, sellTx);
            if (transactions.isEmpty()) {
                fail();
            }
            Page<Transaction> page = new PageImpl<>(transactions);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryRepository.findByAccountId(accountId, Pageable.unpaged()))
                .thenReturn(page);
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionResponse response = service.updateTransation(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }
        @Test
        @DisplayName("Should throw exception when updating with negative price")
        void shouldThrowExceptionWhenNegativePrice() {
            // Given
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                userId,
                accountId,
                transactionId,
                TransactionType.BUY,
                new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null),
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(-150), ValidatedCurrency.USD), // Negative price
                null,
                Instant.now().minusSeconds(3600),
                "Invalid price"
            );
            
            List<Transaction> transactions = List.of(existingTransaction);
            if (transactions.isEmpty()) {
                fail("Something went wrong");
            }

            Page<Transaction> page = new PageImpl<>(transactions);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryRepository.findByAccountId(accountId, Pageable.unpaged()))
                .thenReturn(page);

            // When/Then
            assertThatThrownBy(() -> service.updateTransation(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Price must be positive");
        }

        @Test
        @DisplayName("Should throw exception when transaction not found")
        void shouldThrowExceptionWhenTransactionNotFound() {
            // Given
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                userId,
                accountId,
                TransactionId.randomId(), // Different ID
                TransactionType.BUY,
                new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null),
                BigDecimal.valueOf(15),
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600),
                "Updated purchase"
            );

            List<Transaction> transactions = List.of(existingTransaction);
            if (transactions.isEmpty()) {
                fail("This is empty when it shouldn't be");
            }
            Page<Transaction> page = new PageImpl<>(transactions);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryRepository.findByAccountId(accountId, Pageable.unpaged()))
                .thenReturn(page);

            // When/Then
            assertThatThrownBy(() -> service.updateTransation(command))
                .isInstanceOf(TransactionNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when validation result is not valid")
        void shouldThrowExceptionsWhenValidationResultIsNotValid() {
            UpdateTransactionCommand command = mock(UpdateTransactionCommand.class);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("ERROR"));

            assertThatThrownBy(() -> service.updateTransation(command))
                .isInstanceOf(InvalidTransactionException.class);
            
            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }


        @Test
        @DisplayName("Should correctly calculate holdings from BUY transactions")
        void shouldCalculateHoldingsFromBuyTransactions() {
            // Given - Multiple BUY transactions
            Transaction buy1 = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.BUY,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(30),
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(10800), // 3 hours ago
                "First buy"
            );
            portfolio.recordTransaction(accountId, buy1);
            
            Transaction buy2 = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.BUY,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(40),
                new Money(BigDecimal.valueOf(155), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(7200), // 2 hours ago
                "Second buy"
            );
            portfolio.recordTransaction(accountId, buy2);
            
            Transaction buy3 = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.BUY,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(30),
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600), // 1 hour ago
                "Third buy"
            );
            portfolio.recordTransaction(accountId, buy3);
            
            TransactionId sellTxId = TransactionId.randomId();
            Transaction sellTx = new Transaction(
                sellTxId,
                accountId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(50),
                new Money(BigDecimal.valueOf(165), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(1800), // 30 minutes ago
                "Original sell"
            );
            portfolio.recordTransaction(accountId, sellTx);
            
            // Update sell to 100 (should work: 30 + 40 + 30 = 100 shares available)
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                userId,
                accountId,
                sellTxId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(100), // All available shares
                new Money(BigDecimal.valueOf(165), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(1800),
                "Updated sell - all shares"
            );
            
            List<Transaction> transactions = List.of(buy1, buy2, buy3, sellTx);
            if (transactions.isEmpty()) {
                fail();
            }
            Page<Transaction> page = new PageImpl<>(transactions);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryRepository.findByAccountId(accountId, Pageable.unpaged()))
                .thenReturn(page);
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionResponse response = service.updateTransation(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }
        
        @Test
        @DisplayName("Should correctly subtract SELL transactions in holdings calculation")
        void shouldSubtractSellTransactionsInHoldingsCalculation() {
            // Given - Buy lots, sell some, then update a later sell
            Transaction buy1 = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.BUY,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(200),
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(10800), // 3 hours ago
                "Buy 200"
            );
            portfolio.recordTransaction(accountId, buy1);
            
            Transaction sell1 = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(50),
                new Money(BigDecimal.valueOf(155), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(7200), // 2 hours ago
                "Sell 50"
            );
            portfolio.recordTransaction(accountId, sell1);
            
            Transaction sell2 = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(30),
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(5400), // 1.5 hours ago
                "Sell 30"
            );
            portfolio.recordTransaction(accountId, sell2);
            
            TransactionId sell3Id = TransactionId.randomId();
            Transaction sell3 = new Transaction(
                sell3Id,
                accountId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(20),
                new Money(BigDecimal.valueOf(165), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600), // 1 hour ago
                "Sell 20"
            );
            portfolio.recordTransaction(accountId, sell3);
            
            // Holdings at sell3 time: 200 (buy1) - 50 (sell1) - 30 (sell2) - 20 (sell3 excluded) = 120
            // Update sell3 to 120 (maximum possible)
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                userId,
                accountId,
                sell3Id,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(120), // Sell all remaining shares
                new Money(BigDecimal.valueOf(165), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600),
                "Updated sell to 120"
            );
            
            List<Transaction> transactions = List.of(buy1, sell1, sell2, sell3);
            if (transactions.isEmpty()) {
                fail();
            }
            Page<Transaction> page = new PageImpl<>(transactions);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryRepository.findByAccountId(accountId, Pageable.unpaged()))
                .thenReturn(page);
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionResponse response = service.updateTransation(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should fail when SELL transactions reduce holdings below update quantity")
        void shouldFailWhenSellTransactionsReduceHoldings() {
            // Given - Explicitly test that SELL subtracts from holdings
            Transaction buy1 = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.BUY,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(100),
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(10800), // 3 hours ago
                "Buy 100"
            );
            portfolio.recordTransaction(accountId, buy1);
            
            // This SELL reduces available holdings
            Transaction sell1 = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(60), // Sell 60, leaving 40
                new Money(BigDecimal.valueOf(155), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(7200), // 2 hours ago
                "Sell 60"
            );
            portfolio.recordTransaction(accountId, sell1);
            
            TransactionId sell2Id = TransactionId.randomId();
            Transaction sell2 = new Transaction(
                sell2Id,
                accountId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(10),
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600), // 1 hour ago
                "Sell 10"
            );
            portfolio.recordTransaction(accountId, sell2);
            
            // Try to update sell2 to 50
            // Holdings: 100 (buy) - 60 (sell1) = 40, but trying to sell 50
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                userId,
                accountId,
                sell2Id,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(50), // More than available 40
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600),
                "Updated sell to 50"
            );
            
            List<Transaction> transactions = List.of(buy1, sell1, sell2);
            if (transactions.isEmpty()) {
                fail();
            }
            Page<Transaction> page = new PageImpl<>(transactions);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryRepository.findByAccountId(accountId, Pageable.unpaged()))
                .thenReturn(page);
            // when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));

            // When/Then - Should fail because sell1 reduced holdings to 40
            assertThatThrownBy(() -> service.updateTransation(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient holdings")
                .hasMessageContaining("Available: 40")
                .hasMessageContaining("Attempting to sell: 50");
            
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should correctly calculate holdings with mixed BUY and SELL transactions")
        void shouldCalculateHoldingsWithMixedTransactions() {
            // Given - Mixed BUY and SELL transactions
            Transaction buy1 = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.BUY,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(100),
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(10800), // 3 hours ago
                "Buy 100"
            );
            portfolio.recordTransaction(accountId, buy1);
            
            Transaction sell1 = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(30),
                new Money(BigDecimal.valueOf(155), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(7200), // 2 hours ago
                "Sell 30"
            );
            portfolio.recordTransaction(accountId, sell1);
            
            Transaction buy2 = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.BUY,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(50),
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(5400), // 1.5 hours ago
                "Buy 50"
            );
            portfolio.recordTransaction(accountId, buy2);
            
            Transaction sell2 = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(20),
                new Money(BigDecimal.valueOf(165), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600), // 1 hour ago
                "Sell 20"
            );
            portfolio.recordTransaction(accountId, sell2);
            
            // Now update sell2 to sell more shares
            // Holdings at that point: 100 (buy1) - 30 (sell1) + 50 (buy2) - 20 (sell2 excluded) = 120
            TransactionId sell2Id = sell2.getTransactionId();
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                userId,
                accountId,
                sell2Id,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(100), // Update to sell 100 (was 20)
                new Money(BigDecimal.valueOf(165), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600),
                "Updated sell to 100"
            );
            
            List<Transaction> transactions = List.of(buy1, sell1, buy2, sell2);
            if (transactions.isEmpty()) {
                fail();
            }
            Page<Transaction> page = new PageImpl<>(transactions);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryRepository.findByAccountId(accountId, Pageable.unpaged()))
                .thenReturn(page);
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionResponse response = service.updateTransation(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should ignore transactions after the sell date")
        void shouldIgnoreTransactionsAfterSellDate() {
            // Given - Transactions both before AND after the sell date
            Transaction buy1 = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.BUY,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(50),
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(7200), // 2 hours ago
                "Buy 50 BEFORE sell"
            );
            portfolio.recordTransaction(accountId, buy1);
            
            TransactionId sellTxId = TransactionId.randomId();
            Instant sellDate = Instant.now().minusSeconds(3600); // 1 hour ago
            Transaction sellTx = new Transaction(
                sellTxId,
                accountId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(30),
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                null,
                sellDate,
                "Original sell"
            );
            portfolio.recordTransaction(accountId, sellTx);
            
            // This BUY happens AFTER the sell date, so should be ignored in validation
            Transaction buy2 = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.BUY,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(100), // This should NOT count towards available holdings
                new Money(BigDecimal.valueOf(165), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(1800), // 30 minutes ago (AFTER sell)
                "Buy 100 AFTER sell"
            );
            portfolio.recordTransaction(accountId, buy2);
            
            // Try to update sell to 60 shares
            // Should fail because only 50 shares available at sell time (buy2 is after)
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                userId,
                accountId,
                sellTxId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(60), // Trying to sell more than the 50 available
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                null,
                sellDate,
                "Updated sell"
            );
            
            List<Transaction> transactions = List.of(buy1, sellTx, buy2);
            if (transactions.isEmpty()) {
                fail();
            }
            Page<Transaction> page = new PageImpl<>(transactions);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryRepository.findByAccountId(accountId, Pageable.unpaged()))
                .thenReturn(page);
            // when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.updateTransation(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient holdings")
                .hasMessageContaining("Available: 50")
                .hasMessageContaining("Attempting to sell: 60");
            
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should successfully update when transactions after sell date exist but not needed")
        void shouldUpdateSuccessfullyWithFutureTransactions() {
            // Given - Similar setup but valid update
            Transaction buy1 = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.BUY,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(100),
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(7200), // 2 hours ago
                "Buy 100 BEFORE sell"
            );
            portfolio.recordTransaction(accountId, buy1);
            
            TransactionId sellTxId = TransactionId.randomId();
            Instant sellDate = Instant.now().minusSeconds(3600); // 1 hour ago
            Transaction sellTx = new Transaction(
                sellTxId,
                accountId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(30),
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                null,
                sellDate,
                "Original sell"
            );
            portfolio.recordTransaction(accountId, sellTx);
            
            // This happens after, so ignored
            Transaction buy2 = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.BUY,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(50),
                new Money(BigDecimal.valueOf(165), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(1800), // 30 minutes ago (AFTER sell)
                "Buy 50 AFTER sell"
            );
            portfolio.recordTransaction(accountId, buy2);
            
            // Update sell to 80 (valid because 100 shares available at sell time)
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                userId,
                accountId,
                sellTxId,
                TransactionType.SELL,
                assetInfo.toIdentifier(),
                BigDecimal.valueOf(80), // Valid: only considering buy1 (100 shares)
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                null,
                sellDate,
                "Updated sell to 80"
            );
            
            List<Transaction> transactions = List.of(buy1, sellTx, buy2);
            if (transactions.isEmpty()) {
                fail("Something wen't wrong");
            }
            Page<Transaction> page = new PageImpl<>(transactions);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryRepository.findByAccountId(accountId, Pageable.unpaged()))
                .thenReturn(page);
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionResponse response = service.updateTransation(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }
    }

    @Nested
    @DisplayName("Delete Transaction Tests")
    class DeleteTransactionTests {

        private Transaction transaction;
        private TransactionId transactionId;

        @BeforeEach
        void setUp() {
            transactionId = TransactionId.randomId();
            
            transaction = new Transaction(
                transactionId,
                accountId,
                TransactionType.BUY,
                identifier,
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600),
                "To be deleted"
            );
            
            portfolio.recordTransaction(accountId, transaction);
        }

        @Test
        @DisplayName("Should successfully delete transaction")
        void shouldDeleteTransaction() {
            // Given
            DeleteTransactionCommand command = new DeleteTransactionCommand(
                userId,
                accountId,
                transactionId,
                false,
                null
            );
            
            List<Transaction> transactionList = List.of(transaction);
            if (transactionList.isEmpty()) {
                fail("Something wrong here");
            }
            Page<Transaction> page = new PageImpl<>(transactionList);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryRepository.findByAccountId(accountId, Pageable.unpaged()))
                .thenReturn(page);
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));

            // When
            service.deleteTransaction(command);

            // Then
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when validation result is not valid")
        void shouldThrowExceptionsWhenValidationResultIsNotValid() {
            DeleteTransactionCommand command = mock(DeleteTransactionCommand.class);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("ERROR"));

            assertThatThrownBy(() -> service.deleteTransaction(command))
                .isInstanceOf(InvalidTransactionException.class);
            
            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }
    }

    @Nested
    @DisplayName("Account Management Tests")
    class AccountManagementTests {

        @Test
        @DisplayName("Should successfully add account")
        void shouldAddAccount() {
            // Given
            AddAccountCommand command = new AddAccountCommand(
                userId,
                "New Account",
                AccountType.TFSA,
                ValidatedCurrency.CAD
            );
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);
            when(portfolioMapper.toAccountResponse(any(), any())).thenReturn(mock(AccountResponse.class));

            // When
            AccountResponse response = service.addAccount(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should successfully remove empty account")
        void shouldRemoveEmptyAccount() {
            // Given
            AccountId newAccountId = AccountId.randomId();
            Account emptyAccount = new Account(newAccountId, "Empty", AccountType.TFSA, ValidatedCurrency.CAD);
            emptyAccount.close();
            portfolio.addAccount(emptyAccount);
            
            RemoveAccountCommand command = new RemoveAccountCommand(userId, newAccountId);
            
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));

            // When
            service.removeAccount(command);

            // Then
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when removing non-empty account")
        void shouldThrowExceptionWhenRemovingNonEmptyAccount() {
            // Given - add an asset to the account to make it non-empty
            Transaction buyTx = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.BUY,
                assetInfo.toIdentifier(),
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now(),
                "Asset to prevent deletion"
            );
            portfolio.recordTransaction(accountId, buyTx);
            
            RemoveAccountCommand command = new RemoveAccountCommand(userId, accountId);
            
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));

            // Verify account actually has assets before the test
            Account accountToRemove = portfolio.getAccount(accountId);
            assertThat(accountToRemove.getAssets()).isNotEmpty();

            // When/Then
            assertThatThrownBy(() -> service.removeAccount(command))
                .isInstanceOf(AccountNotEmptyException.class);
                
            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when validation result is not valid")
        void shouldThrowExceptionsWhenValidationResultIsNotValid() {
            AddAccountCommand command = mock(AddAccountCommand.class);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("ERROR"));

            assertThatThrownBy(() -> service.addAccount(command))
                .isInstanceOf(InvalidTransactionException.class);
            
            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }
        
    }

    @Nested
    @DisplayName("Portfolio Lifecycle Tests")
    class PortfolioLifecycleTests {

        @Test
        @DisplayName("Should successfully create portfolio")
        void shouldCreatePortfolio() {
            // Given
            UserId newUserId = UserId.randomId();
            CreatePortfolioCommand command = new CreatePortfolioCommand(
                newUserId,
                ValidatedCurrency.USD,
                true
            );
            
            Portfolio newPortfolio = new Portfolio(newUserId, ValidatedCurrency.USD);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findByUserId(newUserId)).thenReturn(Optional.empty());
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(newPortfolio);
            when(portfolioMapper.toResponse(any(), any())).thenReturn(mock(PortfolioResponse.class));

            // When
            PortfolioResponse response = service.createPortfolio(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should successfully create portfolio w/o default account")
        void shouldCreatePortfolioNoAccount() {
            // Given
            UserId newUserId = UserId.randomId();
            CreatePortfolioCommand command = new CreatePortfolioCommand(
                newUserId,
                ValidatedCurrency.USD,
                false
            );
            
            Portfolio newPortfolio = new Portfolio(newUserId, ValidatedCurrency.USD);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findByUserId(newUserId)).thenReturn(Optional.empty());
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(newPortfolio);
            when(portfolioMapper.toResponse(any(), any())).thenReturn(mock(PortfolioResponse.class));

            // When
            PortfolioResponse response = service.createPortfolio(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
            assertThat(response.accounts()).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when portfolio already exists")
        void shouldThrowExceptionWhenPortfolioExists() {
            // Given
            CreatePortfolioCommand command = new CreatePortfolioCommand(
                userId,
                ValidatedCurrency.USD,
                true
            );
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.createPortfolio(command))
                .isInstanceOf(PortfolioAlreadyExistsException.class);
        }

        @Test
        @DisplayName("Should throw exception when validation result is not valid")
        void shouldThrowExceptionsWhenValidationResultIsNotValid() {
            CreatePortfolioCommand command = mock(CreatePortfolioCommand.class);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("ERROR"));

            assertThatThrownBy(() -> service.createPortfolio(command))
                .isInstanceOf(InvalidCommandException.class);
            
            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        // delete portfolio //
        @Test
        @DisplayName("Should successfully delete empty portfolio with confirmation")
        void shouldDeleteEmptyPortfolio() {
            // Given - create a portfolio with no accounts
            UserId newUserId = UserId.randomId();
            Portfolio emptyPortfolio = new Portfolio(newUserId, ValidatedCurrency.USD);
            
            DeletePortfolioCommand command = new DeletePortfolioCommand(newUserId, true, false);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findByUserId(newUserId)).thenReturn(Optional.of(emptyPortfolio));
            
            // Verify portfolio is empty before deletion
            assertThat(emptyPortfolio.getAccounts()).isEmpty();
            assertThat(emptyPortfolio.containsAccounts()).isFalse();

            // When
            service.deletePortfolio(command);

            // Then
            verify(portfolioRepository).delete(emptyPortfolio.getPortfolioId());
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when deleting without confirmation")
        void shouldThrowExceptionWhenDeletingWithoutConfirmation() {
            // Given
            DeletePortfolioCommand command = new DeletePortfolioCommand(userId, false, true);
            // When/Then
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            assertThatThrownBy(() -> service.deletePortfolio(command))
                .isInstanceOf(PortfolioDeletionRequiresConfirmationException.class);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-empty portfolio")
        void shouldThrowExceptionWhenDeletingNonEmptyPortfolio() {
            // Given - portfolio already has accounts from setUp()
            DeletePortfolioCommand command = new DeletePortfolioCommand(userId, true, false);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            
            // Verify portfolio actually has accounts before the test
            assertThat(portfolio.getAccounts()).isNotEmpty();
            assertThat(portfolio.containsAccounts()).isTrue();

            // When/Then - portfolio has accounts, should fail
            assertThatThrownBy(() -> service.deletePortfolio(command))
                .isInstanceOf(PortfolioNotEmptyException.class)
                .hasMessageContaining("Cannot delete portfolio with existing accounts");
                
            // Verify delete was never called
            verify(portfolioRepository, never()).delete(any(PortfolioId.class));
        }

        @Test
        @DisplayName("Should throw exception when validation result is not valid delete ver")
        void shouldThrowExceptionsWhenValidationResultIsNotValidDelete() {
            DeletePortfolioCommand command = mock(DeletePortfolioCommand.class);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("ERROR"));

            assertThatThrownBy(() -> service.deletePortfolio(command))
                .isInstanceOf(InvalidCommandException.class);
            
            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }
    }

    @Nested
    @DisplayName("Correct Asset Ticker Tests")
    class CorrectAssetTickerTests {

        @Test
        @DisplayName("Should successfully correct asset ticker")
        void shouldCorrectAssetTicker() {
            // Given
            Transaction buyTx = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.BUY,
                new MarketIdentifier("AAPL", null, AssetType.STOCK, "Appled", "USD", null),
                BigDecimal.TEN,
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now(),
                "Wrong ticker"
            );
            portfolio.recordTransaction(accountId, buyTx);
            
            CorrectAssetTickerCommand command = new CorrectAssetTickerCommand(
                userId,
                accountId,
                new MarketIdentifier("AAPL", null, AssetType.STOCK, "Appled", "USD", null),
                new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null)
            );
            
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            service.correctAssetTicket(command);

            // Then
            verify(portfolioRepository).save(any(Portfolio.class));
        }
    }
}
