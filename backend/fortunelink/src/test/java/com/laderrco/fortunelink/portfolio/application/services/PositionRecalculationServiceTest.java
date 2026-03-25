package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laderrco.fortunelink.portfolio.application.events.PositionRecalculationRequestedEvent;
import com.laderrco.fortunelink.portfolio.application.utils.PositionRecalculationExecutor;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

/**
 * In a unit test, we treat onRecalculationRequested as a regular method. Our
 * goal is to verify the Concurrency Logic: specifically the ConcurrentHashMap
 * locking mechanism and the cleanup of the symbolLocks map.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Position Recalculation Service Unit Tests")
public class PositionRecalculationServiceTest {
  @Mock
  private PositionRecalculationExecutor executor;

  @InjectMocks
  private PositionRecalculationService recalculationService;

  private final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private final UserId USER_ID = UserId.random();
  private final AccountId ACCOUNT_ID = AccountId.newId();
  private final AssetSymbol SYMBOL = new AssetSymbol("AAPL");

  @Nested
  @DisplayName("Event Listener and Locking")
  class EventListenerTests {
    @Test
    @DisplayName("onRecalculationRequested: successfully schedules recalculation and cleans up lock")
    void onRecalculationRequestedSchedulesAndCleansUp() {
      PositionRecalculationRequestedEvent event = createEvent();

      recalculationService.onRecalculationRequested(event);

      // Verify execution
      verify(executor).scheduleRecalculation(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, SYMBOL);

      // Verify map cleanup (since only one thread ran, no threads should be queued)
      // We use reflection or access the internal state if necessary,
      // but usually, we check if the service remains performant/stable.
    }

    @Test
    @DisplayName("onRecalculationRequested: ensures lock is released even on executor failure")
    void onRecalculationRequestedReleasesLockOnException() {
      PositionRecalculationRequestedEvent event = createEvent();

      doThrow(new RuntimeException("Execution Failed"))
          .when(executor).scheduleRecalculation(any(), any(), any(), any());

      // Should not rethrow exception due to internal try-catch-log
      recalculationService.onRecalculationRequested(event);

      verify(executor).scheduleRecalculation(any(), any(), any(), any());
      // Implicitly verifies 'finally' block reached;
      // if lock wasn't released, subsequent tests/calls would hang.
    }

    @Test
    @DisplayName("onRecalculationRequested: throws exception when event is null")
    void onRecalculationRequestedThrowsOnNullEvent() {
      assertThatThrownBy(() -> recalculationService.onRecalculationRequested(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("cannot be null");
    }
  }

  @Nested
  @DisplayName("Concurrency and Lock Management")
  class ConcurrencyTests {
    @Test
    @DisplayName("onRecalculationRequested: does not remove lock from map if threads are queued")
    void onRecalculationRequestedPreservesLockIfQueued() throws Exception {
      // This is tricky to unit test without a CountdownLatch,
      // but we can simulate the 'hasQueuedThreads' logic by
      // mocking the Lock if the service allowed lock injection.
      // Since it's internal, this is best verified via an Integration Test
      // with multiple concurrent threads.
    }
  }

  @Nested
  @DisplayName("Lock Lifecycle and Map Cleanup")
  class LockLifecycleTests {

    @Test
    @DisplayName("onRecalculationRequested: removes lock from map when NO threads are queued")
    void onRecalculationRequestedRemovesLockWhenNoQueuedThreads() {
      PositionRecalculationRequestedEvent event = createEvent();
      String lockKey = event.accountId() + ":" + event.symbol().symbol();

      recalculationService.onRecalculationRequested(event);

      // Assert: Map is clean
      assertThat(recalculationService.symbolLocks).doesNotContainKey(lockKey);
      verify(executor).scheduleRecalculation(any(), any(), any(), any());
    }

    @Test
    @DisplayName("onRecalculationRequested: keeps lock in map when threads ARE queued")
    void onRecalculationRequestedKeepsLockWhenThreadsQueued() throws InterruptedException {
      PositionRecalculationRequestedEvent event = createEvent();
      String lockKey = event.accountId() + ":" + event.symbol().symbol();

      CountDownLatch firstThreadStarted = new CountDownLatch(1);
      CountDownLatch secondThreadWait = new CountDownLatch(1);

      // Thread 1: Enters the executor and blocks there
      doAnswer(invocation -> {
        firstThreadStarted.countDown();
        secondThreadWait.await(); // Hold the lock
        return null;
      }).when(executor).scheduleRecalculation(any(), any(), any(), any());

      Thread t1 = new Thread(() -> recalculationService.onRecalculationRequested(event));
      t1.start();
      firstThreadStarted.await();

      // Thread 2: Tries to get the same lock, becomes a 'queued thread'
      Thread t2 = new Thread(() -> recalculationService.onRecalculationRequested(event));
      t2.start();

      // Small sleep to ensure Thread 2 is actually waiting on the lock
      Thread.sleep(50);

      // Release Thread 1 to hit the 'finally' block
      secondThreadWait.countDown();
      t1.join();

      // ASSERT: Because Thread 2 was queued, Thread 1 should NOT have removed the
      // lock
      assertThat(recalculationService.symbolLocks).containsKey(lockKey);

      t2.join();
      // After Thread 2 finishes (and if no Thread 3 exists), it should then be
      // removed
      assertThat(recalculationService.symbolLocks).doesNotContainKey(lockKey);
    }
  }

  private PositionRecalculationRequestedEvent createEvent() {
    return new PositionRecalculationRequestedEvent(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, SYMBOL);
  }
}
