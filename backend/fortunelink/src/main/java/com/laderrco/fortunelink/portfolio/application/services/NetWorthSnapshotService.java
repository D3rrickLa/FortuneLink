package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Writes one net worth snapshot per user per day at 02:00 UTC.
 * <p>
 * Why 02:00 and not midnight? - Market quotes update throughout the day. By 02:00 UTC, North
 * American markets are closed and end-of-day prices are available. - Redis TTL on quotes is 5
 * minutes. The snapshot uses whatever price is current at snapshot time, which will be yesterday's
 * close.
 * <p>
 * Failure handling: - A single user's failure does not abort the job. Log and continue. - If Redis
 * is down, getBatchQuotes() returns cost-basis fallback (hasStaleData = true on the snapshot).
 * That's acceptable , better a stale snapshot than no snapshot.
 * <p>
 * Idempotency: - The UNIQUE INDEX on (user_id, DATE(snapshot_date)) prevents duplicate rows if the
 * job fires twice. Use INSERT ... ON CONFLICT DO NOTHING in the repository.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NetWorthSnapshotService {
  private final PortfolioRepository portfolioRepository;
  private final UserSnapshotWorker worker; // Inject the new worker

  @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
  public void snapshotAllUsers() {
    List<UserId> activeUsers = portfolioRepository.findAllActiveUserIds();
    log.info("Net worth snapshot job started for {} users", activeUsers.size());

    int success = 0, skipped = 0, failed = 0;
    for (UserId userId : activeUsers) {
      try {
        // Calling a different bean naturally triggers the @Transactional proxy
        if (worker.snapshotForUser(userId)) {
          success++;
        } else {
          skipped++;
        }
      } catch (Exception e) {
        failed++;
        log.error("Snapshot failed for userId={}: {}", userId, e.getMessage(), e);
      }
    }
    log.info("Net worth snapshot job complete. success={}, skipped={}, failed={}", success, skipped,
        failed);
  }
}