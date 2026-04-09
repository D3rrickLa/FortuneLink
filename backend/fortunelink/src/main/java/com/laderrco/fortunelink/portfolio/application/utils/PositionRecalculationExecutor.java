package com.laderrco.fortunelink.portfolio.application.utils;

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
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PositionRecalculationExecutor {
  private static final Logger log = LoggerFactory.getLogger(PositionRecalculationExecutor.class);

  private final PortfolioRepository portfolioRepository;
  private final TransactionRepository transactionRepository;
  private final TransactionRecordingService transactionRecordingService;
  private final AccountHealthService accountHealthService;
  private final PortfolioLoader portfolioLoader;

  /**
   * Surgical recalculation for a single symbol.` Corrects ACB/Position but leaves Cash Balance
   * as-is.
   * <p>
   * This filters to affectsHolding() before calling replayTransaction()
   */
  @Transactional
  public void scheduleRecalculation(PortfolioId portfolioId, UserId userId, AccountId accountId,
      AssetSymbol symbol) {
    Portfolio portfolio = portfolioLoader.loadUserPortfolio(portfolioId, userId);
    Account account = portfolio.getAccount(accountId);

    List<Transaction> active = transactionRepository.findByAccountIdAndSymbol(accountId, symbol)
        .stream().filter(tx -> !tx.isExcluded())
        .filter(tx -> tx.transactionType().affectsHoldings())
        .sorted(Comparator.comparing(Transaction::occurredAt)).toList();

    try {
      // Clear only this symbol's position and its realized gains
      // NOTE: had to make these public
      account.clearPositionForRecalculation(symbol);
      account.clearRealizedGainsForSymbol(symbol);

      account.beginReplay();
      active.forEach(tx -> transactionRecordingService.replayTransaction(account, tx));
      account.endReplay();
      portfolio.reportRecalculationSuccess(accountId);
    } catch (Exception e) {
      log.error("Recalculation failed for account {} symbol {}", accountId, symbol, e);
      accountHealthService.markStale(accountId);
      throw e;
    }

    portfolioRepository.save(portfolio);
  }

  /**
   * Full account recovery. Resets everything to zero and re-runs history.
   */
  @Transactional
  public void replayFullAccount(PortfolioId portfolioId, UserId userId, AccountId accountId) {
    Portfolio portfolio = portfolioLoader.loadUserPortfolio(portfolioId, userId);
    Account account = portfolio.getAccount(accountId);

    List<Transaction> allActive = transactionRepository.findByPortfolioIdAndUserIdAndAccountId(
            portfolioId, userId, accountId).stream().filter(tx -> !tx.isExcluded())
        .sorted(Comparator.comparing(Transaction::occurredAt)).toList();

    try {
      // don't need to clear pos, cash, and gains as internally it does that
      transactionRecordingService.replayFullTransaction(account, allActive);
      portfolio.reportRecalculationSuccess(accountId);
    } catch (Exception e) {
      log.error("Full account replay failed for account {}", accountId, e);
      accountHealthService.markStale(accountId);
      throw e; // rollback the transaction, don't commit partial state
    }

    portfolioRepository.save(portfolio);
  }
}
