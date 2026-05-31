package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.utils.PortfolioAccessUtils;
import com.laderrco.fortunelink.portfolio.application.views.ValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.*;
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
   * Creates or refreshes today's valuation snapshots for a user.
   *
   * If a snapshot already exists for today it is replaced with the latest
   * calculated valuation.
   */
  @Transactional
  public boolean snapshotForUser(UserId userId) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);

    List<Portfolio> portfolios = portfolioRepository.findAllActiveByUserId(userId);

    if (portfolios.isEmpty()) {
      log.debug("No portfolios found for userId={}", userId);
      return false;
    }

    Set<AssetSymbol> symbols = portfolios.stream()
        .flatMap(p -> PortfolioAccessUtils.extractSymbols(p).stream())
        .collect(Collectors.toSet());

    Map<AssetSymbol, MarketAssetQuote> quoteCache = symbols.isEmpty()
        ? Map.of()
        : marketDataService.getBatchQuotes(symbols);

    Currency displayCurrency = portfolios.getFirst().getDisplayCurrency();

    ValuationView userValuation = portfolioValuationService.calculateUserValuation(
        portfolios,
        displayCurrency,
        quoteCache);

    upsertUserSnapshot(userId, today, userValuation);

    upsertAccountSnapshots(
        portfolios,
        quoteCache,
        today);

    return true;
  }

  private void upsertUserSnapshot(UserId userId, LocalDate today, ValuationView valuation) {

    ValuationSnapshot snapshot = ValuationSnapshot.fromView(userId, valuation);

    snapshotRepository.save(snapshot); // repository handles upsert

    log.debug("Upserted user snapshot for userId={} date={}", userId, today);
  }

  private void upsertAccountSnapshots(
      List<Portfolio> portfolios,
      Map<AssetSymbol, MarketAssetQuote> quoteCache,
      LocalDate today) {

    for (Portfolio portfolio : portfolios) {

      for (Account account : portfolio.getAccounts()) {

        if (!account.isActive()) {
          continue;
        }

        try {

          ValuationView view = portfolioValuationService.calculateAccountValuation(account, quoteCache);

          AccountValuationSnapshot snapshot = AccountValuationSnapshot.fromView(account.getAccountId(), view);

          accountSnapshotRepository.save(snapshot); // dempotent now

        } catch (Exception e) {
          log.warn(
              "Failed account snapshot accountId={}: {}",
              account.getAccountId(),
              e.getMessage(),
              e);
        }
      }
    }
  }
}