package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
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
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ExchangeRate;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee.FeeMetadata;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Ratio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TransactionMetadata;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.MarketAssetInfoRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.TransactionRecordingService;
import com.laderrco.fortunelink.portfolio.infrastructure.config.cachedidempotency.IdempotencyCache;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions.ExchangeRateUnavailableException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {


  private static final UserId USER_ID = UserId.random();
  private static final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private static final AccountId ACCOUNT_ID = AccountId.newId();
  private static final String SYMBOL_STR = "AAPL";
  private static final AssetType ASSET_TYPE = AssetType.STOCK;
  private static final String NOTES = "Test Note";
  private static final Instant NOW = Instant.now();
  private static final Currency USD = Currency.of("USD");
  private static final Currency CAD = Currency.of("CAD");
  private static final Money AMOUNT = new Money(new BigDecimal("100.00"), USD);
  private static final UUID IDEMPOTENCY_KEY = UUID.randomUUID();
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
  private MarketAssetInfoRepository infoRepository;
  @Mock
  private ExchangeRateService exchangeRateService;
  @Mock
  private TransactionRecordingService transactionRecordingService;
  @Mock
  private CacheManager cacheManager;
  @Mock
  private IdempotencyCache idempotencyCache;

  @Mock
  private Appender<ILoggingEvent> mockAppender;
  @Captor
  private ArgumentCaptor<ILoggingEvent> logCaptor;

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

    Logger logger = (Logger) LoggerFactory.getLogger(TransactionService.class);
    logger.addAppender(mockAppender);

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

  private RecordPurchaseCommand createPurchaseCommand() {
    return new RecordPurchaseCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID, USER_ID, ACCOUNT_ID, SYMBOL_STR,
        ASSET_TYPE, Quantity.of(10), new Price(AMOUNT), List.of(), NOW, NOTES, false);
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

      AssetSymbol symbol = new AssetSymbol("AAPL");
      MarketAssetInfo info = new MarketAssetInfo(symbol, NOTES, ASSET_TYPE, NOTES, USD, SYMBOL_STR,
          NOTES);
      when(infoRepository.findBySymbol(any())).thenReturn(Optional.of(info));
      when(transactionRecordingService.recordBuy(any(), any(), any(), any(), any(), any(), any(),
          any(), anyBoolean())).thenReturn(transaction);
      when(transaction.transactionType()).thenReturn(TransactionType.BUY);
      when(transactionViewMapper.toTransactionView(transaction)).thenReturn(transactionView);

      TransactionView result = service.recordPurchase(command);

      assertThat(result).isEqualTo(transactionView);
      verify(portfolioRepository).save(portfolio);
      verify(transactionRepository).save(transaction, portfolio.getPortfolioId(), IDEMPOTENCY_KEY);
    }

    @Test
    @DisplayName("recordPurchase: success when asset exists, converts price")
    void recordPurchaseSuccessConvertsPrice() {
      Currency CAD = Currency.CAD;
      AssetSymbol symbol = new AssetSymbol("SHOP.TO");
      Money AMOUNT = new Money(new BigDecimal("100.00"), CAD);
      Price shopPriceToUsd = new Price(Money.of(75, USD));
      RecordPurchaseCommand command = new RecordPurchaseCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID,
          USER_ID, ACCOUNT_ID, symbol.symbol(), ASSET_TYPE, Quantity.of(10), new Price(AMOUNT),
          List.of(), NOW, NOTES, false);

      when(transactionRecordingService.recordBuy(any(), any(), any(), any(), any(), any(), any(),
          any(), anyBoolean())).thenReturn(transaction);
      when(exchangeRateService.convert(any(), any())).thenReturn(shopPriceToUsd.pricePerUnit());
      when(transactionViewMapper.toTransactionView(transaction)).thenReturn(transactionView);

      TransactionView result = service.recordPurchase(command);

      assertThat(result).isEqualTo(transactionView);
      assertThat(command.totalFees(CAD)).isEqualTo(Money.zero(CAD));
      verify(portfolioRepository).save(portfolio);
      verify(transactionRepository).save(transaction, portfolio.getPortfolioId(), IDEMPOTENCY_KEY);
      verify(exchangeRateService).convert(AMOUNT, USD);
    }

    @Test
    @DisplayName("recordPurchase: trigger evictBuyFeeCache success")
    void recordPurchaseEvictsFeeCache() {
      AssetSymbol symbol = new AssetSymbol("AAPL");
      Fee fee = Fee.of(FeeType.ACCOUNT_MAINTENANCE, Money.of(5, USD), NOW);
      MarketAssetInfo info = new MarketAssetInfo(symbol, NOTES, ASSET_TYPE, NOTES, USD, SYMBOL_STR,
          NOTES);

      when(infoRepository.findBySymbol(any())).thenReturn(Optional.of(info));
      when(idempotencyCache.get(anyString())).thenReturn(null);
      when(transactionRepository.findByIdempotencyKeyAndPortfolioId(any(), any())).thenReturn(
          Optional.empty());
      when(transactionRecordingService.recordBuy(any(), any(), any(), any(), any(), any(), any(),
          any(), anyBoolean())).thenReturn(transaction);
      when(transaction.transactionType()).thenReturn(TransactionType.BUY);
      when(transaction.fees()).thenReturn(List.of(fee));
      when(transaction.accountId()).thenReturn(ACCOUNT_ID);
      when(cacheManager.getCache(any())).thenReturn(mock(Cache.class));

      RecordPurchaseCommand command = new RecordPurchaseCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID,
          USER_ID, ACCOUNT_ID, symbol.symbol(), ASSET_TYPE, Quantity.of(10), new Price(AMOUNT),
          List.of(), NOW, NOTES, false);

      service.recordPurchase(command);
      verify(cacheManager).getCache(anyString());
    }

    @ParameterizedTest
    @EnumSource(value = AssetType.class, names = {"CASH", "OTHER"})
    @NullSource
    @DisplayName("recordPurchase: should sanitize invalid hints to STOCK when asset not found")
    void recordPurchaseSanitizeType(AssetType hint) {
      AssetSymbol symbol = new AssetSymbol("AAPL");

      when(infoRepository.findBySymbol(any())).thenReturn(Optional.empty());
      when(transactionRecordingService.recordBuy(any(), any(), any(), any(), any(), any(), any(),
          any(), anyBoolean())).thenReturn(transaction);
      when(transaction.transactionType()).thenReturn(TransactionType.BUY);

      RecordPurchaseCommand command = new RecordPurchaseCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID,
          USER_ID, ACCOUNT_ID, symbol.symbol(), hint, Quantity.of(10), new Price(AMOUNT), List.of(),
          NOW, NOTES, false);

      service.recordPurchase(command);

      verify(transactionRecordingService).recordBuy(any(), eq(symbol), eq(AssetType.STOCK), any(),
          any(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("recordSale: success when asset exists")
    void recordSaleSuccess() {
      RecordSaleCommand command = new RecordSaleCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, SYMBOL_STR, Quantity.of(0), new Price(AMOUNT), List.of(), NOW, NOTES);

      when(account.hasPosition(any())).thenReturn(true);
      when(transactionRecordingService.recordSell(any(), any(), any(), any(), any(), any(),
          any())).thenReturn(transaction);

      when(transactionViewMapper.toTransactionView(transaction)).thenReturn(transactionView);
      TransactionView result = service.recordSale(command);

      assertThat(result).isEqualTo(transactionView);
      assertThat(command.totalFees(USD)).isEqualTo(Money.zero(USD));
      verify(portfolioRepository).save(portfolio);
      verify(transactionRepository).save(transaction, portfolio.getPortfolioId(), IDEMPOTENCY_KEY);
    }

    @Test
    @DisplayName("recordSale: throw InsufficientQuantityException when no position exists")
    void recordSaleThrowsWhenNoPosition() {
      RecordSaleCommand command = new RecordSaleCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, SYMBOL_STR, Quantity.of(0), new Price(AMOUNT), List.of(), NOW, NOTES);
      when(account.hasPosition(any())).thenReturn(false);

      assertThatThrownBy(() -> service.recordSale(command)).isInstanceOf(
          InsufficientQuantityException.class);
    }

    @Test
    @DisplayName("recordDividendReinvestment: verify success flow")
    void recordDividendReinvestmentSuccess() {
      DripExecution exec = new DripExecution(Quantity.of(10.0), Price.of("150", USD));
      RecordDividendReinvestmentCommand command = new RecordDividendReinvestmentCommand(
          IDEMPOTENCY_KEY, PORTFOLIO_ID, USER_ID, ACCOUNT_ID, "GOOGL", exec, NOW, "Reinvest");

      when(transactionRecordingService.recordDividendReinvestment(any(), any(), any(), any(), any(),
          any())).thenReturn(transaction);
      when(transaction.transactionType()).thenReturn(TransactionType.DIVIDEND_REINVEST);
      service.recordDividendReinvestment(command);

      verify(transactionRecordingService).recordDividendReinvestment(any(), any(AssetSymbol.class),
          eq(Quantity.of(10.0)), eq(Price.of("150", USD)), anyString(), any());
      assertThat(command.execution().totalCost()).isEqualTo(Money.of(1500, USD));
    }

    @Test
    @DisplayName("recordDividend: logs warning when a reinvestment already exists within 24h")
    void recordDividendWarnsOnExistingReinvestment() {
      RecordDividendCommand command = new RecordDividendCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID,
          USER_ID, ACCOUNT_ID, "AAPL", usd(10), NOW, "Interest");
      AssetSymbol symbol = new AssetSymbol(command.assetSymbol());

      Instant expectedStart = command.transactionDate().minus(24, ChronoUnit.HOURS);
      Instant expectedEnd = command.transactionDate().plus(24, ChronoUnit.HOURS);
      Transaction dummyTx = Transaction.builder().transactionId(TransactionId.newId())
          .accountId(ACCOUNT_ID).transactionType(TransactionType.DIVIDEND).cashDelta(AMOUNT)
          .fees(List.of()).notes(NOTES).occurredAt(NOW)
          .metadata(TransactionMetadata.manual(AssetType.BOND)).build();

      when(
          transactionRecordingService.recordDividend(any(), any(), any(), any(), any())).thenReturn(
          dummyTx);
      when(transactionRepository.existsConflict(eq(command.accountId()),
          eq(TransactionType.DIVIDEND_REINVEST), eq(symbol), eq(expectedStart),
          eq(expectedEnd))).thenReturn(true);

      service.recordDividend(command);

      verify(mockAppender, times(2)).doAppend(logCaptor.capture());
      List<ILoggingEvent> capturedLogs = logCaptor.getAllValues();

      assertThat(capturedLogs.getFirst().getLevel()).isEqualTo(Level.WARN);
      assertThat(capturedLogs.getFirst().getFormattedMessage()).contains("DRIP recorded for symbol=AssetSymbol[symbol=AAPL]");
    }
  }

  @Nested
  @DisplayName("Cash Flow & Distributions")
  class CashFlowTests {
    @Test
    @DisplayName("recordDeposit: verify success flow")
    void recordDepositSuccess() {
      RecordDepositCommand cmd = new RecordDepositCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, AMOUNT, NOW, NOTES);
      when(transactionRecordingService.recordDeposit(eq(account), eq(AMOUNT), eq(NOTES),
          eq(NOW))).thenReturn(transaction);

      service.recordDeposit(cmd);

      verify(transactionRecordingService).recordDeposit(any(), any(), any(), any());
      verify(transactionRepository).save(transaction, portfolio.getPortfolioId(), IDEMPOTENCY_KEY);
    }

    @Test
    @DisplayName("recordWithdrawal: verify success flow")
    void recordWithdrawalSuccess() {
      RecordWithdrawalCommand command = new RecordWithdrawalCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID,
          USER_ID, ACCOUNT_ID, AMOUNT, NOW, NOTES);

      Transaction dummyTx = Transaction.builder().transactionId(TransactionId.newId())
          .accountId(ACCOUNT_ID).transactionType(TransactionType.WITHDRAWAL).cashDelta(AMOUNT)
          .fees(List.of()).notes(NOTES).occurredAt(NOW)
          .metadata(TransactionMetadata.manual(AssetType.BOND)).build();

      when(transactionRecordingService.recordWithdrawal(any(), any(), any(), any())).thenReturn(
          dummyTx);

      service.recordWithdrawal(command);

      verify(transactionRecordingService).recordWithdrawal(any(), eq(command.amount()),
          eq(command.notes()), eq(command.transactionDate()));
    }

    @Test
    @DisplayName("recordFee: verify success flow")
    void recordFeeSuccess() {
      RecordFeeCommand command = new RecordFeeCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, AMOUNT, FeeType.ACCOUNT_MAINTENANCE, NOW, NOTES);

      Transaction dummyTx = Transaction.builder().transactionId(TransactionId.newId())
          .accountId(ACCOUNT_ID).transactionType(TransactionType.FEE).cashDelta(AMOUNT)
          .fees(List.of()).notes(NOTES).occurredAt(NOW)
          .metadata(TransactionMetadata.manual(AssetType.BOND)).build();

      when(transactionRecordingService.recordFee(any(), any(), any(), any(), any())).thenReturn(
          dummyTx);
      service.recordFee(command);

      verify(transactionRecordingService).recordFee(any(), eq(command.amount()), any(),
          eq(command.notes()), eq(command.transactionDate()));
    }

    @Test
    @DisplayName("recordInterest: verify success flow with no symbol")
    void recordInterestSuccessCashInterest() {
      RecordInterestCommand command = RecordInterestCommand.cashInterest(IDEMPOTENCY_KEY,
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID, usd(5), NOW, "Interest");
      RecordInterestCommand command2 = new RecordInterestCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID,
          USER_ID, ACCOUNT_ID, "", usd(5), NOW, "INTEREST");

      Transaction dummyTx = Transaction.builder().transactionId(TransactionId.newId())
          .accountId(ACCOUNT_ID).transactionType(TransactionType.INTEREST).cashDelta(AMOUNT)
          .fees(List.of()).notes(NOTES).occurredAt(NOW)
          .metadata(TransactionMetadata.manual(AssetType.BOND)).build();

      when(
          transactionRecordingService.recordInterest(any(), any(), any(), any(), any())).thenReturn(
          dummyTx);

      service.recordInterest(command);

      verify(transactionRecordingService).recordInterest(any(), any(), eq(command.amount()),
          eq(command.notes()), eq(command.transactionDate()));
      assertThat(command.isAssetInterest()).isFalse();
      assertThat(command2.isAssetInterest()).isFalse();
    }

    @Test
    @DisplayName("recordInterest: verify success flow with symbol")
    void recordInterestSuccessAssetInterest() {
      RecordInterestCommand command = RecordInterestCommand.assetInterest(IDEMPOTENCY_KEY,
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID, "CAD.3TBILL", usd(15), NOW, "3 month GIC");

      Transaction dummyTx = Transaction.builder().transactionId(TransactionId.newId())
          .accountId(ACCOUNT_ID).transactionType(TransactionType.INTEREST).cashDelta(AMOUNT)
          .fees(List.of()).notes(NOTES).occurredAt(NOW)
          .metadata(TransactionMetadata.manual(AssetType.BOND)).build();

      when(
          transactionRecordingService.recordInterest(any(), any(), any(), any(), any())).thenReturn(
          dummyTx);

      service.recordInterest(command);

      verify(transactionRecordingService).recordInterest(any(), any(AssetSymbol.class),
          eq(command.amount()), eq(command.notes()), eq(command.transactionDate()));
      assertThat(command.isAssetInterest()).isTrue();
    }

    @Test
    @DisplayName("recordDividend: verify success flow")
    void recordDividendSuccess() {
      RecordDividendCommand command = new RecordDividendCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID,
          USER_ID, ACCOUNT_ID, "AAPL", usd(10), NOW, "Interest");
      Transaction dummyTx = Transaction.builder().transactionId(TransactionId.newId())
          .accountId(ACCOUNT_ID).transactionType(TransactionType.DIVIDEND).cashDelta(AMOUNT)
          .fees(List.of()).notes(NOTES).occurredAt(NOW)
          .metadata(TransactionMetadata.manual(AssetType.BOND)).build();

      when(
          transactionRecordingService.recordDividend(any(), any(), any(), any(), any())).thenReturn(
          dummyTx);

      service.recordDividend(command);

      verify(transactionRecordingService).recordDividend(any(), any(AssetSymbol.class),
          eq(command.amount()), eq(command.notes()), eq(command.transactionDate()));
    }

    @Test
    @DisplayName("recordDividendReinvestment: logs warning when a dividend already exists within 24h")
    void recordReinvestmentWarnsOnExistingDividend() {
      DripExecution exec = new DripExecution(Quantity.of(10.0), Price.of("150", USD));
      RecordDividendReinvestmentCommand command = new RecordDividendReinvestmentCommand(
          IDEMPOTENCY_KEY, PORTFOLIO_ID, USER_ID, ACCOUNT_ID, "GOOGL", exec, NOW, "Reinvest");
      AssetSymbol symbol = new AssetSymbol(command.assetSymbol());

      Instant expectedStart = command.transactionDate().minus(24, ChronoUnit.HOURS);
      Instant expectedEnd = command.transactionDate().plus(24, ChronoUnit.HOURS);
      when(transactionRecordingService.recordDividendReinvestment(any(), any(), any(), any(), any(),
          any())).thenReturn(transaction);
      when(transactionRepository.existsConflict(eq(command.accountId()),
          eq(TransactionType.DIVIDEND), eq(symbol), eq(expectedStart), eq(expectedEnd))).thenReturn(
          true);

      service.recordDividendReinvestment(command);

      verify(mockAppender, times(2)).doAppend(logCaptor.capture());
      List<ILoggingEvent> capturedLogs = logCaptor.getAllValues();
      assertThat(capturedLogs.getFirst().getLevel()).isEqualTo(Level.WARN);
      assertThat(capturedLogs.getFirst().getFormattedMessage()).contains(
          "DRIP recorded for symbol=AssetSymbol[symbol=GOOGL] on");
      assertThat(capturedLogs.get(1).getLevel()).isEqualTo(Level.INFO);
    }
  }

  @Nested
  @DisplayName("Management")
  public class ManagementTests {
    @Test
    @DisplayName("recordSplit: passes and splits data")
    void recordSplitNoPositionSuccess() {
      RecordSplitCommand command = new RecordSplitCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, "TSLA", new Ratio(2, 1), NOW, "Split");

      when(transactionRecordingService.recordSplit(any(), any(), any(), any(), any())).thenReturn(
          transaction);
      when(transaction.transactionType()).thenReturn(TransactionType.SPLIT);
      when(account.hasPosition(any())).thenReturn(true);

      service.recordSplit(command);
      verify(transactionRecordingService).recordSplit(any(), any(AssetSymbol.class),
          eq(new Ratio(2, 1)), anyString(), any());

    }

    @Test
    @DisplayName("recordSplit: verify throws exception when position does not exist")
    void recordSplitNoPositionFailure() {
      RecordSplitCommand command = new RecordSplitCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, "TSLA", new Ratio(2, 1), NOW, "Split");

      when(account.hasPosition(any())).thenReturn(false);

      assertThatThrownBy(() -> service.recordSplit(command)).isInstanceOf(
          InsufficientQuantityException.class);
    }

    @Test
    @DisplayName("recordReturnOfCapital: verify success flow")
    void recordReturnOfCapitalSuccess() {
      RecordReturnOfCaptialCommand command = new RecordReturnOfCaptialCommand(IDEMPOTENCY_KEY,
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID, "ABC", Price.of("100.0", USD), Quantity.of(0.5), NOW,
          "ROC");

      when(transactionRecordingService.recordReturnOfCapital(any(), any(), any(), any(), any(),
          any())).thenReturn(transaction);
      when(transaction.transactionType()).thenReturn(TransactionType.RETURN_OF_CAPITAL);

      service.recordReturnOfCapital(command);

      verify(transactionRecordingService).recordReturnOfCapital(any(), any(AssetSymbol.class),
          eq(Quantity.of(0.5)), eq(Price.of("100.0", USD)), anyString(), any());
    }

    @Test
    @DisplayName("recordTransferIn: verify success flow")
    void recordTransferInSuccess() {
      RecordTransferInCommand command = new RecordTransferInCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID,
          USER_ID, ACCOUNT_ID, AMOUNT, NOW, "Transfer In");

      when(transactionRecordingService.recordTransferIn(any(), any(), any(), any())).thenReturn(
          transaction);
      when(transaction.transactionType()).thenReturn(TransactionType.TRANSFER_IN);

      service.recordTransferIn(command);

      verify(transactionRecordingService).recordTransferIn(any(), eq(command.amount()),
          eq(command.notes()), eq(command.transactionDate()));
    }

    @Test
    @DisplayName("recordTransferOut: verify success flow")
    void recordTransferOutSuccess() {
      RecordTransferOutCommand command = new RecordTransferOutCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID,
          USER_ID, ACCOUNT_ID, usd(500), NOW, "Transfer Out");

      when(transactionRecordingService.recordTransferOut(any(), any(), any(), any())).thenReturn(
          transaction);
      when(transaction.transactionType()).thenReturn(TransactionType.TRANSFER_OUT);

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
      RestoreTransactionCommand command = new RestoreTransactionCommand(IDEMPOTENCY_KEY,
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID, transactionId);

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

      verify(transactionRepository).save(restored, PORTFOLIO_ID, IDEMPOTENCY_KEY);
      verify(eventPublisher).publishEvent(any(PositionRecalculationRequestedEvent.class));
      verify(transactionViewMapper).toTransactionView(restored);
      assertNotNull(result);
    }

    @Test
    @DisplayName("restoreTransaction: verify success flow and event publication, branch")
    void restoreTransactionSuccessNotBuyTransaction() {
      RestoreTransactionCommand command = new RestoreTransactionCommand(IDEMPOTENCY_KEY,
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID, transactionId);

      Transaction existing = mock(Transaction.class);
      Transaction restored = mock(Transaction.class);
      TransactionView transactionView = mock(TransactionView.class);
      when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(eq(transactionId),
          eq(PORTFOLIO_ID), eq(USER_ID), eq(ACCOUNT_ID))).thenReturn(Optional.of(existing));

      when(existing.isExcluded()).thenReturn(true);
      when(existing.restore()).thenReturn(restored);

      when(existing.transactionType()).thenReturn(TransactionType.DEPOSIT);
      when(transactionViewMapper.toTransactionView(any())).thenReturn(transactionView);

      TransactionView result = service.restoreTransaction(command);

      verify(transactionRepository).save(restored, PORTFOLIO_ID, IDEMPOTENCY_KEY);
      verify(transactionViewMapper).toTransactionView(restored);
      assertNotNull(result);
    }

    @Test
    @DisplayName("restoreTransaction: throw exception when transaction is not excluded")
    void restoreTransactionFailureNotExcluded() {

      RestoreTransactionCommand command = new RestoreTransactionCommand(IDEMPOTENCY_KEY,
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID, transactionId);

      Transaction existing = mock(Transaction.class);
      when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(),
          any())).thenReturn(Optional.of(existing));

      when(existing.isExcluded()).thenReturn(false);

      assertThrows(InvalidTransactionException.class, () -> {
        service.restoreTransaction(command);
      });

      verify(transactionRepository, never()).save(any(), any(), any());
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("excludeTransaction: does not publish event when transaction does not affect holdings")
    void excludeTransactionNoEventWhenNotAffectingHoldings() {
      TransactionId TX_ID = TransactionId.newId();
      ExcludeTransactionCommand command = new ExcludeTransactionCommand(IDEMPOTENCY_KEY,
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID, TX_ID, "Reason");

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
      ExcludeTransactionCommand command = new ExcludeTransactionCommand(IDEMPOTENCY_KEY,
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID, TX_ID, "Reason");

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
      ExcludeTransactionCommand cmd = new ExcludeTransactionCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID,
          USER_ID, ACCOUNT_ID, transactionId, "reason");

      when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(),
          any())).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.excludeTransaction(cmd)).isInstanceOf(
          TransactionNotFoundException.class);
    }

    @Test
    @DisplayName("excludeTransaction: throw exception when already excluded")
    void excludeTransactionThrowsWhenAlreadyExcluded() {
      ExcludeTransactionCommand cmd = new ExcludeTransactionCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID,
          USER_ID, ACCOUNT_ID, transactionId, "reason");
      when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(),
          any())).thenReturn(Optional.of(transaction));
      when(transaction.isExcluded()).thenReturn(true);

      assertThatThrownBy(() -> service.excludeTransaction(cmd)).isInstanceOf(
          InvalidTransactionException.class);
    }

    @Test
    @DisplayName("excludeTransaction: publish event when holdings are affected")
    void excludeTransactionPublishesEvent() {
      ExcludeTransactionCommand cmd = new ExcludeTransactionCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID,
          USER_ID, ACCOUNT_ID, transactionId, "reason");
      TradeExecution execution = mock(TradeExecution.class);

      when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(),
          any())).thenReturn(Optional.of(transaction));
      when(transaction.isExcluded()).thenReturn(false);
      when(transaction.markAsExcluded(any(), any())).thenReturn(transaction);
      when(transaction.transactionType()).thenReturn(TransactionType.BUY);
      when(transaction.execution()).thenReturn(execution);

      service.excludeTransaction(cmd);

      verify(eventPublisher).publishEvent(any(PositionRecalculationRequestedEvent.class));
    }
  }

  @Nested
  @DisplayName("Execute test idempotent")
  public class IdempotencyTest {
    private final TransactionId TRANSACTION_ID = TransactionId.newId();

    @Test
    @DisplayName("execute: returns existing transaction when idempotency key is a replay")
    void executeIdempotentReplay() {
      RecordPurchaseCommand command = createPurchaseCommand();
      UUID key = command.idempotencyKey();

      when(transactionRepository.findByIdempotencyKeyAndPortfolioId(key,
          command.portfolioId())).thenReturn(Optional.of(transaction));
      when(transactionViewMapper.toTransactionView(transaction)).thenReturn(transactionView);

      TransactionView result = service.recordPurchase(command);

      assertThat(result).isEqualTo(transactionView);
      verify(transactionRecordingService, never()).recordBuy(any(), any(), any(), any(), any(),
          any(), any(), any(), anyBoolean());
      verify(portfolioRepository, never()).save(any());
    }

    @Test
    @DisplayName("execute: records and persists when idempotency key is new")
    void executeInitialRequest() {

      RecordPurchaseCommand command = createPurchaseCommand();
      UUID key = command.idempotencyKey();

      when(transactionRepository.findByIdempotencyKeyAndPortfolioId(eq(key),
          eq(command.portfolioId()))).thenReturn(Optional.empty());

      MarketAssetInfo info = new MarketAssetInfo(new AssetSymbol("AAPL"), NOTES, ASSET_TYPE, NOTES,
          USD, SYMBOL_STR, NOTES);
      when(infoRepository.findBySymbol(any())).thenReturn(Optional.of(info));
      when(transactionRecordingService.recordBuy(any(), any(), any(), any(), any(), any(), any(),
          any(), anyBoolean())).thenReturn(transaction);
      when(transactionViewMapper.toTransactionView(transaction)).thenReturn(transactionView);

      TransactionView result = service.recordPurchase(command);

      assertThat(result).isEqualTo(transactionView);
      verify(transactionRepository).findByIdempotencyKeyAndPortfolioId(key, command.portfolioId());
      verify(transactionRecordingService).recordBuy(any(), any(), any(), any(), any(), any(), any(),
          any(), anyBoolean());
      verify(portfolioRepository).save(portfolio);
    }

    @Test
    @DisplayName("execute: proceeds normally when idempotency key is null")
    void executeNullKey() {
      RecordPurchaseCommand command = new RecordPurchaseCommand(null, PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, SYMBOL_STR, ASSET_TYPE, Quantity.of(10), new Price(AMOUNT), List.of(), NOW,
          NOTES, false);

      when(infoRepository.findBySymbol(any())).thenReturn(Optional.of(mock(MarketAssetInfo.class)));
      when(transactionRecordingService.recordBuy(any(), any(), any(), any(), any(), any(), any(),
          any(), anyBoolean())).thenReturn(transaction);

      service.recordPurchase(command);

      verify(transactionRepository, never()).findByIdempotencyKey(null);
      verify(portfolioRepository).save(any());
    }

    @Test
    @DisplayName("executeWithIdempotency: returns cached view immediately if present")
    void shouldReturnCachedViewOnHit() {

      UUID key = UUID.randomUUID();
      RecordPurchaseCommand command = new RecordPurchaseCommand(key, PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, SYMBOL_STR, ASSET_TYPE, Quantity.of(10), new Price(AMOUNT), List.of(), NOW,
          NOTES, false);
      TransactionView mockView = mock(TransactionView.class);

      when(idempotencyCache.get(key.toString())).thenReturn(mockView);

      TransactionView result = service.recordPurchase(command);

      assertThat(result).isEqualTo(mockView);

      verify(transactionRepository, never()).findByIdempotencyKeyAndPortfolioId(any(), any());
    }

    @Test
    @DisplayName("executeWithIdempotency: recovers from DB if cache is empty but key exists")
    void shouldRecoverFromDbOnCacheMiss() {

      UUID key = UUID.randomUUID();
      RecordPurchaseCommand command = new RecordPurchaseCommand(key, PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, SYMBOL_STR, ASSET_TYPE, Quantity.of(10), new Price(AMOUNT), List.of(), NOW,
          NOTES, false);
      Transaction existingTx = mock(Transaction.class);
      TransactionView mappedView = mock(TransactionView.class);

      when(idempotencyCache.get(key.toString())).thenReturn(null);
      when(transactionRepository.findByIdempotencyKeyAndPortfolioId(key, PORTFOLIO_ID)).thenReturn(
          Optional.of(existingTx));
      when(transactionViewMapper.toTransactionView(existingTx)).thenReturn(mappedView);

      TransactionView result = service.recordPurchase(command);

      assertThat(result).isEqualTo(mappedView);
      verify(idempotencyCache).put(key.toString(), mappedView);
    }

    @Test
    @DisplayName("executeWithIdempotency: recovers when DataIntegrityViolation occurs (Race Condition)")
    void shouldRecoverOnDataIntegrityViolation() {
      UUID key = UUID.randomUUID();

      ExcludeTransactionCommand command = new ExcludeTransactionCommand(key, PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, TRANSACTION_ID, "reason");

      Transaction winningTx = mock(Transaction.class);
      when(transactionRepository.findByIdempotencyKeyAndPortfolioId(key, PORTFOLIO_ID)).thenReturn(
          Optional.empty()).thenReturn(Optional.of(winningTx));

      Transaction existingTx = mock(Transaction.class);
      when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(TRANSACTION_ID,
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID)).thenReturn(Optional.of(existingTx));

      doThrow(new DataIntegrityViolationException("Duplicate key")).when(transactionRepository)
          .save(any(), any(), eq(key));

      TransactionView winningView = mock(TransactionView.class);
      when(transactionViewMapper.toTransactionView(winningTx)).thenReturn(winningView);

      TransactionView result = service.excludeTransaction(command);

      assertThat(result).isEqualTo(winningView);
      verify(transactionRepository, times(2)).findByIdempotencyKeyAndPortfolioId(key, PORTFOLIO_ID);
    }

    @Test
    @DisplayName("executeWithIdempotency: rethrows exception if recovery lookup fails")
    void shouldRethrowIfRecoveryLookupEmpty() {

      UUID key = UUID.randomUUID();
      RecordPurchaseCommand command = new RecordPurchaseCommand(key, PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, SYMBOL_STR, ASSET_TYPE, Quantity.of(10), new Price(AMOUNT), List.of(), NOW,
          NOTES, false);

      when(transactionRepository.findByIdempotencyKeyAndPortfolioId(key, PORTFOLIO_ID)).thenReturn(
          Optional.empty());

      doThrow(new DataIntegrityViolationException("Actual DB Error")).when(transactionRepository)
          .save(any(), any(), any());

      assertThatThrownBy(() -> service.recordPurchase(command)).isInstanceOf(
          DataIntegrityViolationException.class).hasMessageContaining("Actual DB Error");
    }

    @Test
    @DisplayName("executeWithIdempotency: throws original exception if key is null")
    void shouldThrowImmediatelyIfKeyIsNull() {

      ExcludeTransactionCommand command = new ExcludeTransactionCommand(null, PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, TRANSACTION_ID, "reason");

      doThrow(new DataIntegrityViolationException("Unique constraint failed")).when(
              transactionRepository)
          .findByIdAndPortfolioIdAndUserIdAndAccountId(any(), any(), any(), any());

      assertThatThrownBy(() -> service.excludeTransaction(command)).isInstanceOf(
          DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("executeWithIdempotency: rethrows original violation if recovery lookup returns empty")
    void shouldRethrowIfRecordMissingAfterViolation() {
      UUID key = UUID.randomUUID();
      ExcludeTransactionCommand command = new ExcludeTransactionCommand(key, PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, TRANSACTION_ID, "reason");

      when(transactionRepository.findByIdempotencyKeyAndPortfolioId(key, PORTFOLIO_ID)).thenReturn(
          Optional.empty());

      Transaction mockTx = mock(Transaction.class);
      when(mockTx.isExcluded()).thenReturn(false);
      when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(TRANSACTION_ID,
          PORTFOLIO_ID, USER_ID, ACCOUNT_ID)).thenReturn(Optional.of(mockTx));

      doThrow(new DataIntegrityViolationException("Conflict occurred")).when(transactionRepository)
          .save(any(), any(), eq(key));

      assertThatThrownBy(() -> service.excludeTransaction(command)).isInstanceOf(
          DataIntegrityViolationException.class).hasMessageContaining("Conflict occurred");
    }
  }

  @Nested
  @DisplayName("processFees Branch Testing")
  class FeeProcessTests {

    @Test
    @DisplayName("processFees: handles null and empty inputs")
    void shouldHandleNullOrEmptyFees() {

      List<Fee> resultNull = ReflectionTestUtils.invokeMethod(service, "processFees", null, account,
          NOW);
      List<Fee> resultEmpty = ReflectionTestUtils.invokeMethod(service, "processFees", List.of(),
          account, NOW);

      assertThat(resultNull).isEmpty();
      assertThat(resultEmpty).isEmpty();
    }

    @Test
    @DisplayName("processFees: uses identity rate when currencies match")
    void shouldSkipConversionForSameCurrency() {
      when(account.getAccountCurrency()).thenReturn(USD);
      Fee sameCurrencyFee = Fee.of(FeeType.BROKERAGE, Money.of(5, "USD"), NOW,
          new FeeMetadata(Map.of()));

      List<Fee> results = ReflectionTestUtils.invokeMethod(service, "processFees",
          List.of(sameCurrencyFee), account, NOW);

      assertThat(results.get(0).accountAmount()).isEqualTo(sameCurrencyFee.nativeAmount());
      verifyNoInteractions(exchangeRateService);
    }

    @Test
    @DisplayName("processFees: throws Exception when exchange rate is missing")
    void shouldThrowWhenRateUnavailable() {
      when(account.getAccountCurrency()).thenReturn(CAD);
      Fee usdFee = Fee.of(FeeType.BROKERAGE, Money.of(5, "USD"), NOW, new FeeMetadata(Map.of()));

      when(exchangeRateService.getRate(Currency.of("USD"), CAD, NOW)).thenReturn(Optional.empty());

      assertThatThrownBy(
          () -> ReflectionTestUtils.invokeMethod(service, "processFees", List.of(usdFee), account,
              NOW)).isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @Test
    @DisplayName("processFees: successfully converts currency when different from account")
    void shouldConvertCurrencyWhenDifferent() {

      when(account.getAccountCurrency()).thenReturn(CAD);

      Money nativeFee = Money.of(10, "USD");
      Fee fee = Fee.of(FeeType.BROKERAGE, nativeFee, NOW, new FeeMetadata(Map.of()));

      ExchangeRate mockRate = mock(ExchangeRate.class);
      Money convertedMoney = Money.of("13.50", CAD);

      when(exchangeRateService.getRate(Currency.of("USD"), CAD, NOW)).thenReturn(
          Optional.of(mockRate));
      when(mockRate.convert(nativeFee)).thenReturn(convertedMoney);

      List<Fee> results = ReflectionTestUtils.invokeMethod(service, "processFees", List.of(fee),
          account, NOW);

      assertThat(results).hasSize(1);
      assertThat(results.get(0).accountAmount()).isEqualTo(convertedMoney);
      verify(exchangeRateService).getRate(Currency.of("USD"), CAD, NOW);
    }

    @Test
    @DisplayName("processFees: throws ExchangeRateUnavailableException when rate service returns empty")
    void shouldThrowWhenExchangeRateIsMissing() {

      when(account.getAccountCurrency()).thenReturn(CAD);
      Fee usdFee = Fee.of(FeeType.BROKERAGE, Money.of(10, "USD"), NOW, new FeeMetadata(Map.of()));

      when(exchangeRateService.getRate(Currency.of("USD"), CAD, NOW)).thenReturn(Optional.empty());

      assertThatThrownBy(
          () -> ReflectionTestUtils.invokeMethod(service, "processFees", List.of(usdFee), account,
              NOW)).isInstanceOf(ExchangeRateUnavailableException.class).hasMessageContaining("USD")
          .hasMessageContaining("CAD");
    }
  }
}