package com.laderrco.fortunelink.portfolio.application.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.*;

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
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio.application.mappers.TransactionViewMapper;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.validators.TransactionCommandValidator;
import com.laderrco.fortunelink.portfolio.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
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

@SpringJUnitConfig(classes = TransactionServiceRetryTest.TestConfig.class)
class TransactionServiceRetryTest {

  @Autowired
  private TransactionService transactionService;

  // We autowire the beans defined in TestConfig to manipulate their behavior
  @Autowired
  private TransactionRecordingService transactionRecordingService;

  @Autowired
  private TransactionCommandValidator validator;

  @Autowired
  private PortfolioLoader portfolioLoader;

  @Autowired
  private AccountHealthService accountHealthService;

  @Autowired
  private MarketAssetInfoRepository infoRepository;

  @Test
  @DisplayName("Should retry 3 times and then trigger @Recover logic")
  void shouldRetryThreeTimesAndRecover() {
    // 1. Setup valid command with non-null Price
    RecordPurchaseCommand command = createSampleCommand();

    // 2. Mock Validator (must succeed)
    when(validator.validate(any(RecordPurchaseCommand.class))).thenReturn(ValidationResult.success());

    // 3. Mock Portfolio Loader & Account (must return valid, non-null objects)
    Portfolio portfolio = buildFakePortfolioForPurchase(command);
    when(portfolioLoader.loadUserPortfolio(command.portfolioId(), command.userId()))
        .thenReturn(portfolio);

    // 4. Mock Asset Info (to avoid resolveAssetType failing)
    when(infoRepository.findBySymbol(any())).thenReturn(Optional.empty());

    // 5. Force the specific exception that triggers @Retryable
    // This is where the loop happens
    when(transactionRecordingService.recordBuy(any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean()))
        .thenThrow(new ObjectOptimisticLockingFailureException("Portfolio", "test-id"));

    // 6. Execute and Assert
    // The @Recover method throws ConcurrentModificationException
    assertThrows(ConcurrentModificationException.class, () -> transactionService.recordPurchase(command));

    // 7. Verify the Retry Logic
    // 1 initial attempt + 2 retries = 3 total calls
    verify(transactionRecordingService, times(3))
        .recordBuy(any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    // 8. Verify the Recovery Logic
    verify(accountHealthService)
        .markStale(command.portfolioId(), command.userId(), command.accountId());
  }

  @Test
  @DisplayName("Should retry recordSale and recover when locking fails")
  void shouldRetrySaleAndRecover() {
    // 1. Setup Command
    RecordSaleCommand command = new RecordSaleCommand(UUID.randomUUID(),
        PortfolioId.newId(), UserId.random(), AccountId.newId(),
        "AAPL", Quantity.of(5), Price.of("150.00", Currency.CAD),
        List.of(), Instant.now(), "Sale Test");

    // 2. Mock Validator
    when(validator.validate(any(RecordSaleCommand.class))).thenReturn(ValidationResult.success());

    // 3. Mock Portfolio/Account
    Portfolio portfolio = buildFakePortfolioForSale(command);
    when(portfolioLoader.loadUserPortfolio(any(), any())).thenReturn(portfolio);

    // 4. Force failure on the recording service
    when(transactionRecordingService.recordSell(any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new ObjectOptimisticLockingFailureException("Portfolio", "test-id"));

    // 5. Assert Recovery Exception
    assertThrows(ConcurrentModificationException.class, () -> transactionService.recordSale(command));

    // 6. Verify 3 attempts
    verify(transactionRecordingService, times(3))
        .recordSell(any(), any(), any(), any(), any(), any(), any());
  }

  // --- Helpers to prevent DomainArgumentException ---

  private RecordPurchaseCommand createSampleCommand() {
    return new RecordPurchaseCommand(
        UUID.randomUUID(),
        PortfolioId.newId(),
        UserId.random(),
        AccountId.newId(),
        "AAPL",
        AssetType.STOCK,
        Quantity.of(10),
        Price.of("150.00", Currency.CAD), // Ensure this matches account currency to skip conversion logic
        List.of(),
        Instant.now(),
        "Test Note",
        false);
  }

  private Portfolio buildFakePortfolioForPurchase(RecordPurchaseCommand command) {
    Portfolio mockPortfolio = mock(Portfolio.class);
    Account mockAccount = mock(Account.class);

    when(mockPortfolio.getPortfolioId()).thenReturn(command.portfolioId());
    when(mockPortfolio.getUserId()).thenReturn(command.userId());
    when(mockPortfolio.getAccount(command.accountId())).thenReturn(mockAccount);

    // Setup Account to satisfy the 'execute' context and 'resolvePrice'
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

    // CRITICAL for recordSale: Must return true so we don't throw
    // InsufficientQuantityException
    when(mockAccount.hasPosition(any(AssetSymbol.class))).thenReturn(true);

    return mockPortfolio;
  }

  // --- Configuration ---

  @Configuration
  @EnableRetry
  static class TestConfig {
    // Define all dependencies as mocks
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
    public TransactionService transactionService(
        PortfolioRepository pr, AccountHealthService ahs, TransactionRepository tr,
        MarketAssetInfoRepository ir, TransactionViewMapper tvm, TransactionCommandValidator v,
        ApplicationEventPublisher ep, PortfolioLoader pl, ExchangeRateService ers,
        TransactionRecordingService trs, CacheManager cm) {
      return new TransactionService(pr, ahs, tr, ir, tvm, v, ep, pl, ers, trs, cm);
    }
  }
}