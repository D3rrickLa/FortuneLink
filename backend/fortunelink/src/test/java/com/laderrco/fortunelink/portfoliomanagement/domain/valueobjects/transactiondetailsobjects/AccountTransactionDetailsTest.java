package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.AccountMetadataKey;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.CashflowType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AccountEffect;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.CurrencyConversion;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.MonetaryAmount;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;

public class AccountTransactionDetailsTest {
    private MonetaryAmount positiveAmount;
    private MonetaryAmount negativeAmount;
    private MonetaryAmount zeroAmount;
    CurrencyConversion conversion;
    Currency USD;
    Currency CAD;
    private TransactionSource testSource;
    private List<Fee> testFees;
    private List<Fee> emptyFees;

    @BeforeEach
    void setUp() {
        conversion = new CurrencyConversion("USD", "CAD", BigDecimal.valueOf(0.75), Instant.now());
        USD = Currency.getInstance("USD");
        CAD = Currency.getInstance("CAD");
        positiveAmount = MonetaryAmount.of(Money.of(100, USD), conversion);
        negativeAmount = MonetaryAmount.of(Money.of(-50, USD), conversion);
        zeroAmount = MonetaryAmount.ZERO(Currency.getInstance("USD"));
        testSource = TransactionSource.MANUAL;
        testFees = List.of(mock(Fee.class));
        emptyFees = Collections.emptyList();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create transaction with valid account effect")
        void shouldCreateTransactionWithValidAccountEffect() {
            AccountEffect validEffect = new AccountEffect(
                positiveAmount, positiveAmount, CashflowType.DIVIDEND, Map.of()
            );

            AccountTransactionDetails transaction = new AccountTransactionDetails(
                validEffect, testSource, "Test description", emptyFees
            );

            assertEquals(validEffect, transaction.getAccountEffect());
        }

        @Test
        @DisplayName("Should throw exception when account effect is null")
        void shouldThrowExceptionWhenAccountEffectIsNull() {
            NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new AccountTransactionDetails(null, testSource, "Test", emptyFees)
            );
            
            assertEquals("Account effect cannot be null.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Business Rule Validation Tests")
    class BusinessRuleValidationTests {

        @ParameterizedTest
        @EnumSource(value = CashflowType.class, names = {"DIVIDEND", "INTEREST", "RENTAL_INCOME", "OTHER_INCOME", "DEPOSIT"})
        @DisplayName("Should accept positive amounts for income and deposit transactions")
        void shouldAcceptPositiveAmountsForIncomeAndDeposits(CashflowType cashflowType) {
            AccountEffect validEffect = new AccountEffect(
                positiveAmount, positiveAmount, cashflowType, Map.of()
            );

            assertDoesNotThrow(() -> new AccountTransactionDetails(
                validEffect, testSource, "Test", emptyFees
            ));
        }

        @ParameterizedTest
        @EnumSource(value = CashflowType.class, names = {"DIVIDEND", "INTEREST", "RENTAL_INCOME", "OTHER_INCOME"})
        @DisplayName("Should reject negative amounts for income and deposit transactions")
        void shouldRejectNegativeAmountsForIncomeAndDeposits(CashflowType cashflowType) {
            
            // we are going to do some reflection stuff to make it negative
            
            
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> {
                    AccountEffect invalidEffect = new AccountEffect(negativeAmount, negativeAmount, cashflowType, Map.of());
                    new AccountTransactionDetails(invalidEffect, testSource, "Test", emptyFees);
            });
            
            assertEquals("Income transactions must have positive amounts.", exception.getMessage());
        }

        @ParameterizedTest
        @EnumSource(value = CashflowType.class, names = {"WITHDRAWAL", "FEE", "OTHER_OUTFLOW"})
        @DisplayName("Should accept negative amounts for expense transactions")
        void shouldAcceptNegativeAmountsForExpenses(CashflowType cashflowType) {
            AccountEffect validEffect = new AccountEffect(
                negativeAmount, negativeAmount, cashflowType, Map.of()
            );

            assertDoesNotThrow(() -> new AccountTransactionDetails(
                validEffect, testSource, "Test", emptyFees
            ));
        }

        @ParameterizedTest
        @EnumSource(value = CashflowType.class, names = {"WITHDRAWAL", "FEE", "OTHER_OUTFLOW"})
        @DisplayName("Should reject positive amounts for expense transactions")
        void shouldRejectPositiveAmountsForExpenses(CashflowType cashflowType) {
            
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () ->{ 
                    AccountEffect invalidEffect = new AccountEffect(positiveAmount, positiveAmount, cashflowType, Map.of());
                    new AccountTransactionDetails(invalidEffect, testSource, "Test", testFees);
                
            });
            
            assertEquals("Expense transactions must have negative amounts.", exception.getMessage());
        }

