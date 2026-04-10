package com.laderrco.fortunelink.portfolio.application.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioSummaryView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio.application.views.PositionView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.PercentageChange;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TaxLot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.FifoPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioViewMapper Unit Tests")
class PortfolioViewMapperTest {

  // --- Test Constants ---
  private static final PortfolioId PORTFOLIOID = PortfolioId.newId();
  private static final AccountId ACCOUNTID = AccountId.newId();
  private static final UserId USERID = UserId.random();
  private static final Currency CAD = Currency.of("CAD");
  private static final String SYMBOL = "SHOP.TO";
  private static final Instant NOW = Instant.now();
  @InjectMocks
  private PortfolioViewMapper mapper;

  @Nested
  @DisplayName("Portfolio Mapping")
  class PortfolioMapping {

    @Test
    @DisplayName("toNewPortfolioView should initialize with empty account views and zero value")
    void toNewPortfolioViewShouldMapBasicFields() {
      Portfolio portfolio = mock(Portfolio.class);
      when(portfolio.getPortfolioId()).thenReturn(PORTFOLIOID);
      when(portfolio.getDisplayCurrency()).thenReturn(CAD);
      when(portfolio.getAccounts()).thenReturn(Collections.emptyList());

      PortfolioView result = mapper.toNewPortfolioView(portfolio);

      assertThat(result.portfolioId()).isEqualTo(PORTFOLIOID);
      assertThat(result.totalValue().amount()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(result.accounts()).isEmpty();
    }

    @Test
    @DisplayName("toPortfolioSummaryView should map essential fields for list views")
    void toPortfolioSummaryViewShouldMapCorrectly() {
      Portfolio portfolio = mock(Portfolio.class);
      Money totalVal = new Money(new BigDecimal("1500.00"), CAD);
      when(portfolio.getPortfolioId()).thenReturn(PORTFOLIOID);
      when(portfolio.getName()).thenReturn("Retirement");

      PortfolioSummaryView result = mapper.toPortfolioSummaryView(portfolio, totalVal);

      assertThat(result.id()).isEqualTo(PORTFOLIOID);
      assertThat(result.totalValue()).isEqualTo(totalVal);
      assertThat(result.name()).isEqualTo("Retirement");
    }
  }

  @Nested
  @DisplayName("Account Mapping")
  class AccountMapping {

    @Test
    @DisplayName("toAccountView should assemble full view from position list")
    void toAccountViewShouldMapAllFields() {
      Account account = mock(Account.class);
      when(account.getAccountId()).thenReturn(ACCOUNTID);
      when(account.getAccountCurrency()).thenReturn(CAD);

      List<PositionView> positions = Collections.emptyList();
      Money total = new Money(new BigDecimal("100.00"), CAD);
      Money cash = new Money(new BigDecimal("50.00"), CAD);

      AccountView result = mapper.toAccountView(account, positions, total, cash, false, 0);

      assertThat(result.accountId()).isEqualTo(ACCOUNTID);
      assertThat(result.totalValue()).isEqualTo(total);
      assertThat(result.cashBalance()).isEqualTo(cash);
    }
  }

  @Nested
  @DisplayName("Position Mapping & Calculations")
  class PositionMapping {

    @Mock
    private MarketAssetQuote quote;
    @Mock
    private Price currentPrice;
    @Mock
    private AcbPosition position;

    @BeforeEach
    void setUp() {
      lenient().when(position.symbol()).thenReturn(new AssetSymbol(SYMBOL));
      lenient().when(position.accountCurrency()).thenReturn(CAD);
      lenient().when(position.totalCostBasis())
          .thenReturn(new Money(new BigDecimal("1000.00"), CAD));
    }

    @Test
    @DisplayName("toPositionView should return zeroed price fields when quote is null")
    void toPositionViewWhenQuoteIsNullShouldHandleGracefully() {
      when(position.costPerUnit()).thenReturn(Money.of(125, CAD));
      PositionView result = mapper.toPositionView(position, null);

      assertThat(result.currentPrice().pricePerUnit().isZero()).isTrue();
      assertThat(result.marketValue().isZero()).isTrue();
      assertThat(result.unrealizedPnL().isZero()).isTrue();
      assertThat(result.returnPercentage()).isEqualTo(PercentageChange.ZERO);
    }

