package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.events.PositionRecalculationRequestedEvent;
import com.laderrco.fortunelink.portfolio.application.services.redislock.DistributedLock;
import com.laderrco.fortunelink.portfolio.application.services.redislock.DistributedLockProvider;
import com.laderrco.fortunelink.portfolio.application.utils.PositionRecalculationExecutor;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("Position Recalculation Service Unit Tests")
class PositionRecalculationServiceTest {
  private final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private final UserId USER_ID = UserId.random();
  private final AccountId ACCOUNT_ID = AccountId.newId();
  private final AssetSymbol SYMBOL = new AssetSymbol("AAPL");
  private LogCaptor logCaptor;

  @Mock
  private StringRedisTemplate redisTemplate;
  @Mock
  private ValueOperations<String, String> valueOperations;
  @Mock
  private DistributedLockProvider lockProvider;
  @Mock
  private DistributedLock lock;
  @Mock
  private PositionRecalculationExecutor executor;
  @Mock
  private AccountHealthService accountHealthService;

  @InjectMocks
  private PositionRecalculationService recalculationService;

  @BeforeEach
  void setup() {
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    logCaptor = LogCaptor.forClass(PositionRecalculationService.class);
  }

  @AfterEach
  void tearDown() {
    logCaptor.clearLogs();
    logCaptor.close();
  }

  @Test
  @DisplayName("onRecalculationRequested: debounce logic sets key in Redis")
  void debounceLogicSetsKey() {
    PositionRecalculationRequestedEvent event = createEvent();

    when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(
        true);

    recalculationService.onRecalculationRequested(event);

    verify(valueOperations).setIfAbsent(startsWith("recalc:"), anyString(),
        eq(Duration.ofSeconds(3)));

  }

  @Test
  @DisplayName("onRecalculationRequested: subsequent requests update token but don't schedule new task")
  void subsequentRequestsUpdateToken() {
    PositionRecalculationRequestedEvent event = createEvent();

    when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(
        false);

    recalculationService.onRecalculationRequested(event);

    verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
  }

  @Test
  @DisplayName("acquireAndRun: handles Redis connection failure by marking account stale")
  void handlesLockingRedisFailure() throws InterruptedException {
    PositionRecalculationRequestedEvent event = createEvent();

    when(lockProvider.getLock(anyString())).thenReturn(lock);
    when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenThrow(
        new RuntimeException("Redis down"));

    ReflectionTestUtils.invokeMethod(recalculationService, "acquireAndRun", "test-lock", event);

    verify(accountHealthService).markStale(event.accountId());
  }

  @Test
  @DisplayName("runRecalculation: marks stale if executor throws exception")
  void marksStaleOnExecutorError() {
    PositionRecalculationRequestedEvent event = createEvent();
    doThrow(new RuntimeException("Failed")).when(executor)
        .scheduleRecalculation(any(), any(), any(), any());

    assertThatThrownBy(
        () -> ReflectionTestUtils.invokeMethod(recalculationService, "runRecalculation",
            event)).isInstanceOf(RuntimeException.class);

    verify(accountHealthService).markStale(event.accountId());
  }

  @Test
  @DisplayName("onRecalculationRequested: schedules execution when it's the first event for a symbol")
  void onRecalculationRequestedSchedulesFirst() {
    PositionRecalculationRequestedEvent event = createEvent();

    when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(
        true);

    recalculationService.onRecalculationRequested(event);

    verify(valueOperations).setIfAbsent(contains(SYMBOL.symbol()), anyString(),
        eq(Duration.ofSeconds(3)));
  }

  @Test
  @DisplayName("onRecalculationRequested: updates token but doesn't re-schedule on duplicate")
  void onRecalculationRequestedUpdatesDuplicate() {
    PositionRecalculationRequestedEvent event = createEvent();

    when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(
        false);

    recalculationService.onRecalculationRequested(event);

    verify(valueOperations).set(anyString(), anyString(), eq(Duration.ofSeconds(3)));
  }

  @Test
  @DisplayName("runIfLatest: skips execution if token is no longer current")
  void runIfLatestSkipsIfObsolete() {
    PositionRecalculationRequestedEvent event = createEvent();
    String key = "recalc:key";
    String oldToken = "token-123";
    String newToken = "token-456";

    when(valueOperations.get(key)).thenReturn(newToken);

    ReflectionTestUtils.invokeMethod(recalculationService, "runIfLatest", event, key, oldToken);

    verify(redisTemplate, never()).delete(anyString());
    verifyNoInteractions(lockProvider);
  }

  @Test
  @DisplayName("runIfLatest: proceeds and deletes key if token is current")
  void runIfLatestProceedsIfCurrent() throws InterruptedException {
    PositionRecalculationRequestedEvent event = createEvent();
    String key = "recalc:key";
    String token = "token-123";

    when(valueOperations.get(key)).thenReturn(token);
    when(lockProvider.getLock(anyString())).thenReturn(lock);
    when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);

