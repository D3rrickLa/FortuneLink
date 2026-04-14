package com.laderrco.fortunelink.portfolio.application.mappers;

import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioSummaryView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio.application.views.PositionView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.PercentageChange;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.FifoPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortfolioViewMapper {
  private final ExchangeRateService exchangeRateService; // New Dependency

  /**
   * Calculates return percentage: (gain / cost basis) * 100 Returns zero if cost basis is zero/null
   * to avoid division by zero.
   *
   * @implNote: PercentageChange stores decimal form: 0.10 = 10%. Do NOT multiply by 100 here,
   * toPercent() does that
   */
  private static PercentageChange calculateReturnPercentage(Money gain, Money costBasis) {
    if (costBasis == null || costBasis.isZero()) {
      return new PercentageChange(BigDecimal.ZERO);
    }

    BigDecimal decimalValue = gain.amount()
        .divide(costBasis.amount(), Precision.PERCENTAGE.getDecimalPlaces(),
            Rounding.PERCENTAGE.getMode());

    return new PercentageChange(decimalValue);
  }

  /**
   * Determines the cost basis methodology used by the position. Returns "ACB" for Canadian tax
   * method or "FIFO" for US tax method.
   */
  private static String determineMethodology(Position position) {
    return switch (position) {
      case AcbPosition _ -> "ACB";
      case FifoPosition _ -> "FIFO";
    };
  }

  /**
   * Extracts the earliest acquisition date from the position. For ACB: would need to track
   * separately if you want this For FIFO: first lot's acquisition date
   * <p>
   * NOTE: Your Position interface doesn't expose this yet. You may need to add this to the
   * interface or track separately.
   */
  private static Instant extractFirstAcquiredDate(Position position) {
    return switch (position) {
      case AcbPosition acb -> acb.firstAcquiredAt();
      case FifoPosition fifo -> {
        var lots = fifo.lots();
        yield lots.isEmpty() ? null : lots.get(0).acquiredDate();
      }
    };
  }

  /**
   * Extracts the most recent modification date. This would typically come from the aggregate root
   * or event sourcing.
   * <p>
   * NOTE: Your Position interface doesn't expose this. Consider adding lastModifiedAt to Position
   * interface or tracking at Account level.
   */
  private static Instant extractLastModifiedDate(Position position) {
    return position.lastModifiedAt();
  }

  public PortfolioView toNewPortfolioView(Portfolio portfolio) {
    List<AccountView> accountViews = portfolio.getAccounts().stream().map(this::toNewAccountView)
        .toList();

    return new PortfolioView(portfolio.getPortfolioId(), portfolio.getUserId(), portfolio.getName(),
        portfolio.getDescription(), accountViews, Money.zero(portfolio.getDisplayCurrency()), false,
        portfolio.getCreatedAt(), portfolio.getLastUpdatedAt());
  }

  public PortfolioView toPortfolioView(Portfolio portfolio, List<AccountView> accountViews,
      Money totalValue, boolean hasStaleData) {
    Objects.requireNonNull(portfolio, "Portfolio cannot be null");

    return new PortfolioView(portfolio.getPortfolioId(), portfolio.getUserId(), portfolio.getName(),
        portfolio.getDescription(), accountViews, totalValue, hasStaleData,
        portfolio.getCreatedAt(), portfolio.getLastUpdatedAt());
  }

  public PortfolioSummaryView toPortfolioSummaryView(Portfolio portfolio, Money totalValue) {
    Objects.requireNonNull(portfolio, "Portfolio cannot be null");
    Objects.requireNonNull(totalValue, "Total value cannot be null");

    return new PortfolioSummaryView(portfolio.getPortfolioId(), portfolio.getName(), totalValue,
        portfolio.getLastUpdatedAt());
  }

  public AccountView toNewAccountView(Account account) {
    return new AccountView(account.getAccountId(), account.getName(), account.getAccountType(),
        account.getState(), Collections.emptyList(), account.getAccountCurrency(),
        Money.zero(account.getAccountCurrency()), Money.zero(account.getAccountCurrency()),
        account.getCreationDate(), false, 0);
  }

  public AccountView toAccountView(Account account, List<PositionView> positionViews,
      Money totalValue, Money cashBalance, boolean hasCashImbalance, int excludedTransactionCount) {

    return new AccountView(account.getAccountId(), account.getName(), account.getAccountType(),
        account.getState(), positionViews, account.getAccountCurrency(), cashBalance, totalValue,
        account.getCreationDate(), hasCashImbalance, excludedTransactionCount);
  }

  /**
   * Maps a position to its view with no fee data. Used for summary screens where tax data is not
   * needed. totalFeesIncurred will be Price.zero.
   */
  public PositionView toPositionView(Position position, MarketAssetQuote quote) {
    return toPositionView(position, quote, Money.zero(position.accountCurrency()));
  }

  /**
   * Maps a position to its view. * Fee Display Note: Following Canadian Tax Law (CRA), fees ARE
   * included in the position's totalCostBasis. The feesForSymbol parameter is used strictly for
   * breakdown/display purposes (transparency).
   * <p>
   * UI contract: Holdings screen -> totalCostBasis (already includes fees) Tax / ACB screen ->
   * totalCostBasis (this IS the effective ACB)
   *
   * @param position      the position value object
   * @param quote         current market quote (nullable, falls back to cost basis)
   * @param feesForSymbol cumulative BUY fees for this symbol in account currency
   */
  public PositionView toPositionView(Position position, MarketAssetQuote quote,
      Money feesForSymbol) {
    AssetSymbol symbol = position.symbol();
    Currency currency = position.accountCurrency();

    Money fees =
        (feesForSymbol != null && feesForSymbol.currency().equals(currency)) ? feesForSymbol
            : Money.zero(currency);

    if (quote == null || quote.currentPrice() == null || quote.currentPrice().pricePerUnit()
        .isZero()) {
      return new PositionView(symbol.symbol(), position.type(), position.totalQuantity(),
          new Price(position.totalCostBasis()), new Price(position.costPerUnit()), fees,
          // fees even when quote unavailable
          Price.zero(currency), // current price unknown
          Money.zero(currency), // market value unknown
          Money.zero(currency), // unrealized P&L unknown
          PercentageChange.ZERO, determineMethodology(position), extractFirstAcquiredDate(position),
          extractLastModifiedDate(position));
    }

    // FIX: Handle Currency Mismatch for Market Price
    Price rawPrice = quote.currentPrice();
    Price normalizedPrice = rawPrice;

    if (!rawPrice.currency().equals(currency)) {
      Money converted = exchangeRateService.convert(rawPrice.pricePerUnit(), currency);
      normalizedPrice = new Price(converted);
    }

    // marketValue is now guaranteed to be in the account currency
    Money marketValue = position.currentValue(normalizedPrice);

    // This subtraction is now safe because marketValue and totalCostBasis are both
    // in 'currency'
    Money unrealizedPnL = marketValue.subtract(position.totalCostBasis());

    PercentageChange returnPct = calculateReturnPercentage(unrealizedPnL,
        position.totalCostBasis());

    return new PositionView(symbol.symbol(), position.type(), position.totalQuantity(),
        new Price(position.totalCostBasis()), new Price(position.costPerUnit()), fees,
        normalizedPrice, marketValue, unrealizedPnL, returnPct, determineMethodology(position),
        extractFirstAcquiredDate(position), extractLastModifiedDate(position));
  }
}