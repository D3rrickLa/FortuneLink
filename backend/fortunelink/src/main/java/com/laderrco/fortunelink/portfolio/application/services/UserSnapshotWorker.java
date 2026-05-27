package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.utils.PortfolioAccessUtils;
import com.laderrco.fortunelink.portfolio.application.views.ValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.AccountValuationSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ValuationSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.AccountValuationSnapshotRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.ValuationSnapshotRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSnapshotWorker {
  private final PortfolioRepository portfolioRepository;
  private final ValuationSnapshotRepository snapshotRepository;
  private final AccountValuationSnapshotRepository accountSnapshotRepository;
  private final MarketDataService marketDataService;
  private final PortfolioValuationService portfolioValuationService;

  /**
   * creates a snapshot or the user's portfolio. the snapshot now gets total cost
   * basis, total cash
   * balance, and total invested value
   *
   * @param userId is the user
   * @return boolean if there is a snapshot
   */
  @Transactional
  public boolean snapshotForUser(UserId userId) {
    if (snapshotRepository.existsForToday(userId)) {
      log.debug("Snapshot already exists today for userId={}", userId);
      return false;
    }

    List<Portfolio> portfolios = portfolioRepository.findAllActiveByUserId(userId);
    if (portfolios.isEmpty())
      return false;

    Set<AssetSymbol> allSymbols = portfolios.stream()
        .flatMap(p -> PortfolioAccessUtils.extractSymbols(p).stream())
        .collect(Collectors.toSet());

    Map<AssetSymbol, MarketAssetQuote> quoteCache = allSymbols.isEmpty()
        ? Map.of()
        : marketDataService.getBatchQuotes(allSymbols);

    Currency displayCurrency = portfolios.getFirst().getDisplayCurrency();

    ValuationView valuation = portfolioValuationService
        .calculateUserValuation(portfolios, displayCurrency, quoteCache);

    // User snapshot commits first — account snapshots reference this as the
    // authoritative user-level record for the day
    snapshotRepository.save(ValuationSnapshot.fromView(userId, valuation));

    // Account snapshots run after — a failure here is isolated per account
    snapshotAccountsForUser(portfolios, quoteCache);

    return true;
  }

  private void snapshotAccountsForUser(
      List<Portfolio> portfolios,
      Map<AssetSymbol, MarketAssetQuote> quoteCache) {

    LocalDate today = LocalDate.now(ZoneOffset.UTC);

    for (Portfolio portfolio : portfolios) {
      for (Account account : portfolio.getAccounts()) {
        AccountId accountId = account.getAccountId();

        if (accountSnapshotRepository.existsByAccountIdAndSnapshotDate(accountId, today)) {
          log.debug("Account snapshot already exists today for accountId={}", accountId);
          continue;
        }

        try {
          ValuationView view = portfolioValuationService
              .calculateAccountValuation(account, quoteCache);
          accountSnapshotRepository.save(AccountValuationSnapshot.fromView(accountId, view));
        } catch (Exception e) {
          // Isolated — user snapshot already committed above
          log.warn("Account snapshot failed for accountId={}: {}", accountId, e.getMessage());
        }
      }
    }
  }
}