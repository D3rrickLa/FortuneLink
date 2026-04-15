package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.events.PositionRecalculationRequestedEvent;
import com.laderrco.fortunelink.portfolio.application.services.redislock.DistributedLock;
import com.laderrco.fortunelink.portfolio.application.services.redislock.DistributedLockProvider;
import com.laderrco.fortunelink.portfolio.application.utils.PositionRecalculationExecutor;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class PositionRecalculationService {
  private static final Logger log = LoggerFactory.getLogger(PositionRecalculationService.class);
  private static final Duration DEBOUNCE_WINDOW = Duration.ofSeconds(3);
  private final StringRedisTemplate redisTemplate;
  private final PositionRecalculationExecutor executor;
  private final AccountHealthService accountHealthService;
  private final DistributedLockProvider lockProvider;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async("recalculationExecutor")
  public void onRecalculationRequested(PositionRecalculationRequestedEvent event) {
    String key = String.format("recalc:%s:%s", event.accountId().id(), event.symbol().symbol());

    String token = UUID.randomUUID().toString();

    Boolean isFirst = redisTemplate.opsForValue().setIfAbsent(key, token, DEBOUNCE_WINDOW);

    if (Boolean.TRUE.equals(isFirst)) {
      schedule(event, key, token);
    } else {
      redisTemplate.opsForValue().set(key, token, DEBOUNCE_WINDOW);
    }
  }

  private void schedule(PositionRecalculationRequestedEvent event, String key, String token) {
    CompletableFuture.delayedExecutor(DEBOUNCE_WINDOW.toMillis(), TimeUnit.MILLISECONDS)
        .execute(() -> runIfLatest(event, key, token));
  }

  private void runIfLatest(PositionRecalculationRequestedEvent event, String key, String token) {
    String current = redisTemplate.opsForValue().get(key);

    if (!token.equals(current)) {
      // A newer event replaced this one → skip
      return;
    }

    // We are the latest → delete key and execute
    redisTemplate.delete(key);

    try {
      acquireAndRun(String.format("lock:account:%s", event.accountId().id()), event);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Interrupted", e);
    }
  }

  private void acquireAndRun(String lockKey, PositionRecalculationRequestedEvent event)
      throws InterruptedException {

    DistributedLock lock = lockProvider.getLock(lockKey);
    boolean acquired = false;

    try {
      acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw e;
    } catch (Exception redisEx) {
      log.error("Redis unavailable. Marking account STALE.", redisEx);
      accountHealthService.markStale(event.accountId());
      return;
    }

    if (acquired) {
      try {
        runRecalculation(event);
      } finally {
        try {
          lock.unlock();
        } catch (Exception e) {
          log.debug("Lock already released or expired.");
        }
      }
    } else {
      log.warn("Lock busy for accountId={}", event.accountId());
    }
  }

  private void runRecalculation(PositionRecalculationRequestedEvent event) {
    try {
      executor.scheduleRecalculation(event.portfolioId(), event.userId(), event.accountId(),
          event.symbol());
    } catch (Exception e) {
      log.error("Recalculation failed for accountId={} symbol={}", event.accountId(),
          event.symbol().symbol(), e);
      accountHealthService.markStale(event.accountId());
      throw e;
    }
  }
}
