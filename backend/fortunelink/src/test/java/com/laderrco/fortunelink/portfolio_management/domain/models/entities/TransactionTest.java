package com.laderrco.fortunelink.portfolio_management.domain.models.entities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CashIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.shared.exceptions.InvalidQuantityException;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class TransactionTest {
    
    private static final TransactionId VALID_ID = mock(TransactionId.class);
    private static final AccountId VAL_ACCOUNT_ID = mock(AccountId.class);
    private static final AssetIdentifier VALID_ASSET = mock(AssetIdentifier.class);
    private static final Instant VALID_DATE = Instant.now();
    private AssetIdentifier assetIdentifier;
    private BigDecimal quantity;

    @BeforeEach
    void init() {
        assetIdentifier = new MarketIdentifier("AAPL", null, AssetType.STOCK, "APPLE", "USD", null);
        quantity = BigDecimal.valueOf(100);
    }

    @Nested
    @DisplayName("Constructor Validation Tests")
    class ConstructorValidationTests {

        @Test
        @DisplayName("Should create dedicated dividend transaction")
        void shouldCreateDedicatedDividendTransaction() {
            Money price = Money.of(BigDecimal.valueOf(100), ValidatedCurrency.USD);
            Money divMoney = Money.of(200, "USD");
            BigDecimal quantity = BigDecimal.valueOf(2).setScale(2);
            Transaction transaction = Transaction.createDripTransaction(VALID_ID, VAL_ACCOUNT_ID, assetIdentifier, divMoney, price, VALID_DATE, null);
            assertNotNull(transaction);
            assertEquals(TransactionType.DIVIDEND, transaction.getTransactionType());
            assertEquals(quantity, transaction.getQuantity().setScale(2));
            assertEquals(price, transaction.getPricePerUnit());
        }

        @Test
        @DisplayName("Should create valid BUY transaction")
        void shouldCreateValidBuyTransaction() {
            Money price = Money.of(BigDecimal.valueOf(100), ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(10);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.BUY,
                VALID_ASSET,
                quantity,
                price,
                null,
                VALID_DATE,
                "Test note"
            );

            assertNotNull(transaction);
            assertEquals(TransactionType.BUY, transaction.getTransactionType());
            assertEquals(quantity, transaction.getQuantity());
            assertEquals(price, transaction.getPricePerUnit());
        }

        @Test
        @DisplayName("Should create valid DEPOSIT transaction without asset")
        void shouldCreateValidDepositTransaction() {
            Money amount = Money.of(BigDecimal.valueOf(1000), ValidatedCurrency.USD);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.DEPOSIT,
                new CashIdentifier(amount.currency().toString()),
                BigDecimal.ONE,
                amount,
                null,
                VALID_DATE,
                ""
            );

            assertNotNull(transaction);
            assertEquals(TransactionType.DEPOSIT, transaction.getTransactionType());
        }

        @Test
        @DisplayName("Should throw exception when BUY transaction missing asset")
        void shouldThrowExceptionWhenBuyMissingAsset() {
            Money price = Money.of(BigDecimal.valueOf(100), ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(10);

            assertThrows(IllegalArgumentException.class, () -> 
                new Transaction(
                    VALID_ID,
                    VAL_ACCOUNT_ID,
                    TransactionType.BUY,
                    null, // Missing asset
                    quantity,
                    price,
                    null,
                    VALID_DATE,
                    ""
                )
            );
        }

        @Test
        @DisplayName("Should throw exception when BUY has zero quantity")
        void shouldThrowExceptionWhenBuyHasZeroQuantity() {
            Money price = Money.of(BigDecimal.valueOf(100), ValidatedCurrency.USD);

            assertThrows(InvalidQuantityException.class, () -> 
                new Transaction(
                    VALID_ID,
                    VAL_ACCOUNT_ID,
                    TransactionType.BUY,
                    VALID_ASSET,
                    BigDecimal.ZERO, // Invalid quantity
                    price,
                    null,
                    VALID_DATE,
                    ""
                )
            );
        }

        @Test
        @DisplayName("Should throw exception when BUY has negative quantity")
        void shouldThrowExceptionWhenBuyHasNegativeQuantity() {
            Money price = Money.of(BigDecimal.valueOf(100), ValidatedCurrency.USD);

            assertThrows(InvalidQuantityException.class, () -> 
                new Transaction(
                    VALID_ID,
                    VAL_ACCOUNT_ID,
                    TransactionType.BUY,
                    VALID_ASSET,
                    BigDecimal.valueOf(-5), // Negative quantity
                    price,
                    null,
                    VALID_DATE,
                    ""
                )
            );
        }

        @Test
        @DisplayName("Should throw exception when BUY has zero price")
        void shouldThrowExceptionWhenBuyHasZeroPrice() {
            Money price = Money.of(BigDecimal.ZERO, ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(10);

            assertThrows(IllegalArgumentException.class, () -> 
                new Transaction(
                    VALID_ID,
                    VAL_ACCOUNT_ID,
                    TransactionType.BUY,
                    VALID_ASSET,
                    quantity,
                    price, // Invalid price
                    null,
                    VALID_DATE,
                    ""
                )
            );
        }

        @Test
        @DisplayName("Should throw exception when DEPOSIT has zero amount")
        void shouldThrowExceptionWhenDepositHasZeroAmount() {
            Money amount = Money.of(BigDecimal.ZERO, ValidatedCurrency.USD);

            assertThrows(IllegalArgumentException.class, () -> 
                new Transaction(
                    VALID_ID,
                    VAL_ACCOUNT_ID,
                    TransactionType.DEPOSIT,
                    null,
                    null,
                    amount, // Invalid amount
                    null,
                    VALID_DATE,
                    ""
                )
            );
        }

        @Test
        @DisplayName("Should initialize empty fees list when fees is null")
        void shouldInitializeEmptyFeesListWhenNull() {
            Money price = Money.of(BigDecimal.valueOf(100), ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(10);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.BUY,
                VALID_ASSET,
                quantity,
                price,
                null, // Null fees
                VALID_DATE,
                ""
            );

            assertNotNull(transaction.getFees());
            assertTrue(transaction.getFees().isEmpty());
        }

        @Test
        @DisplayName("Should trim and store notes properly")
        void shouldTrimAndStoreNotes() {
            Money price = Money.of(BigDecimal.valueOf(100), ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(10);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.BUY,
                VALID_ASSET,
                quantity,
                price,
                null,
                VALID_DATE,
                "  Test note with spaces  "
            );

            assertEquals("Test note with spaces", transaction.getNotes());
        }
    }

    @Nested
    @DisplayName("Calculate Total Cost Tests")
    class CalculateTotalCostTests {

        @Test
        @DisplayName("Should calculate total cost for BUY with fees")
        void shouldCalculateTotalCostForBuyWithFees() {
            Money price = Money.of(BigDecimal.valueOf(100), ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(10);
            Money feeAmount = Money.of(BigDecimal.valueOf(5), ValidatedCurrency.USD);
            Fee fee = mock(Fee.class);
            when(fee.amountInNativeCurrency()).thenReturn(feeAmount);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.BUY,
                VALID_ASSET,
                quantity,
                price,
                List.of(fee),
                VALID_DATE,
                ""
            );

            // Total = (100 * 10) + 5 = 1005
            Money totalCost = transaction.calculateTotalCost();
            assertEquals(new BigDecimal("1005").setScale(Precision.getMoneyPrecision()), totalCost.amount());
        }

        @Test
        @DisplayName("Should calculate total cost for SELL with fees")
        void shouldCalculateTotalCostForSellWithFees() {
            Money price = Money.of(BigDecimal.valueOf(100), ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(10);
            Money feeAmount = Money.of(BigDecimal.valueOf(5), ValidatedCurrency.USD);
            Fee fee = mock(Fee.class);
            when(fee.amountInNativeCurrency()).thenReturn(feeAmount);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.SELL,
                VALID_ASSET,
                quantity,
                price,
                List.of(fee),
                VALID_DATE,
                ""
            );

            // Total = (100 * 10) - 5 = 995
            Money totalCost = transaction.calculateTotalCost();
            assertEquals(new BigDecimal("995").setScale(Precision.getMoneyPrecision()), totalCost.amount());
        }

        @Test
        @DisplayName("Should return amount for DEPOSIT")
        void shouldReturnAmountForDeposit() {
            Money amount = Money.of(BigDecimal.valueOf(1000), ValidatedCurrency.USD);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.DEPOSIT,
                new CashIdentifier("USD"),
                BigDecimal.ONE,
                amount,
                null,
                VALID_DATE,
                ""
            );

            Money totalCost = transaction.calculateTotalCost();
            assertEquals(amount, totalCost);
        }

        @Test
        @DisplayName("Should calculate total cost for DIVIDEND without fees")
        void shouldCalculateTotalCostForDividend() {
            Money price = Money.of(BigDecimal.valueOf(2.50), ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(100);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.DIVIDEND,
                VALID_ASSET,
                quantity,
                price,
                null,
                VALID_DATE,
                ""
            );

            // Total = 2.50 * 100 = 250
            Money totalCost = transaction.calculateTotalCost();
            assertEquals(new BigDecimal("250.00").setScale(Precision.getMoneyPrecision()), totalCost.amount());
        }

        @Test
        @DisplayName("Should throw error when default route")
        void shouldThrowUnsupportedOperationException() {
            Money price = Money.of(BigDecimal.valueOf(2.50), ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(100);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.TRANSFER_IN,
                VALID_ASSET,
                quantity,
                price,
                null,
                VALID_DATE,
                ""
            );

            // Total = 2.50 * 100 = 250
            assertThrows(UnsupportedOperationException.class, () ->transaction.calculateTotalCost());
            
        }
    }

    @Nested
    @DisplayName("Calculate Net Amount Tests")
    class CalculateNetAmountTests {

        @Test
        @DisplayName("Should calculate net amount for BUY with fees")
        void shouldCalculateNetAmountForBuyWithFees() {
            Money price = Money.of(BigDecimal.valueOf(100), ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(10);
            Money feeAmount = Money.of(BigDecimal.valueOf(5), ValidatedCurrency.USD);
            Fee fee = mock(Fee.class);
            when(fee.amountInNativeCurrency()).thenReturn(feeAmount);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.BUY,
                VALID_ASSET,
                quantity,
                price,
                List.of(fee),
                VALID_DATE,
                ""
            );

            // Net = (100 * 10) - 5 = 995 (cash outflow perspective)
            Money netAmount = transaction.calculateNetAmount();
            assertEquals(new BigDecimal("995").setScale(Precision.getMoneyPrecision()), netAmount.amount());
        }

        @Test
        @DisplayName("Should calculate net amount for SELL with fees")
        void shouldCalculateNetAmountForSellWithFees() {
            Money price = Money.of(BigDecimal.valueOf(100), ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(10);
            Money feeAmount = Money.of(BigDecimal.valueOf(5), ValidatedCurrency.USD);
            Fee fee = mock(Fee.class);
            when(fee.amountInNativeCurrency()).thenReturn(feeAmount);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.SELL,
                VALID_ASSET,
                quantity,
                price,
                List.of(fee),
                VALID_DATE,
                ""
            );

            // Net = (100 * 10) + 5 = 1005 (cash inflow perspective)
            Money netAmount = transaction.calculateNetAmount();
            assertEquals(new BigDecimal("1005").setScale(Precision.getMoneyPrecision()), netAmount.amount());
        }

        @Test
        @DisplayName("Should return positive amount for DEPOSIT")
        void shouldReturnPositiveAmountForDeposit() {
            Money amount = Money.of(BigDecimal.valueOf(1000), ValidatedCurrency.USD);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.DEPOSIT,
                new CashIdentifier("USD"),
                BigDecimal.ONE,
                amount,
                null,
                VALID_DATE,
                ""
            );

            Money netAmount = transaction.calculateNetAmount();
            assertEquals(amount, netAmount);
        }

        @Test
        @DisplayName("Should return negative amount for WITHDRAWAL")
        void shouldReturnNegativeAmountForWithdrawal() {
            Money amount = Money.of(BigDecimal.valueOf(500), ValidatedCurrency.USD);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.WITHDRAWAL,
                new CashIdentifier("USD"),
                BigDecimal.ONE,
                amount,
                null,
                VALID_DATE,
                ""
            );

            Money netAmount = transaction.calculateNetAmount();
            assertEquals(new BigDecimal("-500").setScale(Precision.getMoneyPrecision()), netAmount.amount());
        }

        @Test
        @DisplayName("Should return negative amount for WITHDRAWAL")
        void shouldReturnIllegalArgumentEXceptionWhenSwitchTypeNotRecognized() {
            Money amount = Money.of(BigDecimal.valueOf(500), ValidatedCurrency.USD);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.TRANSFER_IN,
                assetIdentifier,
                quantity,
                amount,
                null,
                VALID_DATE,
                ""
            );

            assertThrows(IllegalArgumentException.class, ()-> transaction.calculateNetAmount());
        }
    }

    @Nested
    @DisplayName("Calculate Gross Amount Tests")
    class CalculateGrossAmountTests {

        @Test
        @DisplayName("Should calculate gross amount correctly")
        void shouldCalculateGrossAmount() {
            Money price = Money.of(BigDecimal.valueOf(50.75), ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(20);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.BUY,
                VALID_ASSET,
                quantity,
                price,
                null,
                VALID_DATE,
                ""
            );

            // Gross = 50.75 * 20 = 1015.00
            Money grossAmount = transaction.calculateGrossAmount();
            assertEquals(new BigDecimal("1015.00").setScale(Precision.getMoneyPrecision()), grossAmount.amount());
        }

        @Test
        @DisplayName("Should calcualte gross amount with the div branch correctly")
        void shouldCalculateGrossAmountWihtDividendBranch() {
            Money price = Money.of(BigDecimal.valueOf(100), ValidatedCurrency.USD);
            Money divMoney = Money.of(200, "USD");
            Transaction transaction = Transaction.createDripTransaction(VALID_ID, VAL_ACCOUNT_ID, assetIdentifier, divMoney, price, VALID_DATE, null);
            Money actualGrossAmount = transaction.calculateGrossAmount();
            assertEquals(new BigDecimal("200.00").setScale(Precision.getMoneyPrecision()), actualGrossAmount.amount());
        }

        @Test
        @DisplayName("Should calculate gross amount correctly, when isDrip = false")
        void shouldCalculateGrossAmountWhenIsDripIsFalse() {
            Money price = Money.of(BigDecimal.valueOf(50.75), ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(20);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.BUY,
                VALID_ASSET,
                quantity,
                price,
                Money.of(BigDecimal.valueOf(10), ValidatedCurrency.USD),
                null,
                VALID_DATE,
                "",
                false
            );

            // Gross = 50.75 * 20 = 1015.00
            Money grossAmount = transaction.calculateGrossAmount();
            assertEquals(new BigDecimal("1015.00").setScale(Precision.getMoneyPrecision()), grossAmount.amount());
        }

        // this technically shouldn't even happen....
        @Test
        @DisplayName("Should calculate gross amount correctly, when dividendAmount = null")
        void shouldCalculateGrossAmountWhenDividendIsNull() {
            Money price = Money.of(BigDecimal.valueOf(50.75), ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(20);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.BUY,
                VALID_ASSET,
                quantity,
                price,
                null,
                null,
                VALID_DATE,
                "",
                true
            );

            // Gross = 50.75 * 20 = 1015.00
            Money grossAmount = transaction.calculateGrossAmount();
            assertEquals(new BigDecimal("1015.00").setScale(Precision.getMoneyPrecision()), grossAmount.amount());
        }
    }

    @Nested
    @DisplayName("Calculate Total Fees Tests")
    class CalculateTotalFeesTests {

        @Test
        @DisplayName("Should calculate total fees with multiple fees")
        void shouldCalculateTotalFeesWithMultipleFees() {
            Money price = Money.of(BigDecimal.valueOf(100), ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(10);
            
            Money fee1Amount = Money.of(BigDecimal.valueOf(5), ValidatedCurrency.USD);
            Money fee2Amount = Money.of(BigDecimal.valueOf(3), ValidatedCurrency.USD);
            Fee fee1 = mock(Fee.class);
            Fee fee2 = mock(Fee.class);
            when(fee1.amountInNativeCurrency()).thenReturn(fee1Amount);
            when(fee2.amountInNativeCurrency()).thenReturn(fee2Amount);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.BUY,
                VALID_ASSET,
                quantity,
                price,
                List.of(fee1, fee2),
                VALID_DATE,
                ""
            );

            Money totalFees = transaction.calculateTotalFees();
            assertEquals(new BigDecimal("8").setScale(Precision.getMoneyPrecision()), totalFees.amount());
        }

        @Test
        @DisplayName("Should return zero when no fees")
        void shouldReturnZeroWhenNoFees() {
            Money price = Money.of(BigDecimal.valueOf(100), ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(10);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.BUY,
                VALID_ASSET,
                quantity,
                price,
                Collections.emptyList(),
                VALID_DATE,
                ""
            );

            Money totalFees = transaction.calculateTotalFees();
            assertEquals(BigDecimal.ZERO.setScale(Precision.getMoneyPrecision()), totalFees.amount());
        }

        @Test
        @DisplayName("Should throw exception when fee currency mismatches transaction currency")
        void shouldThrowExceptionWhenFeeCurrencyMismatches() {
            Money price = Money.of(BigDecimal.valueOf(100), ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(10);
            
            Money feeAmount = Money.of(BigDecimal.valueOf(5), ValidatedCurrency.CAD); // Different currency
            Fee fee = mock(Fee.class);
            when(fee.amountInNativeCurrency()).thenReturn(feeAmount);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.BUY,
                VALID_ASSET,
                quantity,
                price,
                List.of(fee),
                VALID_DATE,
                ""
            );

            assertThrows(CurrencyMismatchException.class, () -> 
                transaction.calculateTotalFees()
            );
        }
    }

    @Nested
    @DisplayName("Testing getDividendAmount")
    public class GetDividendAMountTests {
        @Test
        void testGetDividendAmountSuccess() {
            Money price = Money.of(BigDecimal.valueOf(100), ValidatedCurrency.USD);
            Money divMoney = Money.of(200, "USD");
            Transaction transaction = Transaction.createDripTransaction(VALID_ID, VAL_ACCOUNT_ID, assetIdentifier, divMoney, price, VALID_DATE, null); 
            assertEquals(divMoney, transaction.getDividendAmount());
        }

        @Test
        void testGetDividendAmountFailsWhenIsDripIsFalse() {
            Money price = Money.of(BigDecimal.valueOf(50.75), ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(20);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.BUY,
                VALID_ASSET,
                quantity,
                price,
                Money.of(BigDecimal.valueOf(10), ValidatedCurrency.USD),
                null,
                VALID_DATE,
                "",
                false
            );

            assertThrows(IllegalStateException.class, ()->transaction.getDividendAmount());
        }

        @Test
        void testGetDividendAmountFailsWhenIsDividendAmountIsNull() {
            Money price = Money.of(BigDecimal.valueOf(50.75), ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(20);

            Transaction transaction = new Transaction(
                VALID_ID,
                VAL_ACCOUNT_ID,
                TransactionType.BUY,
                VALID_ASSET,
                quantity,
                price,
               null,
                null,
                VALID_DATE,
                "",
                true
            );

            assertThrows(IllegalStateException.class, ()->transaction.getDividendAmount());
        }
        
    }

    @Nested
    @DisplayName("Transaction Type Specific Tests")
    class TransactionTypeSpecificTests {

        @ParameterizedTest
        @EnumSource(value = TransactionType.class, names = {"BUY", "SELL", "DIVIDEND", "INTEREST"})
        @DisplayName("Should require asset for trading transactions")
        void shouldRequireAssetForTradingTransactions(TransactionType type) {
            Money price = Money.of(BigDecimal.valueOf(100), ValidatedCurrency.USD);
            BigDecimal quantity = BigDecimal.valueOf(10);

            assertThrows(IllegalArgumentException.class, () -> 
                new Transaction(
                    VALID_ID,
                    VAL_ACCOUNT_ID,
                    type,
                    null, // Missing asset
                    quantity,
                    price,
                    null,
                    VALID_DATE,
                    ""
                )
            );
        }

        @ParameterizedTest
        @EnumSource(value = TransactionType.class, names = {"DEPOSIT", "WITHDRAWAL"})
        @DisplayName("Should allow null asset for cash transactions")
        void shouldAllowNullAssetForCashTransactions(TransactionType type) {
            Money amount = Money.of(BigDecimal.valueOf(1000), ValidatedCurrency.USD);

            assertDoesNotThrow(() -> 
                new Transaction(
                    VALID_ID,
                    VAL_ACCOUNT_ID,
                    type,
                    assetIdentifier, // No asset needed
                    quantity,
                    amount,
                    null,
                    VALID_DATE,
                    ""
                )
            );
        }
    }
}
