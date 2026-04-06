package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.Collections;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
    @DisplayName("getAllAccounts: returns empty page when portfolio has no accounts")
    void getAllAccountsEmptyPortfolioReturnsEmptyPage() {
      PortfolioId portfolioId = PortfolioId.newId();
      UserId userId = UserId.random();
      Portfolio portfolio = mock(Portfolio.class);
      // Using a concrete PageRequest is often safer than mocking for math logic
      Pageable pageable = PageRequest.of(0, 10);

      when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(portfolio);
      when(portfolio.hasAccounts()).thenReturn(false);

      Page<AccountView> result = accountQueryService.getAllAccounts(
          new GetAllAccountsQuery(portfolioId, userId, pageable));

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
      verifyNoInteractions(marketDataService, transactionRepository);
    }

    @Test
    @DisplayName("getAllAccounts: maps single account with market data and fees")
    void getAllAccountsSingleAccountReturnsMappedView() {
      UserId userId = UserId.random();
      PortfolioId portfolioId = PortfolioId.newId();
      AssetSymbol btc = new AssetSymbol("BTC");
      AccountId accountId = AccountId.newId();
      Pageable pageable = PageRequest.of(0, 10);

      Account account = mock(Account.class);
      when(account.getAccountId()).thenReturn(accountId);
      when(account.getPositionEntries())
          .thenReturn(Collections.unmodifiableCollection(Map.of(btc, mock(Position.class)).entrySet()));

      Portfolio portfolio = mock(Portfolio.class);
      when(portfolio.getAccounts()).thenReturn(List.of(account));
      when(portfolio.hasAccounts()).thenReturn(true);
      when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(portfolio);

      Map<AssetSymbol, MarketAssetQuote> quoteMap = Map.of(btc, mock(MarketAssetQuote.class));
      when(marketDataService.getBatchQuotes(anySet())).thenReturn(quoteMap);

      AccountView expectedView = mock(AccountView.class);
      when(accountViewBuilder.build(eq(account), eq(quoteMap), anyMap())).thenReturn(expectedView);

      Page<AccountView> result = accountQueryService.getAllAccounts(
          new GetAllAccountsQuery(portfolioId, userId, pageable));

      assertThat(result.getContent()).containsExactly(expectedView);
      assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getAllAccounts: handles pagination correctly by slicing list")
    void getAllAccountsSlicesListBasedOnPageable() {
      UserId userId = UserId.random();
      PortfolioId portfolioId = PortfolioId.newId();

      // Setup: 3 accounts, but we only want page 1 with size 2 (the 3rd account)
      Account acc1 = mock(Account.class);
      Account acc2 = mock(Account.class);
      Account acc3 = mock(Account.class);
      when(acc3.getAccountId()).thenReturn(AccountId.newId());
      when(acc3.getPositionEntries()).thenReturn(Collections.emptySet());

      Portfolio portfolio = mock(Portfolio.class);
      when(portfolio.hasAccounts()).thenReturn(true);
      when(portfolio.getAccounts()).thenReturn(List.of(acc1, acc2, acc3));
      when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(portfolio);

      Pageable pageable = PageRequest.of(1, 2); // Page 1 (offset 2), Size 2

      AccountView view3 = mock(AccountView.class);
      when(accountViewBuilder.build(eq(acc3), anyMap(), anyMap())).thenReturn(view3);
      when(marketDataService.getBatchQuotes(anySet())).thenReturn(Map.of());

      Page<AccountView> result = accountQueryService.getAllAccounts(
          new GetAllAccountsQuery(portfolioId, userId, pageable));

      assertThat(result.getContent()).hasSize(1).containsExactly(view3);
      assertThat(result.getTotalElements()).isEqualTo(3); // Total in DB
      assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("getAllAccounts: throws exception when query is null")
    void getAllAccountsNullQueryThrowsException() {
      assertThatThrownBy(() -> accountQueryService.getAllAccounts(null)).isInstanceOf(
          NullPointerException.class).hasMessageContaining("GetAllAccountsQuery cannot be null");
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

      Account account = new Account(accountId, "Resp", AccountType.RESP, Currency.CAD,
          PositionStrategy.ACB);
      AcbPosition updated = new AcbPosition(shop, AssetType.STOCK, Currency.CAD, Quantity.of(100),
          Money.of(2010, "CAD"), Instant.now(), Instant.now());
      account.applyPositionResult(shop, updated);

      Portfolio mockPortfolio = mock(Portfolio.class);
      when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(mockPortfolio);
      when(mockPortfolio.findAccount(accountId)).thenReturn(Optional.of(account));

      Map<AssetSymbol, MarketAssetQuote> quoteCache = Map.of(shop, mock(MarketAssetQuote.class));
      when(marketDataService.getBatchQuotes(anySet())).thenReturn(quoteCache);

      AccountView expectedView = mock(AccountView.class);
      when(accountViewBuilder.build(eq(account), eq(quoteCache), anyMap())).thenReturn(
          expectedView);

      AccountView result = accountQueryService.getAccountSummary(
          new GetAccountSummaryQuery(portfolioId, userId, accountId));

      assertThat(result).isEqualTo(expectedView);
    }

    @Test
    @DisplayName("getAccountSummary: throws exception when account ID not found")
    void getAccountSummaryAccountNotFoundThrowsException() {
      AccountId accId = AccountId.newId();
      Portfolio portfolio = mock(Portfolio.class);
      when(portfolio.findAccount(accId)).thenReturn(Optional.empty());
      when(portfolioLoader.loadUserPortfolio(any(), any())).thenReturn(portfolio);

      assertThatThrownBy(() -> accountQueryService.getAccountSummary(
          new GetAccountSummaryQuery(PortfolioId.newId(), UserId.random(), accId))).isInstanceOf(
              AccountNotFoundException.class)
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

      AccountView result = accountQueryService.getAccountSummary(
          new GetAccountSummaryQuery(PortfolioId.newId(), UserId.random(), accId));

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

      accountQueryService.getAccountSummary(
          new GetAccountSummaryQuery(PortfolioId.newId(), UserId.random(), accId));

      verify(accountViewBuilder).build(eq(account), anyMap(), eq(Map.of()));
    }

    @Test
    @DisplayName("getAccountSummary: throws exception when query is null")
    void getAccountSummaryNullQueryThrowsException() {
      assertThatThrownBy(() -> accountQueryService.getAccountSummary(null)).isInstanceOf(
          NullPointerException.class).hasMessageContaining("GetAccountSummaryQuery cannot be null");
    }
  }
}