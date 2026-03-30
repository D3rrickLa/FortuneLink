package com.laderrco.fortunelink.portfolio.application.mappers;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioSummaryView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio.application.views.PositionView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.*;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.FifoPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioViewMapper Unit Tests")
class PortfolioViewMapperTest {

  @InjectMocks
  private PortfolioViewMapper mapper;

  // --- Test Constants ---
  private static final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private static final AccountId ACCOUNT_ID = AccountId.newId();
  private static final Currency CAD = Currency.of("CAD");
  private static final String SYMBOL = "SHOP.TO";
  private static final Instant NOW = Instant.now();

  @Nested
  @DisplayName("Portfolio Mapping")
  class PortfolioMapping {

    @Test
    @DisplayName("toNewPortfolioView should initialize with empty account views and zero value")
    void toNewPortfolioView_ShouldMapBasicFields() {
      Portfolio portfolio = mock(Portfolio.class);
      when(portfolio.getPortfolioId()).thenReturn(PORTFOLIO_ID);
      when(portfolio.getDisplayCurrency()).thenReturn(CAD);
      when(portfolio.getAccounts()).thenReturn(Collections.emptyList());

      PortfolioView result = mapper.toNewPortfolioView(portfolio);

      assertThat(result.portfolioId()).isEqualTo(PORTFOLIO_ID);
      assertThat(result.totalValue().amount()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(result.accounts()).isEmpty();
    }

    @Test
    @DisplayName("toPortfolioSummaryView should map essential fields for list views")
    void toPortfolioSummaryView_ShouldMapCorrectly() {
      Portfolio portfolio = mock(Portfolio.class);
      Money totalVal = new Money(new BigDecimal("1500.00"), CAD);
      when(portfolio.getPortfolioId()).thenReturn(PORTFOLIO_ID);
      when(portfolio.getName()).thenReturn("Retirement");

      PortfolioSummaryView result = mapper.toPortfolioSummaryView(portfolio, totalVal);

      assertThat(result.id()).isEqualTo(PORTFOLIO_ID);
      assertThat(result.totalValue()).isEqualTo(totalVal);
      assertThat(result.name()).isEqualTo("Retirement");
    }
  }

  @Nested
  @DisplayName("Account Mapping")
  class AccountMapping {

    @Test
    @DisplayName("toAccountView should assemble full view from position list")
    void toAccountView_ShouldMapAllFields() {
      Account account = mock(Account.class);
      when(account.getAccountId()).thenReturn(ACCOUNT_ID);
      when(account.getAccountCurrency()).thenReturn(CAD);

      List<PositionView> positions = Collections.emptyList();
      Money total = new Money(new BigDecimal("100.00"), CAD);
      Money cash = new Money(new BigDecimal("50.00"), CAD);

      AccountView result = mapper.toAccountView(account, positions, total, cash);

      assertThat(result.accountId()).isEqualTo(ACCOUNT_ID);
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
      lenient().when(position.totalCostBasis()).thenReturn(new Money(new BigDecimal("1000.00"), CAD));
    }

    @Test
    @DisplayName("toPositionView should return zeroed price fields when quote is null")
    void toPositionView_WhenQuoteIsNull_ShouldHandleGracefully() {
      when(position.costPerUnit()).thenReturn(Money.of(125, CAD));
      PositionView result = mapper.toPositionView(position, null);

      assertThat(result.currentPrice().pricePerUnit().isZero()).isTrue();
      assertThat(result.marketValue().isZero()).isTrue();
      assertThat(result.unrealizedPnL().isZero()).isTrue();
      assertThat(result.returnPercentage()).isEqualTo(PercentageChange.ZERO);
    }

    @Test
    @DisplayName("toPositionView should calculate PnL and return percentage correctly")
    void toPositionView_WithQuote_ShouldCalculateReturn() {
      // Setup: Cost 1000, Market 1100 = 10% gain
      when(quote.currentPrice()).thenReturn(currentPrice);
      // when(currentPrice.isZero()).thenReturn(false);
      when(currentPrice.pricePerUnit()).thenReturn(new Money(new BigDecimal("110.00"), CAD));

      // when(position.totalQuantity()).thenReturn(new Quantity(BigDecimal.TEN));
      // when(position.currentValue(currentPrice)).thenReturn(new Money(new BigDecimal("1100.00"), CAD));
      // when(position.lastModifiedAt()).thenReturn(NOW);

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
    void toPositionView_WithFifo_ShouldExtractCorrectDate() {
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
    void toPositionView_WithMismatchedFeeCurrency_ShouldDefaultToZero() {
      Currency usd = Currency.of("USD");
      Money mismatchedFee = new Money(BigDecimal.TEN, usd);
      when(position.costPerUnit()).thenReturn(Money.of(125, CAD));

      PositionView result = mapper.toPositionView(position, null, mismatchedFee);

      assertThat(result.totalFeesIncurred().currency()).isEqualTo(CAD);
      assertThat(result.totalFeesIncurred().isZero()).isTrue();
    }
  }
}