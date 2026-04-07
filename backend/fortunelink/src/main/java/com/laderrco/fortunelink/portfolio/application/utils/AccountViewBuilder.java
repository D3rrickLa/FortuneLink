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
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
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
  private final PortfolioViewMapper portfolioViewMapper;

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

    return portfolioViewMapper.toAccountView(account, positionViews, totalValue, cashBalance);
  }

  /**
   * Builds an AccountView from a summary projection (no full aggregate loaded).
   * <p>
   * Positions are not included in this path because the caller fetches quotes for
   * all accounts in
   * one batch and hands them in via {@code quotes}. If the account has no open
   * positions, it gets
   * an empty position list and its totalValue is just its cash balance.
   * <p>
   * Fee data is optional — pass {@code Map.of()} if not available.
   *
   * @param projection     lightweight DB projection, no positions loaded
   * @param allQuotes      quotes for ALL symbols across ALL accounts in this page
   * @param feesForAccount fees keyed by symbol for this specific account
   */
  public AccountView buildFromProjection(AccountSummaryProjection projection,
      Map<AssetSymbol, MarketAssetQuote> allQuotes, Map<AssetSymbol, Money> feesForAccount) {

    AccountId accountId = AccountId.fromString(projection.getId().toString());
    Currency currency = Currency.of(projection.getBaseCurrencyCode());
    Money cashBalance = new Money(projection.getCashBalanceAmount(), currency);

    // Without loading the full Account aggregate we don't have position objects.
    // For the list view this is fine — return empty positions.
    // getAccountSummary (single account) still uses the full build() path with real
    // positions.
    List<PositionView> positionViews = List.of();
    Money totalValue = cashBalance; // positions would add to this; revisit if list needs market value

    return new AccountView(accountId, projection.getName(),
        AccountType.valueOf(projection.getAccountType()), AccountLifecycleState.valueOf(projection.getLifecycleState()),
        positionViews, currency, cashBalance, totalValue, projection.getCreatedDate());
  }

  /**
   * Builds an AccountView without fee data - for summary screens where tax
   * breakdown is not needed.
   * Avoids the extra transaction fetch. totalFeesIncurred will be Price.zero on
   * all PositionViews.
   */
  public AccountView buildSummary(Account account, Map<AssetSymbol, MarketAssetQuote> quoteCache) {
    List<PositionView> positionViews = account.getPositionEntries().stream().map(
        entry -> portfolioViewMapper.toPositionView(entry.getValue(),
            quoteCache.get(entry.getKey())))
        .toList();

    Money totalValue = portfolioValuationService.calculateAccountValue(account, quoteCache);
    Money cashBalance = account.getCashBalance();

    return portfolioViewMapper.toAccountView(account, positionViews, totalValue, cashBalance);
  }
}