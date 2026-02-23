package com.laderrco.fortunelink.portfolio.application.services;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    // Fix #3
    private static final Logger log = LoggerFactory.getLogger(PositionRecalculationService.class);

    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionRecordingService transactionRecordingService;

    /**
     * Async event listener — fires AFTER the publishing transaction commits.
     *
     * Phase = AFTER_COMMIT is critical: ensures the excluded/restored transaction
     * flag is visible in the DB before we read it back during recalculation.
     * Without this, the recalculation could replay using stale pre-commit state.
     *
     * IMPORTANT: This method only recalculates POSITION state (quantity, cost
     * basis).
     * It does NOT reset or replay cash balance. scheduleRecalculation calls
     * account.clearPosition() then replays only position-affecting transactions.
     * Cash balance is NOT touched — do not use this for cash corrections.
     *
     * Failure is logged but silent — no retry, no dead letter queue for MVP.
     * Replace log.error with proper alerting before going to production.
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

    /*
     * IMPORTANT: this only works for posistions the affect transaction, but the
     * replayTransaction also replays cash events - i.e. DEPOSITIS< WITHDRAWAL, etc.
     * 
     * recalcaulton does account.clearPosistion(symbol) then replays all
     * non-excluded
     * but the replay also calls the deposit and withdraw for cash events - those
     * cash changes get APPLIED AGAIN
     * 
     * we are douvle-counting cash on recalculation
     * 
     * replay was designed like that but schedule recalculation isn't clearing cash
     * state first - only position state
     * 
     * MVP - this won't trigger because recaulcation is only triggered by
     * exclude/restore on trade transaction
     * 
     * but it's a latent issue
     */
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
