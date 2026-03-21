package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetAllAccountsQuery;
import com.laderrco.fortunelink.portfolio.application.utils.AccountViewBuilder;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountQueryServiceTest {
  @Mock
  private MarketDataService marketDataService;

  @Mock
  private TransactionRepository transactionRepository;

  @Mock
  private PortfolioLoader portfolioLoader;

  @Mock
  private AccountViewBuilder accountViewBuilder;

  @InjectMocks
  private AccountQueryService accountQueryService;

  @BeforeEach
  void setUp() {
  }

  @Test
  void testGetAllAccounts_success_emptyPortfolio_returnsEmptyList() {
    PortfolioId portfolioId = PortfolioId.newId();
    UserId userId = UserId.random();

    Portfolio portfolio = mock(Portfolio.class);

    when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(portfolio);

    GetAllAccountsQuery query = new GetAllAccountsQuery(portfolioId, userId);
    List<AccountView> result = accountQueryService.getAllAccounts(query);

    assertThat(result).isEmpty();
    verifyNoInteractions(marketDataService, transactionRepository);
  }

  @Test
  void testGetAllAccounts_success_getAllAccounts_returnsSingle() {
    UserId userId = UserId.random();
    PortfolioId portfolioId = PortfolioId.newId();
    AssetSymbol btc = new AssetSymbol("BTC");
    AccountId accountId = AccountId.newId();

    GetAllAccountsQuery query = new GetAllAccountsQuery(portfolioId, userId);

    Account account = mock(Account.class);
    when(account.getAccountId()).thenReturn(accountId);

    Map<AssetSymbol, Position> positions = Map.of(btc, mock(AcbPosition.class));
    when(account.getPositionEntries()).thenReturn(positions.entrySet());

    Portfolio portfolio = mock(Portfolio.class);
    when(portfolio.getAccounts()).thenReturn(List.of(account));
    when(portfolio.hasAccounts()).thenReturn(true);
    when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(portfolio);

    MarketAssetQuote mockQuote = mock(MarketAssetQuote.class);
    Map<AssetSymbol, MarketAssetQuote> quoteMap = Map.of(btc, mockQuote);
    when(marketDataService.getBatchQuotes(anySet())).thenReturn(quoteMap);

    Money mockFee = Money.of("5", Currency.USD);
    Map<AssetSymbol, Money> feeMap = Map.of(btc, mockFee);
    when(transactionRepository.sumBuyFeesByAccountAndSymbol(anyList())).thenReturn(
        Map.of(accountId, feeMap));

    AccountView expectedView = mock(AccountView.class);
    when(accountViewBuilder.build(eq(account), eq(quoteMap), eq(feeMap))).thenReturn(expectedView);

    List<AccountView> result = accountQueryService.getAllAccounts(query);
    assertThat(result).hasSize(1).containsExactly(expectedView);

    verify(marketDataService).getBatchQuotes(argThat(set -> set.contains(btc)));
    verify(transactionRepository).sumBuyFeesByAccountAndSymbol(List.of(accountId));
  }

  @Test
  void testGetAllAccounts_success_multipleAccounts_mapsAll() {
    UserId userId = UserId.random();
    PortfolioId portfolioId = PortfolioId.newId();
    AccountId acc1Id = AccountId.newId();
    AccountId acc2Id = AccountId.newId();
    AssetSymbol btc = new AssetSymbol("BTC");
    AssetSymbol eth = new AssetSymbol("ETH");

    Account acc1 = mock(Account.class);
    when(acc1.getAccountId()).thenReturn(acc1Id);
    Map<AssetSymbol, Position> btcPosition = Map.of(btc, mock(AcbPosition.class));
    when(acc1.getPositionEntries()).thenReturn(btcPosition.entrySet());

    Account acc2 = mock(Account.class);
    when(acc2.getAccountId()).thenReturn(acc2Id);
    Map<AssetSymbol, Position> ethPosition = Map.of(eth, mock(AcbPosition.class));
    when(acc2.getPositionEntries()).thenReturn(ethPosition.entrySet());

    Portfolio portfolio = mock(Portfolio.class);
    when(portfolio.hasAccounts()).thenReturn(true);
    when(portfolio.getAccounts()).thenReturn(List.of(acc1, acc2));
    when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(portfolio);

    Map<AssetSymbol, MarketAssetQuote> quoteCache = Map.of(btc, mock(MarketAssetQuote.class), eth,
        mock(MarketAssetQuote.class));
    when(marketDataService.getBatchQuotes(anySet())).thenReturn(quoteCache);

    Map<AssetSymbol, Money> feesAcc1 = Map.of(btc, Money.of("10", Currency.USD));
    Map<AssetSymbol, Money> feesAcc2 = Map.of(eth, Money.of("20", Currency.USD));

    when(transactionRepository.sumBuyFeesByAccountAndSymbol(List.of(acc1Id, acc2Id))).thenReturn(
        Map.of(acc1Id, feesAcc1, acc2Id, feesAcc2));

    AccountView view1 = mock(AccountView.class);
    AccountView view2 = mock(AccountView.class);
    when(accountViewBuilder.build(acc1, quoteCache, feesAcc1)).thenReturn(view1);
    when(accountViewBuilder.build(acc2, quoteCache, feesAcc2)).thenReturn(view2);

    List<AccountView> result = accountQueryService.getAllAccounts(
        new GetAllAccountsQuery(portfolioId, userId));

    assertThat(result).hasSize(2).containsExactly(view1, view2);
  }

  @Test
  void testGetAllAccounts_failure_nullQueryThrowsException() {
    assertThatThrownBy(() -> accountQueryService.getAllAccounts(null)).isInstanceOf(
        NullPointerException.class).hasMessageContaining("GetAllAccountsQuery cannot be null");
  }

  @Test
  void testGetAccountSummary_success_getAccount() {
    UserId userId = UserId.random();
    PortfolioId portfolioId = PortfolioId.newId();
    AssetSymbol shop = new AssetSymbol("SHOP.TO");
    AccountId accountId = AccountId.newId();

    GetAccountSummaryQuery query = new GetAccountSummaryQuery(portfolioId, userId, accountId);

    Account account = new Account(accountId, "Account name", AccountType.RESP, Currency.CAD,
        PositionStrategy.ACB);

    Position position = new AcbPosition(shop, AssetType.STOCK, Currency.CAD, Quantity.of(100),
        Money.of(2010, "CAD"), Instant.now(), Instant.now());

    account.updatePosition(shop, position);

    Portfolio mockPortfolio = mock(Portfolio.class);
    when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(mockPortfolio);
    when(mockPortfolio.findAccount(accountId)).thenReturn(Optional.of(account));

    MarketAssetQuote shopQuote = mock(MarketAssetQuote.class);
    Map<AssetSymbol, MarketAssetQuote> quoteCache = Map.of(shop, shopQuote);
    when(marketDataService.getBatchQuotes(anySet())).thenReturn(quoteCache);

    Money shopFee = Money.of(10, "CAD");
    Map<AccountId, Map<AssetSymbol, Money>> feeCache = Map.of(accountId, Map.of(shop, shopFee));
    when(transactionRepository.sumBuyFeesByAccountAndSymbol(List.of(accountId))).thenReturn(
        feeCache);

    AccountView expectedView = mock(AccountView.class);
    when(accountViewBuilder.build(eq(account), eq(quoteCache), anyMap())).thenReturn(expectedView);

    AccountView result = accountQueryService.getAccountSummary(query);

    assertThat(result).isNotNull().isEqualTo(expectedView);
    verify(marketDataService).getBatchQuotes(argThat(set -> set.contains(shop)));
  }

  // getAccountSummary - account not found throws AccountNotFoundException
  @Test
  void testGetAccountSummary_failure_accountNotFound_throwsException() {
    AccountId accId = AccountId.newId();
    PortfolioId pId = PortfolioId.newId();
    UserId uId = UserId.random();
    GetAccountSummaryQuery query = new GetAccountSummaryQuery(pId, uId, accId);

    Portfolio portfolio = mock(Portfolio.class);
    when(portfolio.findAccount(accId)).thenReturn(Optional.empty());
    when(portfolioLoader.loadUserPortfolio(pId, uId)).thenReturn(portfolio);

    assertThatThrownBy(() -> accountQueryService.getAccountSummary(query)).isInstanceOf(
        AccountNotFoundException.class).hasMessageContaining(accId.toString());
  }

  // getAccountSummary - account with no positions still returns view
  @Test
  void testGetAccountSummary_success_accountWithNoPositions_returnsViewWithEmptyPositions() {
    AccountId accId = AccountId.newId();
    GetAccountSummaryQuery query = new GetAccountSummaryQuery(PortfolioId.newId(), UserId.random(),
        accId);

    Account emptyAccount = mock(Account.class);
    when(emptyAccount.getPositionCount()).thenReturn(0); // Trigger the early return

    Portfolio portfolio = mock(Portfolio.class);
    when(portfolio.findAccount(accId)).thenReturn(Optional.of(emptyAccount));
    when(portfolioLoader.loadUserPortfolio(any(), any())).thenReturn(portfolio);

    AccountView expectedView = mock(AccountView.class);
    when(accountViewBuilder.build(emptyAccount, Map.of(), Map.of())).thenReturn(expectedView);
    AccountView result = accountQueryService.getAccountSummary(query);

    assertThat(result).isEqualTo(expectedView);
    // Verify we skipped the heavy lifting
    verifyNoInteractions(marketDataService, transactionRepository);
  }

  // getAccountSummary - fee cache miss for account returns empty fee map
  @Test
  void testGetAccountSummary_success_noFeesForAccount_buildsViewWithEmptyFees() {
    AccountId accId = AccountId.newId();
    GetAccountSummaryQuery query = new GetAccountSummaryQuery(PortfolioId.newId(), UserId.random(),
        accId);

    Account account = mock(Account.class);
    when(account.getAccountId()).thenReturn(accId);
    when(account.getPositionCount()).thenReturn(1); // Bypass the early return guard

    Portfolio portfolio = mock(Portfolio.class);
    when(portfolio.findAccount(accId)).thenReturn(Optional.of(account));
    when(portfolioLoader.loadUserPortfolio(any(), any())).thenReturn(portfolio);

    // Stubbing the required service calls that happen when positions > 0
    when(marketDataService.getBatchQuotes(anySet())).thenReturn(Map.of());
    when(transactionRepository.sumBuyFeesByAccountAndSymbol(anyList())).thenReturn(Map.of());

    accountQueryService.getAccountSummary(query);

    // We verify that because the feeCache was empty, the builder got an empty map
    // for the 3rd arg
    verify(accountViewBuilder).build(eq(account), anyMap(), eq(Map.of()));
  }

  // no test asserting that accountViewBuilder.build() is called with an empty map
  // when the repository returns no fees for that account.
  // Map<AssetSymbol, Money> feeBreakdown =
  // feeCache.getOrDefault(account.getAccountId(), Map.of());

  @Test
  void testAccountSummary_failure_nullQueryThrowsException() {
    assertThatThrownBy(() -> accountQueryService.getAccountSummary(null)).isInstanceOf(
        NullPointerException.class).hasMessageContaining("GetAccountSummaryQuery cannot be null");
  }
}