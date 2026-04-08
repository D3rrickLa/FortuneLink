package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.laderrco.fortunelink.portfolio.application.events.PositionRecalculationRequestedEvent;
import com.laderrco.fortunelink.portfolio.application.utils.PositionRecalculationExecutor;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("Position Recalculation Service Unit Tests")
class PositionRecalculationServiceTest {
  private final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private final UserId USER_ID = UserId.random();
  private final AccountId ACCOUNT_ID = AccountId.newId();
  private final AssetSymbol SYMBOL = new AssetSymbol("AAPL");

  @Mock
  private Appender<ILoggingEvent> mockAppender;
  @Captor
  private ArgumentCaptor<ILoggingEvent> logCaptor;

  @Mock
  private RedissonClient redissonClient;

  @Mock
  private PositionRecalculationExecutor executor;
  @Mock
  private AccountHealthService accountHealthService;
  @InjectMocks
  private PositionRecalculationService recalculationService;

  @BeforeEach
  void setupLogging() {
    Logger logger = (Logger) LoggerFactory.getLogger(PositionRecalculationService.class);
    logger.addAppender(mockAppender);
  }

  @AfterEach
  void tearDownLogging() {
    Logger logger = (Logger) LoggerFactory.getLogger(PositionRecalculationService.class);
    logger.detachAppender(mockAppender);
  }

  private PositionRecalculationRequestedEvent createEvent() {
    return new PositionRecalculationRequestedEvent(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, SYMBOL);
  }

  @Test
  @DisplayName("onRecalculationRequested: throws exception when event is null")
  void onRecalculationRequestedThrowsOnNullEvent() {
    assertThatThrownBy(() -> recalculationService.onRecalculationRequested(null)).isInstanceOf(
        NullPointerException.class).hasMessageContaining("cannot be null");
  }

  @Test
  @DisplayName("onRecalculationRequested: successfully schedules when lock is acquired")
  void onRecalculationRequestedSchedulesSuccessfully() throws InterruptedException {
    PositionRecalculationRequestedEvent event = createEvent();
    RLock mockLock = mock(RLock.class);

    when(redissonClient.getLock(anyString())).thenReturn(mockLock);
    when(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

    recalculationService.onRecalculationRequested(event);

    verify(executor).scheduleRecalculation(any(), any(), any(), any());
    verify(mockLock).unlock();
  }

  @Test
  @DisplayName("onRecalculationRequested: skips execution when lock is busy")
  void onRecalculationRequestedHandlesLockContention() throws InterruptedException {
    PositionRecalculationRequestedEvent event = createEvent();
    RLock mockLock = mock(RLock.class);

    when(redissonClient.getLock(anyString())).thenReturn(mockLock);
    when(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

    recalculationService.onRecalculationRequested(event);

    verifyNoInteractions(executor);
    verify(mockLock, never()).unlock();
  }

  @Test
  @DisplayName("onRecalculationRequested: marks stale and skips execution when Redis is unreachable")
  void onRecalculationRequestedHandlesRedisDown() throws InterruptedException {
    PositionRecalculationRequestedEvent event = createEvent();
    RLock mockLock = mock(RLock.class);

    when(redissonClient.getLock(anyString())).thenReturn(mockLock);
    when(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
        .thenThrow(new RuntimeException("Redis connection refused"));

    recalculationService.onRecalculationRequested(event);

    // Verify executor is NOT called (safety first)
    // Verify fallback logic
    verifyNoInteractions(executor);
    verify(accountHealthService).markStale(eq(PORTFOLIO_ID), eq(USER_ID), eq(ACCOUNT_ID));
  }

  @Test
  @DisplayName("onRecalculationRequested: marks stale and restores interrupt on InterruptedException")
  void onRecalculationRequestedHandlesInterruptedException() throws InterruptedException {
    PositionRecalculationRequestedEvent event = createEvent();
    RLock mockLock = mock(RLock.class);

    when(redissonClient.getLock(anyString())).thenReturn(mockLock);

    doThrow(new InterruptedException("Interrupted!")).when(mockLock)
        .tryLock(anyLong(), anyLong(), any(TimeUnit.class));

    recalculationService.onRecalculationRequested(event);

    // Explicitly check for interactions with markStale using class matchers
    verify(accountHealthService).markStale(any(PortfolioId.class), any(UserId.class),
        any(AccountId.class));

    assertTrue(Thread.interrupted(), "Interrupt flag should be set");
    verifyNoInteractions(executor);
  }

  @Test
  @DisplayName("onRecalculationRequested: marks stale and bubbles exception if executor fails")
  void onRecalculationRequestedMarksStaleOnExecutorFailure() throws InterruptedException {
    PositionRecalculationRequestedEvent event = createEvent();
    RLock mockLock = mock(RLock.class);
    when(redissonClient.getLock(anyString())).thenReturn(mockLock);
    when(mockLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);

    doThrow(new RuntimeException("Computation error")).when(executor)
        .scheduleRecalculation(any(), any(), any(), any());

    // We expect the exception to bubble up to the test
    assertThatThrownBy(() -> recalculationService.onRecalculationRequested(event))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Computation error");

    // Verify side effects happened despite the crash
    verify(accountHealthService).markStale(eq(PORTFOLIO_ID), eq(USER_ID), eq(ACCOUNT_ID));
    verify(mockLock).unlock();
  }

  @Test
  @DisplayName("onRecalculationRequested: suppresses failure during lock release")
  void suppressesUnlockErrors() throws InterruptedException {
    PositionRecalculationRequestedEvent event = createEvent();
    RLock mockLock = mock(RLock.class);

    when(redissonClient.getLock(anyString())).thenReturn(mockLock);
    when(mockLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
    doThrow(new RuntimeException("Network lost")).when(mockLock).unlock();

    recalculationService.onRecalculationRequested(event);

    verify(executor).scheduleRecalculation(any(), any(), any(), any());
    verify(mockAppender, never()).doAppend(argThat(l -> l.getLevel().equals(Level.ERROR)));
  }

}