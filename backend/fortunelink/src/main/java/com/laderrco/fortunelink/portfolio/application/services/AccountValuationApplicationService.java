package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio.application.repositories.AccountQueryRepository;
import com.laderrco.fortunelink.portfolio.application.views.ValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.AccountValuationSnapshotRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountValuationApplicationService {
  private final AccountQueryRepository accountQueryRepository;
  private final AccountValuationSnapshotRepository accountSnapshotRepository;
  private final MarketDataService marketDataService;

  public ValuationView computeAccountValuation(GetAccountSummaryQuery query) {

    Account account = accountQueryRepository.findByIdWithDetails(query.accountId(),
        query.portfolioId(), query.userId())
        .orElseThrow(() -> new EntityNotFoundException("Account not found: " + query.accountId()));

    Currency currency = account.getAccountCurrency();

    var positions = account.getPositionEntries();

    /*
     * Fetch all quotes in one request
     */
    Set<AssetSymbol> symbols = positions.stream().map(entry -> entry.getValue().symbol())
        .collect(Collectors.toSet());

    Map<AssetSymbol, MarketAssetQuote> quotes = marketDataService.getBatchQuotes(symbols);

    Money totalCostBasis = positions.stream()
        .map(entry -> nullSafe(entry.getValue().totalCostBasis(), currency))
        .reduce(Money.zero(currency), Money::add);

    Money totalMarketValue = positions.stream().map(entry -> {

      var position = entry.getValue();

      MarketAssetQuote quote = quotes.get(position.symbol());

      if (quote == null) {
        return Money.zero(currency);
      }

      return nullSafe(position.currentValue(quote.currentPrice()), currency);
    }).reduce(Money.zero(currency), Money::add);

    Money unrealizedGainLoss = totalMarketValue.subtract(totalCostBasis);

    BigDecimal gainLossPercent = totalCostBasis.amount().compareTo(BigDecimal.ZERO) > 0 ? unrealizedGainLoss.amount()
        .divide(totalCostBasis.amount(), Precision.PERCENTAGE.getDecimalPlaces(),
            Rounding.PERCENTAGE.getMode())
        .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

    Money cashBalance = nullSafe(account.getCashBalance(), currency);

    Money totalAccountValue = totalMarketValue.add(cashBalance);

    return new ValuationView(
        totalAccountValue,
        totalCostBasis,
        unrealizedGainLoss,
        gainLossPercent,
        cashBalance,
        totalMarketValue,
        currency,
        false,
        Instant.now());
  }

  public List<ValuationView> getAccountValuationHistory(AccountId accountId, int days) {
    LocalDate after = LocalDate.now(ZoneOffset.UTC).minusDays(days);

    return accountSnapshotRepository
        .findByAccountIdAndSnapshotDateAfterOrderBySnapshotDateAsc(accountId, after)
        .stream()
        .map(snapshot -> new ValuationView(
            snapshot.totalValue(), 
            snapshot.totalCostBasis(),
            snapshot.unrealizedGainLoss(),
            snapshot.gainLossPercent().change(),
            snapshot.cashBalance(),
            snapshot.investedValue(),
            snapshot.totalValue().currency(),
            snapshot.hasStaleData(),
            snapshot.snapshotDate().atStartOfDay(ZoneOffset.UTC).toInstant()))
        .toList();
  }

  private Money nullSafe(Money money, Currency currency) {
    return money != null ? money : Money.zero(currency);
  }
}