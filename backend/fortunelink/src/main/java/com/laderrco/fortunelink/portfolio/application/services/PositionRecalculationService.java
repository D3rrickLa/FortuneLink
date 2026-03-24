package com.laderrco.fortunelink.portfolio.application.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.laderrco.fortunelink.portfolio.application.events.PositionRecalculationRequestedEvent;
import com.laderrco.fortunelink.portfolio.application.utils.PositionRecalculationExecutor;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
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
  private final Cache<String, Object> symbolLocks = Caffeine.newBuilder()
      .expireAfterAccess(5, TimeUnit.MINUTES).maximumSize(10_000).build();

  /**
   * Async listener that triggers after a transaction commit. Ensures excluded/restored flags are
   * persisted before we replay them.
   */
  @Async("recalculationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRecalculationRequested(PositionRecalculationRequestedEvent event) {
    String lockKey = event.accountId() + ":" + event.symbol().symbol();
    Object lock = symbolLocks.get(lockKey, k -> new Object());

    synchronized (lock) {
      try {
        executor.scheduleRecalculation(event.portfolioId(), event.userId(), event.accountId(),
            event.symbol());
      } catch (Exception e) {
        log.error("Recalculation failed...", e);
      }
    }
  }
}
