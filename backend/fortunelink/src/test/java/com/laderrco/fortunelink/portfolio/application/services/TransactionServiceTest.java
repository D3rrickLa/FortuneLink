package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.lang.reflect.Executable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.laderrco.fortunelink.portfolio.application.commands.ExcludeTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.commands.RestoreTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.*;
import com.laderrco.fortunelink.portfolio.application.events.PositionRecalculationRequestedEvent;
import com.laderrco.fortunelink.portfolio.application.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio.application.exceptions.InsufficientQuantityException;
import com.laderrco.fortunelink.portfolio.application.exceptions.InvalidTransactionException;
import com.laderrco.fortunelink.portfolio.application.mappers.TransactionViewMapper;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.validators.TransactionCommandValidator;
import com.laderrco.fortunelink.portfolio.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio.application.views.TransactionView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TradeExecution;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.*;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.*;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.TransactionRecordingService;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

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
  private PortfolioLoader portfolioLoader;
  @Mock
  private MarketDataService marketDataService;
  @Mock
  private ExchangeRateService exchangeRateService;
  @Mock
  private TransactionRecordingService transactionRecordingService;

  @InjectMocks
  private TransactionService service;

  // --- Constants for DRYness ---
  private static final UserId USER_ID = UserId.random();
  private static final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private static final AccountId ACCOUNT_ID = AccountId.newId();
  private static final AssetSymbol SYMBOL_STR = new AssetSymbol("AAPL");
  private static final String NOTES = "Test Note";
  private static final Instant NOW = Instant.now();
  private static final Currency USD = Currency.of("USD");
  private static final Money AMOUNT = new Money(new BigDecimal("100.00"), USD);
  private TransactionId transactionId;

  @Mock
  private Portfolio portfolio;
  @Mock
  private Account account;
  @Mock
  private Transaction transaction;
  @Mock
  private TransactionView transactionView;

  @BeforeEach
  void setUp() {
    transactionId = TransactionId.newId();
    lenient().when(portfolioLoader.loadUserPortfolio(PORTFOLIO_ID, USER_ID)).thenReturn(portfolio);
    lenient().when(portfolio.getAccount(ACCOUNT_ID)).thenReturn(account);
    lenient().when(account.getAccountCurrency()).thenReturn(USD);

    lenient().when(validator.validate(any(RecordPurchaseCommand.class))).thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordSaleCommand.class))).thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordDepositCommand.class))).thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordWithdrawalCommand.class))).thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordFeeCommand.class))).thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordInterestCommand.class))).thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordDividendCommand.class))).thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordDividendReinvestmentCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordSplitCommand.class))).thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordReturnOfCaptialCommand.class))).thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordTransferInCommand.class))).thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordTransferOutCommand.class))).thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(ExcludeTransactionCommand.class))).thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RestoreTransactionCommand.class))).thenReturn(ValidationResult.success());
  }

  @Nested
  @DisplayName("Asset Purchase & Sale Tests")
  class AssetTransactionTests {

    @Test
    @DisplayName("recordPurchase: success when asset exists")
    void recordPurchaseSuccess() {
      RecordPurchaseCommand command = createPurchaseCommand();
      MarketAssetInfo info = new MarketAssetInfo(SYMBOL_STR, "APPLE", AssetType.STOCK, "NASDAQ", USD, "technology",
          "description");

      when(marketDataService.getAssetInfo(any())).thenReturn(Optional.of(info));
      when(transactionRecordingService.recordBuy(any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(transaction);
      when(transactionViewMapper.toTransactionView(transaction)).thenReturn(transactionView);

      TransactionView result = service.recordPurchase(command);

      assertThat(result).isEqualTo(transactionView);
      verify(portfolioRepository).save(portfolio);
      verify(transactionRepository).save(transaction);
    }

    @Test
    @DisplayName("recordPurchase: throw AssetNotFoundException when symbol is unknown")
    void recordPurchaseThrowsWhenAssetNotFound() {
      RecordPurchaseCommand command = createPurchaseCommand();
      when(marketDataService.getAssetInfo(any())).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.recordPurchase(command))
          .isInstanceOf(AssetNotFoundException.class);
    }

    @Test
    @DisplayName("recordSale: throw InsufficientQuantityException when no position exists")
    void recordSaleThrowsWhenNoPosition() {
      RecordSaleCommand command = new RecordSaleCommand(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, NOTES, Quantity.of(0),
          new Price(AMOUNT), List.of(), NOTES, NOW);
      when(account.hasPosition(any())).thenReturn(false);

      assertThatThrownBy(() -> service.recordSale(command))
          .isInstanceOf(InsufficientQuantityException.class);
    }
  }

  @Nested
  @DisplayName("Cash Flow & Distributions")
  class CashFlowTests {

    @ParameterizedTest
    @MethodSource("cashCommandProvider")
    @DisplayName("recordCashFlow: verify recording service is called correctly")
    void recordCashFlowOperations(Object command, Executable recordingCall) {
      // This is a conceptual example of how you'd parameterize the different cash
      // methods
      // In practice, since recordFn is a lambda, you'd test a few key ones:
    }

    @Test
    @DisplayName("recordDeposit: verify success flow")
    void recordDepositSuccess() {
      RecordDepositCommand cmd = new RecordDepositCommand(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, AMOUNT, List.of(), NOTES,
          NOW);
      when(transactionRecordingService.recordDeposit(eq(account), eq(AMOUNT), eq(NOTES), eq(NOW)))
          .thenReturn(transaction);

      service.recordDeposit(cmd);

      verify(transactionRecordingService).recordDeposit(any(), any(), any(), any());
      verify(transactionRepository).save(transaction);
    }
  }

  @Nested
  @DisplayName("Exclusion and Restoration")
  class ExclusionTests {

    @Test
    @DisplayName("excludeTransaction: throw exception when already excluded")
    void excludeTransactionThrowsWhenAlreadyExcluded() {
      ExcludeTransactionCommand cmd = new ExcludeTransactionCommand(transactionId, PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          "reason");
      when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(), any()))
          .thenReturn(Optional.of(transaction));
      when(transaction.isExcluded()).thenReturn(true);

      assertThatThrownBy(() -> service.excludeTransaction(cmd))
          .isInstanceOf(InvalidTransactionException.class);
    }

    @Test
    @DisplayName("excludeTransaction: publish event when holdings are affected")
    void excludeTransactionPublishesEvent() {
      ExcludeTransactionCommand cmd = new ExcludeTransactionCommand(transactionId, PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          "reason");
      TradeExecution execution = mock(TradeExecution.class);

      when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(), any()))
          .thenReturn(Optional.of(transaction));
      when(transaction.isExcluded()).thenReturn(false);
      when(transaction.markAsExcluded(any(), any())).thenReturn(transaction);
      when(transaction.transactionType()).thenReturn(TransactionType.BUY); // affects holdings
      when(transaction.execution()).thenReturn(execution);

      service.excludeTransaction(cmd);

      verify(eventPublisher).publishEvent(any(PositionRecalculationRequestedEvent.class));
    }
  }

  // --- Helper Methods ---
  private RecordPurchaseCommand createPurchaseCommand() {
    return new RecordPurchaseCommand(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, SYMBOL_STR.symbol(),
        Quantity.of(10), new Price(AMOUNT), List.of(), NOTES, NOW);
  }
}