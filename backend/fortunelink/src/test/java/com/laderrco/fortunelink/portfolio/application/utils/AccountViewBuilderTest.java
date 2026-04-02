package com.laderrco.fortunelink.portfolio.application.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.PositionView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountViewBuilderTest {

  private static final Currency USD = Currency.USD;
  @Mock
  private PortfolioValuationService valuationService;
  @Mock
  private PortfolioViewMapper viewMapper;
  @InjectMocks
  private AccountViewBuilder accountViewBuilder;
  private Account account;
  private AssetSymbol appleSymbol;
  private MarketAssetQuote appleQuote;
  private Money zeroMoney;

  @BeforeEach
  void setUp() {
    appleSymbol = new AssetSymbol("AAPL");
    appleQuote = mock(MarketAssetQuote.class);
    zeroMoney = Money.zero(USD);

    Position mockPosition = mock(AcbPosition.class);
    Map<AssetSymbol, Position> positions = Map.of(appleSymbol, mockPosition);

    account = mock(Account.class);
    lenient().when(account.getAccountCurrency()).thenReturn(USD);
    when(account.getCashBalance()).thenReturn(zeroMoney);

    when(account.getPositionEntries()).thenReturn(positions.entrySet());
  }

  @Test
  @DisplayName("build: should include fees in position views when fee data is provided")
  void buildshouldIncludeFeesInPositionViews() {
    Money fee = Money.of(10, USD);
    Map<AssetSymbol, MarketAssetQuote> quotes = Map.of(appleSymbol, appleQuote);
    Map<AssetSymbol, Money> fees = Map.of(appleSymbol, fee);

    PositionView mockPosView = mock(PositionView.class);
    AccountView expectedView = mock(AccountView.class);

    when(viewMapper.toPositionView(any(), eq(appleQuote), eq(fee))).thenReturn(mockPosView);
    when(valuationService.calculateAccountValue(account, quotes)).thenReturn(zeroMoney);
    when(viewMapper.toAccountView(eq(account), any(), any(), any())).thenReturn(expectedView);

    AccountView result = accountViewBuilder.build(account, quotes, fees);

    assertEquals(expectedView, result);
    verify(viewMapper).toPositionView(any(), eq(appleQuote), eq(fee));
  }

  @Test
  @DisplayName("build: should use zero fees when symbol is missing in the fee map")
  void buildshouldUseZeroFeesWhenSymbolMissingInFeeMap() {
    Map<AssetSymbol, MarketAssetQuote> quotes = Map.of(appleSymbol, appleQuote);
    Map<AssetSymbol, Money> emptyFees = Collections.emptyMap();

    accountViewBuilder.build(account, quotes, emptyFees);

    verify(viewMapper).toPositionView(any(), eq(appleQuote), eq(zeroMoney));
  }

  @Test
  @DisplayName("buildSummary: should map positions without requesting fee data")
  void buildSummaryshouldNotRequestFees() {
    Map<AssetSymbol, MarketAssetQuote> quotes = Map.of(appleSymbol, appleQuote);
    PositionView mockPosView = mock(PositionView.class);
    AccountView expectedView = mock(AccountView.class);

    when(viewMapper.toPositionView(any(), eq(appleQuote))).thenReturn(mockPosView);
    when(valuationService.calculateAccountValue(account, quotes)).thenReturn(zeroMoney);
    when(viewMapper.toAccountView(eq(account), any(), any(), any())).thenReturn(expectedView);

    AccountView result = accountViewBuilder.buildSummary(account, quotes);

    assertEquals(expectedView, result);
    verify(viewMapper).toPositionView(any(), eq(appleQuote));
  }
}