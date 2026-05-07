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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Pure math implementation of PortfolioValuationService.
 * <p>
 * Never calls MarketDataService. All quotes are pre-fetched by the calling
 * application service and
 * passed in via quoteCache.
 */
@Service
@RequiredArgsConstructor
public final class PortfolioValuationServiceImpl implements PortfolioValuationService {
  private final ExchangeRateService exchangeRateService;

  // =========================================================
  // PUBLIC API (ONLY ENTRY POINTS)
  // =========================================================

  @Override
  public ValuationView calculateAccountValuation(Account account, Map<AssetSymbol, MarketAssetQuote> quoteCache) {
    Objects.requireNonNull(account, "Account cannot be null");
    Objects.requireNonNull(quoteCache, "Quote cache cannot be null");

    Money positions = calculatePositionsValueInternal(account, quoteCache);
    Money cash = account.isActive() ? account.getCashBalance() : Money.zero(account.getAccountCurrency());
    Money total = positions.add(cash);

    return buildValuation(total, cash, positions, account.getAccountCurrency());
  }

  @Override
  public ValuationView calculatePortfolioValuation(Portfolio portfolio, Currency targetCurrency,
      Map<AssetSymbol, MarketAssetQuote> quoteCache) {
    Objects.requireNonNull(portfolio, "Portfolio cannot be null");
    Objects.requireNonNull(targetCurrency, "Target currency cannot be null");
    Objects.requireNonNull(quoteCache, "Quote cache cannot be null");

    Money positions = portfolio.getAccounts().stream()
        .filter(Account::isActive)
        .map(acc -> calculatePositionsValueInternal(acc, quoteCache))
        .map(m -> exchangeRateService.convert(m, targetCurrency))
        .reduce(Money.zero(targetCurrency), Money::add);

    Money cash = portfolio.getAccounts().stream()
        .filter(Account::isActive)
        .map(Account::getCashBalance)
        .map(m -> exchangeRateService.convert(m, targetCurrency))
        .reduce(Money.zero(targetCurrency), Money::add);

    Money total = positions.add(cash);

    return buildValuation(total, cash, positions, targetCurrency);
  }

  @Override
  public ValuationView calculateUserValuation(List<Portfolio> portfolios, Currency targetCurrency,
      Map<AssetSymbol, MarketAssetQuote> quoteCache) {

    Objects.requireNonNull(portfolios, "Portfolios cannot be null");
    Objects.requireNonNull(targetCurrency, "Target currency cannot be null");
    Objects.requireNonNull(quoteCache, "Quote cache cannot be null");

    Money positions = portfolios.stream()
        .flatMap(p -> p.getAccounts().stream())
        .filter(Account::isActive)
        .map(acc -> calculatePositionsValueInternal(acc, quoteCache))
        .map(m -> exchangeRateService.convert(m, targetCurrency))
        .reduce(Money.zero(targetCurrency), Money::add);

    Money cash = portfolios.stream()
        .flatMap(p -> p.getAccounts().stream())
        .filter(Account::isActive)
        .map(Account::getCashBalance)
        .map(m -> exchangeRateService.convert(m, targetCurrency))
        .reduce(Money.zero(targetCurrency), Money::add);

    Money total = positions.add(cash);

    return buildValuation(total, cash, positions, targetCurrency);
  }

  // =========================================================
  // INTERNAL CORE LOGIC (MATH ENGINE)
  // =========================================================

  private Money calculatePositionsValueInternal(Account account,
      Map<AssetSymbol, MarketAssetQuote> quoteCache) {

    Currency accountCurrency = account.getAccountCurrency();

    return account.getPositionEntries().stream()
        .filter(e -> e.getValue().type() != AssetType.CASH)
        .map(e -> resolvePositionValue(
            e.getValue(),
            quoteCache.get(e.getKey()),
            accountCurrency))
        .reduce(Money::add)
        .orElse(Money.zero(accountCurrency));
  }

  private Money resolvePositionValue(Position position,
      MarketAssetQuote quote,
      Currency accountCurrency) {

    if (quote == null
        || quote.currentPrice() == null
        || quote.currentPrice().pricePerUnit().isZero()) {
      return position.totalCostBasis();
    }

    Price currentPrice = quote.currentPrice();

    if (!currentPrice.currency().equals(accountCurrency)) {
      Money converted = exchangeRateService.convert(
          currentPrice.pricePerUnit(),
          accountCurrency);
      currentPrice = new Price(converted);
    }

    return position.currentValue(currentPrice);
  }

  // =========================================================
  // VALUE ASSEMBLY (SINGLE SOURCE OF TRUTH)
  // =========================================================

  private ValuationView buildValuation(Money total,
      Money cash,
      Money positions,
      Currency currency) {

    return ValuationView.builder()
        .totalValue(total)
        .totalCashBalance(cash)
        .totalInvestedValue(positions)
        .displayCurrency(currency)
        .build();
  }
}