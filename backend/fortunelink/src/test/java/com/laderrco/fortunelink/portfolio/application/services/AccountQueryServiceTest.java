package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Account Query Service Tests")
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

  @Nested
  @DisplayName("getAllAccounts()")
  class GetAllAccountsTests {
    @Test
    @DisplayName("getAllAccounts: returns empty list when portfolio has no accounts")
    void getAllAccountsEmptyPortfolioReturnsEmptyList() {
      PortfolioId portfolioId = PortfolioId.newId();
      UserId userId = UserId.random();
      Portfolio portfolio = mock(Portfolio.class);

      when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(portfolio);

      List<AccountView> result = accountQueryService.getAllAccounts(new GetAllAccountsQuery(portfolioId, userId));

      assertThat(result).isEmpty();
      verifyNoInteractions(marketDataService, transactionRepository);
    }

    @Test
    @DisplayName("getAllAccounts: maps single account with market data and fees")
    void getAllAccountsSingleAccountReturnsMappedView() {
      UserId userId = UserId.random();
      PortfolioId portfolioId = PortfolioId.newId();
      AssetSymbol btc = new AssetSymbol("BTC");
      AccountId accountId = AccountId.newId();

      Account account = mock(Account.class);
      Map<AssetSymbol, Position> positions = Map.of(btc, mock(AcbPosition.class));
      when(account.getAccountId()).thenReturn(accountId);
      when(account.getPositionEntries()).thenReturn(positions.entrySet());

      Portfolio portfolio = mock(Portfolio.class);
      when(portfolio.getAccounts()).thenReturn(List.of(account));
      when(portfolio.hasAccounts()).thenReturn(true);
      when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(portfolio);

      MarketAssetQuote mockQuote = mock(MarketAssetQuote.class);
      Map<AssetSymbol, MarketAssetQuote> quoteMap = Map.of(btc, mockQuote);
      when(marketDataService.getBatchQuotes(anySet())).thenReturn(quoteMap);

      Map<AssetSymbol, Money> feeMap = Map.of(btc, Money.of("5", Currency.USD));
      when(transactionRepository.sumBuyFeesByAccountAndSymbol(anyList())).thenReturn(Map.of(accountId, feeMap));

      AccountView expectedView = mock(AccountView.class);
      when(accountViewBuilder.build(eq(account), eq(quoteMap), eq(feeMap))).thenReturn(expectedView);

      List<AccountView> result = accountQueryService.getAllAccounts(new GetAllAccountsQuery(portfolioId, userId));

      assertThat(result).containsExactly(expectedView);
    }

    @Test
    @DisplayName("getAllAccounts: maps multiple accounts and aggregates cache data")
    void getAllAccountsMultipleAccountsMapsAllWithCorrectCaches() {
      UserId userId = UserId.random();
      PortfolioId portfolioId = PortfolioId.newId();
      AccountId acc1Id = AccountId.newId();
      AccountId acc2Id = AccountId.newId();
      AssetSymbol btc = new AssetSymbol("BTC");
      AssetSymbol eth = new AssetSymbol("ETH");

      Account acc1 = mock(Account.class);
      Map<AssetSymbol, Position> positions1 = Map.of(btc, mock(AcbPosition.class));
      when(acc1.getAccountId()).thenReturn(acc1Id);
      when(acc1.getPositionEntries()).thenReturn(positions1.entrySet());

      Account acc2 = mock(Account.class);
      Map<AssetSymbol, Position> positions2 = Map.of(eth, mock(AcbPosition.class));
      when(acc2.getAccountId()).thenReturn(acc2Id);
      when(acc2.getPositionEntries()).thenReturn( positions2.entrySet());

      Portfolio portfolio = mock(Portfolio.class);
      when(portfolio.hasAccounts()).thenReturn(true);
      when(portfolio.getAccounts()).thenReturn(List.of(acc1, acc2));
      when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(portfolio);

      Map<AssetSymbol, MarketAssetQuote> quoteCache = Map.of(btc, mock(MarketAssetQuote.class), eth,
          mock(MarketAssetQuote.class));
      when(marketDataService.getBatchQuotes(anySet())).thenReturn(quoteCache);

      Map<AssetSymbol, Money> feesAcc1 = Map.of(btc, Money.of("10", Currency.USD));
      Map<AssetSymbol, Money> feesAcc2 = Map.of(eth, Money.of("20", Currency.USD));
      when(transactionRepository.sumBuyFeesByAccountAndSymbol(List.of(acc1Id, acc2Id)))
          .thenReturn(Map.of(acc1Id, feesAcc1, acc2Id, feesAcc2));

      AccountView view1 = mock(AccountView.class);
      AccountView view2 = mock(AccountView.class);
      when(accountViewBuilder.build(acc1, quoteCache, feesAcc1)).thenReturn(view1);
      when(accountViewBuilder.build(acc2, quoteCache, feesAcc2)).thenReturn(view2);

      List<AccountView> result = accountQueryService.getAllAccounts(new GetAllAccountsQuery(portfolioId, userId));

      assertThat(result).containsExactly(view1, view2);
    }

    @Test
    @DisplayName("getAllAccounts: throws exception when query is null")
    void getAllAccountsNullQueryThrowsException() {
      assertThatThrownBy(() -> accountQueryService.getAllAccounts(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("GetAllAccountsQuery cannot be null");
    }
  }

  @Nested
  @DisplayName("getAccountSummary()")
  class GetAccountSummaryTests {
    @Test
    @DisplayName("getAccountSummary: successfully retrieves summary for valid account")
    void getAccountSummaryValidIdReturnsMappedView() {
      UserId userId = UserId.random();
      PortfolioId portfolioId = PortfolioId.newId();
      AccountId accountId = AccountId.newId();
      AssetSymbol shop = new AssetSymbol("SHOP.TO");

      Account account = new Account(accountId, "Resp", AccountType.RESP, Currency.CAD, PositionStrategy.ACB);
      account.updatePosition(shop, new AcbPosition(shop, AssetType.STOCK, Currency.CAD, Quantity.of(100),
          Money.of(2010, "CAD"), Instant.now(), Instant.now()));

      Portfolio mockPortfolio = mock(Portfolio.class);
      when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(mockPortfolio);
      when(mockPortfolio.findAccount(accountId)).thenReturn(Optional.of(account));

      Map<AssetSymbol, MarketAssetQuote> quoteCache = Map.of(shop, mock(MarketAssetQuote.class));
      when(marketDataService.getBatchQuotes(anySet())).thenReturn(quoteCache);

      Map<AccountId, Map<AssetSymbol, Money>> feeCache = Map.of(accountId, Map.of(shop, Money.of(10, "CAD")));
      when(transactionRepository.sumBuyFeesByAccountAndSymbol(List.of(accountId))).thenReturn(feeCache);

      AccountView expectedView = mock(AccountView.class);
      when(accountViewBuilder.build(eq(account), eq(quoteCache), anyMap())).thenReturn(expectedView);

      AccountView result = accountQueryService
          .getAccountSummary(new GetAccountSummaryQuery(portfolioId, userId, accountId));

      assertThat(result).isEqualTo(expectedView);
    }

    @Test
    @DisplayName("getAccountSummary: throws exception when account ID not found")
    void getAccountSummaryAccountNotFoundThrowsException() {
      AccountId accId = AccountId.newId();
      Portfolio portfolio = mock(Portfolio.class);
      when(portfolio.findAccount(accId)).thenReturn(Optional.empty());
      when(portfolioLoader.loadUserPortfolio(any(), any())).thenReturn(portfolio);

      assertThatThrownBy(() -> accountQueryService
          .getAccountSummary(new GetAccountSummaryQuery(PortfolioId.newId(), UserId.random(), accId)))
          .isInstanceOf(AccountNotFoundException.class)
          .hasMessageContaining(accId.toString());
    }

    @Test
    @DisplayName("getAccountSummary: returns view with empty data for account with no positions")
    void getAccountSummaryAccountWithNoPositionsReturnsEmptyView() {
      AccountId accId = AccountId.newId();
      Account emptyAccount = mock(Account.class);
      when(emptyAccount.getPositionCount()).thenReturn(0);

      Portfolio portfolio = mock(Portfolio.class);
      when(portfolio.findAccount(accId)).thenReturn(Optional.of(emptyAccount));
      when(portfolioLoader.loadUserPortfolio(any(), any())).thenReturn(portfolio);

      AccountView expectedView = mock(AccountView.class);
      when(accountViewBuilder.build(emptyAccount, Map.of(), Map.of())).thenReturn(expectedView);

      AccountView result = accountQueryService
          .getAccountSummary(new GetAccountSummaryQuery(PortfolioId.newId(), UserId.random(), accId));

      assertThat(result).isEqualTo(expectedView);
      verifyNoInteractions(marketDataService, transactionRepository);
    }

    @Test
    @DisplayName("getAccountSummary: builds view with empty map when fee cache miss occurs")
    void getAccountSummaryFeeCacheMissBuildsViewWithEmptyFees() {
      AccountId accId = AccountId.newId();
      Account account = mock(Account.class);
      when(account.getAccountId()).thenReturn(accId);
      when(account.getPositionCount()).thenReturn(1);

      Portfolio portfolio = mock(Portfolio.class);
      when(portfolio.findAccount(accId)).thenReturn(Optional.of(account));
      when(portfolioLoader.loadUserPortfolio(any(), any())).thenReturn(portfolio);

      when(marketDataService.getBatchQuotes(anySet())).thenReturn(Map.of());
      when(transactionRepository.sumBuyFeesByAccountAndSymbol(anyList())).thenReturn(Map.of());

      accountQueryService.getAccountSummary(new GetAccountSummaryQuery(PortfolioId.newId(), UserId.random(), accId));

      verify(accountViewBuilder).build(eq(account), anyMap(), eq(Map.of()));
    }

    @Test
    @DisplayName("getAccountSummary: throws exception when query is null")
    void getAccountSummaryNullQueryThrowsException() {
      assertThatThrownBy(() -> accountQueryService.getAccountSummary(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("GetAccountSummaryQuery cannot be null");
    }
  }
}