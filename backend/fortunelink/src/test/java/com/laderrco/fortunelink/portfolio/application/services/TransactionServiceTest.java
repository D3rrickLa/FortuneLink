package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.commands.ExcludeTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.commands.RestoreTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.*;
import com.laderrco.fortunelink.portfolio.application.events.PositionRecalculationRequestedEvent;
import com.laderrco.fortunelink.portfolio.application.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio.application.exceptions.InsufficientQuantityException;
import com.laderrco.fortunelink.portfolio.application.exceptions.InvalidTransactionException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.application.mappers.TransactionViewMapper;
import com.laderrco.fortunelink.portfolio.application.validators.TransactionCommandValidator;
import com.laderrco.fortunelink.portfolio.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.*;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.*;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.TransactionRecordingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    // Test Constants
    private final UserId userId = UserId.random();
    private final PortfolioId portfolioId = PortfolioId.newId();
    private final AccountId accountId = AccountId.newId();
    private final Currency USD = Currency.of("USD");
    private final Currency CAD = Currency.of("CAD");
    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionViewMapper transactionViewMapper;
    @Mock
    private TransactionCommandValidator validator;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private MarketDataService marketDataService;
    @Mock
    private ExchangeRateService exchangeRateService;
    @Mock
    private TransactionRecordingService transactionRecordingService;
    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        lenient().when(validator.validate((any(RecordPurchaseCommand.class)))).thenReturn(ValidationResult.success());
        lenient().when(validator.validate((any(RecordSaleCommand.class)))).thenReturn(ValidationResult.success());
        lenient().when(validator.validate((any(RecordDividendCommand.class)))).thenReturn(ValidationResult.success());
        lenient().when(validator.validate((any(RecordDividendReinvestmentCommand.class)))).thenReturn(ValidationResult.success());
        lenient().when(validator.validate((any(RecordDepositCommand.class)))).thenReturn(ValidationResult.success());
        lenient().when(validator.validate((any(RecordWithdrawalCommand.class)))).thenReturn(ValidationResult.success());
        lenient().when(validator.validate((any(RecordFeeCommand.class)))).thenReturn(ValidationResult.success());
    }

    @Nested
    @DisplayName("recordPurchase Tests")
    class RecordPurchaseTests {

        @Test
        @DisplayName("recordPurchase_Success_SameCurrency")
        void recordPurchase_Success_NoCurrencyConversionNeeded() {
            var command = new RecordPurchaseCommand(
                    portfolioId,
                    userId,
                    accountId,
                    "AAPL",
                    Quantity.of(10.0),
                    Price.of(new BigDecimal("150.0"), USD),
                    List.of(),
                    Instant.now(),
                    "Buy Apple"
            );

            Portfolio portfolio = mock(Portfolio.class);
            Account account = mock(Account.class);
            Transaction transaction = mock(Transaction.class);

            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.of(portfolio));
            when(portfolio.getAccount(accountId)).thenReturn(account);
            when(account.getAccountCurrency()).thenReturn(USD);
            when(marketDataService.getAssetInfo(any())).thenReturn(Optional.of(new MarketAssetInfo(
                    new AssetSymbol("AAPL"),
                    "Apple",
                    AssetType.STOCK,
                    "NASDAQ",
                    USD,
                    "Tech",
                    "some description here"
            )));
            when(transactionRecordingService.recordBuy(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(transaction);

            transactionService.recordPurchase(command);

            InOrder inOrder = inOrder(portfolioRepository, transactionRepository);
            inOrder.verify(portfolioRepository).save(portfolio);
            inOrder.verify(transactionRepository).save(transaction);
        }

        @Test
        @DisplayName("recordPurchase_Failure_AssetNotFound")
        void recordPurchase_Failure_WhenSymbolIsUnknown() {
            var command = new RecordPurchaseCommand(
                    portfolioId,
                    userId,
                    accountId,
                    "UNKNOWN",
                    Quantity.of(1),
                    new Price(new Money(BigDecimal.ONE, USD)),
                    null,
                    Instant.now(),
                    "notes"
            );

            Portfolio portfolio = mock(Portfolio.class);
            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.of(portfolio));
            when(marketDataService.getAssetInfo(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.recordPurchase(command))
                    .isInstanceOf(AssetNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("recordSale Tests")
    class RecordSaleTests {

        @Test
        @DisplayName("recordSale_Failure_InsufficientQuantity")
        void recordSale_Failure_WhenNoPositionExists() {
            var command = new RecordSaleCommand(
                    portfolioId,
                    userId,
                    accountId,
                    "TSLA",
                    Quantity.of(5.0),
                    new Price(new Money(BigDecimal.TEN, USD)),
                    List.of(),
                    Instant.now(),
                    "notes"
            );

            Portfolio portfolio = mock(Portfolio.class);
            Account account = mock(Account.class);

            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.of(portfolio));
            when(portfolio.getAccount(accountId)).thenReturn(account);
            when(account.hasPosition(any())).thenReturn(false);

            assertThatThrownBy(() -> transactionService.recordSale(command))
                    .isInstanceOf(InsufficientQuantityException.class);
        }

        @Test
        @DisplayName("recordSale_Success_WithCurrencyConversion")
        void recordSale_Success_WhenCurrencyDiffers() {
            var command = new RecordSaleCommand(
                    portfolioId,
                    userId,
                    accountId,
                    "TSLA",
                    Quantity.of(1.0),
                    Price.of(new BigDecimal("100.0"), CAD),
                    null,
                    Instant.now(),
                    null
            );

            Portfolio portfolio = mock(Portfolio.class);
            Account account = mock(Account.class);

            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.of(portfolio));
            when(portfolio.getAccount(accountId)).thenReturn(account);
            when(account.getAccountCurrency()).thenReturn(USD);
            when(account.hasPosition(any())).thenReturn(true);
            when(exchangeRateService.convertToPrice(any(), eq(USD))).thenReturn(Price.of(new BigDecimal("75.0"), USD));

            transactionService.recordSale(command);

            verify(exchangeRateService).convertToPrice(any(), eq(USD));
            verify(transactionRecordingService).recordSell(eq(account), any(), any(Quantity.class), argThat(p -> p.currency().equals(USD)), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("excludeTransaction Tests")
    class ExcludeTransactionTests {

        @Test
        @DisplayName("excludeTransaction_Success_PositionAffecting")
        void excludeTransaction_Success_TriggersRecalculationForTrades() {
            var command = new ExcludeTransactionCommand(TransactionId.newId(), portfolioId, userId, accountId, "Error");
            Transaction transaction = mock(Transaction.class);
            Transaction.TradeExecution execution = mock(Transaction.TradeExecution.class);
            AssetSymbol asset = new AssetSymbol("AAPL");

            when(validator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(), any())).thenReturn(Optional.of(transaction));
            when(transaction.isExcluded()).thenReturn(false);
            when(transaction.markAsExcluded(any(), any())).thenReturn(transaction);
            when(transaction.transactionType()).thenReturn(TransactionType.BUY); // affectsHoldings = true
            when(transaction.execution()).thenReturn(execution);
            when(execution.asset()).thenReturn(asset);

            transactionService.excludeTransaction(command);

            verify(eventPublisher).publishEvent(any(PositionRecalculationRequestedEvent.class));
            verify(transactionRepository).save(transaction);
        }

        @Test
        @DisplayName("excludeTransaction_Success_CashEvent")
        void excludeTransaction_Success_DoesntTriggerRecalculation() {
            var command = new ExcludeTransactionCommand(TransactionId.newId(), portfolioId, userId, accountId, "Error");
            Transaction transaction = mock(Transaction.class);

            when(validator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(), any())).thenReturn(Optional.of(transaction));
            when(transaction.isExcluded()).thenReturn(false);
            when(transaction.markAsExcluded(any(), any())).thenReturn(transaction);
            when(transaction.transactionType()).thenReturn(TransactionType.BUY); // affectsHoldings = true
            when(transaction.execution()).thenReturn(null);

            transactionService.excludeTransaction(command);

            verify(transactionRepository).save(transaction);
        }

        @Test
        @DisplayName("excludeTransaction_Success_NonPositionAffecting")
        void excludeTransaction_Success_DoesNotTriggerRecalculationForCash() {
            var command = new ExcludeTransactionCommand(TransactionId.newId(), portfolioId, userId, accountId, "Error");
            Transaction transaction = mock(Transaction.class);

            when(validator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(), any())).thenReturn(Optional.of(transaction));
            when(transaction.transactionType()).thenReturn(TransactionType.DEPOSIT); // affectsHoldings = false

            transactionService.excludeTransaction(command);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("excludeTransaction_Failure_AlreadyExcluded")
        void excludeTransaction_Failure_WhenTransactionIsAlreadyExcluded() {
            var command = new ExcludeTransactionCommand(TransactionId.newId(), portfolioId, userId, accountId, "Error");
            Transaction transaction = mock(Transaction.class);

            when(validator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(), any())).thenReturn(Optional.of(transaction));
            when(transaction.isExcluded()).thenReturn(true);

            assertThatThrownBy(() -> transactionService.excludeTransaction(command))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("already excluded");
        }
    }

    @Nested
    @DisplayName("Cash and Dividend Tests")
    class CashAndDividendTests {

        @Test
        @DisplayName("recordDeposit_Success_PersistsCashIncrease")
        void recordDeposit_Success_CallsRecordingService() {
            var command = new RecordDepositCommand(
                    portfolioId,
                    userId,
                    accountId,
                    Money.of(1000.0, "USD"),
                    null,
                    Instant.now(),
                    "Initial fund"
            );

            Portfolio portfolio = mock(Portfolio.class);
            Account account = mock(Account.class);
            Transaction transaction = mock(Transaction.class);

            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.of(portfolio));
            when(portfolio.getAccount(accountId)).thenReturn(account);
            when(transactionRecordingService.recordDeposit(any(), any(), any(), any())).thenReturn(transaction);

            transactionService.recordDeposit(command);

            verify(transactionRecordingService).recordDeposit(eq(account), eq(command.amount()), eq(command.notes()), eq(command.transactionDate()));
            verify(portfolioRepository).save(portfolio);
        }

        @Test
        @DisplayName("recordDividend_Success_AddsCashToAccount")
        void recordDividend_Success_ValidAssetAndAmount() {
            var command = new RecordDividendCommand(
                    portfolioId,
                    userId,
                    accountId,
                    "AAPL",
                    Money.of(50.0, "USD"),
                    Instant.now(),
                    "Quarterly"
            );

            Portfolio portfolio = mock(Portfolio.class);
            Account account = mock(Account.class);
            Transaction transaction = mock(Transaction.class);

            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.of(portfolio));
            when(portfolio.getAccount(accountId)).thenReturn(account);
            when(transactionRecordingService.recordDividend(any(), any(), any(), any(), any())).thenReturn(transaction);

            transactionService.recordDividend(command);

            verify(transactionRecordingService).recordDividend(eq(account), argThat(s ->
                    s.symbol().equals("AAPL")), eq(command.amount()), any(), any());
        }
    }

    @Nested
    @DisplayName("Dividend Reinvestment Tests")
    class DividendReinvestmentTests {

        @Test
        @DisplayName("recordDividendReinvestment_Success_ExecutesComplexCommand")
        void recordDividendReinvestment_Success_WithNestedExecution() {
            var execution = new RecordDividendReinvestmentCommand.DripExecution(Quantity.of(5.0), Price.of(new BigDecimal("100.0"), USD));
            var command = new RecordDividendReinvestmentCommand(portfolioId, userId, accountId, "AAPL", execution, Instant.now(), "DRIP");

            Portfolio portfolio = mock(Portfolio.class);
            Account account = mock(Account.class);
            Transaction transaction = mock(Transaction.class);

            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.of(portfolio));
            when(portfolio.getAccount(accountId)).thenReturn(account);
            when(transactionRecordingService.recordDividendReinvestment(any(), any(), any(), any(), any(), any())).thenReturn(transaction);

            transactionService.recordDividendReinvestment(command);

            verify(transactionRecordingService).recordDividendReinvestment(
                    eq(account),
                    argThat(s -> s.symbol().equals("AAPL")),
                    eq(Quantity.of(5.0)),
                    eq(execution.pricePerShare()),
                    any(),
                    any()
            );
        }
    }

    @Nested
    @DisplayName("Restore Transaction Tests")
    class RestoreTransactionTests {

        @Test
        @DisplayName("restoreTransaction_Success_PositionAffecting")
        void restoreTransaction_Success_WhenTransactionIsExcluded() {
            var command = new RestoreTransactionCommand(TransactionId.newId(), portfolioId, userId, accountId);
            Transaction existing = mock(Transaction.class);
            Transaction restored = mock(Transaction.class);
            Transaction.TradeExecution execution = mock(Transaction.TradeExecution.class);

            when(validator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(), any()))
                    .thenReturn(Optional.of(existing));
            when(existing.isExcluded()).thenReturn(true);
            when(existing.restore()).thenReturn(restored);
            when(existing.transactionType()).thenReturn(TransactionType.SELL);
            when(existing.execution()).thenReturn(execution);
            when(execution.asset()).thenReturn(new AssetSymbol("MSFT"));

            transactionService.restoreTransaction(command);

            verify(transactionRepository).save(restored);
            verify(eventPublisher).publishEvent(any(PositionRecalculationRequestedEvent.class));
        }

        @Test
        @DisplayName("restoreTransaction_Failure_NotExcluded")
        void restoreTransaction_Failure_WhenTransactionIsAlreadyActive() {
            var command = new RestoreTransactionCommand(TransactionId.newId(), portfolioId, userId, accountId);
            Transaction existing = mock(Transaction.class);

            when(validator.validate(command)).thenReturn(ValidationResult.success());

            when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(), any()))
                    .thenReturn(Optional.of(existing));
            when(existing.isExcluded()).thenReturn(false);

            assertThatThrownBy(() -> transactionService.restoreTransaction(command))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("not excluded");
        }
    }

    @Nested
    @DisplayName("Other Transaction Types Tests")
    class OtherTransactionTypesTests {

        @Test
        @DisplayName("recordWithdrawal_Success_ReducesCash")
        void recordWithdrawal_Success_CallsRecordingService() {
            var command = new RecordWithdrawalCommand(portfolioId, userId, accountId, Money.of(10, "USD"), List.of(), Instant.now(), null);
            Portfolio portfolio = mock(Portfolio.class);
            Account account = mock(Account.class);
            Transaction transaction = mock(Transaction.class);

            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.of(portfolio));
            when(portfolio.getAccount(accountId)).thenReturn(account);
            when(transactionRecordingService.recordWithdrawal(any(), any(), any(), any())).thenReturn(transaction);

            transactionService.recordWithdrawal(command);

            verify(transactionRecordingService).recordWithdrawal(eq(account), eq(Money.of(10, "USD")), any(), any());
        }

        @Test
        @DisplayName("recordFee_Success_ReducesCash")
        void recordFee_Success_CallsRecordingService() {
            var command = new RecordFeeCommand(portfolioId, userId, accountId, new Money(BigDecimal.ONE, USD), Instant.now(), "Monthly fee");
            Portfolio portfolio = mock(Portfolio.class);
            Account account = mock(Account.class);
            Transaction transaction = mock(Transaction.class);

            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.of(portfolio));
            when(portfolio.getAccount(accountId)).thenReturn(account);
            when(transactionRecordingService.recordFee(any(), any(), any(), any())).thenReturn(transaction);

            transactionService.recordFee(command);

            verify(transactionRecordingService).recordFee(eq(account), eq(new Money(BigDecimal.ONE, USD)), any(), any());
        }
    }

    @Nested
    @DisplayName("Validation Branch Tests")
    class ValidationTests {

        @Test
        @DisplayName("validate_Failure_ThrowsInvalidTransactionException")
        void validate_Failure_WhenValidatorReturnsErrors() {
            var command = new RecordDepositCommand(
                    portfolioId,
                    userId,
                    accountId,
                    Money.of(10, "USD"),
                    null,
                    Instant.now(),
                    null
            );
            when(validator.validate(command)).thenReturn(ValidationResult.failure(List.of("Amount must be positive")));

            assertThatThrownBy(() -> transactionService.recordDeposit(command))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("Invalid recordDeposit command");
        }
    }

    @Nested
    @DisplayName("Portfolio Context Tests")
    class PortfolioContextTests {

        @Test
        @DisplayName("getPortfolioContext_Failure_PortfolioNotFound")
        void getPortfolioContext_Failure_WhenRepositoryReturnsEmpty() {
            var command = new RecordFeeCommand(portfolioId, userId, accountId, Money.of(1, "USD"), Instant.now(), null);
            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.recordFee(command))
                    .isInstanceOf(PortfolioNotFoundException.class);
        }
    }
}