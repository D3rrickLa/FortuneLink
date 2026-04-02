package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.laderrco.fortunelink.portfolio.application.events.PositionRecalculationRequestedEvent;
import com.laderrco.fortunelink.portfolio.application.utils.PositionRecalculationExecutor;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;

@ExtendWith(MockitoExtension.class)
@DisplayName("Position Recalculation Service Unit Tests")
public class PositionRecalculationServiceTest {
  private final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private final UserId USER_ID = UserId.random();
  private final AccountId ACCOUNT_ID = AccountId.newId();
  private final AssetSymbol SYMBOL = new AssetSymbol("AAPL");

  @Mock
  private RedissonClient redissonClient;
  @Mock
  private PositionRecalculationExecutor executor;
  @InjectMocks
  private PositionRecalculationService recalculationService;

  private PositionRecalculationRequestedEvent createEvent() {
    return new PositionRecalculationRequestedEvent(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, SYMBOL);
  }

  @Nested
  @DisplayName("Creation and Basic Validation")
  class ValidationTests {
    @Test
    @DisplayName("onRecalculationRequested: throws exception when event is null")
    void onRecalculationRequestedThrowsOnNullEvent() {
      assertThatThrownBy(() -> recalculationService.onRecalculationRequested(null)).isInstanceOf(
          NullPointerException.class);
    }
  }

  @Nested
  @DisplayName("Execution and Locking")
  class ExecutionTests {

    @Test
    @DisplayName("onRecalculationRequested: successfully schedules recalculation")
    void onRecalculationRequestedSchedulesSuccessfully() {
      PositionRecalculationRequestedEvent event = createEvent();

      recalculationService.onRecalculationRequested(event);

      verify(executor).scheduleRecalculation(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, SYMBOL);
    }

    @Test
    @DisplayName("onRecalculationRequested: handles executor exceptions gracefully")
    void onRecalculationRequestedHandlesException() {
      PositionRecalculationRequestedEvent event = createEvent();
      doThrow(new RuntimeException("Fail")).when(executor)
          .scheduleRecalculation(any(), any(), any(), any());

      // Should not throw exception
      recalculationService.onRecalculationRequested(event);

      verify(executor).scheduleRecalculation(any(), any(), any(), any());
    }

    @Test
    @DisplayName("onRecalculationRequested: ensures sequential execution for same symbol")
    void onRecalculationRequestedEnsuresLocking() throws InterruptedException {
      PositionRecalculationRequestedEvent event = createEvent();
      CountDownLatch t1Started = new CountDownLatch(1);
      CountDownLatch t1Hold = new CountDownLatch(1);
      AtomicInteger activeThreads = new AtomicInteger(0);
      AtomicInteger maxConcurrentThreads = new AtomicInteger(0);

      // Mock executor to track concurrency
      doAnswer(invocation -> {
        int count = activeThreads.incrementAndGet();
        // Update max concurrency seen
        if (count > maxConcurrentThreads.get()) {
          maxConcurrentThreads.set(count);
        }

        t1Started.countDown();
        t1Hold.await(); // Block the first thread here

        activeThreads.decrementAndGet();
        return null;
      }).when(executor).scheduleRecalculation(any(), any(), any(), any());

      Thread t1 = new Thread(() -> recalculationService.onRecalculationRequested(event));
      Thread t2 = new Thread(() -> recalculationService.onRecalculationRequested(event));

      t1.start();
      t1Started.await(); // Ensure T1 is inside the executor

      t2.start();
      // Give T2 time to attempt to enter (it should be blocked by the lock)
      Thread.sleep(100);

      t1Hold.countDown(); // Release T1
      t1.join();
      t2.join();

      // If locking works, max concurrent threads in the executor must be 1
      assertThat(maxConcurrentThreads.get()).isEqualTo(1);
      verify(executor, times(2)).scheduleRecalculation(any(), any(), any(), any());
    }
  }
}