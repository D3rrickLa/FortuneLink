package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionPurgeService {
  private static final Logger log = LoggerFactory.getLogger(TransactionPurgeService.class);
  private static final int DEFAULT_RETENTION_DAYS = 365;

  private final TransactionRepository transactionRepository;
  private final int retentionDays;

  // @Value here istead of var because it can be 0 in unit testing + that
  // annotation only applies
  // when Spring resolves the property
  public TransactionPurgeService(TransactionRepository transactionRepository, @Value(
      "${fortunelink.purge.excluded-transaction-retention-days:" + DEFAULT_RETENTION_DAYS
          + "}") int retentionDays) {
    this.transactionRepository = transactionRepository;
    this.retentionDays = retentionDays;
  }

  @Scheduled(cron = "0 0 0 * * *") // midnight every night
  @Transactional
  public void purgeExpiredTransactions() {
    try {
      Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
      int deleted = transactionRepository.deleteAllExpiredTransactions(cutoff);
      if (deleted > 0) {
        log.info("Purged {} excluded transactions (cutoff={}, retention={}d)", deleted, cutoff,
            retentionDays);
      }
    } catch (Exception e) {
      // Don't rethrow - let the scheduler continue running on future ticks.
      // In production you'd fire an alert here.
      log.error("Transaction purge failed - manual review required", e);
    }
  }
}
