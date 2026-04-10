package com.laderrco.fortunelink.portfolio.application.services;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
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

/**
 * Writes one net worth snapshot per user per day at 02:00 UTC.
 *
 * Why 02:00 and not midnight?
 * - Market quotes update throughout the day. By 02:00 UTC, North American
 * markets are closed and end-of-day prices are available.
 * - Redis TTL on quotes is 5 minutes. The snapshot uses whatever price is
 * current at snapshot time, which will be yesterday's close.
 *
 * Failure handling:
 * - A single user's failure does not abort the job. Log and continue.
 * - If Redis is down, getBatchQuotes() returns cost-basis fallback
 * (hasStaleData = true on the snapshot). That's acceptable — better
 * a stale snapshot than no snapshot.
 *
 * Idempotency:
 * - The UNIQUE INDEX on (user_id, DATE(snapshot_date)) prevents duplicate
 * rows if the job fires twice. Use INSERT ... ON CONFLICT DO NOTHING
 * in the repository.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NetWorthSnapshotService {
  private final PortfolioRepository portfolioRepository;
  private final NetWorthSnapshotRepository snapshotRepository;
  private final MarketDataService marketDataService;
  private final PortfolioValuationService portfolioValuationService;

  // Use @Autowired on a field or setter for self-injection
  @Autowired
  private NetWorthSnapshotService self;

  @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
  public void snapshotAllUsers() {
    List<UserId> activeUsers = portfolioRepository.findAllActiveUserIds();
    log.info("Net worth snapshot job started for {} users", activeUsers.size());

    int success = 0, skipped = 0, failed = 0;
    for (UserId userId : activeUsers) {
      try {
        // FIX: Use 'self' to trigger @Transactional proxy
        boolean wrote = self.snapshotForUser(userId);
        if (wrote)
          success++;
        else
          skipped++;
      } catch (Exception e) {
        failed++;
        log.error("Snapshot failed for userId={}: {}", userId, e.getMessage(), e);
      }
    }
    log.info("Net worth snapshot job complete. success={}, skipped={}, failed={}", success, skipped, failed);
  }

  /**
   * Exposed for on-demand snapshot (e.g., triggered when a user first signs up
   * or completes their first import). Returns false if a snapshot already exists
   * today for this user.
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

    // Single quote fetch for all symbols across all portfolios
    Set<AssetSymbol> allSymbols = portfolios.stream()
        .flatMap(p -> PortfolioAccessUtils.extractSymbols(p).stream())
        .collect(Collectors.toSet());

    Map<AssetSymbol, MarketAssetQuote> quoteCache = allSymbols.isEmpty()
        ? Map.of()
        : marketDataService.getBatchQuotes(allSymbols);

    // Use the first portfolio's display currency as the user's "home" currency.
    // When multi-portfolio ships, you'll want a user-level currency preference.
    var primary = portfolios.get(0);
    var displayCurrency = primary.getDisplayCurrency();

    Money totalAssets = portfolios.stream()
        .map(p -> portfolioValuationService.calculateTotalValue(p, displayCurrency, quoteCache))
        .reduce(Money.zero(displayCurrency), Money::add);

    Money totalLiabilities = Money.zero(displayCurrency); // MVP stub

    boolean hasStale = portfolios.stream()
        .flatMap(p -> p.getAccounts().stream())
        .anyMatch(Account::isStale);

    var snapshot = NetWorthSnapshot.create(userId, totalAssets, totalLiabilities, displayCurrency, hasStale);

    snapshotRepository.save(snapshot);
    return true;
  }
}