package com.laderrco.fortunelink.portfolio.application.services;

import com.google.common.util.concurrent.Striped;
import com.laderrco.fortunelink.portfolio.application.events.PositionRecalculationRequestedEvent;
import com.laderrco.fortunelink.portfolio.application.utils.PositionRecalculationExecutor;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
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
  private final Striped<Lock> symbolLocks = Striped.lock(1024);

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
    String lockKey = event.accountId().toString();
    Lock lock = symbolLocks.get(lockKey);

    lock.lock();
    try {
      executor.scheduleRecalculation(event.portfolioId(), event.userId(), event.accountId(),
          event.symbol());
    } catch (Exception e) {
      log.error("Recalculation failed for portfolioId={} accountId={} symbol={}",
          event.portfolioId(), event.accountId(), event.symbol().symbol(), e);
    } finally {
      lock.unlock(); // No map.remove() needed!
    }
  }
}
