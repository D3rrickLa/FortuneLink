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
  final ConcurrentHashMap<String, ReentrantLock> symbolLocks = new ConcurrentHashMap<>();

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
      /*
       * There is a race condition here between the if and remove call. Basically the
       * scenario is this:
       * Thread A - finishes work and checks the !lock.hasQueuedThreads(), returns
       * true - no one waiting
       * 
       * Thread B - proceeds to symbolLocks.remove(lockKey, lock), sees the lock is
       * still in the map, calls lock.lock(); now a queued thread
       * 
       * Thread A is removed, lock is removed from the map
       * 
       * Thread C arrives -> calls the computeIfAbsent and sees that Thread A removed
       * old lock, thread C creates a new Lock Object
       * 
       * Now both Thread B and C are running reclculation for the same symbol
       * simultaneously as they are
       * sync on different lock objects
       * 
       * The fix is that we allow it for now, unless we need to save a lot of memory - i.e. remove
       * them both for atomic removal via a Striped lock form Guava - no point. Another option is
       * to remove the finally statement
       */
      if (!lock.hasQueuedThreads()) {
        symbolLocks.remove(lockKey, lock);
      }
      lock.unlock();
    }

  }
}
