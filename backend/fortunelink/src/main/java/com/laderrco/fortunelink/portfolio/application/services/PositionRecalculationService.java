package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.events.PositionRecalculationRequestedEvent;
import com.laderrco.fortunelink.portfolio.application.utils.PositionRecalculationExecutor;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

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
  private final ConcurrentHashMap<String, ReentrantLock> symbolLocks = new ConcurrentHashMap<>();

  /**
   * Async listener that triggers after a transaction commit. Ensures
   * excluded/restored flags are
   * persisted before we replay them.
   */
  @Async("recalculationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRecalculationRequested(PositionRecalculationRequestedEvent event) {
    Objects.requireNonNull(event, "PositionRecalculationRequestedEvent cannot be null");

    String lockKey = event.accountId() + ":" + event.symbol().symbol();
    ReentrantLock lock = symbolLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());

    lock.lock();
    try {
      executor.scheduleRecalculation(
          event.portfolioId(), event.userId(), event.accountId(), event.symbol());
    } catch (Exception e) {
      log.error("Recalculation failed for portfolioId={} accountId={} symbol={}",
          event.portfolioId(), event.accountId(), event.symbol().symbol(), e);
    } finally {
      if (!lock.hasQueuedThreads()) {
        symbolLocks.remove(lockKey, lock);
      }
      lock.unlock();
    }

  }
}