    @Test
    @DisplayName("toPositionView should calculate PnL and return percentage correctly")
    void toPositionViewWithQuoteShouldCalculateReturn() {
      // Setup: Cost 1000, Market 1100 = 10% gain
      when(quote.currentPrice()).thenReturn(currentPrice);
      when(currentPrice.pricePerUnit()).thenReturn(new Money(new BigDecimal("110.00"), CAD));

      AcbPosition acbPos = mock(AcbPosition.class);
      when(acbPos.symbol()).thenReturn(new AssetSymbol(SYMBOL));
      when(acbPos.accountCurrency()).thenReturn(CAD);
      when(acbPos.totalCostBasis()).thenReturn(new Money(new BigDecimal("1000.00"), CAD));
      when(acbPos.currentValue(any())).thenReturn(new Money(new BigDecimal("1100.00"), CAD));
      when(acbPos.firstAcquiredAt()).thenReturn(NOW);
      when(acbPos.lastModifiedAt()).thenReturn(NOW);
      when(acbPos.costPerUnit()).thenReturn(Money.of(125, CAD));

      PositionView result = mapper.toPositionView(acbPos, quote);

      assertThat(result.marketValue().amount()).isEqualByComparingTo("1100.00");
      assertThat(result.unrealizedPnL().amount()).isEqualByComparingTo("100.00");
      // Note: Per @implNote, PercentageChange stores decimal 0.10 for 10%
      assertThat(result.returnPercentage().change()).isEqualByComparingTo("0.10");
      assertThat(result.costBasisMethod()).isEqualTo("ACB");
    }

