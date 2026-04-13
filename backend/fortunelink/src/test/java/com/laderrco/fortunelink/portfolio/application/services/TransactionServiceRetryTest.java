package com.laderrco.fortunelink.portfolio.application.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.laderrco.fortunelink.portfolio.application.commands.ExcludeTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio.application.mappers.TransactionViewMapper;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.utils.annotations.IdentifiedTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.utils.annotations.TransactionCommand;
import com.laderrco.fortunelink.portfolio.application.validators.TransactionCommandValidator;
import com.laderrco.fortunelink.portfolio.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.*;
import com.laderrco.fortunelink.portfolio.domain.repositories.MarketAssetInfoRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio.domain.services.TransactionRecordingService;
import com.laderrco.fortunelink.portfolio.infrastructure.config.cachedidempotency.IdempotencyCache;

import java.time.Instant;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = TransactionServiceRetryTest.TestConfig.class)
class TransactionServiceRetryTest {

  @Autowired
  private TransactionService transactionService;

  
  @Autowired
  private TransactionRecordingService transactionRecordingService;

  @Autowired
  private TransactionCommandValidator validator;

  @Autowired
  private PortfolioLoader portfolioLoader;

  @Autowired
  private AccountHealthService accountHealthService;

  @Autowired
  private TransactionRepository transactionRepository;

  @Autowired
  private MarketAssetInfoRepository infoRepository;

  @Test
  @DisplayName("recover(TransactionCommand): marks account stale and throws exception")
  void recoverTransactionCommandDirectly() {
    
    var ex = new ObjectOptimisticLockingFailureException("Portfolio", "123");
    var cmd = mock(TransactionCommand.class);
    var accountId = AccountId.newId();
    when(cmd.accountId()).thenReturn(accountId);

    
    
    
    assertThrows(ConcurrentModificationException.class, () -> transactionService.recover(ex, cmd));

    
    verify(accountHealthService).markStale(accountId);
  }

  @Test
  @DisplayName("recover(IdentifiedTransactionCommand): marks account stale and throws exception")
  void recoverIdentifiedCommandDirectly() {
    
    var ex = new ObjectOptimisticLockingFailureException("Transaction", "456");
    var cmd = mock(IdentifiedTransactionCommand.class);
    var accountId = AccountId.newId();
    when(cmd.accountId()).thenReturn(accountId);

    
    assertThrows(ConcurrentModificationException.class, () -> transactionService.recover(ex, cmd));

    
    verify(accountHealthService).markStale(accountId);
  }

  @Test
  @DisplayName("Should retry excludeTransaction and trigger IdentifiedTransactionCommand recover logic")
  void shouldRetryExcludeAndRecover() {
    ExcludeTransactionCommand command = new ExcludeTransactionCommand(
        UUID.randomUUID(), PortfolioId.newId(), UserId.random(),
        AccountId.newId(), TransactionId.newId(), "Reason");

    when(validator.validate(any(ExcludeTransactionCommand.class))).thenReturn(ValidationResult.success());

    Transaction mockTx = mock(Transaction.class);
    when(mockTx.isExcluded()).thenReturn(false);
    when(mockTx.markAsExcluded(any(), any())).thenReturn(mockTx);

    when(transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(
        command.transactionId(), command.portfolioId(), command.userId(), command.accountId()))
        .thenReturn(Optional.of(mockTx));

    doThrow(new ObjectOptimisticLockingFailureException("Transaction", command.transactionId().toString()))
        .when(transactionRepository).save(any(Transaction.class), eq(command.portfolioId()), any(UUID.class));

    assertThrows(ConcurrentModificationException.class,
        () -> transactionService.excludeTransaction(command));

    verify(transactionRepository, times(3)).save(any(), any(), any());
    verify(accountHealthService).markStale(command.accountId());
    verify(accountHealthService, times(1)).markStale(command.accountId());
  }

  @Test
  @DisplayName("Should retry 3 times and then trigger @Recover logic")
  void shouldRetryThreeTimesAndRecover() {
    
    RecordPurchaseCommand command = createSampleCommand();

    
    when(validator.validate(any(RecordPurchaseCommand.class))).thenReturn(
        ValidationResult.success());

    
    Portfolio portfolio = buildFakePortfolioForPurchase(command);
    when(portfolioLoader.loadUserPortfolio(command.portfolioId(), command.userId())).thenReturn(
        portfolio);

    
    when(infoRepository.findBySymbol(any())).thenReturn(Optional.empty());

    
    
    when(transactionRecordingService.recordBuy(any(), any(), any(), any(), any(), any(), any(),
        any(), anyBoolean())).thenThrow(
            new ObjectOptimisticLockingFailureException("Portfolio", "test-id"));

    
    
    assertThrows(ConcurrentModificationException.class,
        () -> transactionService.recordPurchase(command));

    
    
    verify(transactionRecordingService, times(3)).recordBuy(any(), any(), any(), any(), any(),
        any(), any(), any(), anyBoolean());

    
    verify(accountHealthService).markStale(command.accountId());
    verify(accountHealthService, times(1)).markStale(command.accountId());
  }

