package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionPurgeService {
  private final TransactionRepository transactionRepository;
  private final Logger log = LoggerFactory.getLogger(TransactionPurgeService.class);

  /**
   * Default is 365 days. Financial data should never disappear on a 30-day timer with no user
   * warning -> a user who excludes a transaction by mistake and misses a 30-day window loses it
   * permanently, corrupting their ACB history.
   * <p>
   * Override in application.yml: fortunelink.purge.excluded-transaction-retention-days: 365
   * <p>
   * If you later want to warn users before purge, compare excludedAt against (cutoff +
   * warning-threshold-days) and surface it in the UI before deletion.
   */
  @Value("${fortunelink.purge.excluded-transaction-retention-days:365}")
  private int retentionDays;

  @Scheduled(cron = "0 0 0 * * *") // midnight every night
  @Transactional
  public void purgeExpiredTransactions() {
    Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
    int deleted = transactionRepository.deleteAllExpiredTransactions(cutoff);
    log.info("Purged {} excluded transactions excluded before {} ({} day retention)", deleted,
        cutoff, retentionDays);
  }
}
