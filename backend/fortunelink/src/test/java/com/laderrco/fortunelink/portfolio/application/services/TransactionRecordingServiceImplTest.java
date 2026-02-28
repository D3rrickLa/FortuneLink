package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.SplitDetails;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TradeExecution;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.FeeType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.*;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.ApplyResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.FifoPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.shared.enums.Precision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionRecordingServiceImplTest {
    // Common test data
    private final AssetSymbol symbol = new AssetSymbol("AAPL");
    private final AssetType type = AssetType.STOCK;
    private final Quantity qty = Quantity.of(10);
    private final Price price = new Price(new Money(BigDecimal.valueOf(150), Currency.USD));
    private final Instant now = Instant.now();
    private final Fee.FeeMetadata metadata = new Fee.FeeMetadata(null);
    private final List<Fee> fees = List
            .of(new Fee(FeeType.CUSTODY_FEE, new Money(BigDecimal.valueOf(5), Currency.USD),
                    new Money(BigDecimal.valueOf(5), Currency.USD), ExchangeRate.identity(Currency.USD, now), now,
                    metadata));

    @Mock
    private Account account;
    @Mock
    private FifoPosition position;

    @InjectMocks
    private TransactionRecordingServiceImpl service;

    @BeforeEach
    void setup() {
        lenient().when(account.getAccountCurrency()).thenReturn(Currency.USD);
        lenient().when(account.getAccountId()).thenReturn(AccountId.newId());
    }

    @Nested
    @DisplayName("recordBuy Tests")
    class RecordBuyTests {

        @Test
        @DisplayName("recordBuy_Success_WithFees")
        void recordBuy_Success_WithFees() {
            ApplyResult<Position> result = new ApplyResult.Purchase<>(mock(FifoPosition.class));

            when(account.ensurePosition(symbol, type)).thenReturn(position);
            when(position.buy(any(), any(), any())).thenReturn((ApplyResult) result);

            Transaction tx = service.recordBuy(account, symbol, type, qty, price, fees, "Buy Note", now);

            assertThat(tx.transactionType()).isEqualTo(TransactionType.BUY);
            assertThat(tx.cashDelta().amount()).isEqualTo(BigDecimal.valueOf(-1505)
                    .setScale(Precision.getMoneyPrecision(), RoundingMode.HALF_EVEN)); // (150 * 10) + 5
            verify(account).withdraw(any(Money.class), eq("BUY AAPL"));
        }

        @Test
        @DisplayName("recordBuy_Failure_NullInputs")
        void recordBuy_Failure_NullInputs() {
            assertThatThrownBy(() -> service.recordBuy(null, symbol, type, qty, price, fees, "", now))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("recordSell Tests")
    class RecordSellTests {

        @Test
        @DisplayName("recordSell_Success_CalculatesNetProceeds")
        void recordSell_Success_CalculatesNetProceeds() {
            ApplyResult<Position> result = new ApplyResult.Purchase<>(mock(AcbPosition.class));

            when(account.getPosition(symbol)).thenReturn(Optional.of(position));
            when(position.type()).thenReturn(type);
            when(position.sell(any(), any(), any())).thenReturn((ApplyResult) result);

            Transaction tx = service.recordSell(account, symbol, qty, price, fees, "Sell Note", now);

            assertThat(tx.cashDelta().amount()).isEqualTo(BigDecimal.valueOf(1495)
                    .setScale(Precision.getMoneyPrecision(), RoundingMode.HALF_EVEN)); // (150 * 10) - 5
            verify(account).deposit(any(Money.class), eq("SELL AAPL"));
        }

        @Test
        @DisplayName("recordSell_Failure_NoPositionExists")
        void recordSell_Failure_NoPositionExists() {
            when(account.getPosition(symbol)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.recordSell(account, symbol, qty, price, fees, "Note", now))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No position");
        }
    }

    @Nested
    @DisplayName("recordDeposit & Withdrawal Tests")
    class CashFlowTests {

        @Test
        @DisplayName("recordDeposit_Success_IncreasesCash")
        void recordDeposit_Success_IncreasesCash() {
            Money amount = new Money(BigDecimal.valueOf(1000), Currency.USD);
            Transaction tx = service.recordDeposit(account, amount, "Deposit", now);

            assertThat(tx.cashDelta()).isEqualTo(amount);
            verify(account).deposit(amount, "DEPOSIT");
        }

        @Test
        @DisplayName("recordWithdrawal_Success_DecreasesCash")
        void recordWithdrawal_Success_DecreasesCash() {
            Money amount = new Money(BigDecimal.valueOf(500), Currency.USD);
            Transaction tx = service.recordWithdrawal(account, amount, "Out", now);

            assertThat(tx.cashDelta().isNegative()).isTrue();
            verify(account).withdraw(amount, "WITHDRAWAL");
        }
    }

    @Nested
    @DisplayName("recordDividend & Reinvestment Tests")
    class DividendTests {

        @Test
        @DisplayName("recordDividend_Success_CreditsCashOnly")
        void recordDividend_Success_CreditsCashOnly() {
            Money div = new Money(BigDecimal.valueOf(50), Currency.USD);
            Transaction tx = service.recordDividend(account, symbol, div, "Div", now);

            assertThat(tx.cashDelta()).isEqualTo(div);
            verify(account).deposit(div, "DIVIDEND from AAPL");
            verify(account, never()).updatePosition(any(), any());
        }

        @Test
        @DisplayName("recordDividendReinvestment_Success_ZeroCashImpact")
        void recordDividendReinvestment_Success_ZeroCashImpact() {
            ApplyResult<Position> result = new ApplyResult.Purchase<>(mock(AcbPosition.class));

            when(account.ensurePosition(symbol, AssetType.STOCK)).thenReturn(position);
            when(position.buy(any(), any(), any())).thenReturn((ApplyResult) result);

            Transaction tx = service.recordDividendReinvestment(account, symbol, qty, price, "DRIP", now);

            assertThat(tx.cashDelta().isZero()).isTrue();
            verify(account, never()).deposit(any(), any());
            verify(account).updatePosition(eq(symbol), any());
        }
    }

    @Nested
    @DisplayName("replayTransaction Tests (Position-Only)")
    class ReplayTests {

        @Test
        @DisplayName("replayTransaction_Success_BuyIgnoresFeesInCostBasis")
        void replayTransaction_Success_BuyIgnoresFeesInCostBasis() {
            // 1. Setup Data
            TradeExecution exec = new TradeExecution(symbol, qty, price); // Gross = 1500
            // Use the concrete record type from your interface for the result
            ApplyResult<Position> result = new ApplyResult.Purchase<>(mock(FifoPosition.class));

            Transaction tx = mock(Transaction.class);
            when(tx.transactionType()).thenReturn(TransactionType.BUY);
            when(tx.execution()).thenReturn(exec);
            when(tx.occurredAt()).thenReturn(TransactionDate.of(now));
            when(tx.metadata()).thenReturn(Transaction.TransactionMetadata.manual(AssetType.STOCK));

            // 2. Setup Mocks
            when(account.ensurePosition(eq(symbol), any())).thenReturn(position);
            // Use doReturn to bypass some strict generic checks if necessary,
            // but when().thenReturn() usually works if types match Position
            when(position.buy(any(), any(), any())).thenReturn((ApplyResult) result);

            // 3. Execute
            service.replayTransaction(account, tx);

            // 4. Verify: Ensure the 'grossValue' from execution was used, NOT a modified
            // cash delta
            verify(position).buy(eq(qty), eq(exec.grossValue()), eq(now));
            verify(account).updatePosition(eq(symbol), any(Position.class));
        }

        @Test
        @DisplayName("replayTransaction_Failure_ThrowsOnCashOnlyType")
        void replayTransaction_Failure_ThrowsOnCashOnlyType() {
            Transaction tx = mock(Transaction.class);
            when(tx.transactionType()).thenReturn(TransactionType.DEPOSIT);

            assertThatThrownBy(() -> service.replayTransaction(account, tx))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("position-only");
        }

        @Test
        @DisplayName("replayTransaction_Success_HandlesSplit")
        void replayTransaction_Success_HandlesSplit() {
            // 1. Setup Data
            AssetSymbol symbol = new AssetSymbol("AAPL");
            // Ensure Ratio is correctly instantiated based on your domain (e.g., 2:1)
            Ratio ratio = new Ratio(2, 1);
            SplitDetails split = new SplitDetails(ratio);

            // Mock the concrete implementation
            FifoPosition fifoMock = mock(FifoPosition.class);
            // Use the specific record subtype for the result
            ApplyResult<FifoPosition> result = new ApplyResult.NoChange<>(fifoMock);

            Transaction tx = mock(Transaction.class);
            when(tx.transactionType()).thenReturn(TransactionType.SPLIT);
            // Note: ensure tx.execution() doesn't return null if the service accesses it
            when(tx.execution()).thenReturn(new TradeExecution(symbol, qty, price));
            when(tx.split()).thenReturn(split);

            // 2. Setup Account Mock
            when(account.getPosition(symbol)).thenReturn(Optional.of(fifoMock));

            // 3. Stub the split call - Use the concrete mock
            // Casting to (ApplyResult) handles the wildcard capture in the service
            when(fifoMock.split(any(Ratio.class))).thenReturn((ApplyResult) result);

            // 4. Execute
            service.replayTransaction(account, tx);

            // 5. Verify
            verify(fifoMock).split(eq(ratio));
            verify(account).updatePosition(eq(symbol), eq(fifoMock));
        }
    }

    @Nested
    @DisplayName("replayFullTransaction Tests")
    class ReplayFullTests {

        @Test
        @DisplayName("replayFullTransaction_Success_ProcessesCashEvents")
        void replayFullTransaction_Success_ProcessesCashEvents() {
            Transaction tx = mock(Transaction.class);
            Money amount = new Money(BigDecimal.valueOf(100), Currency.USD);
            when(tx.transactionType()).thenReturn(TransactionType.INTEREST);
            when(tx.cashDelta()).thenReturn(amount);

            service.replayFullTransaction(account, tx);

            verify(account).deposit(amount, "REPLAY INTEREST");
        }

        @Test
        @DisplayName("replayFullTransaction_Failure_ThrowsOnUnhandledType")
        void replayFullTransaction_Failure_ThrowsOnUnhandledType() {
            Transaction tx = mock(Transaction.class);
            // Assuming we added a new type but didn't update the switch
            when(tx.transactionType()).thenReturn(TransactionType.OTHER);

            assertThatThrownBy(() -> service.replayFullTransaction(account, tx))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}