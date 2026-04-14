package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.queries.GetRealizedGainsQuery;
import com.laderrco.fortunelink.portfolio.application.repositories.RealizedGainsQueryRepository;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.utils.valueobjects.GainsAggregation;
import com.laderrco.fortunelink.portfolio.application.views.RealizedGainView;
import com.laderrco.fortunelink.portfolio.application.views.RealizedGainsSummaryView;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.RealizedGainRecord;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RealizedGainsQueryService {
  private final RealizedGainsQueryRepository repository;
  private final PortfolioLoader portfolioLoader;

  public RealizedGainsSummaryView getRealizedGains(GetRealizedGainsQuery query) {
    Objects.requireNonNull(query, "GetRealizedGainsQuery cannot be null");

    portfolioLoader.validatePortfolioAndAccountOwnership(query.portfolioId(), query.userId(),
        query.accountId());

    Page<RealizedGainRecord> recordPage = fetchRecords(query);
    GainsAggregation totals = repository.calculateTotals(query.accountId(), query.taxYear(),
        query.symbol());

    Currency currency = resolveCurrency(query.accountId());

    return buildSummary(recordPage, totals, currency, query.taxYear());
  }

  private Page<RealizedGainRecord> fetchRecords(GetRealizedGainsQuery query) {
    Pageable pageable = query.toPageable();
    boolean hasYear = query.taxYear() != null;
    boolean hasSymbol = query.symbol() != null;

    if (hasYear && hasSymbol) {
      return repository.findByAccountIdAndYearAndSymbol(query.accountId(), query.taxYear(),
          query.symbol(), pageable);
    } else if (hasYear) {
      return repository.findByAccountIdAndYear(query.accountId(), query.taxYear(), pageable);
    } else if (hasSymbol) {
      return repository.findByAccountIdAndSymbol(query.accountId(), query.symbol(), pageable);
    } else {
      return repository.findByAccountId(query.accountId(), pageable);
    }
  }

  // Only checks from one source of truth, the account
  private Currency resolveCurrency(AccountId accountId) {
    return repository.findAccountCurrencyCode(accountId).map(Currency::of).orElse(Currency.CAD);
  }

  private RealizedGainsSummaryView buildSummary(Page<RealizedGainRecord> recordPage,
      GainsAggregation totals, Currency currency, Integer taxYear) {

    // Convert domain records to views for the current page
    List<RealizedGainView> views = recordPage.getContent().stream().map(
        r -> new RealizedGainView(r.symbol().symbol(), r.realizedGainLoss(), r.costBasisSold(),
            r.occurredAt(), r.isGain())).toList();

    // Use the totals calculated by the DB aggregation
    Money totalGains = new Money(totals.sumGains(), currency);
    Money totalLosses = new Money(totals.sumLosses(), currency);
    Money netGainLoss = totalGains.subtract(totalLosses);

    return new RealizedGainsSummaryView(views, totalGains, totalLosses, netGainLoss, currency,
        taxYear, recordPage.getTotalElements(), recordPage.getTotalPages());
  }
}