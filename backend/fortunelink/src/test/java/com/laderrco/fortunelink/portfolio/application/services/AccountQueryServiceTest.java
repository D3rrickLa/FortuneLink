package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.queries.GetAllAccountsQuery;
import com.laderrco.fortunelink.portfolio.application.utils.AccountViewBuilder;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
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
import java.util.List;
import java.util.Map;
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

  private PortfolioViewMapper portfolioViewMapper;

  @InjectMocks
  private AccountQueryService accountQueryService;


  @BeforeEach
  void setUp() {
  }

  @Test
  void testGetAllAccounts_success_getAllAccounts() {
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
    when(portfolioLoader.loadUserPortfolio(portfolioId, userId)).thenReturn(portfolio);

    MarketAssetQuote mockQuote = mock(MarketAssetQuote.class);
    Map<AssetSymbol, MarketAssetQuote> quoteMap = Map.of(btc, mockQuote);
    when(marketDataService.getBatchQuotes(anySet())).thenReturn(quoteMap);

    Money mockFee = Money.of("5", Currency.USD);
    Map<AssetSymbol, Money> feeMap = Map.of(btc, mockFee);
    when(transactionRepository.sumBuyFeesByAccountAndSymbol(List.of(accountId))).thenReturn(
        Map.of(accountId, feeMap));

    AccountView expectedView = mock(AccountView.class);
    when(accountViewBuilder.build(eq(account), eq(quoteMap), eq(feeMap)))
        .thenReturn(expectedView);

    List<AccountView> result = accountQueryService.getAllAccounts(query);
    assertThat(result).hasSize(1).containsExactly(expectedView);

    verify(marketDataService).getBatchQuotes(argThat(set -> set.contains(btc)));
    verify(transactionRepository).sumBuyFeesByAccountAndSymbol(List.of(accountId));
  }

  @Test
  void testGetAllAccounts_failure_nullQueryThrowsException() {
    assertThatThrownBy(() -> accountQueryService.getAllAccounts(null)).isInstanceOf(
        NullPointerException.class).hasMessageContaining("GetAllAccountsQuery cannot be null");
  }
}