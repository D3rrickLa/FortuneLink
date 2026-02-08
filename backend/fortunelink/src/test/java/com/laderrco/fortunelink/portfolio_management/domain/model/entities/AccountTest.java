package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AccountClosedException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.positions.FifoPosition;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;

class AccountTest {

    private AccountId accountId;
    private Currency usd;
    private Account account;
    private PositionStrategy strategy;

    @BeforeEach
    void setUp() {
        accountId = AccountId.newId();
        usd = Currency.of("USD");
        strategy = PositionStrategy.SPECIFIC_ID;
        account = new Account(accountId, "Main Investment", AccountType.CHEQUING, usd, strategy);
    }

    @Nested
    class Constructor {

        @Test
        void testConstructor_isSuccessful() {
            assertNotNull(account.getAccountId());
            assertEquals("Main Investment", account.getName());
            assertEquals(usd, account.getAccountCurrency());
            assertTrue(account.getCashBalance().isZero());
            assertTrue(account.isActive());
            assertNotNull(account.getCreationDate());
            assertEquals(AccountType.CHEQUING, account.getAccountType());
            assertNotNull(account.getLastUpdatedOn());
            assertEquals(0, account.getPositionCount());
        }

        @Test
        void testConstructor_Protected_isSuccessful() {
            account = new Account();
            assertAll(
                    () -> assertNull("is null", account.getAccountId()),
                    () -> assertNull("is null", account.getCreationDate()),
                    () -> assertNull("is null", account.getAccountCurrency()));
        }

