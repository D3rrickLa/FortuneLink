package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.utils.PortfolioAccessUtils;
import com.laderrco.fortunelink.portfolio.application.views.ValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ValuationSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.ValuationSnapshotRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;
import jakarta.transaction.Transactional;
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
  private final MarketDataService marketDataService;
  private final PortfolioValuationService portfolioValuationService;

  /**
   * creates a snapshot or the user's portfolio. the snapshot now gets
   * total cost basis, total cash balance, and total invested value
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

    if (portfolios.isEmpty()) {
      return false;
    }

    Set<AssetSymbol> allSymbols = portfolios.stream()
        .flatMap(p -> PortfolioAccessUtils.extractSymbols(p).stream())
        .collect(Collectors.toSet());

    Map<AssetSymbol, MarketAssetQuote> quoteCache = allSymbols.isEmpty()
        ? Map.of()
        : marketDataService.getBatchQuotes(allSymbols);

    Currency displayCurrency = portfolios.getFirst().getDisplayCurrency();

    ValuationView valuation = portfolioValuationService.calculateUserValuation(portfolios, displayCurrency, quoteCache);

    ValuationSnapshot snapshot = ValuationSnapshot.fromView(userId, valuation);

    snapshotRepository.save(snapshot);

    return true;
  }
}