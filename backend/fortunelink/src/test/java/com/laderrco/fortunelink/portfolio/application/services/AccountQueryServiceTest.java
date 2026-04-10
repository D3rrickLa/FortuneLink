package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountLifecycleState;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  @DisplayName("getAllAccounts")
  class GetAllAccountsTests {
    @Test
    @DisplayName("getAllAccounts: returns empty page when no accounts found")
    void getAllAccountsNoAccountsReturnsEmptyPage() {
      PortfolioId portfolioId = PortfolioId.newId();
      UserId userId = UserId.random();
      GetAllAccountsQuery query = new GetAllAccountsQuery(portfolioId, userId, 0, 10);

      when(accountQueryRepository.findByPortfolioId(eq(portfolioId), any(Pageable.class)))
          .thenReturn(Page.empty());

      Page<AccountView> result = accountQueryService.getAllAccounts(query);

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();

      verify(portfolioLoader).validateOwnership(portfolioId, userId);
      // Important: Ensure no batch calls are made if the page is empty
      verifyNoInteractions(marketDataService, transactionRepository, accountViewBuilder);
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
      when(projection.getLifecycleState()).thenReturn("ACTIVE");

      when(accountQueryRepository.findByPortfolioId(eq(portfolioId), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(projection), PageRequest.of(0, 10), 1));

      Map<AssetSymbol, Quantity> quantities = Map.of(new AssetSymbol("BTC"), Quantity.of(1));
      when(accountQueryRepository.findQuantitiesForAccounts(anyList()))
          .thenReturn(Map.of(accountId, quantities));

      when(marketDataService.getBatchQuotes(anySet())).thenReturn(Map.of());

      AccountView expectedView = mock(AccountView.class);
      when(accountViewBuilder.buildFromProjection(eq(projection), eq(quantities), anyMap(), eq(Map.of())))
          .thenReturn(expectedView);

      Page<AccountView> result = accountQueryService.getAllAccounts(query);

      assertThat(result.getContent()).containsExactly(expectedView);
    }

    @Test
    @DisplayName("getAllAccounts: handles page out of bounds gracefully")
    void getAllAccountsOutOfBoundsReturnsEmptyPageFromRepo() {
      PortfolioId portfolioId = PortfolioId.newId();
      UserId userId = UserId.random();
      GetAllAccountsQuery query = new GetAllAccountsQuery(portfolioId, userId, 5, 10);

      when(accountQueryRepository.findByPortfolioId(eq(portfolioId), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(), PageRequest.of(5, 10), 2));

      Page<AccountView> result = accountQueryService.getAllAccounts(query);

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isEqualTo(2);

      // Ensure early return optimization works
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
      AccountSummaryProjection projection = mock(AccountSummaryProjection.class);
      when(projection.getId()).thenReturn(accountUuid);

      when(accountQueryRepository.findByPortfolioId(eq(portfolioId), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(projection), PageRequest.of(0, 10), 1));

      AccountView expectedView = mock(AccountView.class);
      when(accountViewBuilder.buildFromProjection(any(), anyMap(), anyMap(), anyMap())).thenReturn(expectedView);

      accountQueryService.getAllAccounts(query);

      // Verify batching works but quote service is skipped due to no symbols
      verifyNoInteractions(marketDataService);
    }

    @Test
    @DisplayName("getAllAccounts: fails if ownership validation fails")
    void getAllAccountsFailsOnInvalidOwnership() {
      PortfolioId portfolioId = PortfolioId.newId();
      UserId userId = UserId.random();
      GetAllAccountsQuery query = new GetAllAccountsQuery(portfolioId, userId, 0, 10);

      doThrow(new RuntimeException("Unauthorized"))
          .when(portfolioLoader).validateOwnership(portfolioId, userId);

      assertThatThrownBy(() -> accountQueryService.getAllAccounts(query))
          .isInstanceOf(RuntimeException.class);

      verifyNoInteractions(accountQueryRepository, transactionRepository, marketDataService);
    }

    @Test
    @DisplayName("getAllAccounts: skips batch lookups when all accounts are inactive (CLOSED/REPLAYING)")
    void getAllAccountsShortCircuitsForInactiveAccounts() {
      AccountSummaryProjection replayingAcc = mock(AccountSummaryProjection.class);
      when(replayingAcc.getLifecycleState()).thenReturn(AccountLifecycleState.REPLAYING.name());

      AccountSummaryProjection closedAcc = mock(AccountSummaryProjection.class);
      when(closedAcc.getLifecycleState()).thenReturn(AccountLifecycleState.CLOSED.name());

      when(accountQueryRepository.findByPortfolioId(any(), any()))
          .thenReturn(new PageImpl<>(List.of(replayingAcc, closedAcc)));

      when(accountViewBuilder.buildFromProjection(any(), anyMap(), anyMap(), anyMap()))
          .thenReturn(mock(AccountView.class));

      accountQueryService.getAllAccounts(new GetAllAccountsQuery(PortfolioId.newId(), UserId.random(), 0, 10));

      verify(accountQueryRepository, never()).findQuantitiesForAccounts(any());
    }

    @Test
    @DisplayName("getAllAccounts: proceeds to batch lookup if at least one account is ACTIVE")
    void getAllAccountsProceedsIfOneAccountIsActive() {
      AccountSummaryProjection closedAcc = mock(AccountSummaryProjection.class);
      when(closedAcc.getLifecycleState()).thenReturn(AccountLifecycleState.CLOSED.name());
      when(closedAcc.getId()).thenReturn(UUID.randomUUID());

      AccountSummaryProjection activeAcc = mock(AccountSummaryProjection.class);
      when(activeAcc.getLifecycleState()).thenReturn(AccountLifecycleState.ACTIVE.name());
      when(activeAcc.getId()).thenReturn(UUID.randomUUID());

      when(accountQueryRepository.findByPortfolioId(any(), any()))
          .thenReturn(new PageImpl<>(List.of(closedAcc, activeAcc)));
      when(accountQueryRepository.findQuantitiesForAccounts(any())).thenReturn(Map.of());

      accountQueryService.getAllAccounts(new GetAllAccountsQuery(PortfolioId.newId(), UserId.random(), 0, 10));

      // Assert
      verify(accountQueryRepository, times(1)).findQuantitiesForAccounts(any());
    }
  }

  @Nested
  @DisplayName("getAccountSummary")
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
              AccountNotFoundException.class)
          .hasMessageContaining(accountId.toString());
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