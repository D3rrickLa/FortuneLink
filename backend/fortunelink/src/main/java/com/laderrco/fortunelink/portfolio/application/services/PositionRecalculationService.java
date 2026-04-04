package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.events.PositionRecalculationRequestedEvent;
import com.laderrco.fortunelink.portfolio.application.utils.PositionRecalculationExecutor;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class PositionRecalculationService {
  private static final Logger log = LoggerFactory.getLogger(PositionRecalculationService.class);
  private final PositionRecalculationExecutor executor;
  private final AccountHealthService accountHealthService;
  private final RedissonClient redisson; // Injected by Spring Boot Starter

  @Async("recalculationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRecalculationRequested(PositionRecalculationRequestedEvent event) {
    Objects.requireNonNull(event, "PositionRecalculationRequestedEvent cannot be null");

    String lockKey = String.format("lock:account:%s", event.accountId().id().toString());

    try {
      acquireAndRun(lockKey, event);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Recalculation interrupted for portfolioId={} accountId={}",
          event.portfolioId(), event.accountId(), e);
      accountHealthService.markStale(event.portfolioId(), event.userId(), event.accountId());
    }
  }

  private void acquireAndRun(String lockKey, PositionRecalculationRequestedEvent event)
      throws InterruptedException {

    RLock lock;
    try {
      lock = redisson.getLock(lockKey);
    } catch (Exception redisEx) {
      // Redis is unavailable. Proceed without the distributed lock.
      // Two concurrent recalculations for the same account may run.
      // The last writer wins on the portfolio save due to optimistic locking,
      // so the final state will still be consistent — just potentially wasteful.
      log.warn("Redis unavailable for lock key={}, proceeding without lock. " +
          "Concurrent recalculation risk accepted.", lockKey, redisEx);
      runRecalculation(event);
      return;
    }

    boolean acquired = false;
    try {
      acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);
      if (acquired) {
        runRecalculation(event);
      } else {
        log.warn("Could not acquire lock for accountId={} within 10s. " +
            "Another recalculation is likely in progress.",
            event.accountId());
      }
    } finally {
      if (acquired) {
        try {
          lock.unlock();
        } catch (Exception e) {
          log.warn("Failed to release lock={}, it will expire automatically.", lockKey, e);
        }
      }
    }
  }

  private void runRecalculation(PositionRecalculationRequestedEvent event) {
    try {
      executor.scheduleRecalculation(
          event.portfolioId(), event.userId(), event.accountId(), event.symbol());
    } catch (Exception e) {
      log.error("Recalculation failed for accountId={} symbol={}",
          event.accountId(), event.symbol().symbol(), e);
      accountHealthService.markStale(event.portfolioId(), event.userId(), event.accountId());
      throw e;
    }
  }
}
