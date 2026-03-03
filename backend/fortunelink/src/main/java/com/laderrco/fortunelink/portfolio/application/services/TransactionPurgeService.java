package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
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
    private final Logger logger = LoggerFactory.getLogger(TransactionPurgeService.class);

    // TODO we also need to add '@EnableScheduling' in the main app
    @Scheduled(cron = "0 0 0 * * *") // midnight every night
    @Transactional
    public void purgeExpiredTransactions(AccountId accountId) {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);

        int deletedCount = transactionRepository.deleteExpiredTransactions(
                accountId,
                cutoff
        );
        logger.info("Purged {} expired transactions for account {}", deletedCount, accountId);
    }
}
