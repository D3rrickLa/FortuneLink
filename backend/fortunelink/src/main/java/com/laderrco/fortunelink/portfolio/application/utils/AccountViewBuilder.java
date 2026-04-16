package com.laderrco.fortunelink.portfolio.application.utils;

import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.PositionView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountLifecycleState;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.projections.AccountSummaryProjection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountViewBuilder {
  private final PortfolioValuationService portfolioValuationService;
  private final ExchangeRateService exchangeRateService;
  private final PortfolioViewMapper portfolioViewMapper;
  private final TransactionRepository transactionRepository;

  /**
   * Full detail build , includes position views, fee breakdown, and cash imbalance check. Used by
   * getAccountSummary (single account detail page).
   *
   * <p>
   * This fires one extra DB query (countExcludedPositionAffecting) beyond the basic build. That's
   * acceptable for the detail path where the user explicitly navigated to an account. Do NOT use
   * this for the list path (getAllAccounts) , use {@link #buildFromProjection} there.
   */
  public AccountView build(Account account, Map<AssetSymbol, MarketAssetQuote> quoteCache,
      Map<AssetSymbol, Money> feeBreakdownBySymbol) {

    List<PositionView> positionViews = account.getPositionEntries().stream().map(entry -> {
      AssetSymbol symbol = entry.getKey();
      Money feesIncurred = feeBreakdownBySymbol.getOrDefault(symbol,
          Money.zero(account.getAccountCurrency()));
      return portfolioViewMapper.toPositionView(entry.getValue(), quoteCache.get(symbol),
          feesIncurred);
    }).toList();

    Money totalValue = portfolioValuationService.calculateAccountValue(account, quoteCache);
    Money cashBalance = account.getCashBalance();

    int excludedCount = transactionRepository.countExcludedPositionAffecting(
        account.getAccountId());

    return portfolioViewMapper.toAccountView(account, positionViews, totalValue, cashBalance,
        excludedCount > 0, excludedCount);
  }

  /**
   * Summary build from a lightweight projection , used by getAllAccounts (paginated list).
   *
   * <p>
   * Deliberately does NOT check for cash imbalance. The list view shows hasCashImbalance=false for
   * all accounts. The user gets the warning when they navigate into the account detail page (which
   * calls {@link #build}).
   *
   * <p>
   * This keeps getAllAccounts at O(3 queries total) regardless of account count: 1.
   * findByPortfolioId (accounts page) 2. findQuantitiesForAccounts (batch) 3. getBatchQuotes
   * (Redis/FMP)
   */
  public AccountView buildFromProjection(AccountSummaryProjection projection,
      Map<AssetSymbol, Quantity> quantities, Map<AssetSymbol, MarketAssetQuote> allQuotes,
      Map<AssetSymbol, Money> feesForAccount) {

    Currency currency = Currency.of(projection.getBaseCurrencyCode());
    Money cashBalance = new Money(projection.getCashBalanceAmount(), currency);

    // Money marketValue = quantities.entrySet().stream().map(entry -> {
    //   MarketAssetQuote quote = allQuotes.get(entry.getKey());
    //   if (quote == null || quote.currentPrice().isZero()) {
    //     return Money.zero(currency);
    //   }
    //   return quote.currentPrice().calculateValue(entry.getValue());
    // }).reduce(Money.zero(currency), Money::add);

    Money marketValue = quantities.entrySet().stream().map(entry -> {
      MarketAssetQuote quote = allQuotes.get(entry.getKey());
      if (quote == null || quote.currentPrice().isZero()) {
        return Money.zero(currency);
      }
      Money value = quote.currentPrice().calculateValue(entry.getValue());
      return exchangeRateService.convert(value, currency);
    }).reduce(Money.zero(currency), Money::add);

    Money totalValue = cashBalance.add(marketValue);

    return new AccountView(new AccountId(projection.getId()), projection.getName(),
        AccountType.valueOf(projection.getAccountType()),
        AccountLifecycleState.valueOf(projection.getLifecycleState()), List.of(),
        // positions not loaded on list view
        currency, cashBalance, totalValue, projection.getCreatedDate(), false,
        // imbalance check skipped on list view , see Javadoc
        0);
  }

  /**
   * Summary build from a full Account aggregate , used internally when the aggregate is already
   * loaded but fee detail is not needed (e.g., portfolio-level rollups).
   *
   * <p>
   * Same as buildFromProjection: no imbalance check, no fee breakdown.
   */
  public AccountView buildSummary(Account account, Map<AssetSymbol, MarketAssetQuote> quoteCache) {

    List<PositionView> positionViews = account.getPositionEntries().stream().map(
        entry -> portfolioViewMapper.toPositionView(entry.getValue(),
            quoteCache.get(entry.getKey()))).toList();

    Money totalValue = portfolioValuationService.calculateAccountValue(account, quoteCache);
    Money cashBalance = account.getCashBalance();

    return portfolioViewMapper.toAccountView(account, positionViews, totalValue, cashBalance, false,
        0);
  }
}
