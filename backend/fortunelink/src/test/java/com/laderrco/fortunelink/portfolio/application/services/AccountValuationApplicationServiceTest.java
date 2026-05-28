package com.laderrco.fortunelink.portfolio.application.services;

import static com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency.CAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio.application.repositories.AccountQueryRepository;
import com.laderrco.fortunelink.portfolio.application.views.ValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.PercentageChange;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountValuationApplicationServiceTest {
  private static final UUID USER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID PORTFOLIO_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final UUID ACCOUNT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
  @Mock
  private AccountQueryRepository repository;

  @Mock
  private MarketDataService marketDataService;
  
  @Mock
  private ExchangeRateService exchangeRateService;
  
  @InjectMocks
  private AccountValuationApplicationService service;

  @Test
  void shouldComputeAccountValuation() {

    Account account = mock(Account.class);
    when(account.getAccountCurrency()).thenReturn(CAD);

    Map<AssetSymbol, MarketAssetQuote> quote = new java.util.HashMap<>(Map.of());
    quote.put(new AssetSymbol("AAPL"),
        new MarketAssetQuote(new AssetSymbol("AAPL"), Price.of("100", CAD), Price.of("100", CAD),
            Price.of("100", CAD), Price.of("100", CAD), Price.of("100", CAD), PercentageChange.ZERO,
            BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TWO, "I made it up", Instant.now()));

    when(repository.findByIdWithDetails(any(), any(), any())).thenReturn(Optional.of(account));
    when(marketDataService.getBatchQuotes(any())).thenReturn(quote);
    when(repository.findByIdWithDetails(any(), any(), any())).thenReturn(Optional.of(account));

    ValuationView result = service.computeAccountValuation(
        new GetAccountSummaryQuery(new PortfolioId(PORTFOLIO_ID), new UserId(USER_UUID),
            new AccountId(ACCOUNT_ID)));

    assertThat(result.totalValue()).isNotNull();
    assertThat(result.displayCurrency().getCode()).isEqualTo("CAD");
  }

  @Test
  void shouldHandleMissingAndValidQuotesDuringValuation() {
    Account account = mock(Account.class);
    when(account.getAccountCurrency()).thenReturn(Currency.CAD);
    when(account.getCashBalance()).thenReturn(Money.zero(Currency.CAD));

    AssetSymbol appleSymbol = new AssetSymbol("AAPL");
    AssetSymbol googleSymbol = new AssetSymbol("GOOGL");

    // Mock Entry 1 (AAPL) - Will have a quote
    Map.Entry<AssetSymbol, Position> appleEntry = mock(Map.Entry.class);
    Position applePosition = mock(AcbPosition.class);
    when(appleEntry.getValue()).thenReturn(applePosition);
    when(applePosition.symbol()).thenReturn(appleSymbol);
    when(applePosition.totalCostBasis()).thenReturn(Money.of("50.00", Currency.CAD));
    when(applePosition.currentValue(any())).thenReturn(Money.of("150.00", Currency.CAD));

    // Mock Entry 2 (GOOGL) - Missing quote branch
    Map.Entry<AssetSymbol, Position> googleEntry = mock(Map.Entry.class);
    Position googlePosition = mock(AcbPosition.class);
    when(googleEntry.getValue()).thenReturn(googlePosition);
    when(googlePosition.symbol()).thenReturn(googleSymbol);
    when(googlePosition.totalCostBasis()).thenReturn(Money.of("50.00", Currency.CAD));

    // Return the mock map entries from the account
    when(account.getPositionEntries()).thenReturn(List.of(appleEntry, googleEntry));

    // Mock MarketDataService to ONLY return a quote for AAPL
    MarketAssetQuote appleQuote = new MarketAssetQuote(appleSymbol,
        Price.of("150.00", Currency.CAD), Price.of("150.00", Currency.CAD),
        Price.of("150.00", Currency.CAD), Price.of("150.00", Currency.CAD),
        Price.of("150.00", Currency.CAD), PercentageChange.ZERO, BigDecimal.ZERO, BigDecimal.TEN,
        BigDecimal.TWO, "Test", Instant.now());
    when(marketDataService.getBatchQuotes(Set.of(appleSymbol, googleSymbol))).thenReturn(
        Map.of(appleSymbol, appleQuote));

    when(repository.findByIdWithDetails(any(), any(), any())).thenReturn(Optional.of(account));

    // Act
    ValuationView result = service.computeAccountValuation(
        new GetAccountSummaryQuery(new PortfolioId(PORTFOLIO_ID), new UserId(USER_UUID),
            new AccountId(ACCOUNT_ID)));

    // Assert
    // Total cost basis = AAPL (50) + GOOGL (50) = 100
    // Total market value = AAPL (150) + GOOGL (0, because quote is null) = 150
    assertThat(result.totalValue()).isEqualTo(Money.of("150.00", Currency.CAD));
    assertThat(result.totalCostBasis()).isEqualTo(Money.of("100.00", Currency.CAD));
    assertThat(result.unrealizedGainLoss()).isEqualTo(Money.of("50.00", Currency.CAD));
  }

  @Test
  void shouldCalculateGainLossPercentageWhenCostBasisIsPositive() {
    Account account = mock(Account.class);
    when(account.getAccountCurrency()).thenReturn(Currency.CAD);
    when(account.getCashBalance()).thenReturn(Money.zero(Currency.CAD));

    AssetSymbol symbol = new AssetSymbol("AAPL");
    Map.Entry<AssetSymbol, Position> entry = mock(Map.Entry.class);
    Position position = mock(AcbPosition.class);

    when(entry.getValue()).thenReturn(position);
    when(position.symbol()).thenReturn(symbol);
    when(position.totalCostBasis()).thenReturn(Money.of("100.00", Currency.CAD));
    when(position.currentValue(any())).thenReturn(Money.of("150.00", Currency.CAD));

    when(account.getPositionEntries()).thenReturn(List.of(entry));

    MarketAssetQuote quote = mock(MarketAssetQuote.class);
    when(quote.currentPrice()).thenReturn(Price.of("150.00", Currency.CAD));
    when(marketDataService.getBatchQuotes(any())).thenReturn(Map.of(symbol, quote));
    when(repository.findByIdWithDetails(any(), any(), any())).thenReturn(Optional.of(account));

    // Act
    ValuationView result = service.computeAccountValuation(
        new GetAccountSummaryQuery(new PortfolioId(PORTFOLIO_ID), new UserId(USER_UUID), new AccountId(ACCOUNT_ID))
    );

    // Assert
    // ($50.00 gain / $100.00 cost) * 100 = 50.00%
    assertThat(result.gainLossPercent()).isEqualByComparingTo(new BigDecimal("50.00"));
  }

  @Test
  void shouldReturnZeroPercentageWhenCostBasisIsZero() {
    Account account = mock(Account.class);
    when(account.getAccountCurrency()).thenReturn(Currency.CAD);
    when(account.getCashBalance()).thenReturn(Money.zero(Currency.CAD));

    AssetSymbol symbol = new AssetSymbol("FREE");
    Map.Entry<AssetSymbol, Position> entry = mock(Map.Entry.class);
    Position position = mock(AcbPosition.class);

    when(entry.getValue()).thenReturn(position);
    when(position.symbol()).thenReturn(symbol);
    when(position.totalCostBasis()).thenReturn(Money.zero(Currency.CAD));
    when(position.currentValue(any())).thenReturn(Money.of("20.00", Currency.CAD));

    when(account.getPositionEntries()).thenReturn(List.of(entry));

    MarketAssetQuote quote = mock(MarketAssetQuote.class);
    when(quote.currentPrice()).thenReturn(Price.of("20.00", Currency.CAD));
    when(marketDataService.getBatchQuotes(any())).thenReturn(Map.of(symbol, quote));
    when(repository.findByIdWithDetails(any(), any(), any())).thenReturn(Optional.of(account));

    // Act
    ValuationView result = service.computeAccountValuation(
        new GetAccountSummaryQuery(new PortfolioId(PORTFOLIO_ID), new UserId(USER_UUID), new AccountId(ACCOUNT_ID))
    );

    // Assert
    // The ternary operator should bypass division and confidently return BigDecimal.ZERO
    assertThat(result.gainLossPercent()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void shouldFallbackToZeroMoneyWhenValuesAreNull() {
    Account account = mock(Account.class);
    when(account.getAccountCurrency()).thenReturn(Currency.CAD);

    // Force cashBalance to return null to hit the nullSafe path
    when(account.getCashBalance()).thenReturn(null);

    AssetSymbol symbol = new AssetSymbol("AAPL");
    Map.Entry<AssetSymbol, Position> entry = mock(Map.Entry.class);
    Position position = mock(AcbPosition.class);

    when(entry.getValue()).thenReturn(position);
    when(position.symbol()).thenReturn(symbol);

    // Force totalCostBasis and currentValue to return null to check alternative branches
    when(position.totalCostBasis()).thenReturn(null);
    when(position.currentValue(any())).thenReturn(null);

    when(account.getPositionEntries()).thenReturn(List.of(entry));

    MarketAssetQuote quote = mock(MarketAssetQuote.class);
    when(marketDataService.getBatchQuotes(any())).thenReturn(Map.of(symbol, quote));
    when(repository.findByIdWithDetails(any(), any(), any())).thenReturn(Optional.of(account));

    // Act
    ValuationView result = service.computeAccountValuation(
        new GetAccountSummaryQuery(new PortfolioId(PORTFOLIO_ID), new UserId(USER_UUID), new AccountId(ACCOUNT_ID))
    );

    // Assert
    // Everything should safely swap to Money.zero(Currency.CAD) instead of blowing up with an NPE
    assertThat(result.totalCashBalance()).isEqualTo(Money.zero(Currency.CAD));
    assertThat(result.totalCostBasis()).isEqualTo(Money.zero(Currency.CAD));
    assertThat(result.totalValue()).isEqualTo(Money.zero(Currency.CAD));
    assertThat(result.totalValue()).isEqualTo(Money.zero(Currency.CAD));
  }
}