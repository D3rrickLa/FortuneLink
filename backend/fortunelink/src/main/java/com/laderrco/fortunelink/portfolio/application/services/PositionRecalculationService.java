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
    private final AccountHealthService accountHealthService;

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

            accountHealthService.markStale(event.portfolioId(), event.userId(), event.accountId());
        }
    }

    /**
     * Surgical recalculation for a single symbol.`
     * Corrects ACB/Position but leaves Cash Balance as-is.
     * 
     * This filters to affectsHolding() before calling replayTransaction()
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

        try {
            account.clearPosition(symbol);
            account.clearRealizedGains(symbol);
            active.forEach(tx -> transactionRecordingService.replayTransaction(account, tx));
            portfolio.reportRecalculationSuccess(accountId);
        } catch (Exception e) {
            log.error("Recalculation failed for account {} symbol {}", accountId, symbol, e);
            accountHealthService.markStale(portfolioId, userId, accountId); // mark it dirty
            throw e; // let @Transactional roll back — don't persist partial state
        }

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

        try {
            account.clearAllPositions();
            account.resetCashToZero();
            account.clearAllRealizedGains();
            allActive.forEach(tx -> transactionRecordingService.replayFullTransaction(account, tx));
            portfolio.reportRecalculationSuccess(accountId);
        } catch (Exception e) {
            log.error("Full account replay failed for account {}", accountId, e);
            accountHealthService.markStale(portfolioId, userId, accountId);
            throw e; // rollback the transaction, don't commit partial state
        }

        portfolioRepository.save(portfolio);
    }

    private Portfolio loadPortfolio(PortfolioId portfolioId, UserId userId) {
        return portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
    }

}
