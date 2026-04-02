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
  private final RedissonClient redisson; // Injected by Spring Boot Starter

  /**
   * Async listener that triggers after a transaction commit. Ensures excluded/restored flags are
   * persisted before we replay them.
   */
  @Async("recalculationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRecalculationRequested(PositionRecalculationRequestedEvent event) {
    Objects.requireNonNull(event, "PositionRecalculationRequestedEvent cannot be null");

    // Lock at account level, not symbol level
    // A full replay and a partial replay must not run concurrently on same account
    String lockKey = String.format("lock:account:%s", event.accountId().id().toString());
    RLock lock = redisson.getLock(lockKey);

    try {
      // Attempt to acquire lock for 10s, lease for 30s
      if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
        try {
          executor.scheduleRecalculation(event.portfolioId(), event.userId(), event.accountId(),
              event.symbol());
        } finally {
          lock.unlock();
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Recalculation failed for portfolioId={} accountId={} symbol={}",
          event.portfolioId(), event.accountId(), event.symbol().symbol(), e);
      throw new RuntimeException("Interrupted while waiting for position lock", e);
    }
  }
}