    @Test
    @DisplayName("toPositionView should handle FIFO logic and extract first lot date")
    void toPositionViewWithFifoShouldExtractCorrectDate() {
      FifoPosition fifoPos = mock(FifoPosition.class);
      TaxLot lot = mock(TaxLot.class);

      when(fifoPos.symbol()).thenReturn(new AssetSymbol(SYMBOL));
      when(fifoPos.accountCurrency()).thenReturn(CAD);
      when(fifoPos.totalQuantity()).thenReturn(Quantity.of(10));
      when(fifoPos.totalCostBasis()).thenReturn(Money.of(1250, CAD));
      when(fifoPos.costPerUnit()).thenReturn(Money.of(125, CAD));

      when(fifoPos.lots()).thenReturn(List.of(lot));
      when(lot.acquiredDate()).thenReturn(NOW);
      when(fifoPos.lastModifiedAt()).thenReturn(NOW);

      PositionView result = mapper.toPositionView(fifoPos, null);

      assertThat(result.costBasisMethod()).isEqualTo("FIFO");
      assertThat(result.firstAcquiredDate()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("toPositionView should defend against currency mismatch in fees")
    void toPositionViewWithMismatchedFeeCurrencyShouldDefaultToZero() {
      Currency usd = Currency.of("USD");
      Money mismatchedFee = new Money(BigDecimal.TEN, usd);
      when(position.costPerUnit()).thenReturn(Money.of(125, CAD));

      PositionView result = mapper.toPositionView(position, null, mismatchedFee);

      assertThat(result.totalFeesIncurred().currency()).isEqualTo(CAD);
      assertThat(result.totalFeesIncurred().isZero()).isTrue();
    }
  }

  @Nested
  @DisplayName("Edge Case & Branch Coverage")
  class EdgeCaseCoverage {

    @Test
    @DisplayName("calculateReturnPercentage should return ZERO when cost basis is zero (prevent division by zero)")
    void calculateReturnPercentageShouldHandleZeroCostBasis() {
      // Arrange: Use a position with 0 cost basis
      AcbPosition position = mock(AcbPosition.class);
      MarketAssetQuote quote = mock(MarketAssetQuote.class);
      Price price = mock(Price.class);

      when(position.symbol()).thenReturn(new AssetSymbol(SYMBOL));
      when(position.accountCurrency()).thenReturn(CAD);
      when(position.totalCostBasis()).thenReturn(Money.zero(CAD)); // Trigger!
      when(position.costPerUnit()).thenReturn(Money.zero(CAD));
      when(position.totalQuantity()).thenReturn(Quantity.of(0));

      when(quote.currentPrice()).thenReturn(price);
      when(price.pricePerUnit()).thenReturn(Money.of(100, CAD));
      when(position.currentValue(any())).thenReturn(Money.of(100, CAD));

      PositionView result = mapper.toPositionView(position, quote);

      assertThat(result.returnPercentage()).isEqualTo(PercentageChange.ZERO);
    }

    @Test
    @DisplayName("extractFirstAcquiredDate should return null when FIFO position has no lots")
    void extractFirstAcquiredDateShouldHandleEmptyFifoLots() {
      FifoPosition fifoPos = mock(FifoPosition.class);
      when(fifoPos.symbol()).thenReturn(new AssetSymbol(SYMBOL));
      when(fifoPos.accountCurrency()).thenReturn(CAD);
      when(fifoPos.totalCostBasis()).thenReturn(Money.zero(CAD));
      when(fifoPos.costPerUnit()).thenReturn(Money.zero(CAD));
      when(fifoPos.totalQuantity()).thenReturn(Quantity.of(0));
      when(fifoPos.lots()).thenReturn(Collections.emptyList()); // Trigger empty branch

      PositionView result = mapper.toPositionView(fifoPos, null);

      assertThat(result.firstAcquiredDate()).isNull();
    }

    @Test
    @DisplayName("toPortfolioView should map full state including stale data flag")
    void toPortfolioViewShouldMapAllFields() {
      Portfolio portfolio = mock(Portfolio.class);
      List<AccountView> accountViews = Collections.emptyList();
      Money totalValue = Money.of(5000, CAD);
      boolean hasStaleData = true;

      when(portfolio.getPortfolioId()).thenReturn(PORTFOLIOID);
      when(portfolio.getUserId()).thenReturn(USERID);
      when(portfolio.getName()).thenReturn("Main");
      when(portfolio.getCreatedAt()).thenReturn(NOW);

      PortfolioView result = mapper.toPortfolioView(portfolio, accountViews, totalValue,
          hasStaleData);

      assertThat(result.totalValue()).isEqualTo(totalValue);
      assertThat(result.hasStaleData()).isTrue();
      assertThat(result.portfolioId()).isEqualTo(PORTFOLIOID);
    }

    @Test
    @DisplayName("toNewAccountView should initialize with empty positions and zero balances")
    void toNewAccountViewShouldInitializeCorrectly() {
      Account account = mock(Account.class);
      when(account.getAccountId()).thenReturn(ACCOUNTID);
      when(account.getName()).thenReturn("Savings");
      when(account.getAccountCurrency()).thenReturn(CAD);
      when(account.getCreationDate()).thenReturn(NOW);

      AccountView result = mapper.toNewAccountView(account);

      assertThat(result.assets()).isEmpty();
      assertThat(result.cashBalance().isZero()).isTrue();
      assertThat(result.totalValue().isZero()).isTrue();
      assertThat(result.baseCurrency()).isEqualTo(CAD);
    }
  }

  @Nested
  @DisplayName("Defensive Null Handling Tests")
  class DefensiveNullHandling {

    @Test
    @DisplayName("toPositionView should handle null feesForSymbol by defaulting to zero")
    void toPositionViewWithNullFeesShouldDefaultToZero() {
      AcbPosition position = mock(AcbPosition.class);
      when(position.symbol()).thenReturn(new AssetSymbol(SYMBOL));
      when(position.accountCurrency()).thenReturn(CAD);
      when(position.totalCostBasis()).thenReturn(Money.zero(CAD));
      when(position.costPerUnit()).thenReturn(Money.zero(CAD));
      when(position.totalQuantity()).thenReturn(Quantity.of(0));

      // Act: Explicitly passing null for feesForSymbol
      PositionView result = mapper.toPositionView(position, null, null);

      // Assert: Ensure it didn't NPE and used Money.zero
      assertThat(result.totalFeesIncurred()).isNotNull();
      assertThat(result.totalFeesIncurred().isZero()).isTrue();
      assertThat(result.totalFeesIncurred().currency()).isEqualTo(CAD);
    }

    @Test
    @DisplayName("toPositionView should trigger 'quote unavailable' branch when quote.currentPrice() is null")
    void toPositionViewWhenQuotePriceIsNullShouldReturnInitialView() {
      AcbPosition position = mock(AcbPosition.class);
      MarketAssetQuote quote = mock(MarketAssetQuote.class);

      when(position.symbol()).thenReturn(new AssetSymbol(SYMBOL));
      when(position.accountCurrency()).thenReturn(CAD);
      when(position.totalCostBasis()).thenReturn(Money.of(100, CAD));
      when(position.costPerUnit()).thenReturn(Money.of(10, CAD));
      when(position.totalQuantity()).thenReturn(Quantity.of(10));

      when(quote.currentPrice()).thenReturn(null);

      PositionView result = mapper.toPositionView(position, quote);

      assertThat(result.currentPrice().isZero()).isTrue();
      assertThat(result.marketValue().isZero()).isTrue();
      assertThat(result.unrealizedPnL().isZero()).isTrue();
      assertThat(result.returnPercentage()).isEqualTo(PercentageChange.ZERO);

      assertThat(result.totalCostBasis().amount()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("toPositionView should trigger 'quote unavailable' branch when quote.currentPrice().pricePerUnit() is zero")
    void toPositionViewWhenQuotePriceIsZeroShouldReturnInitialView() {
      AcbPosition position = mock(AcbPosition.class);
      MarketAssetQuote quote = mock(MarketAssetQuote.class);

      when(position.symbol()).thenReturn(new AssetSymbol(SYMBOL));
      when(position.accountCurrency()).thenReturn(CAD);
      when(position.totalCostBasis()).thenReturn(Money.of(0, CAD));
      when(position.costPerUnit()).thenReturn(Money.of(10, CAD));
      when(position.totalQuantity()).thenReturn(Quantity.of(10));

      when(quote.currentPrice()).thenReturn(Price.zero(CAD));

      PositionView result = mapper.toPositionView(position, quote);

      assertThat(result.currentPrice().isZero()).isTrue();
      assertThat(result.marketValue().isZero()).isTrue();
      assertThat(result.unrealizedPnL().isZero()).isTrue();
      assertThat(result.returnPercentage()).isEqualTo(PercentageChange.ZERO);

      assertThat(result.totalCostBasis().amount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("toPositionView: percentage change is zero when cost basis is null")
    void toPositionViewNullQuoteGivesUsPercentageChangeOfZeroWhenCostBasisIsNull() {
      AcbPosition position = mock(AcbPosition.class);
      Money costBasis = Money.of(100, CAD);
      Money cpu = Money.of(10, CAD);

      when(position.symbol()).thenReturn(new AssetSymbol(SYMBOL));
      when(position.accountCurrency()).thenReturn(CAD);
      when(position.totalCostBasis()).thenReturn(costBasis);
      when(position.costPerUnit()).thenReturn(cpu);
      when(position.totalQuantity()).thenReturn(Quantity.of(10));
      when(position.lastModifiedAt()).thenReturn(NOW);
      when(position.firstAcquiredAt()).thenReturn(NOW);

      PositionView result = mapper.toPositionView(position, null);

      assertThat(result.currentPrice().isZero()).isTrue();
      assertThat(result.marketValue().isZero()).isTrue();
      assertThat(result.unrealizedPnL().isZero()).isTrue();
      assertThat(result.returnPercentage()).isEqualTo(PercentageChange.ZERO);

      assertThat(result.totalCostBasis().amount()).isEqualByComparingTo("100");
      assertThat(result.averageCostPerUnit().amount()).isEqualByComparingTo("10");
      assertThat(result.costBasisMethod()).isEqualTo("ACB");
    }

    @Test
    @DisplayName("calculateReturnPercentage: should calculate correct decimal percentage")
    void calculateReturnPercentageShouldReturnCorrectValue() {
      AcbPosition position = mock(AcbPosition.class);
      MarketAssetQuote quote = mock(MarketAssetQuote.class);
      Price currentPrice = new Price(Money.of(150, CAD));

      when(position.symbol()).thenReturn(new AssetSymbol(SYMBOL));
      when(position.accountCurrency()).thenReturn(CAD);
      when(position.totalQuantity()).thenReturn(Quantity.of(1));

      when(position.totalCostBasis()).thenReturn(Money.of(100, CAD));
      when(position.costPerUnit()).thenReturn(Money.of(100, CAD));

      when(quote.currentPrice()).thenReturn(currentPrice);
      when(position.currentValue(currentPrice)).thenReturn(Money.of(150, CAD));

      PositionView result = mapper.toPositionView(position, quote);

      assertThat(result.returnPercentage().change()).isEqualByComparingTo("0.50");
    }

    @Test
    @DisplayName("calculateReturnPercentage: should return ZERO when cost basis is zero to avoid division by zero")
    void calculateReturnPercentageShouldHandleZeroCostBasis() {
      AcbPosition position = mock(AcbPosition.class);
      MarketAssetQuote quote = mock(MarketAssetQuote.class);
      Price currentPrice = new Price(Money.of(50, CAD));

      when(position.symbol()).thenReturn(new AssetSymbol(SYMBOL));
      when(position.accountCurrency()).thenReturn(CAD);
      when(position.totalQuantity()).thenReturn(Quantity.of(10));
      when(position.costPerUnit()).thenReturn(Money.zero(CAD));

      when(position.totalCostBasis()).thenReturn(Money.zero(CAD));

      when(quote.currentPrice()).thenReturn(currentPrice);
      when(position.currentValue(currentPrice)).thenReturn(Money.of(500, CAD));
      when(position.lastModifiedAt()).thenReturn(NOW);
      when(position.firstAcquiredAt()).thenReturn(NOW);

      PositionView result = mapper.toPositionView(position, quote);

      assertThat(result.returnPercentage()).isEqualTo(PercentageChange.ZERO);

      assertThat(result.totalCostBasis().amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("calculateReturnPercentage: reflection on if branch")
    void calculateReturnPercentageTestingBranches()
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
      Method method = mapper.getClass()
          .getDeclaredMethod("calculateReturnPercentage", Money.class, Money.class);
      method.setAccessible(true);

      var result = method.invoke(mapper, null, null);
      assertThat(((PercentageChange) result)).isEqualTo(PercentageChange.ZERO);
    }
  }
}