package com.laderrco.fortunelink.portfolio.application.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.PositionView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountLifecycleState;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.projections.AccountSummaryProjection;
import com.laderrco.fortunelink.shared.enums.Precision;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
  @Mock
  private TransactionRepository transactionRepository;
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
    lenient().when(account.getCashBalance()).thenReturn(zeroMoney);
    lenient().when(account.getPositionEntries()).thenReturn(positions.entrySet());
    lenient().when(transactionRepository.countExcludedPositionAffecting(any())).thenReturn(0);

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
    when(viewMapper.toAccountView(eq(account), any(), any(), any(), anyBoolean(),
        anyInt())).thenReturn(expectedView);

    AccountView result = accountViewBuilder.build(account, quotes, fees);

    assertEquals(expectedView, result);
    verify(viewMapper).toPositionView(any(), eq(appleQuote), eq(fee));
  }

  @Test
  @DisplayName("build: should use zero fees when symbol is missing in the fee map")
  void buildshouldUseZeroFeesWhenSymbolMissingInFeeMap() {
    Map<AssetSymbol, MarketAssetQuote> quotes = Map.of(appleSymbol, appleQuote);
    Map<AssetSymbol, Money> emptyFees = Collections.emptyMap();
    lenient().when(transactionRepository.countExcludedPositionAffecting(any())).thenReturn(2);

    accountViewBuilder.build(account, quotes, emptyFees);

    verify(viewMapper).toPositionView(any(), eq(appleQuote), eq(zeroMoney));
    verify(transactionRepository).countExcludedPositionAffecting(any());
  }

  @Test
  @DisplayName("buildSummary: should map positions without requesting fee data")
  void buildSummaryshouldNotRequestFees() {
    Map<AssetSymbol, MarketAssetQuote> quotes = Map.of(appleSymbol, appleQuote);
    PositionView mockPosView = mock(PositionView.class);
    AccountView expectedView = mock(AccountView.class);

    when(viewMapper.toPositionView(any(), eq(appleQuote))).thenReturn(mockPosView);
    when(valuationService.calculateAccountValue(account, quotes)).thenReturn(zeroMoney);
    when(viewMapper.toAccountView(eq(account), any(), any(), any(), anyBoolean(),
        anyInt())).thenReturn(expectedView);

    AccountView result = accountViewBuilder.buildSummary(account, quotes);

    assertEquals(expectedView, result);
    verify(viewMapper).toPositionView(any(), eq(appleQuote));
  }

  @Nested
  @DisplayName("buildFromProjection")
  class BuildFromProjectionTests {
    private final UUID accountUuid = UUID.randomUUID();
    private final String accountName = "TFSA Trading";
    private final String currencyCode = "USD";
    private final BigDecimal cashBalance = new BigDecimal("1500.50");
    private final Instant createdDate = Instant.now();

    @Test
    @DisplayName("buildFromProjection: successfully creates AccountView with empty positions")
    void buildsViewFromProjectionSuccessfully() {

      AccountSummaryProjection projection = mock(AccountSummaryProjection.class);
      when(projection.getId()).thenReturn(accountUuid);
      when(projection.getName()).thenReturn(accountName);
      when(projection.getAccountType()).thenReturn("CHEQUING");
      when(projection.getBaseCurrencyCode()).thenReturn(currencyCode);
      when(projection.getCashBalanceAmount()).thenReturn(cashBalance);
      when(projection.getLifecycleState()).thenReturn(AccountLifecycleState.ACTIVE.name());
      when(projection.getCreatedDate()).thenReturn(createdDate);

      AccountView result = accountViewBuilder.buildFromProjection(projection, Map.of(), Map.of(),
          Map.of());

      assertThat(result.accountId()).isEqualTo(AccountId.fromString(accountUuid.toString()));
      assertThat(result.name()).isEqualTo(accountName);
      assertThat(result.type()).isEqualTo(AccountType.CHEQUING);
      assertThat(result.cashBalance().currency().getCode()).isEqualTo(currencyCode);
      assertThat(result.cashBalance().amount()).isEqualTo(
          cashBalance.setScale(Precision.MONEY.getDecimalPlaces()));

      assertThat(result.totalValue().amount()).isEqualTo(
          cashBalance.setScale(Precision.MONEY.getDecimalPlaces()));
      assertThat(result.assets()).isEmpty();
      assertThat(result.creationDate()).isEqualTo(createdDate);
    }

    @Test
    @DisplayName("buildFromProjection: correctly maps complex account types")
    void mapsDifferentAccountTypesCorrectly() {

      AccountSummaryProjection projection = mock(AccountSummaryProjection.class);
      when(projection.getId()).thenReturn(accountUuid);
      when(projection.getAccountType()).thenReturn("RRSP");
      when(projection.getBaseCurrencyCode()).thenReturn("CAD");
      when(projection.getLifecycleState()).thenReturn(AccountLifecycleState.ACTIVE.name());
      when(projection.getCashBalanceAmount()).thenReturn(BigDecimal.ZERO);

      AccountView result = accountViewBuilder.buildFromProjection(projection, Map.of(), Map.of(),
          Map.of());

      assertThat(result.type()).isEqualTo(AccountType.RRSP);
    }

    @Test
    @DisplayName("buildFromProjection: ignores quotes and fees as positions are currently empty")
    void ignoresEnrichmentDataForProjection() {
      AccountSummaryProjection projection = mock(AccountSummaryProjection.class);
      when(projection.getId()).thenReturn(accountUuid);
      when(projection.getAccountType()).thenReturn("CHEQUING");
      when(projection.getBaseCurrencyCode()).thenReturn("CAD");
      when(projection.getCashBalanceAmount()).thenReturn(BigDecimal.TEN);
      when(projection.getLifecycleState()).thenReturn(AccountLifecycleState.ACTIVE.name());
      AssetSymbol symbol = new AssetSymbol("AAPL");
      Map<AssetSymbol, MarketAssetQuote> quotes = Map.of(symbol, mock(MarketAssetQuote.class));
      Map<AssetSymbol, Money> fees = Map.of(symbol, Money.of(5, "CAD"));

      AccountView result = accountViewBuilder.buildFromProjection(projection, Map.of(), quotes,
          fees);
      assertThat(result.assets()).isEmpty();
      assertThat(result.totalValue().amount()).isEqualTo(
          BigDecimal.TEN.setScale(Precision.MONEY.getDecimalPlaces()));
    }

    @Test
    @DisplayName("buildFromProjection: calculates market value correctly across different quote scenarios")
    void buildFromProjectionCalculatesMarketValue() {
      Currency cad = Currency.of("CAD");
      AssetSymbol apple = new AssetSymbol("AAPL");
      AssetSymbol google = new AssetSymbol("GOOGL");
      AssetSymbol tesla = new AssetSymbol("TSLA");

      AccountSummaryProjection projection = mock(AccountSummaryProjection.class);
      when(projection.getBaseCurrencyCode()).thenReturn("CAD");
      when(projection.getCashBalanceAmount()).thenReturn(new BigDecimal("100.00"));
      when(projection.getAccountType()).thenReturn("FHSA");
      when(projection.getLifecycleState()).thenReturn("ACTIVE");
      when(projection.getId()).thenReturn(accountUuid);

      Map<AssetSymbol, Quantity> quantities = Map.of(apple, Quantity.of(10), google, Quantity.of(5),
          tesla, Quantity.of(2));

      MarketAssetQuote appleQuote = mock(MarketAssetQuote.class);
      Price applePrice = new Price(new Money(new BigDecimal("150.00"), cad));
      when(appleQuote.currentPrice()).thenReturn(applePrice);

      MarketAssetQuote teslaQuote = mock(MarketAssetQuote.class);
      Price zeroPrice = new Price(Money.zero(cad));
      when(teslaQuote.currentPrice()).thenReturn(zeroPrice);

      Map<AssetSymbol, MarketAssetQuote> allQuotes = Map.of(apple, appleQuote, tesla, teslaQuote);

      AccountView result = accountViewBuilder.buildFromProjection(projection, quantities, allQuotes,
          Map.of());

      BigDecimal expectedTotal = new BigDecimal("1600.00");
      assertEquals(0, expectedTotal.compareTo(result.totalValue().amount()),
          String.format("Total value should be %s but was %s", expectedTotal,
              result.totalValue().amount()));
      assertEquals(0, new BigDecimal("100.00").compareTo(result.cashBalance().amount()));
    }
  }
}