  @Test
  @DisplayName("Should retry recordSale and recover when locking fails")
  void shouldRetrySaleAndRecover() {
    
    RecordSaleCommand command = new RecordSaleCommand(UUID.randomUUID(), PortfolioId.newId(),
        UserId.random(), AccountId.newId(), "AAPL", Quantity.of(5),
        Price.of("150.00", Currency.CAD), List.of(), Instant.now(), "Sale Test");

    
    when(validator.validate(any(RecordSaleCommand.class))).thenReturn(ValidationResult.success());

    
    Portfolio portfolio = buildFakePortfolioForSale(command);
    when(portfolioLoader.loadUserPortfolio(any(), any())).thenReturn(portfolio);

    
    when(transactionRecordingService.recordSell(any(), any(), any(), any(), any(), any(),
        any())).thenThrow(new ObjectOptimisticLockingFailureException("Portfolio", "test-id"));

    
    assertThrows(ConcurrentModificationException.class,
        () -> transactionService.recordSale(command));

    
    verify(transactionRecordingService, times(3)).recordSell(any(), any(), any(), any(), any(),
        any(), any());
    verify(accountHealthService, times(1)).markStale(command.accountId());
  }

  

  private RecordPurchaseCommand createSampleCommand() {
    return new RecordPurchaseCommand(UUID.randomUUID(), PortfolioId.newId(), UserId.random(),
        AccountId.newId(), "AAPL", AssetType.STOCK, Quantity.of(10),
        Price.of("150.00", Currency.CAD),
        
        List.of(), Instant.now(), "Test Note", false);
  }

  private Portfolio buildFakePortfolioForPurchase(RecordPurchaseCommand command) {
    Portfolio mockPortfolio = mock(Portfolio.class);
    Account mockAccount = mock(Account.class);

    when(mockPortfolio.getPortfolioId()).thenReturn(command.portfolioId());
    when(mockPortfolio.getUserId()).thenReturn(command.userId());
    when(mockPortfolio.getAccount(command.accountId())).thenReturn(mockAccount);

    
    when(mockAccount.getAccountId()).thenReturn(command.accountId());
    when(mockAccount.getAccountCurrency()).thenReturn(Currency.CAD);
    when(mockAccount.isActive()).thenReturn(true);
    when(mockAccount.hasSufficientCash(any())).thenReturn(true);

    return mockPortfolio;
  }

  private Portfolio buildFakePortfolioForSale(RecordSaleCommand command) {
    Portfolio mockPortfolio = mock(Portfolio.class);
    Account mockAccount = mock(Account.class);

    when(mockPortfolio.getAccount(command.accountId())).thenReturn(mockAccount);
    when(mockAccount.getAccountCurrency()).thenReturn(Currency.CAD);
    when(mockAccount.isActive()).thenReturn(true);

    
    
    when(mockAccount.hasPosition(any(AssetSymbol.class))).thenReturn(true);

    return mockPortfolio;
  }

  

  @Configuration
  @EnableRetry
  static class TestConfig {
    
    @Bean
    public PortfolioRepository portfolioRepository() {
      return mock(PortfolioRepository.class);
    }

    @Bean
    public AccountHealthService accountHealthService() {
      return mock(AccountHealthService.class);
    }

    @Bean
    public TransactionRepository transactionRepository() {
      return mock(TransactionRepository.class);
    }

    @Bean
    public MarketAssetInfoRepository infoRepository() {
      return mock(MarketAssetInfoRepository.class);
    }

    @Bean
    public TransactionViewMapper transactionViewMapper() {
      return mock(TransactionViewMapper.class);
    }

    @Bean
    public TransactionCommandValidator validator() {
      return mock(TransactionCommandValidator.class);
    }

    @Bean
    public ApplicationEventPublisher eventPublisher() {
      return mock(ApplicationEventPublisher.class);
    }

    @Bean
    public PortfolioLoader portfolioLoader() {
      return mock(PortfolioLoader.class);
    }

    @Bean
    public ExchangeRateService exchangeRateService() {
      return mock(ExchangeRateService.class);
    }

    @Bean
    public TransactionRecordingService transactionRecordingService() {
      return mock(TransactionRecordingService.class);
    }

    @Bean
    public CacheManager cacheManager() {
      return mock(CacheManager.class);
    }

    @Bean
    public IdempotencyCache ic() {
      return mock(IdempotencyCache.class);
    };

    @Bean
    public TransactionService transactionService(PortfolioRepository pr, AccountHealthService ahs,
        TransactionRepository tr, MarketAssetInfoRepository ir, TransactionViewMapper tvm,
        TransactionCommandValidator v, ApplicationEventPublisher ep, PortfolioLoader pl,
        ExchangeRateService ers, TransactionRecordingService trs, CacheManager cm, IdempotencyCache ic) {
      return new TransactionService(pr, ahs, tr, ir, tvm, v, ep, pl, ers, trs, cm, ic);
    }
  }
}