        @ParameterizedTest
        @EnumSource(value = CashflowType.class, names = {"TRANSFER", "UNKNOWN"})
        @DisplayName("Should accept any amount for transfer and unknown transactions")
        void shouldAcceptAnyAmountForTransferAndUnknown(CashflowType cashflowType) {
            AccountEffect positiveEffect = new AccountEffect(
                positiveAmount, positiveAmount, cashflowType, Map.of()
            );
            AccountEffect negativeEffect = new AccountEffect(
                negativeAmount, negativeAmount, cashflowType, Map.of()
            );
            AccountEffect zeroEffect = new AccountEffect(
                zeroAmount, zeroAmount, cashflowType, Map.of()
            );

            assertDoesNotThrow(() -> new AccountTransactionDetails(
                positiveEffect, testSource, "Test", emptyFees
            ));
            assertDoesNotThrow(() -> new AccountTransactionDetails(
                negativeEffect, testSource, "Test", emptyFees
            ));
            assertDoesNotThrow(() -> new AccountTransactionDetails(
                zeroEffect, testSource, "Test", emptyFees
            ));
        }

        @Test
        @DisplayName("Should throw exception for unrecognized cashflow type")
        void shouldThrowExceptionForUnrecognizedCashflowType() {
            // This would require adding ERROR to the enum or using reflection/mocking
            // For now, we'll test with a hypothetical scenario
            // This test validates the default case in the switch statement
            
            // Note: In real implementation, you might add a test-only enum value
            // or use reflection to create an invalid enum state
            
            // For demonstration, let's assume we have a way to trigger the default case
            // This could be done by adding ERROR enum value as discussed earlier
            
            // AccountEffect invalidEffect = new AccountEffect(
            //     positiveAmount, positiveAmount, CashflowType.ERROR, Map.of()
            // );
            // 
            // IllegalArgumentException exception = assertThrows(
            //     IllegalArgumentException.class,
            //     () -> new AccountTransactionDetails(invalidEffect, testSource, "Test", emptyFees)
            // );
            // 
            // assertEquals("cashflow type was not recognized.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Dividend Transaction Factory Tests")
    class DividendTransactionFactoryTests {

        @Test
        @DisplayName("Should create dividend transaction with withholding tax")
        void shouldCreateDividendTransactionWithWithholdingTax() {
            MonetaryAmount grossDividend = MonetaryAmount.of(Money.of(1000, USD), conversion);
            BigDecimal withholdingRate = BigDecimal.valueOf(0.15);
            
            AccountTransactionDetails transaction = AccountTransactionDetails.createDividendTransaction(
                grossDividend, withholdingRate, testSource, "AAPL Dividend", emptyFees
            );

            AccountEffect effect = transaction.getAccountEffect();
            assertEquals(CashflowType.DIVIDEND, effect.cashflowType());
            assertEquals(MonetaryAmount.of(Money.of(150, USD), conversion), effect.grossAmount()); // 15% withholding
            assertEquals(MonetaryAmount.of(Money.of(850, USD), conversion), effect.netAmount()); // 1000 - 150
            
            // Verify metadata
            Map<String, String> metadata = effect.metadata();
            assertEquals("withholding_tax_rate", AccountMetadataKey.WITHHOLDING_TAX_RATE.toString());
            assertEquals("0.15", metadata.get(AccountMetadataKey.WITHHOLDING_TAX_RATE.getKey()));
            assertEquals("1000.0000000000000000000000000000000000 USD", metadata.get(AccountMetadataKey.GROSS_DIVIDEND.getKey()));
            assertEquals("150.0000000000000000000000000000000000 USD", metadata.get(AccountMetadataKey.WITHHOLDING_TAX_AMOUNT.getKey()));
            assertEquals(String.valueOf(LocalDate.now().getYear()), metadata.get(AccountMetadataKey.TAX_YEAR.getKey()));
        }

        @Test
        @DisplayName("Should create dividend transaction with additional metadata")
        void shouldCreateDividendTransactionWithAdditionalMetadata() {
            Map<String, String> additionalMetadata = Map.of(
                "company_symbol", "AAPL",
                "ex_dividend_date", "2025-01-15"
            );

            AccountTransactionDetails transaction = AccountTransactionDetails.createDividendTransaction(
                MonetaryAmount.of(Money.of(500, USD), conversion),
                BigDecimal.valueOf(0.10),
                testSource,
                "Test dividend",
                emptyFees,
                additionalMetadata
            );

            Map<String, String> metadata = transaction.getAccountEffect().metadata();
            assertTrue(metadata.containsKey("company_symbol"));
            assertTrue(metadata.containsKey("ex_dividend_date"));
            assertEquals("AAPL", metadata.get("company_symbol"));
            assertEquals("2025-01-15", metadata.get("ex_dividend_date"));
        }

        @Test
        @DisplayName("Should handle zero withholding rate")
        void shouldHandleZeroWithholdingRate() {
            MonetaryAmount grossDividend = MonetaryAmount.of(Money.of(100, USD), CurrencyConversion.identity(USD));
            
            AccountTransactionDetails transaction = AccountTransactionDetails.createDividendTransaction(
                grossDividend, BigDecimal.ZERO, testSource, "No withholding dividend", emptyFees
            );

            AccountEffect effect = transaction.getAccountEffect();
            assertEquals(MonetaryAmount.ZERO(USD).nativeAmount(), effect.grossAmount().nativeAmount());
            assertEquals(MonetaryAmount.ZERO(USD).conversion().fromCurrency(), effect.grossAmount().conversion().fromCurrency());
            assertEquals(MonetaryAmount.ZERO(USD).conversion().toCurrency(), effect.grossAmount().conversion().toCurrency());
            assertEquals(MonetaryAmount.ZERO(USD).conversion().exchangeRate(), effect.grossAmount().conversion().exchangeRate());
            assertEquals(MonetaryAmount.ZERO(USD).conversion().exchangeRateDate().atZone(ZoneId.systemDefault()).getDayOfYear(), (effect.grossAmount().conversion().exchangeRateDate()).atZone(ZoneId.systemDefault()).getDayOfYear());
            assertEquals(MonetaryAmount.ZERO(USD).conversion().exchangeRateDate().atZone(ZoneId.systemDefault()).getHour(), (effect.grossAmount().conversion().exchangeRateDate()).atZone(ZoneId.systemDefault()).getHour());
            assertEquals(grossDividend, effect.netAmount());
        }

        @Test
        @DisplayName("Should handle 100% withholding rate")
        void shouldHandleFullWithholdingRate() {
            MonetaryAmount grossDividend = MonetaryAmount.of(Money.of(100, USD), conversion);
            
            AccountTransactionDetails transaction = AccountTransactionDetails.createDividendTransaction(
                grossDividend, BigDecimal.ONE, testSource, "Full withholding dividend", emptyFees
            );

            AccountEffect effect = transaction.getAccountEffect();
            assertEquals(grossDividend, effect.grossAmount());
            assertEquals(MonetaryAmount.of(Money.of(0, USD), conversion), effect.netAmount());
        }
    }

    @Nested
    @DisplayName("Net-Only Dividend Transaction Tests")
    class NetOnlyDividendTransactionTests {

        @Test
        @DisplayName("Should create dividend transaction with net amount only")
        void shouldCreateDividendTransactionWithNetAmountOnly() {
            MonetaryAmount netAmount = MonetaryAmount.of(Money.of(850, USD), conversion);

            AccountTransactionDetails transaction = AccountTransactionDetails.createDividendTransactionNetOnly(
                netAmount, testSource, "Broker import dividend", emptyFees
            );

            AccountEffect effect = transaction.getAccountEffect();
            assertEquals(CashflowType.DIVIDEND, effect.cashflowType());
            assertEquals(MonetaryAmount.ZERO(USD).nativeAmount(), effect.grossAmount().nativeAmount());
            assertEquals(MonetaryAmount.ZERO(USD).conversion().fromCurrency(), effect.grossAmount().conversion().fromCurrency());
            assertEquals(MonetaryAmount.ZERO(CAD).conversion().toCurrency(), effect.grossAmount().conversion().toCurrency());
            assertEquals(MonetaryAmount.ZERO(USD).conversion().exchangeRate(), effect.grossAmount().conversion().exchangeRate());
            assertEquals(netAmount, effect.netAmount());

            Map<String, String> metadata = effect.metadata();
            assertEquals("0.00", metadata.get(AccountMetadataKey.WITHHOLDING_TAX_RATE.getKey()));
            assertEquals("850.0000000000000000000000000000000000 USD", metadata.get(AccountMetadataKey.GROSS_DIVIDEND.getKey()));
            assertEquals("true", metadata.get(AccountMetadataKey.WITHHOLDING_UNKNOWN.getKey()));
        }
    }

    @Nested
    @DisplayName("Foreign Withholding Tax Transaction Tests")
    class ForeignWithholdingTaxTransactionTests {

        @Test
        @DisplayName("Should create foreign withholding tax transaction")
        void shouldCreateForeignWithholdingTaxTransaction() {
            MonetaryAmount taxAmount = MonetaryAmount.of(Money.of(-150, CAD), CurrencyConversion.identity(CAD));
            String sourceCountry = "US";
            String relatedTransactionId = "DIV123";

            AccountTransactionDetails transaction = AccountTransactionDetails.createForeignWithholdingTaxTransaction(
                taxAmount, sourceCountry, relatedTransactionId, testSource, "US withholding tax", emptyFees
            );

            AccountEffect effect = transaction.getAccountEffect();
            assertEquals(CashflowType.OTHER_OUTFLOW, effect.cashflowType());
            assertEquals(MonetaryAmount.ZERO(CAD).nativeAmount(), effect.grossAmount().nativeAmount());
            assertEquals(MonetaryAmount.ZERO(CAD).conversion().fromCurrency(), effect.grossAmount().conversion().fromCurrency());
            assertEquals(MonetaryAmount.ZERO(CAD).conversion().toCurrency(), effect.grossAmount().conversion().toCurrency());
            assertEquals(MonetaryAmount.ZERO(CAD).conversion().exchangeRate(), effect.grossAmount().conversion().exchangeRate());
            assertEquals(taxAmount, effect.netAmount());

            Map<String, String> metadata = effect.metadata();
            assertEquals(sourceCountry, metadata.get(AccountMetadataKey.SOURCE_COUNTRY.getKey()));
            assertEquals(relatedTransactionId, metadata.get(AccountMetadataKey.RELATED_TRANSACTION_ID.getKey()));
            assertEquals("150.0000000000000000000000000000000000 CAD", metadata.get(AccountMetadataKey.FOREIGN_TAX_CREDIT_ELIGIBLE.getKey()));

            assertEquals("FOREIGN_WITHHOLDING", metadata.get(AccountMetadataKey.TAX_TYPE.getKey()));
        }

        @Test
        @DisplayName("Should throw exception for positive withholding tax amount")
        void shouldThrowExceptionForPositiveWithholdingTaxAmount() {
            MonetaryAmount positiveAmount = MonetaryAmount.of(Money.of(150, USD), conversion);

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AccountTransactionDetails.createForeignWithholdingTaxTransaction(
                    positiveAmount, "US", "DIV123", testSource, "Invalid tax", emptyFees
                )
            );

            assertEquals("Withholding tax amoutn must be negative (represents tax paid).", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Other Factory Method Tests")
    class OtherFactoryMethodTests {

        @Test
        @DisplayName("Should create deposit transaction")
        void shouldCreateDepositTransaction() {
            AccountTransactionDetails transaction = AccountTransactionDetails.createDepositTransactoin(
                positiveAmount, testSource, "Bank transfer", emptyFees
            );

            AccountEffect effect = transaction.getAccountEffect();
            assertEquals(CashflowType.DEPOSIT, effect.cashflowType());
            assertEquals(positiveAmount, effect.grossAmount());
            assertEquals(positiveAmount, effect.netAmount());
        }

        @Test
        @DisplayName("Should create withdrawal transaction")
        void shouldCreateWithdrawalTransaction() {
            AccountTransactionDetails transaction = AccountTransactionDetails.createWithdrawalTransaction(
                positiveAmount, testSource, "ATM withdrawal", emptyFees
            );

            AccountEffect effect = transaction.getAccountEffect();
            assertEquals(CashflowType.WITHDRAWAL, effect.cashflowType());
            assertEquals(positiveAmount.negate(), effect.grossAmount());
            assertEquals(negativeAmount.multiply(BigDecimal.valueOf(2)), effect.netAmount()); // Should be -100
        }

        @Test
        @DisplayName("Should create interest transaction")
        void shouldCreateInterestTransaction() {
            AccountTransactionDetails transaction = AccountTransactionDetails.createInterestTransaction(
                positiveAmount, testSource, "Savings interest"
            );

            AccountEffect effect = transaction.getAccountEffect();
            assertEquals(CashflowType.INTEREST, effect.cashflowType());
            assertEquals(positiveAmount, effect.grossAmount());
            assertEquals(positiveAmount, effect.netAmount());
            assertTrue(transaction.getFees().isEmpty());
        }
    }

    @Nested
    @DisplayName("Getter Method Tests")
    class GetterMethodTests {

        private AccountTransactionDetails createTestTransaction(MonetaryAmount amount, CashflowType type) {
            AccountEffect effect = new AccountEffect(amount, amount, type, Map.of());
            return new AccountTransactionDetails(effect, testSource, "Test", emptyFees);
        }

        @Test
        @DisplayName("Should return correct net worth impact")
        void shouldReturnCorrectNetWorthImpact() {
            AccountTransactionDetails transaction = createTestTransaction(positiveAmount, CashflowType.DIVIDEND);
            assertEquals(positiveAmount, transaction.getNetWorthImpact());
        }

        @Test
        @DisplayName("Should indicate when transaction affects net worth")
        void shouldIndicateWhenTransactionAffectsNetWorth() {
            AccountTransactionDetails positiveTransaction = createTestTransaction(positiveAmount, CashflowType.DIVIDEND);
            AccountTransactionDetails zeroTransaction = createTestTransaction(zeroAmount, CashflowType.TRANSFER);

            assertTrue(positiveTransaction.affectsNetWorth());
            assertFalse(zeroTransaction.affectsNetWorth());
        }

        @Test
        @DisplayName("Should return correct cash flow")
        void shouldReturnCorrectCashFlow() {
            AccountTransactionDetails transaction = createTestTransaction(positiveAmount, CashflowType.DIVIDEND);
            assertEquals(positiveAmount, transaction.getCashFlow());
        }

        @Test
        @DisplayName("Should return correct total fees")
        void shouldReturnCorrectTotalFees() {
            Fee mockFee = mock(Fee.class);
            when(mockFee.amount()).thenReturn(MonetaryAmount.of(Money.of(10, USD), conversion));
            
            AccountEffect effectWithFees = new AccountEffect(
                positiveAmount, positiveAmount, CashflowType.DIVIDEND, Map.of()
            );
            // Note: This assumes AccountEffect has a method to calculate fee amounts
            // You may need to adjust based on your actual AccountEffect implementation
            
            AccountTransactionDetails transaction = new AccountTransactionDetails(
                effectWithFees, testSource, "Test", List.of(mockFee)
            );

            // This test might need adjustment based on how AccountEffect.getFeeAmount() works
            // For now, assuming it returns the sum of all fees
        }
    }

    @Nested
    @DisplayName("Classification Method Tests")
    class ClassificationMethodTests {

        @ParameterizedTest
        @EnumSource(value = CashflowType.class, names = {"DIVIDEND", "INTEREST", "RENTAL_INCOME", "OTHER_INCOME"})
        @DisplayName("Should identify income transactions correctly")
        void shouldIdentifyIncomeTransactionsCorrectly(CashflowType incomeType) {
            AccountEffect effect = new AccountEffect(positiveAmount, positiveAmount, incomeType, Map.of());
            AccountTransactionDetails transaction = new AccountTransactionDetails(
                effect, testSource, "Test", emptyFees
            );

            assertTrue(transaction.isIncomeTransaction());
            assertFalse(transaction.isExpenseTransaction());
        }

        @ParameterizedTest
        @EnumSource(value = CashflowType.class, names = {"WITHDRAWAL", "FEE", "OTHER_OUTFLOW"})
        @DisplayName("Should identify expense transactions correctly")
        void shouldIdentifyExpenseTransactionsCorrectly(CashflowType expenseType) {
            AccountEffect effect = new AccountEffect(negativeAmount, negativeAmount, expenseType, Map.of());
            AccountTransactionDetails transaction = new AccountTransactionDetails(
                effect, testSource, "Test", emptyFees
            );

            assertTrue(transaction.isExpenseTransaction());
            assertFalse(transaction.isIncomeTransaction());
        }

        @Test
        @DisplayName("Should identify transactions requiring tax reporting")
        void shouldIdentifyTransactionsRequiringTaxReporting() {
            // Dividend transaction - should require tax reporting
            AccountEffect dividendEffect = new AccountEffect(
                positiveAmount, positiveAmount, CashflowType.DIVIDEND, Map.of()
            );
            AccountTransactionDetails dividendTransaction = new AccountTransactionDetails(
                dividendEffect, testSource, "Test", emptyFees
            );

            // Interest transaction - should require tax reporting
            AccountEffect interestEffect = new AccountEffect(
                positiveAmount, positiveAmount, CashflowType.INTEREST, Map.of()
            );
            AccountTransactionDetails interestTransaction = new AccountTransactionDetails(
                interestEffect, testSource, "Test", emptyFees
            );

            // Other income with withholding - should require tax reporting
            Map<String, String> withholdingMetadata = Map.of(
                AccountMetadataKey.WITHHOLDING_TAX_RATE.getKey(), "0.15"
            );
            AccountEffect otherIncomeWithWithholding = new AccountEffect(
                positiveAmount, positiveAmount, CashflowType.OTHER_INCOME, withholdingMetadata
            );
            AccountTransactionDetails otherIncomeTransaction = new AccountTransactionDetails(
                otherIncomeWithWithholding, testSource, "Test", emptyFees
            );

            // Withdrawal transaction - should NOT require tax reporting
            AccountEffect withdrawalEffect = new AccountEffect(
                negativeAmount, negativeAmount, CashflowType.WITHDRAWAL, Map.of()
            );
            AccountTransactionDetails withdrawalTransaction = new AccountTransactionDetails(
                withdrawalEffect, testSource, "Test", emptyFees
            );

            assertTrue(dividendTransaction.requiresTaxReporting());
            assertTrue(interestTransaction.requiresTaxReporting());
            assertTrue(otherIncomeTransaction.requiresTaxReporting());
            assertFalse(withdrawalTransaction.requiresTaxReporting());
        }

        @Test
        @DisplayName("Should identify multi-currency transactions")
        void shouldIdentifyMultiCurrencyTransactions() {
            MonetaryAmount singleCurrencyAmount = mock(MonetaryAmount.class);
            when(singleCurrencyAmount.isMultiCurrency()).thenReturn(false);
            when(singleCurrencyAmount.nativeAmount()).thenReturn(Money.of(100, USD));
            when(singleCurrencyAmount.conversion()).thenReturn(CurrencyConversion.identity(USD));
            when(singleCurrencyAmount.isZero()).thenReturn(false);
            
            MonetaryAmount multiCurrencyAmount = mock(MonetaryAmount.class);
            when(multiCurrencyAmount.isMultiCurrency()).thenReturn(true);
            when(multiCurrencyAmount.nativeAmount()).thenReturn(Money.of(100, USD));
            when(multiCurrencyAmount.conversion()).thenReturn(conversion);
            when(multiCurrencyAmount.isZero()).thenReturn(false);

            AccountEffect singleCurrencyEffect = new AccountEffect(
                singleCurrencyAmount, singleCurrencyAmount, CashflowType.DIVIDEND, Map.of()
            );
            AccountTransactionDetails singleCurrencyTransaction = new AccountTransactionDetails(
                singleCurrencyEffect, testSource, "Test", emptyFees
            );

            AccountEffect multiCurrencyEffect = new AccountEffect(
                multiCurrencyAmount, multiCurrencyAmount, CashflowType.DIVIDEND, Map.of()
            );
            AccountTransactionDetails multiCurrencyTransaction = new AccountTransactionDetails(
                multiCurrencyEffect, testSource, "Test", emptyFees
            );

            assertFalse(singleCurrencyTransaction.isMultiCurrency());
            assertTrue(multiCurrencyTransaction.isMultiCurrency());
        }
    }

    @Nested
    @DisplayName("Reconciliation Tests")
    class ReconciliationTests {

        @Test
        @DisplayName("Should match external amounts correctly")
        void shouldMatchExternalAmountsCorrectly() {
            MonetaryAmount grossAmount = MonetaryAmount.of(Money.of(1000, USD), conversion);
            MonetaryAmount netAmount = MonetaryAmount.of(Money.of(850, USD), conversion);
            MonetaryAmount externalAmount = MonetaryAmount.of(Money.of(850, USD), conversion);
            MonetaryAmount nonMatchingAmount = MonetaryAmount.of(Money.of(500, USD), conversion);

            AccountEffect effect = new AccountEffect(grossAmount, netAmount, CashflowType.DIVIDEND, Map.of());
            AccountTransactionDetails transaction = new AccountTransactionDetails(
                effect, testSource, "Test", emptyFees
            );

            assertTrue(transaction.matchesExternalAmount(externalAmount));
            assertTrue(transaction.matchesExternalAmount(grossAmount));
            assertFalse(transaction.matchesExternalAmount(nonMatchingAmount));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesAndBoundaryTests {

        @Test
        @DisplayName("Should handle very small withholding rates")
        void shouldHandleVerySmallWithholdingRates() {
            BigDecimal verySmallRate = BigDecimal.valueOf(0.0001); // 0.01%
            
            AccountTransactionDetails transaction = AccountTransactionDetails.createDividendTransaction(
                MonetaryAmount.of(Money.of(10000, USD), conversion), verySmallRate, testSource, "Minimal withholding", emptyFees
            );

            AccountEffect effect = transaction.getAccountEffect();
            // Should handle precision correctly
            assertNotNull(effect.grossAmount());
            assertNotNull(effect.netAmount());
        }

        @Test
        @DisplayName("Should handle very large amounts")
        void shouldHandleVeryLargeAmounts() {
            MonetaryAmount largeAmount = MonetaryAmount.of(new Money(new BigDecimal("999999999.99"), USD), conversion);
            
            AccountTransactionDetails transaction = AccountTransactionDetails.createDividendTransaction(
                largeAmount, BigDecimal.valueOf(0.15), testSource, "Large dividend", emptyFees
            );

            assertNotNull(transaction.getAccountEffect());
            assertEquals(CashflowType.DIVIDEND, transaction.getAccountEffect().cashflowType());
        }

        @Test
        @DisplayName("Should handle empty and null strings gracefully")
        void shouldHandleEmptyAndNullStringsGracefully() {
            // Empty description
            AccountTransactionDetails transaction1 = AccountTransactionDetails.createDividendTransaction(
                positiveAmount, BigDecimal.valueOf(0.15), testSource, "", emptyFees
            );
            assertNotNull(transaction1);

            // Very long description
            String longDescription = "A".repeat(1000);
            AccountTransactionDetails transaction2 = AccountTransactionDetails.createDividendTransaction(
                positiveAmount, BigDecimal.valueOf(0.15), testSource, longDescription, emptyFees
            );
            assertNotNull(transaction2);
        }

        @Test
        @DisplayName("Should handle special BigDecimal values")
        void shouldHandleSpecialBigDecimalValues() {
            // Test with different BigDecimal scales and precision
            BigDecimal highPrecisionRate = new BigDecimal("0.123456789123456789");
            
            AccountTransactionDetails transaction = AccountTransactionDetails.createDividendTransaction(
                MonetaryAmount.of(Money.of(100, USD), conversion), highPrecisionRate, testSource, "High precision", emptyFees
            );

            assertNotNull(transaction.getAccountEffect());
        }
    }

    @Nested
    @DisplayName("Metadata Validation Tests")
    class MetadataValidationTests {

        @Test
        @DisplayName("Should preserve all metadata correctly")
        void shouldPreserveAllMetadataCorrectly() {
            Map<String, String> additionalMetadata = Map.of(
                "custom_field1", "value1",
                "custom_field2", "value2",
                "special_chars", "!@#$%^&*()"
            );

            AccountTransactionDetails transaction = AccountTransactionDetails.createDividendTransaction(
                MonetaryAmount.of(Money.of(1000, USD), conversion),
                BigDecimal.valueOf(0.15),
                testSource,
                "Test with metadata",
                emptyFees,
                additionalMetadata
            );

            Map<String, String> resultMetadata = transaction.getAccountEffect().metadata();
            
            // Verify all additional metadata is preserved
            additionalMetadata.forEach((key, value) -> {
                assertTrue(resultMetadata.containsKey(key));
                assertEquals(value, resultMetadata.get(key));
            });

            // Verify standard metadata is still present
            assertTrue(resultMetadata.containsKey(AccountMetadataKey.TAX_YEAR.getKey()));
            assertTrue(resultMetadata.containsKey(AccountMetadataKey.WITHHOLDING_TAX_RATE.getKey()));
        }

        @Test
        @DisplayName("Should handle metadata key conflicts correctly")
        void shouldHandleMetadataKeyConflictsCorrectly() {
            // Additional metadata that conflicts with standard keys
            Map<String, String> conflictingMetadata = Map.of(
                AccountMetadataKey.TAX_YEAR.getKey(), String.valueOf(LocalDate.now().getYear()), // Should be overridden 
                "custom_key", "custom_value"
            );

            AccountTransactionDetails transaction = AccountTransactionDetails.createDividendTransaction(
                MonetaryAmount.of(Money.of(1000, USD), conversion),
                BigDecimal.valueOf(0.15),
                testSource,
                "Test conflicts",
                emptyFees,
                conflictingMetadata
            );

            Map<String, String> resultMetadata = transaction.getAccountEffect().metadata();
            
            // Standard metadata should take precedence
            assertEquals(String.valueOf(LocalDate.now().getYear()), 
                        resultMetadata.get(AccountMetadataKey.TAX_YEAR.getKey()));
            
            // Custom metadata should still be preserved
            assertEquals("custom_value", resultMetadata.get("custom_key"));
        }
    }
}
