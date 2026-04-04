package com.laderrco.fortunelink.portfolio.application.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laderrco.fortunelink.portfolio.application.queries.GetRealizedGainsQuery;
import com.laderrco.fortunelink.portfolio.application.repositories.RealizedGainsQueryRepository;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.views.RealizedGainView;
import com.laderrco.fortunelink.portfolio.application.views.RealizedGainsSummaryView;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.RealizedGainRecord;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RealizedGainsQueryService {
  private final RealizedGainsQueryRepository repository;
  private final PortfolioLoader portfolioLoader;

  public RealizedGainsSummaryView getRealizedGains(GetRealizedGainsQuery query) {
    Objects.requireNonNull(query, "GetRealizedGainsQuery cannot be null");

    portfolioLoader.validatePortfolioAndAccountOwnership(
        query.portfolioId(), query.userId(), query.accountId());

    List<RealizedGainRecord> records = fetchRecords(query);

    Currency currency = resolveCurrency(query.accountId(), records);
    return buildSummary(records, currency, query.taxYear());
  }

  private List<RealizedGainRecord> fetchRecords(GetRealizedGainsQuery query) {
    boolean hasYear = query.taxYear() != null;
    boolean hasSymbol = query.symbol() != null;

    if (hasYear && hasSymbol) {
      return repository.findByAccountIdAndYearAndSymbol(
          query.accountId(), query.taxYear(), query.symbol());
    } else if (hasYear) {
      return repository.findByAccountIdAndYear(query.accountId(), query.taxYear());
    } else if (hasSymbol) {
      return repository.findByAccountIdAndSymbol(query.accountId(), query.symbol());
    } else {
      return repository.findByAccountId(query.accountId());
    }
  }

  private Currency resolveCurrency(AccountId accountId, List<RealizedGainRecord> records) {
    if (!records.isEmpty()) {
      return records.get(0).realizedGainLoss().currency();
    }
    // No records to derive currency from — do a lightweight lookup.
    return repository.findAccountCurrencyCode(accountId)
        .map(Currency::of)
        .orElse(Currency.CAD); // safe fallback, account should always have a currency
  }

  private RealizedGainsSummaryView buildSummary(
      List<RealizedGainRecord> records,
      Currency currency,
      Integer taxYear) {

    Money totalGains = Money.zero(currency);
    Money totalLosses = Money.zero(currency);
    List<RealizedGainView> views = new ArrayList<>();

    for (RealizedGainRecord r : records) {
      views.add(new RealizedGainView(
          r.symbol().symbol(),
          r.realizedGainLoss(),
          r.costBasisSold(),
          r.occurredAt(),
          r.isGain()));

      if (r.isGain()) {
        totalGains = totalGains.add(r.realizedGainLoss());
      } else if (r.isLoss()) {
        totalLosses = totalLosses.add(r.realizedGainLoss().abs());
      }
    }

    Money netGainLoss = totalGains.subtract(totalLosses);
    return new RealizedGainsSummaryView(views, totalGains, totalLosses, netGainLoss, currency, taxYear);
  }
}