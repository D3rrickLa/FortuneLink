package com.laderrco.fortunelink.portfolio.application.utils;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.application.services.AccountHealthService;
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
public class PositionRecalculationExecutor {
  private static final Logger log = LoggerFactory.getLogger(PositionRecalculationExecutor.class);

  private final PortfolioRepository portfolioRepository;
  private final TransactionRepository transactionRepository;
  private final TransactionRecordingService transactionRecordingService;
  private final AccountHealthService accountHealthService;

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

  private Portfolio loadPortfolio(PortfolioId portfolioId, UserId userId) {
    return portfolioRepository.findByIdAndUserId(portfolioId, userId)
        .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
  }
}
