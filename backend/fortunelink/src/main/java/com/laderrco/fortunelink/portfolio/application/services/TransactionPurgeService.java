package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class TransactionPurgeService {
    private final TransactionRepository transactionRepository;
    private final Logger log = LoggerFactory.getLogger(TransactionPurgeService.class);

    @Scheduled(cron = "0 0 0 * * *") // midnight every night
    @Transactional
    public void purgeExpiredTransactions() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        int deleted = transactionRepository.deleteAllExpiredTransactions(cutoff); // global cutoff
        log.info("Purged {} expired excluded transactions older than {}", deleted, cutoff);
    }
}