        @Test
        void testConstructor_Fails_WhenNameIsEmpty() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Account(accountId, "  ", AccountType.CHEQUING, usd, strategy));
        }

        @Test
        void testConstructor_Fails_WhenParametersNull() {
            assertThrows(RuntimeException.class,
                    () -> new Account(null, "Name", AccountType.CHEQUING, usd, strategy));
        }
    }

    @Nested
    class CashOperations {

        @Test
        void testDeposit_isSuccessful() {
            Money depositAmount = Money.of(1000, "USD");
            account.deposit(depositAmount, "Initial Funding");

            assertEquals(depositAmount, account.getCashBalance());
        }

        @Test
        void testDeposit_Fails_NegativeAmount() {
            Money negativeMoney = Money.of(-100, "USD");
            assertThrows(IllegalArgumentException.class,
                    () -> account.deposit(negativeMoney, "Invalid"));
        }

        @Test
        void testDeposit_Fails_CurrencyMismatch() {
            Money eur = Money.of(100, "EUR");
            assertThrows(CurrencyMismatchException.class,
                    () -> account.deposit(eur, "Wrong Currency"));
        }

        @Test
        void testWithdraw_isSuccessful() {
            account.deposit(Money.of(500, "USD"), "Funding");
            account.withdraw(Money.of(200, "USD"), "Personal use");

            assertEquals(Money.of(300, "USD"), account.getCashBalance());
        }

        @Test
        void testWithdraw_Fails_InsufficientFunds() {
            account.deposit(Money.of(100, "USD"), "Funding");
            assertThrows(InsufficientFundsException.class,
                    () -> account.withdraw(Money.of(200, "USD"), "Too much"));
        }

        @Test
        void testWithdraw_Fails_NegativeAmount() {
            account.deposit(Money.of(100, "USD"), "Funding");
            assertThrows(IllegalArgumentException.class,
                    () -> account.withdraw(Money.of(-200, "USD"), "Too much"));
        }

        @Test
        void testApplyFee_isSuccessful() {
            account.deposit(Money.of(100, "USD"), "Funding");
            account.applyFee(Money.of(10, "USD"), "Brokerage Fee");

            assertEquals(Money.of(90, "USD"), account.getCashBalance());
        }

        @Test
        void testApplyFee_Fails_InsufficientFunds() {
            assertThrows(InsufficientFundsException.class,
                    () -> account.applyFee(Money.of(10, "USD"), "Fee"));
        }

        @Test
        void testApplyFee_Fails_FeeNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> account.applyFee(Money.of(-10, "USD"), "Fee"));
        }

        @Test
        void testApplyFee_Fails_ReasonIsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> account.applyFee(Money.of(10, "USD"), null));
        }

        @Test
        void testApplyFee_Fails_ReasonIsEmpty() {
            assertThrows(IllegalArgumentException.class,
                    () -> account.applyFee(Money.of(10, "USD"), "     "));
        }
    }

    @Nested
    class PositionManagement {

        private AssetSymbol apple;
        private Position emptyPosition;

        @BeforeEach
        void initPositions() {
            apple = new AssetSymbol("AAPL");
            // We use a real object because sealed interfaces are picky about mocks
            emptyPosition = AcbPosition.empty(apple, AssetType.STOCK, usd);
        }

        @Test
        void testUpdatePosition_isSuccessful_WhenAddingNew() {
            Position activePosition = emptyPosition.buy(
                    new Quantity(BigDecimal.TEN),
                    Money.of(1500, "USD"),
                    Instant.now()).getUpdatedPosition(); // Extract position from ApplyResult

            account.updatePosition(apple, activePosition);

            assertTrue(account.hasPosition(apple));
            assertEquals(1, account.getPositionCount());
            assertNotNull(account.getPosition(apple));
        }

        @Test
        void testUpdatePosition_isSuccessful_WhenRemovingZeroQuantity() {
            // Adding then removing
            Position activePosition = emptyPosition
                    .buy(new Quantity(BigDecimal.TEN), Money.of(100, "USD"), Instant.now()).getUpdatedPosition();
            account.updatePosition(apple, activePosition);

            // Use the empty position (which has zero quantity) to simulate a "close"
            account.updatePosition(apple, emptyPosition);

            assertFalse(account.hasPosition(apple), "Position should be removed when quantity is zero");
            assertEquals(0, account.getPositionCount());
        }

        @Test
        void testUpdatePosition_Fails_WhenSymbolMismatch() {
            AssetSymbol google = new AssetSymbol("GOOGL");
            // emptyPosition is for AAPL
            assertThrows(IllegalArgumentException.class, () -> account.updatePosition(google, emptyPosition));
        }

        @Nested
        class PositionStrategyTest {

            @Test
            void testEnsurePosition_CreatesAcb_WhenStrategyIsAcb() {
                // GIVEN: An account initialized with ACB strategy
                Currency cad = Currency.of("CAD");
                PositionStrategy strategy = PositionStrategy.ACB;
                Account cadAccount = new Account(AccountId.newId(), "CAD Account", AccountType.CHEQUING, cad, strategy);
                AssetSymbol symbol = new AssetSymbol("TD.TO");

                // WHEN: Creating a new position
                Position position = cadAccount.ensurePosition(symbol, AssetType.STOCK);

                // THEN: It should be an AcbPosition
                assertTrue(position instanceof AcbPosition, "Account with ACB strategy must produce AcbPosition");
            }

            @Test
            void testEnsurePosition_CreatesFifo_WhenStrategyIsFifo() {
                // GIVEN: An account initialized with FIFO strategy
                Currency usd = Currency.of("USD");
                PositionStrategy strategy = PositionStrategy.FIFO;
                Account usdAccount = new Account(AccountId.newId(), "USD Account", AccountType.CHEQUING, usd, strategy);
                AssetSymbol symbol = new AssetSymbol("MSFT");

                // WHEN: Creating a new position
                Position position = usdAccount.ensurePosition(symbol, AssetType.STOCK);

                // THEN: It should be a FifoPosition
                assertTrue(position instanceof FifoPosition, "Account with FIFO strategy must produce FifoPosition");
            }

            @ParameterizedTest
            @EnumSource(value = PositionStrategy.class, names = {"LIFO", "SPECIFIC_ID"})
            void testCreateEmptyPosition_Fails_OnUnsupportedStrategy(PositionStrategy strategy) {
                // GIVEN: An account with a strategy not covered by the switch (e.g.,
                // SPECIFIC_ID)
                // Note: You may need to mock the enum or use a value that exists in the Enum
                // but not the switch
                PositionStrategy unsupported = strategy;
                Account errorAccount = new Account(AccountId.newId(), "Error", AccountType.CHEQUING, usd, unsupported);

                // WHEN/THEN: Expect the IllegalArgumentException you saw earlier
                assertThrows(IllegalArgumentException.class,
                        () -> errorAccount.ensurePosition(new AssetSymbol("AAPL"), AssetType.STOCK),
                        "Should throw exception for unhandled position strategies");
            }

            @Test
            void testEnsurePosition_ReturnsExistingPosition_WithoutChangingType() {
                AssetSymbol apple = new AssetSymbol("AAPL");
                // Create an initial position via 'buy' to put it in the map
                Position activePosition = AcbPosition.empty(apple, AssetType.STOCK, usd)
                        .buy(new Quantity(BigDecimal.TEN), Money.of(100, "USD"), Instant.now())
                        .getUpdatedPosition();

                account.updatePosition(apple, activePosition);

                // WHEN: Ensuring the position exists
                Position retrieved = account.ensurePosition(apple, AssetType.STOCK);

                // THEN: It returns the existing one, not a new empty one
                assertEquals(activePosition.totalQuantity(), retrieved.totalQuantity());
                assertEquals(1, account.getPositionCount());
            }
        }
    }

    @Nested
    class AccountLifecycle {

        @Test
        void testClose_isSuccessful() {
            account.close();
            assertFalse(account.isActive());
            assertNotNull(account.getCloseDate());
        }

        @Test
        void testClose_Fails_WithOpenPositions() {
            AssetSymbol symbol = new AssetSymbol("AAPL");
            Position pos = mock(AcbPosition.class);
            when(pos.symbol()).thenReturn(symbol);
            when(pos.totalQuantity()).thenReturn(new Quantity(BigDecimal.ONE));

            account.updatePosition(symbol, pos);

            assertThrows(IllegalStateException.class, () -> account.close());
        }

        @Test
        void testClose_Fails_WithCashBalance() {
            account.deposit(Money.of(10, "USD"), "Leftover");
            assertThrows(IllegalStateException.class, () -> account.close());
        }

        @Test
        void testReopen_isSuccessful() {
            account.close();
            account.reopen();

            assertTrue(account.isActive());
            assertNull(account.getCloseDate());
        }

        @Test
        void testReopen_Fail_CannotReOpenAlreadyOpen() {
            assertThrows(IllegalStateException.class, () -> account.reopen());
        }

        @Test
        void testOperations_Fail_WhenAccountClosed() {
            account.close();
            assertThrows(AccountClosedException.class,
                    () -> account.deposit(Money.of(10, "USD"), "Reason"));
        }
    }

    @Nested
    class InternalValidation {
        @Test
        void testValidate_Fails_WhenInactiveMissingCloseDate() {
            // This tests the logic: if (!isActive && closeDate == null)
            // Manual state manipulation would be needed here as the public close() method
            // sets the date.
        }
    }

    @Nested
    class GettersAndEncapsulation {

        @Test
        void testGetAllPositions_ReturnsUnmodifiableCopy() {
            AssetSymbol apple = new AssetSymbol("AAPL");
            Position pos = mock(AcbPosition.class);
            when(pos.symbol()).thenReturn(apple);
            when(pos.totalQuantity()).thenReturn(new Quantity(BigDecimal.ONE));
            when(pos.copy()).thenReturn(pos);

            account.updatePosition(apple, pos);
            Collection<Position> positions = account.getAllPositions();

            assertEquals(1, positions.size());
            assertThrows(UnsupportedOperationException.class, () -> positions.clear());
        }

        @Test
        void testGetPosition_ReturnsOptionalEmptyWhenMissing() {
            Optional<Position> result = account.getPosition(new AssetSymbol("NONE"));
            assertTrue(result.isEmpty());
        }

        @Nested
        class FinancialChecks {

            @Test
            void testHasSufficientCash_ReturnsTrue_WhenBalanceIsExact() {
                Money amount = Money.of(100, "USD");
                account.deposit(amount, "Initial deposit");

                assertTrue(account.hasSufficientCash(amount));
            }

            @Test
            void testHasSufficientCash_ReturnsFalse_WhenBalanceIsLower() {
                account.deposit(Money.of(50, "USD"), "Partial deposit");
                Money required = Money.of(100, "USD");

                assertFalse(account.hasSufficientCash(required));
            }

            @Test
            void testHasSufficientCash_Fails_OnCurrencyMismatch() {
                account.deposit(Money.of(100, "USD"), "USD deposit");
                Money eurAmount = Money.of(50, "EUR");

                assertThrows(CurrencyMismatchException.class,
                        () -> account.hasSufficientCash(eurAmount));
            }
        }
    }
}