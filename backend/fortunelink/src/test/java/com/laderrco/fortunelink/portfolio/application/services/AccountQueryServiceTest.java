package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetAllAccountsQuery;
import com.laderrco.fortunelink.portfolio.application.repositories.AccountQueryRepository;
import com.laderrco.fortunelink.portfolio.application.utils.AccountViewBuilder;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.projections.AccountSummaryProjection;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
  private AccountQueryRepository accountQueryRepository;

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
    @DisplayName("getAllAccounts: returns empty page when no accounts found")
    void getAllAccountsNoAccountsReturnsEmptyPage() {
      PortfolioId portfolioId = PortfolioId.newId();
      UserId userId = UserId.random();
      GetAllAccountsQuery query = new GetAllAccountsQuery(portfolioId, userId, 0, 10);

      when(accountQueryRepository.findByPortfolioId(eq(portfolioId),
          any(Pageable.class))).thenReturn(Page.empty());

      Page<AccountView> result = accountQueryService.getAllAccounts(query);

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();

      verify(portfolioLoader).validateOwnership(portfolioId, userId);
      verifyNoInteractions(marketDataService, transactionRepository);
    }

    @Test
    @DisplayName("getAllAccounts: maps projections with batch data")
    void getAllAccountsMapsProjectionsWithBatchData() {
      PortfolioId portfolioId = PortfolioId.newId();
      UserId userId = UserId.random();
      GetAllAccountsQuery query = new GetAllAccountsQuery(portfolioId, userId, 0, 10);

      UUID accountUuid = UUID.randomUUID();
      AccountId accountId = AccountId.fromString(accountUuid.toString());
      AccountSummaryProjection projection = mock(AccountSummaryProjection.class);
      when(projection.getId()).thenReturn(accountUuid);

      when(accountQueryRepository.findByPortfolioId(eq(portfolioId),
          any(Pageable.class))).thenReturn(
          new PageImpl<>(List.of(projection), PageRequest.of(0, 10), 1));

      when(accountQueryRepository.findSymbolsForAccounts(anyList())).thenReturn(
          Map.of(accountId, Set.of(new AssetSymbol("BTC"))));

      when(marketDataService.getBatchQuotes(anySet())).thenReturn(Map.of());
      when(transactionRepository.sumBuyFeesBySymbolForAccounts(anyList())).thenReturn(Map.of());

      AccountView expectedView = mock(AccountView.class);
      when(accountViewBuilder.buildFromProjection(eq(projection), anyMap(), anyMap())).thenReturn(
          expectedView);

      Page<AccountView> result = accountQueryService.getAllAccounts(query);

      assertThat(result.getContent()).containsExactly(expectedView);
      assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getAllAccounts: handles page out of bounds gracefully via repository")
    void getAllAccountsOutOfBoundsReturnsEmptyPageFromRepo() {
      PortfolioId portfolioId = PortfolioId.newId();
      UserId userId = UserId.random();
      GetAllAccountsQuery query = new GetAllAccountsQuery(portfolioId, userId, 5, 10);

      // Mock returning a page that is empty (content) but has a total count of 2
      when(accountQueryRepository.findByPortfolioId(eq(portfolioId),
          any(Pageable.class))).thenAnswer(invocation -> {
        Pageable requestedPageable = invocation.getArgument(1);
        return new PageImpl<>(List.of(), requestedPageable, 2);
      });

      Page<AccountView> result = accountQueryService.getAllAccounts(query);

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isEqualTo(2);
      assertThat(result.getNumber()).isEqualTo(5);

      // Now this will successfully pass because of the early return in the service!
      verify(accountQueryRepository, times(1)).findByPortfolioId(eq(portfolioId), any());
      verifyNoInteractions(marketDataService, transactionRepository, accountViewBuilder);
    }

    @Test
    @DisplayName("getAllAccounts: skips market data call when accounts have no symbols")
    void getAllAccountsNoSymbolsSkipsMarketDataCall() {
      PortfolioId portfolioId = PortfolioId.newId();
      UserId userId = UserId.random();
      GetAllAccountsQuery query = new GetAllAccountsQuery(portfolioId, userId, 0, 10);

      UUID accountUuid = UUID.randomUUID();
      AccountId accountId = AccountId.fromString(accountUuid.toString());
      AccountSummaryProjection projection = mock(AccountSummaryProjection.class);
      when(projection.getId()).thenReturn(accountUuid);

      when(accountQueryRepository.findByPortfolioId(eq(portfolioId),
          any(Pageable.class))).thenReturn(
          new PageImpl<>(List.of(projection), PageRequest.of(0, 10), 1));

      when(accountQueryRepository.findSymbolsForAccounts(List.of(accountId))).thenReturn(
          Map.of(accountId, Collections.emptySet()));

      when(transactionRepository.sumBuyFeesBySymbolForAccounts(anyList())).thenReturn(Map.of());

      AccountView expectedView = mock(AccountView.class);
      when(accountViewBuilder.buildFromProjection(eq(projection), eq(Map.of()),
          anyMap())).thenReturn(expectedView);

      Page<AccountView> result = accountQueryService.getAllAccounts(query);

      assertThat(result.getContent()).containsExactly(expectedView);

      verifyNoInteractions(marketDataService);
      verify(accountQueryRepository).findSymbolsForAccounts(anyList());
      verify(transactionRepository).sumBuyFeesBySymbolForAccounts(anyList());
    }

    @Test
    @DisplayName("getAllAccounts: fails if ownership validation fails")
    void getAllAccountsFailsOnInvalidOwnership() {
      PortfolioId portfolioId = PortfolioId.newId();
      UserId userId = UserId.random();
      GetAllAccountsQuery query = new GetAllAccountsQuery(portfolioId, userId, 0, 10);

      doThrow(new RuntimeException("Unauthorized")).when(portfolioLoader)
          .validateOwnership(portfolioId, userId);

      assertThatThrownBy(() -> accountQueryService.getAllAccounts(query)).isInstanceOf(
          RuntimeException.class);

      verifyNoInteractions(accountQueryRepository);
    }
  }

  @Nested
  @DisplayName("getAccountSummary()")
  class GetAccountSummaryTests {

    private final UserId userId = UserId.random();
    private final PortfolioId portfolioId = PortfolioId.newId();
    private final AccountId accountId = AccountId.newId();

    @Test
    @DisplayName("getAccountSummary: successfully retrieves summary for valid account")
    void getAccountSummaryValidIdReturnsMappedView() {
      AssetSymbol shop = new AssetSymbol("SHOP.TO");

      // Prepare domain object
      Account account = new Account(accountId, "Resp", AccountType.RESP, Currency.CAD,
          PositionStrategy.ACB);
      AcbPosition updated = new AcbPosition(shop, AssetType.STOCK, Currency.CAD, Quantity.of(100),
          Money.of(2010, "CAD"), Instant.now(), Instant.now());
      account.applyPositionResult(shop, updated);

      // Mock Repository instead of PortfolioLoader
      when(accountQueryRepository.findByIdWithDetails(accountId, portfolioId, userId)).thenReturn(
          Optional.of(account));

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
    @DisplayName("getAccountSummary: throws exception when account ID not found or ownership fails")
    void getAccountSummaryAccountNotFoundThrowsException() {
      when(accountQueryRepository.findByIdWithDetails(accountId, portfolioId, userId)).thenReturn(
          Optional.empty());

      assertThatThrownBy(() -> accountQueryService.getAccountSummary(
          new GetAccountSummaryQuery(portfolioId, userId, accountId))).isInstanceOf(
          AccountNotFoundException.class).hasMessageContaining(accountId.toString());
    }

    @Test
    @DisplayName("getAccountSummary: returns view with empty data for account with no positions")
    void getAccountSummaryAccountWithNoPositionsReturnsEmptyView() {
      Account emptyAccount = mock(Account.class);
      when(emptyAccount.getPositionCount()).thenReturn(0);

      when(accountQueryRepository.findByIdWithDetails(accountId, portfolioId, userId)).thenReturn(
          Optional.of(emptyAccount));

      AccountView expectedView = mock(AccountView.class);
      when(accountViewBuilder.build(emptyAccount, Map.of(), Map.of())).thenReturn(expectedView);

      AccountView result = accountQueryService.getAccountSummary(
          new GetAccountSummaryQuery(portfolioId, userId, accountId));

      assertThat(result).isEqualTo(expectedView);
      verifyNoInteractions(marketDataService, transactionRepository);
    }

    @Test
    @DisplayName("getAccountSummary: builds view with empty map when fee cache miss occurs")
    void getAccountSummaryFeeCacheMissBuildsViewWithEmptyFees() {
      Account account = mock(Account.class);
      when(account.getAccountId()).thenReturn(accountId);
      when(account.getPositionCount()).thenReturn(1);

      when(accountQueryRepository.findByIdWithDetails(accountId, portfolioId, userId)).thenReturn(
          Optional.of(account));

      when(marketDataService.getBatchQuotes(anySet())).thenReturn(Map.of());
      when(transactionRepository.sumBuyFeesBySymbolForAccount(accountId)).thenReturn(Map.of());

      accountQueryService.getAccountSummary(
          new GetAccountSummaryQuery(portfolioId, userId, accountId));

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