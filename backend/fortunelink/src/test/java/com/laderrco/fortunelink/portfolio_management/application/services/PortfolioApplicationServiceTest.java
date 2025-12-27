package com.laderrco.fortunelink.portfolio_management.application.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordIncomeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RemoveAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdateTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.AccountNotEmptyException;
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
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.TransactionNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CashIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.TransactionQueryRepository;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PortfolioValuationService;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    private PortfolioId portfolioId;
    private AccountId accountId;
    private Portfolio portfolio;
    private Account account;
    private MarketAssetInfo assetInfo;

    @BeforeEach
    void setUp() {
        userId = UserId.randomId();
        portfolioId = PortfolioId.randomId();
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
            when(commandValidator.validate(command)).thenReturn(ValidationResult.isValid());
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
                .thenReturn(ValidationResult.invalid(List.of("Invalid quantity")));

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
            when(commandValidator.validate(command)).thenReturn(ValidationResult.valid());
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
            when(commandValidator.validate(command)).thenReturn(ValidationResult.valid());
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
            
            when(commandValidator.validate(largeCommand)).thenReturn(ValidationResult.valid());
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
            // First add some assets to sell
            Transaction buyTx = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.BUY,
                new StockIdentifier("AAPL"),
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
            when(commandValidator.validate(command)).thenReturn(ValidationResult.valid());
            when(marketDataService.getAssetInfo("AAPL")).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionResponse response = service.recordAssetSale(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when selling more than owned")
        void shouldThrowExceptionWhenSellingMoreThanOwned() {
            // Given
            RecordSaleCommand largeCommand = new RecordSaleCommand(
                userId,
                accountId,
                "AAPL",
                BigDecimal.valueOf(100), // More than we own
                new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                null,
                Instant.now(),
                "Large sale"
            );
            
            when(commandValidator.validate(largeCommand)).thenReturn(ValidationResult.valid());
            when(marketDataService.getAssetInfo("AAPL")).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.recordAssetSale(largeCommand))
                .isInstanceOf(InsufficientFundsException.class);
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
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.valid());
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionResponse response = service.recordDeposit(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
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
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.valid());
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
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.valid());
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.recordWithdrawal(command))
                .isInstanceOf(InsufficientFundsException.class);
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
                new StockIdentifier("AAPL"),
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
                false,
                BigDecimal.ZERO,
                Instant.now(),
                "Dividend"
            );
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.valid());
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
                new Money(BigDecimal.valueOf(75), Currency.USD),
                true,
                BigDecimal.valueOf(0.5), // Bought 0.5 shares
                Instant.now(),
                "DRIP Dividend"
            );
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.valid());
            when(marketDataService.getAssetInfo("AAPL")).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionResponse response = service.recordDividendIncome(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
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
                new StockIdentifier("AAPL"),
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
                new StockIdentifier("AAPL"),
                BigDecimal.valueOf(15), // Changed quantity
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600),
                "Updated purchase"
            );
            
            List<Transaction> transactions = List.of(existingTransaction);
            Page<Transaction> page = new PageImpl<>(transactions);
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.valid());
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
        @DisplayName("Should throw exception when transaction not found")
        void shouldThrowExceptionWhenTransactionNotFound() {
            // Given
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                userId,
                accountId,
                TransactionId.randomId(), // Different ID
                TransactionType.BUY,
                new StockIdentifier("AAPL"),
                BigDecimal.valueOf(15),
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now().minusSeconds(3600),
                "Updated purchase"
            );
            
            Page<Transaction> page = new PageImpl<>(List.of(existingTransaction));
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.valid());
            when(transactionQueryRepository.findByAccountId(accountId, Pageable.unpaged()))
                .thenReturn(page);

            // When/Then
            assertThatThrownBy(() -> service.updateTransation(command))
                .isInstanceOf(TransactionNotFoundException.class);
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
                new StockIdentifier("AAPL"),
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
                transactionId
            );
            
            Page<Transaction> page = new PageImpl<>(List.of(transaction));
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.valid());
            when(transactionQueryRepository.findByAccountId(accountId, Pageable.unpaged()))
                .thenReturn(page);
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));

            // When
            service.deleteTransaction(command);

            // Then
            verify(portfolioRepository).save(any(Portfolio.class));
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
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.valid());
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
            // Given - account has assets
            RemoveAccountCommand command = new RemoveAccountCommand(userId, accountId);
            
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.removeAccount(command))
                .isInstanceOf(AccountNotEmptyException.class);
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
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.valid());
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
        @DisplayName("Should throw exception when portfolio already exists")
        void shouldThrowExceptionWhenPortfolioExists() {
            // Given
            CreatePortfolioCommand command = new CreatePortfolioCommand(
                userId,
                ValidatedCurrency.USD,
                true
            );
            
            when(commandValidator.validate(command)).thenReturn(ValidationResult.valid());
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.createPortfolio(command))
                .isInstanceOf(PortfolioAlreadyExistsException.class);
        }

        @Test
        @DisplayName("Should throw exception when deleting without confirmation")
        void shouldThrowExceptionWhenDeletingWithoutConfirmation() {
            // Given
            DeletePortfolioCommand command = new DeletePortfolioCommand(userId, false);

            // When/Then
            assertThatThrownBy(() -> service.deletePortfolio(command))
                .isInstanceOf(PortfolioDeletionRequiresConfirmationException.class);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-empty portfolio")
        void shouldThrowExceptionWhenDeletingNonEmptyPortfolio() {
            // Given
            DeletePortfolioCommand command = new DeletePortfolioCommand(userId, true);
            
            when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));

            // When/Then - portfolio has accounts
            assertThatThrownBy(() -> service.deletePortfolio(command))
                .isInstanceOf(PortfolioNotEmptyException.class);
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
                new StockIdentifier("WRONG"),
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
                new StockIdentifier("WRONG"),
                new StockIdentifier("CORRECT")
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
