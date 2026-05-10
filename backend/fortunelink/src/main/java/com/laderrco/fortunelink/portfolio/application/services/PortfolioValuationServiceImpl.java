package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.views.ValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Pure math implementation of PortfolioValuationService.
 * <p>
 * Never calls MarketDataService. All quotes are pre-fetched by the calling application service and
 * passed in via quoteCache.
 */
@Service
@RequiredArgsConstructor
public final class PortfolioValuationServiceImpl implements PortfolioValuationService {
  private final ExchangeRateService exchangeRateService;

  // =========================================================
  // PUBLIC API
  // =========================================================

  @Override
  public ValuationView calculateAccountValuation(Account account,
      Map<AssetSymbol, MarketAssetQuote> quoteCache) {
    Objects.requireNonNull(account, "Account cannot be null");
    Objects.requireNonNull(quoteCache, "Quote cache cannot be null");

    Currency currency = account.getAccountCurrency();

    Money positions = calculatePositionsValueInternal(account, quoteCache);
    Money costBasis = calculatePositionsCostBasisInternal(account, currency);
    Money cash = account.isActive() ? account.getCashBalance() : Money.zero(currency);
    Money total = positions.add(cash);
    boolean isStale = account.isStale();

    return buildValuation(total, costBasis, cash, positions, currency, isStale);
  }

  @Override
  public ValuationView calculatePortfolioValuation(Portfolio portfolio, Currency targetCurrency,
      Map<AssetSymbol, MarketAssetQuote> quoteCache) {
    Objects.requireNonNull(portfolio, "Portfolio cannot be null");
    Objects.requireNonNull(targetCurrency, "Target currency cannot be null");
    Objects.requireNonNull(quoteCache, "Quote cache cannot be null");

    List<Account> activeAccounts = portfolio.getAccounts().stream().filter(Account::isActive)
        .toList();

    Money positions = activeAccounts.stream()
        .map(acc -> calculatePositionsValueInternal(acc, quoteCache))
        .map(m -> exchangeRateService.convert(m, targetCurrency)).filter(Objects::nonNull)
        .reduce(Money.zero(targetCurrency), Money::add);

    Money costBasis = activeAccounts.stream()
        .map(acc -> calculatePositionsCostBasisInternal(acc, targetCurrency))
        .reduce(Money.zero(targetCurrency), Money::add);

    Money cash = activeAccounts.stream().map(Account::getCashBalance)
        .map(m -> exchangeRateService.convert(m, targetCurrency))
        .reduce(Money.zero(targetCurrency), Money::add);

    Money total = positions.add(cash);
    boolean isStale = activeAccounts.stream().anyMatch(Account::isStale);

    return buildValuation(total, costBasis, cash, positions, targetCurrency, isStale);
  }

  @Override
  public ValuationView calculateUserValuation(List<Portfolio> portfolios, Currency targetCurrency,
      Map<AssetSymbol, MarketAssetQuote> quoteCache) {
    Objects.requireNonNull(portfolios, "Portfolios cannot be null");
    Objects.requireNonNull(targetCurrency, "Target currency cannot be null");
    Objects.requireNonNull(quoteCache, "Quote cache cannot be null");

    List<Account> activeAccounts = portfolios.stream().flatMap(p -> p.getAccounts().stream())
        .filter(Account::isActive).toList();

    Money positions = activeAccounts.stream()
        .map(acc -> calculatePositionsValueInternal(acc, quoteCache))
        .map(m -> exchangeRateService.convert(m, targetCurrency))
        .reduce(Money.zero(targetCurrency), Money::add);

    Money costBasis = activeAccounts.stream()
        .map(acc -> calculatePositionsCostBasisInternal(acc, targetCurrency))
        .reduce(Money.zero(targetCurrency), Money::add);

    Money cash = activeAccounts.stream().map(Account::getCashBalance)
        .map(m -> exchangeRateService.convert(m, targetCurrency))
        .reduce(Money.zero(targetCurrency), Money::add);

    Money total = positions.add(cash);
    boolean isStale = activeAccounts.stream().anyMatch(Account::isStale);

    return buildValuation(total, costBasis, cash, positions, targetCurrency, isStale);
  }

  // =========================================================
  // INTERNAL CORE LOGIC
  // =========================================================

  private Money calculatePositionsCostBasisInternal(Account account, Currency targetCurrency) {
    return account.getPositionEntries().stream().filter(e -> e.getValue().type() != AssetType.CASH)
        .map(e -> {
          Money costBasis = e.getValue().totalCostBasis();
          return costBasis.currency().equals(targetCurrency) ? costBasis
              : exchangeRateService.convert(costBasis, targetCurrency);
        }).filter(Objects::nonNull).reduce(Money::add).orElse(Money.zero(targetCurrency));
  }

  private Money calculatePositionsValueInternal(Account account,
      Map<AssetSymbol, MarketAssetQuote> quoteCache) {
    Currency accountCurrency = account.getAccountCurrency();

    return account.getPositionEntries().stream().filter(e -> e.getValue().type() != AssetType.CASH)
        .map(e -> resolvePositionValue(e.getValue(), quoteCache.get(e.getKey()), accountCurrency))
        .reduce(Money::add).orElse(Money.zero(accountCurrency));
  }

  private Money resolvePositionValue(Position position, MarketAssetQuote quote,
      Currency accountCurrency) {
    if (quote == null || quote.currentPrice() == null || quote.currentPrice().pricePerUnit()
        .isZero()) {
      return position.totalCostBasis(); // stale fallback — intentional
    }

    Price currentPrice = quote.currentPrice();

    if (!currentPrice.currency().equals(accountCurrency)) {
      Money converted = exchangeRateService.convert(currentPrice.pricePerUnit(), accountCurrency);
      currentPrice = new Price(converted);
    }

    return position.currentValue(currentPrice);
  }

  // =========================================================
  // VALUE ASSEMBLY
  // =========================================================

  private ValuationView buildValuation(Money total, Money costBasis, Money cash, Money positions,
      Currency currency, boolean hasStaleData) {
    return ValuationView.of(total, costBasis, cash, positions, currency, hasStaleData,
        Instant.now());
  }
}