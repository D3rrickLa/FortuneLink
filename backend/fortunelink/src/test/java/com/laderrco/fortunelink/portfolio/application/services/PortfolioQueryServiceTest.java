package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.queries.GetNetWorthQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetPortfolioByIdQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetPortfoliosByUserIdQuery;
import com.laderrco.fortunelink.portfolio.application.utils.AccountViewBuilder;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.NetWorthView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioSummaryView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PortfolioQueryServiceTest {
  private static final Currency CAD = Currency.CAD;
  @Mock
  private MarketDataService marketDataService;
  @Mock
  private PortfolioValuationService portfolioValuationService;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private PortfolioViewMapper portfolioViewMapper;
  @Mock
  private AccountViewBuilder accountViewBuilder;
  @Mock
  private PortfolioLoader portfolioLoader;
  @InjectMocks
  private PortfolioQueryService portfolioQueryService;

  private UserId userId;
  private PortfolioId portfolioId;

  @BeforeEach
  void setUp() {
    userId = UserId.random();
    portfolioId = PortfolioId.newId();
  }

  /**
   * Builds a Portfolio with the given accounts pre-created. Uses reconstitution rather than mocking
   * to keep tests honest about domain behavior.
   */
  private Portfolio buildPortfolio(UserId userId, PortfolioId portfolioId, List<Account> accounts) {
    Map<AccountId, Account> accountMap = new java.util.LinkedHashMap<>();
    for (Account a : accounts) {
      accountMap.put(a.getAccountId(), a);
    }
    return Portfolio.reconstitute(portfolioId, userId, "Test Portfolio", "desc", accountMap, CAD,
        false, null, null, Instant.now(), Instant.now());
  }

  private Portfolio buildPortfolioWithCurrency(UserId userId, PortfolioId portfolioId,
      Currency currency) {
    return Portfolio.reconstitute(portfolioId, userId, "Test Portfolio", "desc", Map.of(), currency,
        false, null, null, Instant.now(), Instant.now());
  }

  /**
   * Builds a healthy account with mock positions. Uses a Mockito spy so we can control
   * getPositionEntries() without subclassing.
   */
  private Account buildAccount(AccountId accountId, Set<AssetSymbol> symbols) {
    Account account = new Account(accountId, "Test Account", AccountType.TFSA, CAD,
        PositionStrategy.ACB);

    // Wire up positions entries via spy if symbols present, avoids
    // fighting Hibernate-managed maps directly in unit tests.
    if (!symbols.isEmpty()) {
      Account spy = org.mockito.Mockito.spy(account);
      Collection<Map.Entry<AssetSymbol, Position>> entries = symbols.stream()
          .map(s -> buildPositionEntry(s, account)).toList();
      when(spy.getPositionEntries()).thenReturn(entries);
      return spy;
    }
    return account;
  }

  private Account buildStaleAccount(AccountId accountId, Set<AssetSymbol> symbols) {
    Account account = buildAccount(accountId, symbols);
    account.markStale();
    return account;
  }

  private Map.Entry<AssetSymbol, Position> buildPositionEntry(AssetSymbol symbol, Account account) {
    AcbPosition pos = AcbPosition.empty(symbol, AssetType.STOCK, CAD);
    return Map.entry(symbol, pos);
  }

  private MarketAssetQuote buildQuote(AssetSymbol symbol) {
    return new MarketAssetQuote(symbol,
        com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price.of("100.00",
            CAD), null, null, null, null, null, null, null, null, "TEST", Instant.now());
  }

  private PortfolioView buildPortfolioView(PortfolioId portfolioId, UserId userId) {
    return PortfolioView.builder().portfolioId(portfolioId).userId(userId).name("Test")
        .accounts(List.of()).totalValue(Money.zero(CAD)).creationDate(Instant.now())
        .lastUpdated(Instant.now()).build();
  }

  private AccountView buildAccountView(AccountId accountId) {
    return new AccountView(accountId, "Test Account", AccountType.TFSA, List.of(), CAD,
        Money.zero(CAD), Money.zero(CAD), Instant.now());
  }

  @Nested
  @DisplayName("getPortfolioById")
  class GetPortfolioById {
    @Test
    @DisplayName("throws NullPointerException when query is null")
    void nullQuery_throws() {
      assertThatThrownBy(() -> portfolioQueryService.getPortfolioById(null)).isInstanceOf(
          NullPointerException.class);

      verifyNoInteractions(portfolioLoader, marketDataService, transactionRepository);
    }

    @Test
    @DisplayName("returns PortfolioView for portfolio with no accounts")
    void emptyPortfolio_returnsView() {
      Portfolio portfolio = buildPortfolio(userId, portfolioId, Collections.emptyList());
      PortfolioView expected = buildPortfolioView(portfolioId, userId);

      when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(portfolio);
      when(portfolioValuationService.calculateTotalValue(eq(portfolio), eq(CAD),
          eq(Map.of()))).thenReturn(Money.zero(CAD));
      when(portfolioViewMapper.toPortfolioView(eq(portfolio), eq(List.of()), eq(Money.zero(CAD)),
          eq(false))).thenReturn(expected);

      PortfolioView result = portfolioQueryService.getPortfolioById(
          new GetPortfolioByIdQuery(portfolioId, userId));

      assertThat(result).isEqualTo(expected);

      // Market data must NOT be called when there are no symbols to fetch
      verify(marketDataService, never()).getBatchQuotes(anySet());
    }

    @Test
    @DisplayName("fetches quotes only once per request regardless of account count")
    void multipleAccounts_singleBatchQuoteCall() {
      AssetSymbol aapl = new AssetSymbol("AAPL");
      AssetSymbol googl = new AssetSymbol("GOOGL");

      Account account1 = buildAccount(AccountId.newId(), Set.of(aapl));
      Account account2 = buildAccount(AccountId.newId(), Set.of(googl));
      Portfolio portfolio = buildPortfolio(userId, portfolioId, List.of(account1, account2));

      Map<AssetSymbol, MarketAssetQuote> quotes = Map.of(aapl, buildQuote(aapl), googl,
          buildQuote(googl));

      Money totalValue = new Money(new BigDecimal("50000.0000000000"), CAD);
      AccountView view1 = buildAccountView(account1.getAccountId());
      AccountView view2 = buildAccountView(account2.getAccountId());

      when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(portfolio);
      when(marketDataService.getBatchQuotes(Set.of(aapl, googl))).thenReturn(quotes);
      when(accountViewBuilder.build(eq(account1), eq(quotes), any())).thenReturn(view1);
      when(accountViewBuilder.build(eq(account2), eq(quotes), any())).thenReturn(view2);
      when(portfolioValuationService.calculateTotalValue(any(), eq(CAD), eq(quotes))).thenReturn(
          totalValue);

      portfolioQueryService.getPortfolioById(new GetPortfolioByIdQuery(portfolioId, userId));

      // The critical invariant, one batch call, never N calls for N accounts
      verify(marketDataService, times(1)).getBatchQuotes(any());
    }

    @Test
    @DisplayName("propagates hasStaleData=true when at least one account is stale")
    void staleAccount_propagatesStaleFlagToView() {
      AssetSymbol aapl = new AssetSymbol("AAPL");
      Account staleAccount = buildStaleAccount(AccountId.newId(), Set.of(aapl));
      Portfolio portfolio = buildPortfolio(userId, portfolioId, List.of(staleAccount));

      Map<AssetSymbol, MarketAssetQuote> quotes = Map.of(aapl, buildQuote(aapl));
      Money totalValue = Money.of("10000.00", CAD);
      AccountView accountView = buildAccountView(staleAccount.getAccountId());
      PortfolioView expected = buildPortfolioView(portfolioId, userId);

      when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(portfolio);
      when(marketDataService.getBatchQuotes(Set.of(aapl))).thenReturn(quotes);
      when(accountViewBuilder.build(eq(staleAccount), eq(quotes), any())).thenReturn(accountView);
      when(portfolioValuationService.calculateTotalValue(eq(portfolio), eq(CAD),
          eq(quotes))).thenReturn(totalValue);
      when(portfolioViewMapper.toPortfolioView(eq(portfolio), any(), eq(totalValue),
          eq(true))) // <-- stale = true
          .thenReturn(expected);

      PortfolioView result = portfolioQueryService.getPortfolioById(
          new GetPortfolioByIdQuery(portfolioId, userId));

      assertThat(result).isEqualTo(expected);

      // Verify the mapper received hasStaleData=true
      verify(portfolioViewMapper).toPortfolioView(eq(portfolio), any(), eq(totalValue), eq(true));
    }

    @Test
    @DisplayName("passes fee cache per account to account view builder")
    void feeCacheIsAccountScoped() {
      AssetSymbol aapl = new AssetSymbol("AAPL");
      AccountId accountId = AccountId.newId();
      Account account = buildAccount(accountId, Set.of(aapl));
      Portfolio portfolio = buildPortfolio(userId, portfolioId, List.of(account));

      Money feeAmount = Money.of("9.99", CAD);
      Map<AssetSymbol, MarketAssetQuote> quotes = Map.of(aapl, buildQuote(aapl));
      AccountView accountView = buildAccountView(accountId);
      PortfolioView expected = buildPortfolioView(portfolioId, userId);

      when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(portfolio);
      when(marketDataService.getBatchQuotes(any())).thenReturn(quotes);
      when(accountViewBuilder.build(eq(account), eq(quotes),
          eq(Map.of(aapl, feeAmount)))).thenReturn(accountView);
      when(portfolioValuationService.calculateTotalValue(any(), any(), any())).thenReturn(
          Money.zero(CAD));
      when(portfolioViewMapper.toPortfolioView(any(), any(), any(), anyBoolean())).thenReturn(
          expected);

      portfolioQueryService.getPortfolioById(new GetPortfolioByIdQuery(portfolioId, userId));

      // Verify the correct fee breakdown map was passed, not an empty map
      verify(accountViewBuilder).build(eq(account), eq(quotes), eq(Map.of(aapl, feeAmount)));
    }

    @Test
    @DisplayName("uses empty fee map for accounts not present in fee cache")
    void accountMissingFromFeeCache_usesEmptyMap() {
      AssetSymbol aapl = new AssetSymbol("AAPL");
      AccountId accountId = AccountId.newId();
      Account account = buildAccount(accountId, Set.of(aapl));
      Portfolio portfolio = buildPortfolio(userId, portfolioId, List.of(account));

      // Fee cache returns nothing for this account
      when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(portfolio);
      when(marketDataService.getBatchQuotes(any())).thenReturn(Map.of());
      // account absent
      when(accountViewBuilder.build(eq(account), any(), eq(Map.of()))).thenReturn(
          buildAccountView(accountId));
      when(portfolioValuationService.calculateTotalValue(any(), any(), any())).thenReturn(
          Money.zero(CAD));
      when(portfolioViewMapper.toPortfolioView(any(), any(), any(), anyBoolean())).thenReturn(
          buildPortfolioView(portfolioId, userId));

      portfolioQueryService.getPortfolioById(new GetPortfolioByIdQuery(portfolioId, userId));

      verify(accountViewBuilder).build(eq(account), any(), eq(Map.of()));
    }
  }

  @Nested
  @DisplayName("getPortfolioSummaries")
  class GetPortfolioSummaries {
    @Test
    @DisplayName("throws NullPointerException when query is null")
    void nullQuery_throws() {
      assertThatThrownBy(() -> portfolioQueryService.getPortfolioSummaries(null)).isInstanceOf(
          NullPointerException.class);

      verifyNoInteractions(portfolioLoader, marketDataService);
    }

    @Test
    @DisplayName("returns empty list when user has no portfolios")
    void noPortfolios_returnsEmptyList() {
      when(portfolioLoader.loadAllUserPortfolios(userId)).thenReturn(List.of());

      List<PortfolioSummaryView> result = portfolioQueryService.getPortfolioSummaries(
          new GetPortfoliosByUserIdQuery(userId));

      assertThat(result.isEmpty()).isTrue();

      // Nothing to fetch, guard must prevent market data call
      verifyNoInteractions(marketDataService, portfolioValuationService);
    }

    @Test
    @DisplayName("fetches one batch of quotes across all portfolios")
    void multiplePortfolios_singleBatchQuoteCall() {
      AssetSymbol aapl = new AssetSymbol("AAPL");
      AssetSymbol tsla = new AssetSymbol("TSLA");

      PortfolioId pid1 = PortfolioId.newId();
      PortfolioId pid2 = PortfolioId.newId();
      Portfolio p1 = buildPortfolio(userId, pid1,
          List.of(buildAccount(AccountId.newId(), Set.of(aapl))));
      Portfolio p2 = buildPortfolio(userId, pid2,
          List.of(buildAccount(AccountId.newId(), Set.of(tsla))));

      Map<AssetSymbol, MarketAssetQuote> quotes = Map.of(aapl, buildQuote(aapl), tsla,
          buildQuote(tsla));
      Money val1 = Money.of("20000.00", CAD);
      Money val2 = Money.of("15000.00", CAD);

      PortfolioSummaryView sv1 = new PortfolioSummaryView(pid1, "P1", val1, Instant.now());
      PortfolioSummaryView sv2 = new PortfolioSummaryView(pid2, "P2", val2, Instant.now());

      when(portfolioLoader.loadAllUserPortfolios(userId)).thenReturn(List.of(p1, p2));
      when(marketDataService.getBatchQuotes(Set.of(aapl, tsla))).thenReturn(quotes);
      when(portfolioValuationService.calculateTotalValue(eq(p1), eq(CAD), eq(quotes))).thenReturn(
          val1);
      when(portfolioValuationService.calculateTotalValue(eq(p2), eq(CAD), eq(quotes))).thenReturn(
          val2);
      when(portfolioViewMapper.toPortfolioSummaryView(p1, val1)).thenReturn(sv1);
      when(portfolioViewMapper.toPortfolioSummaryView(p2, val2)).thenReturn(sv2);

      List<PortfolioSummaryView> result = portfolioQueryService.getPortfolioSummaries(
          new GetPortfoliosByUserIdQuery(userId));

      assertThat(result).containsExactlyInAnyOrder(sv1, sv2);
      verify(marketDataService, times(1)).getBatchQuotes(any());
    }

    @Test
    @DisplayName("does not call market data when all portfolios have no positions")
    void portfoliosWithNoPositions_noMarketDataCall() {
      Portfolio p1 = buildPortfolio(userId, PortfolioId.newId(), List.of());
      Portfolio p2 = buildPortfolio(userId, PortfolioId.newId(), List.of());

      Money zero = Money.zero(CAD);
      PortfolioSummaryView sv = new PortfolioSummaryView(p1.getPortfolioId(), "P", zero,
          Instant.now());

      when(portfolioLoader.loadAllUserPortfolios(userId)).thenReturn(List.of(p1, p2));
      when(portfolioValuationService.calculateTotalValue(any(), any(), eq(Map.of()))).thenReturn(
          zero);
      when(portfolioViewMapper.toPortfolioSummaryView(any(), any())).thenReturn(sv);

      portfolioQueryService.getPortfolioSummaries(new GetPortfoliosByUserIdQuery(userId));

      verify(marketDataService, never()).getBatchQuotes(anySet());
    }

    @Test
    @DisplayName("summary view uses each portfolio's own display currency")
    void eachPortfolioUsesItsOwnDisplayCurrency() {
      Currency usd = Currency.USD;

      Portfolio cadPortfolio = buildPortfolioWithCurrency(userId, PortfolioId.newId(), CAD);
      Portfolio usdPortfolio = buildPortfolioWithCurrency(userId, PortfolioId.newId(), usd);

      when(portfolioLoader.loadAllUserPortfolios(eq(userId))).thenReturn(
          List.of(cadPortfolio, usdPortfolio));
      when(portfolioValuationService.calculateTotalValue(any(), any(), any())).thenReturn(
          Money.zero(CAD));
      when(portfolioViewMapper.toPortfolioSummaryView(any(), any())).thenReturn(
          new PortfolioSummaryView(cadPortfolio.getPortfolioId(), "P", Money.zero(CAD),
              Instant.now()));

      portfolioQueryService.getPortfolioSummaries(new GetPortfoliosByUserIdQuery(userId));

      verify(portfolioValuationService).calculateTotalValue(eq(cadPortfolio), eq(CAD), any());
      verify(portfolioValuationService).calculateTotalValue(eq(usdPortfolio), eq(usd), any());
    }
  }

  // =========================================================================
  // getNetWorth
  // =========================================================================
  @Nested
  @DisplayName("getNetWorth")
  class GetNetWorthTests {

    @Test
    @DisplayName("throws NullPointerException when query is null")
    void nullQuery_throws() {
      assertThatThrownBy(() -> portfolioQueryService.getNetWorth(null)).isInstanceOf(
          NullPointerException.class);

      verifyNoInteractions(portfolioLoader, marketDataService);
    }

    @Test
    @DisplayName("liabilities are currently zero and netWorth equals totalAssets")
    void noLiabilities_netWorthEqualsTotalAssets() {
      Portfolio portfolio = buildPortfolio(userId, portfolioId, List.of());
      Money totalAssets = Money.of("75000.00", CAD);

      when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(portfolio);
      when(portfolioValuationService.calculateTotalValue(eq(portfolio), eq(CAD), any())).thenReturn(
          totalAssets);

      NetWorthView result = portfolioQueryService.getNetWorth(
          new GetNetWorthQuery(portfolioId, userId));

      assertThat(result.totalAssets()).isEqualTo(totalAssets);
      assertThat(result.totalLiabilities()).isEqualTo(Money.zero(CAD));
      assertThat(result.netWorth()).isEqualTo(totalAssets); // liabilities = 0
      assertThat(result.displayCurrency()).isEqualTo(CAD);
    }

    @Test
    @DisplayName("propagates isStale=true when at least one account is stale")
    void staleAccount_netWorthViewMarkedStale() {
      Account staleAccount = buildStaleAccount(AccountId.newId(), Set.of());
      Portfolio portfolio = buildPortfolio(userId, portfolioId, List.of(staleAccount));

      when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(portfolio);
      when(portfolioValuationService.calculateTotalValue(any(), any(), any())).thenReturn(
          Money.zero(CAD));

      NetWorthView result = portfolioQueryService.getNetWorth(
          new GetNetWorthQuery(portfolioId, userId));

      assertThat(result.hasStale()).isTrue();
    }

    @Test
    @DisplayName("isStale=false when all accounts are healthy")
    void healthyPortfolio_netWorthViewNotStale() {
      Account healthyAccount = buildAccount(AccountId.newId(), Set.of());
      Portfolio portfolio = buildPortfolio(userId, portfolioId, List.of(healthyAccount));

      when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(portfolio);
      when(portfolioValuationService.calculateTotalValue(any(), any(), any())).thenReturn(
          Money.zero(CAD));

      NetWorthView result = portfolioQueryService.getNetWorth(
          new GetNetWorthQuery(portfolioId, userId));

      assertThat(result.hasStale()).isFalse();
    }

    @Test
    @DisplayName("asOfDate is set and recent")
    void asOfDate_isSetAndRecent() {
      Instant before = Instant.now().minusSeconds(1);

      Portfolio portfolio = buildPortfolio(userId, portfolioId, List.of());
      when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(portfolio);
      when(portfolioValuationService.calculateTotalValue(any(), any(), any())).thenReturn(
          Money.zero(CAD));

      NetWorthView result = portfolioQueryService.getNetWorth(
          new GetNetWorthQuery(portfolioId, userId));

      assertThat(result.asOfDate()).isAfter(before);
    }
  }
}
