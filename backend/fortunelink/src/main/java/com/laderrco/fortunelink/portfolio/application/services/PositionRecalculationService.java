package com.laderrco.fortunelink.portfolio.application.services;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PositionRecalculationService {

    private static final Logger log = LoggerFactory.getLogger(PositionRecalculationService.class);

    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionRecordingService transactionRecordingService;

    /**
     * Async listener that triggers after a transaction commit.
     * Ensures excluded/restored flags are persisted before we replay them.
     */
    @Async("recalculationExecutor")
    @Transactional // its own transaction — reads committed state
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRecalculationRequested(PositionRecalculationRequestedEvent event) {
        try {
            scheduleRecalculation(event.portfolioId(), event.userId(), event.accountId(), event.symbol());
        } catch (Exception e) {
            log.error("Position recalculation failed for account={} symbol={}: {}",
                    event.accountId(), event.symbol(), e.getMessage(), e);

            markAccountStale(event.portfolioId(), event.userId(), event.accountId());

        }
    }

    /**
     * Surgical recalculation for a single symbol.
     * Corrects ACB/Position but leaves Cash Balance as-is.
     */
    @Transactional
    public void scheduleRecalculation(PortfolioId portfolioId, UserId userId, AccountId accountId, AssetSymbol symbol) {
        Portfolio portfolio = loadPortfolio(portfolioId, userId);
        Account account = portfolio.getAccount(accountId);

        List<Transaction> active = transactionRepository
                .findByAccountIdAndSymbol(accountId, symbol)
                .stream()
                .filter(tx -> !tx.isExcluded())
                // EXPLICIT: only replay transactions that affect holdings.
                // Cash events (DEPOSIT, WITHDRAWAL, DIVIDEND, FEE, etc.) are
                // intentionally excluded — cash state is already correct in DB.
                // If you ever need full-account reconstruction, use the dedicated
                // replayFullAccount() path that resets cash to zero first.
                .filter(tx -> tx.transactionType().affectsHoldings())
                .sorted(Comparator.comparing(tx -> tx.occurredAt().timestamp()))
                .toList();

        account.clearPosition(symbol); // reset to zero

        active.forEach(tx -> transactionRecordingService.replayTransaction(account, tx));

        portfolio.reportRecalculationSuccess(accountId);
        portfolioRepository.save(portfolio);
    }

    /**
     * Full account recovery. Resets everything to zero and re-runs history.
     */
    @Transactional
    public void replayFullAccount(PortfolioId portfolioId, UserId userId, AccountId accountId) {
        Portfolio portfolio = loadPortfolio(portfolioId, userId);
        Account account = portfolio.getAccount(accountId);

        List<Transaction> allActive = transactionRepository
                .findByAccountId(accountId)
                .stream()
                .filter(tx -> !tx.isExcluded())
                .sorted(Comparator.comparing(tx -> tx.occurredAt().timestamp()))
                .toList();

        // Reset BOTH position and cash state before replay.
        // Order matters: clear all positions first, then reset cash.
        account.clearAllPositions();
        account.resetCashToZero();

        // Replay using a full-replay path, not replayTransaction (position-only).
        allActive.forEach(tx -> transactionRecordingService.replayFullTransaction(account, tx));

        portfolio.reportRecalculationSuccess(accountId);
        portfolioRepository.save(portfolio);
    }

    private Portfolio loadPortfolio(PortfolioId portfolioId, UserId userId) {
        return portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
    }

    private void markAccountStale(PortfolioId portfolioId, UserId userId, AccountId accountId) {
        try {
            Portfolio portfolio = loadPortfolio(portfolioId, userId);
            portfolio.reportRecalculationFailure(accountId);
            portfolioRepository.save(portfolio);
        } catch (Exception e) {
            log.error("Failed to mark account as stale. Manual intervention required.", e);
        }
    }


}
