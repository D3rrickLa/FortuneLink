package com.laderrco.fortunelink.portfolio.application.services;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.laderrco.fortunelink.portfolio.application.events.PositionRecalculationRequestedEvent;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.TransactionRecordingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionRecalculationService {
    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionRecordingService transactionRecordingService;
    private Logger log;

    /**
     * it's phase = AFTER_COMMMIT because this ensures
     * listener only fires after the publishing transaction has
     * fully committed to the DB
     * 
     * NOTE: IF THIS FAILS, it fails silently - no retry, no dead letter queue, etc.
     * 
     * Should log and replace with proper alert system
     * 
     * @param event
     */
    @Async("recalculationExecutor")
    @Transactional // its own transaction — reads committed state
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRecalculationRequested(PositionRecalculationRequestedEvent event) {
        try {
            scheduleRecalculation(
                    event.portfolioId(),
                    event.userId(),
                    event.accountId(),
                    event.symbol());
        } catch (Exception e) {
            // TODO: replace with proper alerting before production
            log.error("Position recalculation failed for account={} symbol={}: {}",
                    event.accountId(), event.symbol(), e.getMessage(), e);
        }
    }

    @Transactional
    public void scheduleRecalculation(PortfolioId portfolioId, UserId userId, AccountId accountId, AssetSymbol symbol) {
        List<Transaction> active = transactionRepository
                .findByAccountIdAndSymbol(accountId, symbol)
                .stream()
                .filter(tx -> !tx.isExcluded())
                .sorted(Comparator.comparing(tx -> tx.occurredAt().timestamp()))
                .toList();

        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        Account account = portfolio.getAccount(accountId);
        account.clearPosition(symbol); // reset to zero
        active.forEach(tx -> transactionRecordingService.replayTransaction(account, tx));
        portfolioRepository.save(portfolio); // persist corrected state
    }
}