    ReflectionTestUtils.invokeMethod(recalculationService, "runIfLatest", event, key, token);

    verify(redisTemplate).delete(key);
    verify(lockProvider).getLock(contains(ACCOUNT_ID.id().toString()));
  }

  @Test
  @DisplayName("acquireAndRun: logs warning and skips when lock is held by another process")
  void acquireAndRunHandlesLockBusy() throws InterruptedException {
    PositionRecalculationRequestedEvent event = createEvent();
    when(lockProvider.getLock(anyString())).thenReturn(lock);

    when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(false);

    ReflectionTestUtils.invokeMethod(recalculationService, "acquireAndRun", "lock-key", event);

    verifyNoInteractions(executor);
    verify(lock, never()).unlock();
  }

  @Test
  @DisplayName("acquireAndRun: marks account stale if Redis fails during locking")
  void acquireAndRunHandlesRedisFailure() throws InterruptedException {
    PositionRecalculationRequestedEvent event = createEvent();
    when(lockProvider.getLock(anyString())).thenReturn(lock);

    when(lock.tryLock(anyLong(), anyLong(), any())).thenThrow(
        new RuntimeException("Redis connection lost"));

    ReflectionTestUtils.invokeMethod(recalculationService, "acquireAndRun", "lock-key", event);

    verify(accountHealthService).markStale(event.accountId());
    verifyNoInteractions(executor);
  }

  @Test
  @DisplayName("runRecalculation: marks stale and bubbles exception on executor failure")
  void runRecalculationHandlesFailure() {
    PositionRecalculationRequestedEvent event = createEvent();

    doThrow(new RuntimeException("Math error")).when(executor)
        .scheduleRecalculation(any(), any(), any(), any());

    assertThatThrownBy(
        () -> ReflectionTestUtils.invokeMethod(recalculationService, "runRecalculation",
            event)).isInstanceOf(RuntimeException.class);

    verify(accountHealthService).markStale(event.accountId());
  }

  @Test
  @DisplayName("acquireAndRun: handles interruption during lock acquisition")
  void acquireAndRunHandlesInterruption() throws InterruptedException {
    PositionRecalculationRequestedEvent event = createEvent();
    when(lockProvider.getLock(anyString())).thenReturn(lock);

    when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenThrow(
        new InterruptedException("Simulation"));

    assertThatThrownBy(
        () -> ReflectionTestUtils.invokeMethod(recalculationService, "acquireAndRun", "lock-key",
            event)).hasCauseInstanceOf(InterruptedException.class);

    assertTrue(Thread.interrupted(), "Interrupt flag should be restored");
  }

  @Test
  @DisplayName("runIfLatest: logs error when acquireAndRun throws InterruptedException")
  void runIfLatestLogsInterrupted() throws InterruptedException {
    PositionRecalculationRequestedEvent event = createEvent();
    String key = "recalc:key";
    String token = "token";

    when(valueOperations.get(key)).thenReturn(token);
    when(lockProvider.getLock(anyString())).thenReturn(lock);

    when(lock.tryLock(anyLong(), anyLong(), any())).thenThrow(new InterruptedException());

    ReflectionTestUtils.invokeMethod(recalculationService, "runIfLatest", event, key, token);

    assertThat(logCaptor.getErrorLogs()).anyMatch(l -> l.contains("Interrupted"));
  }

  @Test
  @DisplayName("acquireAndRun: suppresses error when unlock fails")
  void acquireAndRunSuppressesUnlockError() throws InterruptedException {
    PositionRecalculationRequestedEvent event = createEvent();
    when(lockProvider.getLock(anyString())).thenReturn(lock);
    when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);

    doThrow(new IllegalMonitorStateException("Lock already released")).when(lock).unlock();

    assertDoesNotThrow(
        () -> ReflectionTestUtils.invokeMethod(recalculationService, "acquireAndRun", "lock-key",
            event));

    verify(lock).unlock();

  }

  @Test
  @DisplayName("acquireAndRun: logs warning when lock is busy")
  void acquireAndRunLogsWarnWhenBusy() throws InterruptedException {
    PositionRecalculationRequestedEvent event = createEvent();
    when(lockProvider.getLock(anyString())).thenReturn(lock);
    when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(false);

    ReflectionTestUtils.invokeMethod(recalculationService, "acquireAndRun", "lock-key", event);

    assertThat(logCaptor.getWarnLogs()).anyMatch(l -> l.contains("Lock busy for accountId"));

    verifyNoInteractions(executor);
  }

  private PositionRecalculationRequestedEvent createEvent() {
    return new PositionRecalculationRequestedEvent(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, SYMBOL);
  }
}