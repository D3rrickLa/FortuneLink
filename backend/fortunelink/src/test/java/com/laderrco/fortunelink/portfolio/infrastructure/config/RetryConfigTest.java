package com.laderrco.fortunelink.portfolio.infrastructure.config;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.laderrco.fortunelink.portfolio.application.commands.records.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio.application.mappers.TransactionViewMapper;
import com.laderrco.fortunelink.portfolio.application.services.AccountHealthService;
import com.laderrco.fortunelink.portfolio.application.services.TransactionService;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.validators.TransactionCommandValidator;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.MarketAssetInfoRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio.domain.services.TransactionRecordingService;
import com.laderrco.fortunelink.portfolio.infrastructure.config.cachedidempotency.IdempotencyCache;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

// We need a Spring context to enable the Retry proxy
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@SpringBootTest(classes = { TransactionService.class, RetryConfig.class })
class TransactionServiceRetryTest {
  private static final UserId USER_ID = UserId.random();
  private static final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private static final AccountId ACCOUNT_ID = AccountId.newId();
  private static final AssetType ASSET_TYPE = AssetType.STOCK;
  private static final String NOTES = "Test Note";
  private static final Instant NOW = Instant.now();
  private static final Currency USD = Currency.of("USD");
  private static final Money AMOUNT = new Money(new BigDecimal("100.00"), USD);
  private static final UUID IDEMPOTENCY_KEY = UUID.randomUUID();

  @Autowired
  private TransactionService transactionService;

  @MockitoBean
  private TransactionCommandValidator validator;
  @MockitoBean
  private PortfolioLoader portfolioLoader;
  @MockitoBean
  private AccountHealthService accountHealthService;
  @MockitoBean
  private PortfolioRepository portfolioRepository;
  @MockitoBean
  private TransactionRepository transactionRepository;
  @MockitoBean
  private MarketAssetInfoRepository infoRepository;
  @MockitoBean
  private TransactionViewMapper transactionViewMapper;
  @MockitoBean
  private ApplicationEventPublisher eventPublisher;
  @MockitoBean
  private ExchangeRateService exchangeRateService;
  @MockitoBean
  private TransactionRecordingService transactionRecordingService;
  @MockitoBean
  private CacheManager cacheManager;
  @MockitoBean
  private IdempotencyCache idempotencyCache;

  @Test
  void shouldRetryThreeTimesThenRecover() {
    RecordPurchaseCommand cmd = createSampleCommand();

    doThrow(new ObjectOptimisticLockingFailureException(Portfolio.class, "123"))
        .when(validator).validate(any(RecordPurchaseCommand.class));

    // Wrap in Assertions to catch the exception thrown by your @Recover method
    assertThatThrownBy(() -> transactionService.recordPurchase(cmd))
        .isInstanceOf(ConcurrentModificationException.class);

    // This proves the retries happened before the recovery logic failed
    verify(validator, times(3)).validate(any(RecordPurchaseCommand.class));
  }

  private RecordPurchaseCommand createSampleCommand() {
    return new RecordPurchaseCommand(IDEMPOTENCY_KEY, PORTFOLIO_ID,
        USER_ID, ACCOUNT_ID, "AAPL", ASSET_TYPE, Quantity.of(10), new Price(AMOUNT),
        List.of(), NOW, NOTES, false);
  }
}