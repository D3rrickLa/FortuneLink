package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.utils.PortfolioAccessUtils;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.views.ValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;
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
public class ValuationApplicationService {
  private final PortfolioLoader portfolioLoader;
  private final MarketDataService marketDataService;
  private final PortfolioValuationService portfolioValuationService;
  private final UserPreferencesService userPreferencesService;

  /**
   * Individual Portfolio: Respects the Portfolio's displayCurrency.
   */
  public ValuationView computePortfolioValuation(PortfolioId portfolioId, UserId userId) {
    Portfolio portfolio = portfolioLoader.loadUserPortfolio(portfolioId, userId);

    return computeView(List.of(portfolio), portfolio.getDisplayCurrency());
  }

  /**
   * Summary View: Aggregates everything into a stable base currency (CAD).
   */
  public ValuationView computeSummaryValuation(UserId userId) {
    List<Portfolio> portfolios = portfolioLoader.loadAllUserPortfolios(userId);
    Currency reportingCurrency = userPreferencesService.get(userId).getBaseCurrency();

    if (portfolios.isEmpty()) {
      // throw new NoActivePortfoliosException(userId);
      return ValuationView.empty(reportingCurrency);
    }

    // We use CAD here so that $100 USD + $100 CAD correctly results
    // in ~$235 CAD rather than a broken "200" total.
    return computeView(portfolios, reportingCurrency);
  }

  private ValuationView computeView(List<Portfolio> portfolios, Currency targetCurrency) {
    Set<AssetSymbol> symbols = portfolios.stream()
        .flatMap(p -> PortfolioAccessUtils.extractSymbols(p).stream()).collect(Collectors.toSet());

    Map<AssetSymbol, MarketAssetQuote> quoteCache =
        symbols.isEmpty() ? Map.of() : marketDataService.getBatchQuotes(symbols);

    return portfolios.size() == 1 ? portfolioValuationService.calculatePortfolioValuation(
        portfolios.getFirst(), targetCurrency, quoteCache)
        : portfolioValuationService.calculateUserValuation(portfolios, targetCurrency, quoteCache);
  }
}
