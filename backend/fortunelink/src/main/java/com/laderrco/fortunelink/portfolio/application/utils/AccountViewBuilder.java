package com.laderrco.fortunelink.portfolio.application.utils;

import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.PositionView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountViewBuilder {
  private final PortfolioValuationService portfolioValuationService;
  private final PortfolioViewMapper portfolioViewMapper;
  private final TransactionRepository transactionRepository;

  public AccountView build(
      Account account,
      Map<AssetSymbol, MarketAssetQuote> quoteCache,
      Map<AssetSymbol, Money> feeBreakdownBySymbol) {

    List<PositionView> positionViews = account.getPositionEntries().stream()
        .map(entry -> {
          AssetSymbol symbol = entry.getKey();
          Money feesIncurred = feeBreakdownBySymbol.getOrDefault(
              symbol, Money.zero(account.getAccountCurrency()));
          return portfolioViewMapper.toPositionView(
              entry.getValue(), quoteCache.get(symbol), feesIncurred);
        }).toList();

    Money totalValue = portfolioValuationService.calculateAccountValue(account, quoteCache);
    Money cashBalance = account.getCashBalance();

    // A cash imbalance exists when a transaction that moved cash was excluded.
    // The position was recalculated without it, but the cash balance was NOT
    // reversed.
    // Example: user excludes a BUY — they still "spent" the cash, but the position
    // is gone. Their displayed cash balance is therefore understated vs. their
    // position state.
    int excludedCount = transactionRepository.countExcludedPositionAffecting(
        account.getAccountId());
    boolean hasCashImbalance = excludedCount > 0;

    return portfolioViewMapper.toAccountView(
        account, positionViews, totalValue, cashBalance,
        hasCashImbalance, excludedCount);
  }

  /**
   * Summary build — no fee data, no cash imbalance check.
   * Use this for the portfolio list view where you don't need the detail.
   */
  public AccountView buildSummary(
      Account account,
      Map<AssetSymbol, MarketAssetQuote> quoteCache) {

    List<PositionView> positionViews = account.getPositionEntries().stream()
        .map(entry -> portfolioViewMapper.toPositionView(
            entry.getValue(), quoteCache.get(entry.getKey())))
        .toList();

    Money totalValue = portfolioValuationService.calculateAccountValue(account, quoteCache);
    Money cashBalance = account.getCashBalance();

    // Summary path — skip the extra query.
    // hasCashImbalance defaults false; the detail view will show the warning.
    return portfolioViewMapper.toAccountView(
        account, positionViews, totalValue, cashBalance, false, 0);
  }
}