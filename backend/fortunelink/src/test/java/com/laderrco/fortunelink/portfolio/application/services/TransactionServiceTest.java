package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.commands.ExcludeTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.commands.RestoreTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordDividendCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordDividendReinvestmentCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordDividendReinvestmentCommand.DripExecution;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordFeeCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordInterestCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordReturnOfCaptialCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordSplitCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordTransferInCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordTransferOutCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio.application.events.PositionRecalculationRequestedEvent;
import com.laderrco.fortunelink.portfolio.application.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio.application.exceptions.InsufficientQuantityException;
import com.laderrco.fortunelink.portfolio.application.exceptions.InvalidTransactionException;
import com.laderrco.fortunelink.portfolio.application.exceptions.TransactionNotFoundException;
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
import com.laderrco.fortunelink.portfolio.domain.model.enums.FeeType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Ratio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.TransactionRecordingService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

  // --- Constants for DRYness ---
  private static final UserId USER_ID = UserId.random();
  private static final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private static final AccountId ACCOUNT_ID = AccountId.newId();
  private static final String SYMBOL_STR = "AAPL";
  private static final AssetType ASSET_TYPE = AssetType.STOCK;
  private static final String NOTES = "Test Note";
  private static final Instant NOW = Instant.now();
  private static final Currency USD = Currency.of("USD");
  private static final Money AMOUNT = new Money(new BigDecimal("100.00"), USD);
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
    lenient().when(portfolio.getPortfolioId()).thenReturn(PORTFOLIO_ID);
    lenient().when(portfolioLoader.loadUserPortfolio(PORTFOLIO_ID, USER_ID)).thenReturn(portfolio);
    lenient().when(portfolio.getAccount(ACCOUNT_ID)).thenReturn(account);
    lenient().when(account.getAccountCurrency()).thenReturn(USD);

    lenient().when(validator.validate(any(RecordPurchaseCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordSaleCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordDepositCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordWithdrawalCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordFeeCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordInterestCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordDividendCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordDividendReinvestmentCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordSplitCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordReturnOfCaptialCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordTransferInCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RecordTransferOutCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(ExcludeTransactionCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(RestoreTransactionCommand.class)))
        .thenReturn(ValidationResult.success());
  }

  // --- Helper Methods ---
  private RecordPurchaseCommand createPurchaseCommand() {
    return new RecordPurchaseCommand(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, SYMBOL_STR, ASSET_TYPE, Quantity.of(10),
        new Price(AMOUNT), List.of(), NOW, NOTES);
  }

  private Money usd(double amount) {
    return Money.of(amount, USD.getCode());
  }

  @Nested
  @DisplayName("Asset Purchase & Sale Tests")
  class AssetTransactionTests {
    @Test
    @DisplayName("recordPurchase: success when asset exists")
    void recordPurchaseSuccess() {
      RecordPurchaseCommand command = createPurchaseCommand();
      AssetSymbol symbol = new AssetSymbol(SYMBOL_STR);
      MarketAssetInfo info = new MarketAssetInfo(symbol, "APPLE", AssetType.STOCK, "NASDAQ", USD,
          "technology", "description");

      when(marketDataService.getAssetInfo(any())).thenReturn(Optional.of(info));
      when(transactionRecordingService.recordBuy(any(), any(), any(), any(), any(), any(), any(),
          any())).thenReturn(transaction);
      when(transactionViewMapper.toTransactionView(transaction)).thenReturn(transactionView);

      TransactionView result = service.recordPurchase(command);

      assertThat(result).isEqualTo(transactionView);
      verify(portfolioRepository).save(portfolio);
      verify(transactionRepository).save(transaction, portfolio.getPortfolioId());
    }

    @Test
    @DisplayName("recordPurchase: success when asset exists, converts price")
    void recordPurchaseSuccessConvertsPrice() {
      Currency CAD = Currency.CAD;
      AssetSymbol symbol = new AssetSymbol("SHOP.TO");
      Money AMOUNT = new Money(new BigDecimal("100.00"), CAD);
      Price shopPriceToUsd = new Price(Money.of(75, USD));
      RecordPurchaseCommand command = new RecordPurchaseCommand(PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          symbol.symbol(), ASSET_TYPE, Quantity.of(10), new Price(AMOUNT), List.of(), NOW, NOTES);
      MarketAssetInfo info = new MarketAssetInfo(symbol, "SHOPIFY", AssetType.STOCK, "NASDAQ", CAD,
          "technology", "description");

      when(marketDataService.getAssetInfo(any())).thenReturn(Optional.of(info));
      when(transactionRecordingService.recordBuy(any(), any(), any(), any(), any(), any(), any(),
          any())).thenReturn(transaction);
      when(exchangeRateService.convertToPrice(any(), any())).thenReturn(shopPriceToUsd);
      when(transactionViewMapper.toTransactionView(transaction)).thenReturn(transactionView);

      TransactionView result = service.recordPurchase(command);

      assertThat(result).isEqualTo(transactionView);
      assertThat(command.totalFees(CAD)).isEqualTo(Money.zero(CAD));
      verify(portfolioRepository).save(portfolio);
      verify(transactionRepository).save(transaction, portfolio.getPortfolioId());
      verify(exchangeRateService).convertToPrice(AMOUNT, USD);
    }

    @Test
    @DisplayName("recordPurchase: throw AssetNotFoundException when symbol is unknown")
    void recordPurchaseThrowsWhenAssetNotFound() {
      RecordPurchaseCommand command = createPurchaseCommand();
      when(marketDataService.getAssetInfo(any())).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.recordPurchase(command)).isInstanceOf(
          AssetNotFoundException.class);
    }

    @Test
    @DisplayName("recordSale: success when asset exists")
    void recordSaleSuccess() {
      RecordSaleCommand command = new RecordSaleCommand(PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          SYMBOL_STR, Quantity.of(0), new Price(AMOUNT), List.of(), NOW, NOTES);

      when(account.hasPosition(any())).thenReturn(true);
      when(transactionRecordingService.recordSell(any(), any(), any(), any(), any(), any(),
          any())).thenReturn(transaction);

      when(transactionViewMapper.toTransactionView(transaction)).thenReturn(transactionView);
      TransactionView result = service.recordSale(command);

      assertThat(result).isEqualTo(transactionView);
      assertThat(command.totalFees(USD)).isEqualTo(Money.zero(USD));
      verify(portfolioRepository).save(portfolio);
      verify(transactionRepository).save(transaction, portfolio.getPortfolioId());
    }

    @Test
    @DisplayName("recordSale: throw InsufficientQuantityException when no position exists")
    void recordSaleThrowsWhenNoPosition() {
      RecordSaleCommand command = new RecordSaleCommand(PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          SYMBOL_STR, Quantity.of(0), new Price(AMOUNT), List.of(), NOW, NOTES);
      when(account.hasPosition(any())).thenReturn(false);

      assertThatThrownBy(() -> service.recordSale(command)).isInstanceOf(
          InsufficientQuantityException.class);
    }

    @Test
    @DisplayName("recordDividendReinvestment: verify success flow")
    void recordDividendReinvestmentSuccess() {
      DripExecution exec = new DripExecution(Quantity.of(10.0), Price.of("150", USD));
      RecordDividendReinvestmentCommand command = new RecordDividendReinvestmentCommand(
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID, "GOOGL", exec, NOW, "Reinvest");

      service.recordDividendReinvestment(command);

      verify(transactionRecordingService).recordDividendReinvestment(any(), any(AssetSymbol.class),
          eq(Quantity.of(10.0)), eq(Price.of("150", USD)), anyString(), any());
      assertThat(command.execution().totalCost()).isEqualTo(Money.of(1500, USD));
    }
  }

  @Nested
  @DisplayName("Cash Flow & Distributions")
  class CashFlowTests {
    @Test
    @DisplayName("recordDeposit: verify success flow")
    void recordDepositSuccess() {
      RecordDepositCommand cmd = new RecordDepositCommand(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, AMOUNT, NOW, NOTES);
      when(transactionRecordingService.recordDeposit(eq(account), eq(AMOUNT), eq(NOTES),
          eq(NOW))).thenReturn(transaction);

      service.recordDeposit(cmd);

      verify(transactionRecordingService).recordDeposit(any(), any(), any(), any());
      verify(transactionRepository).save(transaction, portfolio.getPortfolioId());
    }

    @Test
    @DisplayName("recordWithdrawal: verify success flow")
    void recordWithdrawalSuccess() {
      RecordWithdrawalCommand command = new RecordWithdrawalCommand(PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, AMOUNT, NOW, NOTES);

      service.recordWithdrawal(command);

      verify(transactionRecordingService).recordWithdrawal(any(), eq(command.amount()),
          eq(command.notes()), eq(command.transactionDate()));
    }

    @Test
    @DisplayName("recordFee: verify success flow")
    void recordFeeSuccess() {
      RecordFeeCommand command = new RecordFeeCommand(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, AMOUNT,
          FeeType.ACCOUNT_MAINTENANCE, NOW, NOTES);

      service.recordFee(command);

      verify(transactionRecordingService).recordFee(any(), eq(command.amount()),
          any(), eq(command.notes()), eq(command.transactionDate()));
    }

    @Test
    @DisplayName("recordInterest: verify success flow with no symbol")
    void recordInterestSuccessCashInterest() {
      RecordInterestCommand command = RecordInterestCommand.cashInterest(PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, usd(5), NOW, "Interest");
      RecordInterestCommand command2 = new RecordInterestCommand(PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          "", usd(5), NOW, "INTEREST");
      service.recordInterest(command);

      verify(transactionRecordingService).recordInterest(any(), any(), eq(command.amount()),
          eq(command.notes()), eq(command.transactionDate()));
      assertThat(command.isAssetInterest()).isFalse();
      assertThat(command2.isAssetInterest()).isFalse();

    }

    @Test
    @DisplayName("recordInterest: verify success flow with symbol")
    void recordInterestSuccessAssetInterest() {
      RecordInterestCommand command = RecordInterestCommand.assetInterest(PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, "CAD.3TBILL", usd(15), NOW, "3 month GIC");

      service.recordInterest(command);

      verify(transactionRecordingService).recordInterest(any(), any(AssetSymbol.class),
          eq(command.amount()), eq(command.notes()), eq(command.transactionDate()));
      assertThat(command.isAssetInterest()).isTrue();
    }

    @Test
    @DisplayName("recordDividend: verify success flow")
    void recordDividendSuccess() {
      RecordDividendCommand command = new RecordDividendCommand(PORTFOLIO_ID, USER_ID, ACCOUNT_ID,
          "AAPL", usd(10), NOW, "Interest");

      service.recordDividend(command);

      verify(transactionRecordingService).recordDividend(any(), any(AssetSymbol.class),
          eq(command.amount()), eq(command.notes()), eq(command.transactionDate()));
    }
  }

  @Nested
  @DisplayName("Management")
  public class ManagementTests {
    @Test
    @DisplayName("recordSplit: passes and splits data")
    void recordSplitNoPositionSuccess() {
      RecordSplitCommand command = new RecordSplitCommand(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, "TSLA",
          new Ratio(2, 1), NOW, "Split");

      when(account.hasPosition(any())).thenReturn(true);

      service.recordSplit(command);
      verify(transactionRecordingService).recordSplit(any(), any(AssetSymbol.class),
          eq(new Ratio(2, 1)), anyString(), any());

    }

    @Test
    @DisplayName("recordSplit: verify throws exception when position does not exist")
    void recordSplitNoPositionFailure() {
      RecordSplitCommand command = new RecordSplitCommand(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, "TSLA",
          new Ratio(2, 1), NOW, "Split");

      when(account.hasPosition(any())).thenReturn(false);

      assertThatThrownBy(() -> service.recordSplit(command)).isInstanceOf(
          InsufficientQuantityException.class);
    }

    @Test
    @DisplayName("recordReturnOfCapital: verify success flow")
    void recordReturnOfCapitalSuccess() {
      RecordReturnOfCaptialCommand command = new RecordReturnOfCaptialCommand(PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, "ABC", Price.of("100.0", USD), Quantity.of(0.5), NOW, "ROC");

      service.recordReturnOfCapital(command);

      verify(transactionRecordingService).recordReturnOfCapital(any(), any(AssetSymbol.class),
          eq(Quantity.of(0.5)), eq(Price.of("100.0", USD)), anyString(), any());
    }

    @Test
    @DisplayName("recordTransferIn: verify success flow")
    void recordTransferInSuccess() {
      RecordTransferInCommand command = new RecordTransferInCommand(PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, AMOUNT, List.of(), NOW, "Transfer In");

      service.recordTransferIn(command);

      verify(transactionRecordingService).recordTransferIn(any(), eq(command.amount()),
          eq(command.notes()), eq(command.transactionDate()));
    }

    @Test
    @DisplayName("recordTransferOut: verify success flow")
    void recordTransferOutSuccess() {
      RecordTransferOutCommand command = new RecordTransferOutCommand(PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, usd(500), NOW, "Transfer Out");

      service.recordTransferOut(command);

      verify(transactionRecordingService).recordTransferOut(any(), eq(command.amount()),
          eq(command.notes()), eq(command.transactionDate()));
    }
  }

  @Nested
  @DisplayName("Exclusion and Restoration")
  class ExclusionTests {
    private TransactionId transactionId;

    @BeforeEach
    void setUp() {
      transactionId = TransactionId.newId();
    }

    @Test
    @DisplayName("restoreTransaction: verify success flow and event publication")
    void restoreTransactionSuccess() {
      RestoreTransactionCommand command = new RestoreTransactionCommand(PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, transactionId);

      Transaction existing = mock(Transaction.class);
      Transaction restored = mock(Transaction.class);
      TradeExecution execution = mock(TradeExecution.class);
      TransactionView transactionView = mock(TransactionView.class);
      when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(eq(transactionId),
          eq(PORTFOLIO_ID), eq(USER_ID), eq(ACCOUNT_ID))).thenReturn(Optional.of(existing));

      when(existing.isExcluded()).thenReturn(true);
      when(existing.restore()).thenReturn(restored);

      when(existing.transactionType()).thenReturn(TransactionType.BUY);
      when(existing.execution()).thenReturn(execution);
      when(transactionViewMapper.toTransactionView(any())).thenReturn(transactionView);

      TransactionView result = service.restoreTransaction(command);

      verify(transactionRepository).save(restored, PORTFOLIO_ID);
      verify(eventPublisher).publishEvent(any(PositionRecalculationRequestedEvent.class));
      verify(transactionViewMapper).toTransactionView(restored);
      assertNotNull(result);
    }

    @Test
    @DisplayName("restoreTransaction: throw exception when transaction is not excluded")
    void restoreTransactionFailureNotExcluded() {
      // Arrange
      RestoreTransactionCommand command = new RestoreTransactionCommand(PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, transactionId);

      Transaction existing = mock(Transaction.class);
      when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(),
          any())).thenReturn(Optional.of(existing));

      // If it's already active (not excluded), the method should throw an error
      when(existing.isExcluded()).thenReturn(false);

      // Act & Assert
      assertThrows(InvalidTransactionException.class, () -> {
        service.restoreTransaction(command);
      });

      verify(transactionRepository, never()).save(any(), any());
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("excludeTransaction: does not publish event when transaction does not affect holdings")
    void excludeTransactionNoEventWhenNotAffectingHoldings() {
      TransactionId TX_ID = TransactionId.newId();
      ExcludeTransactionCommand command = new ExcludeTransactionCommand(PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, TX_ID, "Reason");

      TransactionType nonHoldingType = mock(TransactionType.class);
      when(nonHoldingType.affectsHoldings()).thenReturn(false);

      Transaction existingTx = mock(Transaction.class);
      when(existingTx.isExcluded()).thenReturn(false);
      when(existingTx.transactionType()).thenReturn(nonHoldingType);
      when(existingTx.markAsExcluded(any(), any())).thenReturn(existingTx);

      when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(),
          any())).thenReturn(Optional.of(existingTx));

      service.excludeTransaction(command);

      verify(eventPublisher, never()).publishEvent(any(PositionRecalculationRequestedEvent.class));
    }

    @Test
    @DisplayName("excludeTransaction: does not publish event when execution is null")
    void excludeTransaction_NoEvent_WhenExecutionIsNull() {
      TransactionId TX_ID = TransactionId.newId();
      ExcludeTransactionCommand command = new ExcludeTransactionCommand(PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, TX_ID, "Reason");

      TransactionType holdingType = mock(TransactionType.class);
      when(holdingType.affectsHoldings()).thenReturn(true);

      Transaction existingTx = mock(Transaction.class);
      when(existingTx.isExcluded()).thenReturn(false);
      when(existingTx.transactionType()).thenReturn(holdingType);
      when(existingTx.execution()).thenReturn(null);
      when(existingTx.markAsExcluded(any(), any())).thenReturn(existingTx);
      when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(),
          any())).thenReturn(Optional.of(existingTx));

      service.excludeTransaction(command);

      verify(eventPublisher, never()).publishEvent(any(PositionRecalculationRequestedEvent.class));
    }

    @Test
    @DisplayName("excludeTransaction: throw exception when transaction not found")
    void excludeTransactionThrowsWhenNotFound() {
      ExcludeTransactionCommand cmd = new ExcludeTransactionCommand(PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, transactionId, "reason");

      when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(),
          any())).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.excludeTransaction(cmd)).isInstanceOf(
          TransactionNotFoundException.class);
    }

    @Test
    @DisplayName("excludeTransaction: throw exception when already excluded")
    void excludeTransactionThrowsWhenAlreadyExcluded() {
      ExcludeTransactionCommand cmd = new ExcludeTransactionCommand(PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, transactionId, "reason");
      when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(),
          any())).thenReturn(Optional.of(transaction));
      when(transaction.isExcluded()).thenReturn(true);

      assertThatThrownBy(() -> service.excludeTransaction(cmd)).isInstanceOf(
          InvalidTransactionException.class);
    }

    @Test
    @DisplayName("excludeTransaction: publish event when holdings are affected")
    void excludeTransactionPublishesEvent() {
      ExcludeTransactionCommand cmd = new ExcludeTransactionCommand(PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, transactionId, "reason");
      TradeExecution execution = mock(TradeExecution.class);

      when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(),
          any())).thenReturn(Optional.of(transaction));
      when(transaction.isExcluded()).thenReturn(false);
      when(transaction.markAsExcluded(any(), any())).thenReturn(transaction);
      when(transaction.transactionType()).thenReturn(TransactionType.BUY); // affects holdings
      when(transaction.execution()).thenReturn(execution);

      service.excludeTransaction(cmd);

      verify(eventPublisher).publishEvent(any(PositionRecalculationRequestedEvent.class));
    }
  }
}