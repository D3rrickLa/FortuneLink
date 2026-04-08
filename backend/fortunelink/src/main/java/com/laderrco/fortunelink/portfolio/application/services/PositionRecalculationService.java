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
      log.error("Recalculation interrupted for portfolioId={} accountId={}", event.portfolioId(),
          event.accountId(), e);
      accountHealthService.markStale(event.portfolioId(), event.userId(), event.accountId());
    }
  }

  private void acquireAndRun(String lockKey, PositionRecalculationRequestedEvent event)
      throws InterruptedException {
    RLock lock = redisson.getLock(lockKey);
    boolean acquired = false;

    try {
      // The network call actually happens here.
      // If Redis is down, this will throw a RedisException.
      acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);

      if (acquired) {
        runRecalculation(event);
      } else {
        log.warn("Lock busy for accountId={}", event.accountId());
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt(); // preserve interrupt
      throw e; // rethrow so outer handler catches it
    } catch (Exception redisEx) {
      log.error("Redis unavailable. Cannot acquire lock for accountId={}. " +
          "Marking account STALE to prevent race condition.", event.accountId(), redisEx);
      accountHealthService.markStale(event.portfolioId(), event.userId(), event.accountId());
      // DO NOT fall through to runRecalculation
    } finally {
      if (acquired) {
        try {
          lock.unlock();
        } catch (Exception e) {
          log.debug("Lock already released or expired.");
        }
      }
    }
  }

  private void runRecalculation(PositionRecalculationRequestedEvent event) {
    try {
      executor.scheduleRecalculation(event.portfolioId(), event.userId(), event.accountId(),
          event.symbol());
    } catch (Exception e) {
      log.error("Recalculation failed for accountId={} symbol={}", event.accountId(),
          event.symbol().symbol(), e);
      accountHealthService.markStale(event.portfolioId(), event.userId(), event.accountId());
      throw e;
    }
  }
}
