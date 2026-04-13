package com.laderrco.fortunelink.portfolio.application.services;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio.application.utils.PortfolioAccessUtils;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.NetWorthSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.NetWorthSnapshotRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSnapshotWorker {
  private final PortfolioRepository portfolioRepository;
  private final NetWorthSnapshotRepository snapshotRepository;
  private final MarketDataService marketDataService;
  private final PortfolioValuationService portfolioValuationService;

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

    var primary = portfolios.get(0);
    var displayCurrency = primary.getDisplayCurrency();

    Money totalAssets = portfolios.stream()
        .map(p -> portfolioValuationService.calculateTotalValue(p, displayCurrency, quoteCache))
        .reduce(Money.zero(displayCurrency), Money::add);

    boolean hasStale = portfolios.stream()
        .flatMap(p -> p.getAccounts().stream())
        .anyMatch(Account::isStale);

    var snapshot = NetWorthSnapshot.create(userId, totalAssets, Money.zero(displayCurrency), displayCurrency, hasStale);
    snapshotRepository.save(snapshot);
    return true;
  }
}