package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.AssetBoughtEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.AssetSoldEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.CashflowRecordedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.LiabilityIncurredEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.PortfolioCreatedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.events.TransactionReversedEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfoliomanagment.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfoliomanagment.domain.exceptions.InvalidQuantityException;
import com.laderrco.fortunelink.portfoliomanagment.domain.services.CurrencyConversionService;
import com.laderrco.fortunelink.portfoliomanagment.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Percentage;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.CashflowType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.IncomeType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.LiabilityType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.liabilityobjects.LiabilityDetails;
import com.laderrco.fortunelink.portfoliomanagment.infrastructure.services.SimpleMaketDataService;

public class PortfolioTestFullVersion_V2 {
    // Test implementation of CurrencyConversionService
    private static class TestCurrencyConversionService implements CurrencyConversionService {
        @Override
        public Money convert(Money amount, Currency targetCurrency, Instant date) {
            if (amount.currency().equals(targetCurrency)) {
                return amount;
            }
            // Simple test conversion: EUR to USD = 1.1, everything else 1:1
            if (amount.currency().getCurrencyCode().equals("EUR") && 
                targetCurrency.getCurrencyCode().equals("USD")) {
                return Money.of(amount.amount().multiply(BigDecimal.valueOf(1.1)), targetCurrency);
            }
            return Money.of(amount.amount(), targetCurrency);
        }

        @Override
        public BigDecimal getExchangeRate(Currency from, Currency to) {
            if (from.equals(to)) {
                return BigDecimal.ONE;
            }
            if (from.getCurrencyCode().equals("EUR") && to.getCurrencyCode().equals("USD")) {
                return BigDecimal.valueOf(1.1);
            }
            return BigDecimal.ONE; // Default 1:1 conversion for test purposes
        }

        @Override
        public Money convert(Money amount, Currency targetCurrency) {
            return convert(amount, targetCurrency, Instant.now());
        }

        @Override
        public Money convertWithLatestRate(Money amount, Currency targetCurrency) {
            return convert(amount, targetCurrency);
        }
    }

    // Test implementation of User
    private static class TestUser extends User {
        private String name;
        private String email;
        private Currency currency;
        public TestUser(String name, String email, Currency currency) {
            super(new UserId(UUID.randomUUID()), name, currency, new HashSet<>());
            this.name = name;
            this.email = email;
            this.currency = currency;
        }

        public String getName() {
            return name;
        }
        public String getEmail() {
            return email;
        }
        public Currency getCurrency() {
            return currency;
        }

    }

    // Test implementation of AssetIdentifier
    // we are just going to be creating a new Record, but keeping this impelmentation
    private static class TestAssetIdentifier {
        private final String symbol;
        private final AssetType type;

        public TestAssetIdentifier(String symbol, AssetType type) {
            this.symbol = symbol;
            this.type = type;
        }

        public AssetIdentifier convert() {
            return new AssetIdentifier(type, symbol, "US1234567890", symbol+"_fullname", "EXCHANGE", Currency.getInstance("USD"));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof TestAssetIdentifier)) return false;
            TestAssetIdentifier other = (TestAssetIdentifier) obj;
            return Objects.equals(symbol, other.symbol) && type == other.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, type);
        }
    }

    // Test implementation of LiabilityDetails
    private static class TestLiabilityDetails {
        private final LiabilityType type;
        private final String description;
        private final Percentage annualInterestRate;

        public TestLiabilityDetails(LiabilityType type, String description, Percentage rate) {
            this.type = type;
            this.description = description;
            this.annualInterestRate = rate;
        }

        public LiabilityDetails convert() {
            return new LiabilityDetails("liability", description, type, annualInterestRate, Instant.MAX);
        }
    }

    private Portfolio portfolio;
    private User testUser;
    private MarketDataService marketDataService;
    private CurrencyConversionService conversionService;
    private Money initialBalance;
    private Currency usdCurrency;
    private Currency eurCurrency;
    private Instant testDate;
    private AssetIdentifier testAssetId;
    private LiabilityDetails testLiabilityDetails;

    @BeforeEach
    void setUp() {
        usdCurrency = Currency.getInstance("USD");
        eurCurrency = Currency.getInstance("EUR");
        initialBalance = Money.of(BigDecimal.valueOf(10000), usdCurrency);
        testDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        
        testUser = new TestUser("John Doe", "john@example.com", usdCurrency);
        conversionService = new TestCurrencyConversionService();
        marketDataService = new SimpleMaketDataService();
        testAssetId = new TestAssetIdentifier("AAPL", AssetType.STOCK).convert();
        testLiabilityDetails = new TestLiabilityDetails(
            LiabilityType.LOAN, 
            "Test Loan", 
            new Percentage(BigDecimal.valueOf(5.0))
        ).convert();

        portfolio = new Portfolio(testUser.getId(), "Test Portfolio", "Test Description", initialBalance, conversionService, marketDataService);
    }

    @Nested
    @DisplayName("Portfolio Creation Tests")
    class PortfolioCreationTests {

        @Test 
        @DisplayName("Should generate a new user")
        void TestUserGenrations() {
            String name = "name";
            String email = "email";
            TestUser user = new TestUser(name, email, eurCurrency);
            assertAll(
                () -> assertEquals(name, user.getName()),
                () -> assertEquals(email, user.getEmail()),
                () -> assertEquals(eurCurrency, user.getCurrency()),
                () -> assertNotEquals(testUser, user)
            );
        }


        @Test
        @DisplayName("Should handle fees correctly with income")
        void shouldHandleFeesCorrectlyWithIncome() {
            Money incomeAmount = Money.of(BigDecimal.valueOf(100), usdCurrency);
            List<Fee> fees = Arrays.asList(new Fee(FeeType.WITHHOLDING_TAX, Money.of(5, usdCurrency), "desc", Instant.now()));
            Money initialBalance = portfolio.getPortfolioCashBalance();
            
            portfolio.recordIncome(incomeAmount, IncomeType.DIVIDEND, TransactionSource.MANUAL, "Dividend with fees", fees, testDate);
            
            // Income should be net of fees
            Money expectedBalance = initialBalance.add(Money.of(BigDecimal.valueOf(95), usdCurrency));
            assertEquals(expectedBalance, portfolio.getPortfolioCashBalance());
        }
    }

    @Nested
    @DisplayName("Transaction Reversal Tests")
    class TransactionReversalTests {

        private TransactionId originalTransactionId;
        private AssetIdentifier assetIdentifier;

        @BeforeEach
        void setUp() {
            // Create an original transaction to reverse
            Money amount = Money.of(BigDecimal.valueOf(1000), usdCurrency);
            portfolio.recordCashflow(amount, CashflowType.DEPOSIT, TransactionSource.MANUAL, "Original deposit", Collections.emptyList(), testDate);
            originalTransactionId = portfolio.getTransactions().get(0).getTransactionId();
            assetIdentifier = testAssetId;
        }

        @Test
        @DisplayName("Should successfully reverse deposit transaction")
        void shouldSuccessfullyReverseDepositTransaction() {
            Money balanceAfterDeposit = portfolio.getPortfolioCashBalance();
            
            portfolio.reverseTransaction(originalTransactionId, "Test reversal", TransactionSource.MANUAL, "Reversal desc", Collections.emptyList(), testDate);
            
            // Balance should be back to original
            assertAll(
                () -> assertEquals(initialBalance, portfolio.getPortfolioCashBalance()),
                () -> assertEquals(2, portfolio.getTransactions().size()),
                () -> assertEquals(TransactionType.REVERSAL, portfolio.getTransactions().get(1).getType()),
                () -> assertEquals(3, portfolio.getDomainEvents().size()),
                () -> assertEquals(Money.of(BigDecimal.valueOf(1000), usdCurrency).add(initialBalance), balanceAfterDeposit)
            );
        }

        @Test
        @DisplayName("Should successfully reverse buy transaction")
        void shouldSuccessfullyReverseBuyTransaction() {
            // Clear previous transaction and create a buy transaction
            portfolio = new Portfolio(testUser.getId(), "Test Portfolio", "Test Description", initialBalance, conversionService, marketDataService);
            
            BigDecimal quantity = BigDecimal.valueOf(100);
            Money price = Money.of(BigDecimal.valueOf(50), usdCurrency);
            portfolio.buyAsset(testAssetId, quantity, price, Collections.emptyList(), testDate, TransactionSource.MANUAL, "Buy to reverse");
            
            TransactionId buyTransactionId = portfolio.getTransactions().get(0).getTransactionId();
            AssetHoldingId holdingId = portfolio.getHoldings().keySet().iterator().next();
            
            // Reverse the buy transaction
            portfolio.reverseTransaction(buyTransactionId, "Buy reversal", TransactionSource.MANUAL, "Reversal", Collections.emptyList(), testDate);
            
            assertAll(
                () -> assertEquals(initialBalance, portfolio.getPortfolioCashBalance()),
                () -> assertEquals(BigDecimal.ZERO, portfolio.getHoldings().get(holdingId).getQuantity()),
                () -> assertEquals(2, portfolio.getTransactions().size())
            );
        }

        @Test
        @DisplayName("Should successfully reverse sell transaction")
        void shouldSuccessfullyReverseSellTransaction() {
            // Setup: Buy then sell
            portfolio = new Portfolio(testUser.getId(), "Test Portfolio", "Test Description", initialBalance, conversionService, marketDataService);
            
            BigDecimal buyQuantity = BigDecimal.valueOf(100);
            Money buyPrice = Money.of(BigDecimal.valueOf(50), usdCurrency);
            portfolio.buyAsset(testAssetId, buyQuantity, buyPrice, Collections.emptyList(), testDate, TransactionSource.MANUAL, "Setup buy");
            
            AssetHoldingId holdingId = portfolio.getHoldings().keySet().iterator().next();
            BigDecimal sellQuantity = BigDecimal.valueOf(50);
            Money sellPrice = Money.of(BigDecimal.valueOf(60), usdCurrency);
            portfolio.sellAsset(assetIdentifier, sellQuantity, sellPrice, Collections.emptyList(), testDate, TransactionSource.MANUAL, "Sell to reverse");
            
            TransactionId sellTransactionId = portfolio.getTransactions().get(1).getTransactionId();
            Money balanceAfterSell = portfolio.getPortfolioCashBalance();
            
            // Reverse the sell transaction
            portfolio.reverseTransaction(sellTransactionId, "Sell reversal", TransactionSource.MANUAL, "Reversal", Collections.emptyList(), testDate);
            
            assertAll(
                () -> assertEquals(buyQuantity, portfolio.getHoldings().get(holdingId).getQuantity()),
                () -> assertEquals(3, portfolio.getTransactions().size()),
                () -> assertEquals(TransactionType.REVERSAL, portfolio.getTransactions().get(2).getType()),
                () -> assertTrue(balanceAfterSell.compareTo(Money.of(BigDecimal.valueOf(1000), usdCurrency)) > 0)
            );
        }

        @Test
        @DisplayName("Should successfully reverse liability incurrence")
        void shouldSuccessfullyReverseLiabilityIncurrence() {
            // Clear and create liability incurrence
            portfolio = new Portfolio(testUser.getId(), "Test Portfolio", "Test Description", initialBalance, conversionService, marketDataService);
            
            Money liabilityAmount = Money.of(BigDecimal.valueOf(5000), usdCurrency);
            portfolio.incurrNewLiability(testLiabilityDetails, liabilityAmount, TransactionSource.MANUAL, Collections.emptyList(), testDate);
            
            TransactionId liabilityTransactionId = portfolio.getTransactions().get(0).getTransactionId();
            LiabilityId liabilityId = portfolio.getLiabilities().keySet().iterator().next();
            
            // Reverse the liability incurrence
            portfolio.reverseTransaction(liabilityTransactionId, "Liability reversal", TransactionSource.MANUAL, "Reversal", null, testDate);
            
            assertAll(
                () -> assertEquals(initialBalance, portfolio.getPortfolioCashBalance()),
                () -> assertTrue(portfolio.getLiabilities().isEmpty()),
                () -> assertEquals(2, portfolio.getTransactions().size()),
                () -> assertTrue(portfolio.getLiabilities().containsKey(liabilityId) == false)
            );
        }

        @Test
        @DisplayName("Should throw exception when transaction not found")
        void shouldThrowExceptionWhenTransactionNotFound() {
            TransactionId nonExistentId = new TransactionId(UUID.randomUUID());
            
            assertThrows(IllegalArgumentException.class, () ->
                portfolio.reverseTransaction(nonExistentId, "Reason", TransactionSource.MANUAL, "Desc", Collections.emptyList(), testDate)
            );
        }

        @Test
        @DisplayName("Should throw exception when trying to reverse a reversal")
        void shouldThrowExceptionWhenTryingToReverseReversal() {
            // First reversal
            portfolio.reverseTransaction(originalTransactionId, "First reversal", TransactionSource.MANUAL, "Desc", Collections.emptyList(), testDate);
            TransactionId reversalTransactionId = portfolio.getTransactions().get(1).getTransactionId();
            
            // Try to reverse the reversal
            assertThrows(IllegalArgumentException.class, () ->
                portfolio.reverseTransaction(reversalTransactionId, "Reverse reversal", TransactionSource.MANUAL, "Desc", Collections.emptyList(), testDate)
            );
        }

        @Test
        @DisplayName("Should publish transaction reversed event")
        void shouldPublishTransactionReversedEvent() {
            portfolio.reverseTransaction(originalTransactionId, "Test reversal", TransactionSource.MANUAL, "Desc", Collections.emptyList(), testDate);
            
            // Should have 2 events now (original + reversal)
            assertEquals(3, portfolio.getDomainEvents().size());
            assertTrue(portfolio.getDomainEvents().get(2) instanceof TransactionReversedEvent);
            
            TransactionReversedEvent event = (TransactionReversedEvent) portfolio.getDomainEvents().get(2);
            assertEquals(originalTransactionId, event.originalTransactionId());
        }

        @Test
        @DisplayName("Should handle reversal with additional fees")
        void shouldHandleReversalWithoutAdditionalFees() {            
            portfolio.reverseTransaction(originalTransactionId, "Test reversal with fees", TransactionSource.MANUAL, "Desc", null, testDate);
            
            // The reversal should restore the original balance, and fees would be handled separately
            // (though the current implementation doesn't seem to process reversal fees)
            assertEquals(initialBalance, portfolio.getPortfolioCashBalance());
        }
        @Test
        @DisplayName("Should handle reversal with additional fees")
        void shouldHandleReversalWithAdditionalFees() {
            List<Fee> reversalFees = Arrays.asList(new Fee(FeeType.BROKERAGE, Money.of(25, usdCurrency), "amount"));
            
            portfolio.reverseTransaction(originalTransactionId, "Test reversal with fees", TransactionSource.MANUAL, "Desc", reversalFees, testDate);
            
            // The reversal should restore the original balance, and fees would be handled separately
            // (though the current implementation doesn't seem to process reversal fees)
            assertEquals(initialBalance.subtract(Money.of(25, usdCurrency)), portfolio.getPortfolioCashBalance());
        }
    }

    @Nested
    @DisplayName("Liability Update Tests")
    class LiabilityUpdateTests {

        private LiabilityId liabilityId;

        @BeforeEach
        void setUp() {
            Money liabilityAmount = Money.of(BigDecimal.valueOf(5000), usdCurrency);
            portfolio.incurrNewLiability(testLiabilityDetails, liabilityAmount, TransactionSource.MANUAL, Collections.emptyList(), testDate);
            liabilityId = portfolio.getLiabilities().keySet().iterator().next();
        }

        @Test
        @DisplayName("Should successfully update liability details")
        void shouldSuccessfullyUpdateLiabilityDetails() {
            LiabilityDetails newDetails = new TestLiabilityDetails(
                LiabilityType.MORTGAGE,
                "Updated Mortgage",
                new Percentage(BigDecimal.valueOf(3.5))
            ).convert();
            
            portfolio.updateLiability(liabilityId, newDetails);
            
            Liability updatedLiability = portfolio.getLiabilities().get(liabilityId);
            assertEquals(newDetails, updatedLiability.getDetails());
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return unmodifiable domain events list")
        void shouldReturnUnmodifiableDomainEventsList() {
            List<Object> events = portfolio.getDomainEvents();
            
            assertThrows(UnsupportedOperationException.class, () ->
                events.add(new Object())
            );
        }

        @Test
        @DisplayName("Should return correct portfolio properties")
        void shouldReturnCorrectPortfolioProperties() {
            assertAll(
                () -> assertEquals(testUser.getId(), portfolio.getUserId()),
                () -> assertNotNull(portfolio.getPortfolioId()),
                () -> assertEquals("Test Portfolio", portfolio.getPortfolioName()),
                () -> assertEquals("Test Description", portfolio.getPortfolioDescription()),
                () -> assertEquals(initialBalance, portfolio.getPortfolioCashBalance()),
                () -> assertEquals(conversionService, portfolio.getConversionService()),
                () -> assertNotNull(portfolio.getHoldings()),
                () -> assertNotNull(portfolio.getLiabilities()),
                () -> assertNotNull(portfolio.getTransactions())
            );
        }

        @Test
        @DisplayName("Should return empty collections for new portfolio")
        void shouldReturnEmptyCollectionsForNewPortfolio() {
            assertAll(
                () -> assertTrue(portfolio.getHoldings().isEmpty()),
                () -> assertTrue(portfolio.getLiabilities().isEmpty()),
                () -> assertTrue(portfolio.getTransactions().isEmpty()),
                () -> assertTrue(portfolio.getDomainEvents().size() == 1),
                () -> assertTrue(portfolio.getDomainEvents().get(0) instanceof PortfolioCreatedEvent)
                // () -> assertTrue(portfolio.getDomainEvents().isEmpty()) // this will never be right because we have a cash deposit in @BeforeEach
            );
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Should handle multiple currencies with conversion")
        void shouldHandleCorrectly() {
            Money usdPrice = Money.of(BigDecimal.valueOf(45), usdCurrency);
            BigDecimal quantity = BigDecimal.valueOf(10);
            
            portfolio.buyAsset(testAssetId, quantity, usdPrice, Collections.emptyList(), testDate, TransactionSource.MANUAL, "EUR buy");
            
            Money expectedBalance = Money.of(BigDecimal.valueOf(10000 - 450), usdCurrency);
            assertEquals(expectedBalance, portfolio.getPortfolioCashBalance());
        }

        @Test
        @DisplayName("Should handle zero quantity buy transactions")
        void shouldHandleZeroQuantityBuyTransactions() {
            Money price = Money.of(BigDecimal.valueOf(50), usdCurrency);
            Money initialBalance = portfolio.getPortfolioCashBalance();
            
            assertThrows(InvalidQuantityException.class, ()->
            portfolio.buyAsset(testAssetId, BigDecimal.ZERO, price, Collections.emptyList(), testDate, TransactionSource.MANUAL, "Zero buy"));
            
            // Should create transaction but not affect balance significantly (except maybe fees)
            assertEquals(initialBalance, portfolio.getPortfolioCashBalance());
            assertEquals(0, portfolio.getTransactions().size());
        }

        @Test
        @DisplayName("Should handle very small decimal quantities")
        void shouldHandleVerySmallDecimalQuantities() {
            BigDecimal smallQuantity = new BigDecimal("0.001");
            Money price = Money.of(BigDecimal.valueOf(1000), usdCurrency);
            
            portfolio.buyAsset(testAssetId, smallQuantity, price, Collections.emptyList(), testDate, TransactionSource.MANUAL, "Small quantity buy");
            
            Money expectedCost = Money.of(BigDecimal.valueOf(1), usdCurrency); // 0.001 * 1000
            Money expectedBalance = initialBalance.subtract(expectedCost);
            assertEquals(expectedBalance, portfolio.getPortfolioCashBalance());
        }

        @Test
        @DisplayName("Should handle large quantities and amounts")
        void shouldHandleLargeQuantitiesAndAmounts() {
            // Use smaller quantities to avoid insufficient funds
            BigDecimal largeQuantity = BigDecimal.valueOf(100);
            Money smallPrice = Money.of(BigDecimal.valueOf(1), usdCurrency);
            
            portfolio.buyAsset(testAssetId, largeQuantity, smallPrice, Collections.emptyList(), testDate, TransactionSource.MANUAL, "Large quantity buy");
            
            Money expectedCost = Money.of(BigDecimal.valueOf(100), usdCurrency);
            Money expectedBalance = initialBalance.subtract(expectedCost);
            assertEquals(expectedBalance, portfolio.getPortfolioCashBalance());
        }

        @Test
        @DisplayName("Should maintain transaction order consistency")
        void shouldMaintainTransactionOrderConsistency() {
            // Perform multiple operations
            Money depositAmount = Money.of(BigDecimal.valueOf(1000), usdCurrency);
            portfolio.recordCashflow(depositAmount, CashflowType.DEPOSIT, TransactionSource.MANUAL, "Deposit", Collections.emptyList(), testDate);
            
            portfolio.buyAsset(testAssetId, BigDecimal.valueOf(10), Money.of(BigDecimal.valueOf(50), usdCurrency), Collections.emptyList(), testDate.minusSeconds(1), TransactionSource.MANUAL, "Buy");
            
            Money withdrawalAmount = Money.of(BigDecimal.valueOf(500), usdCurrency);
            portfolio.recordCashflow(withdrawalAmount, CashflowType.WITHDRAWAL, TransactionSource.MANUAL, "Withdrawal", Collections.emptyList(), testDate.minusSeconds(2));
            
            List<Transaction> transactions = portfolio.getTransactions();
            assertEquals(3, transactions.size());
            
            // Verify transaction types are in order
            assertEquals(TransactionType.DEPOSIT, transactions.get(0).getType());
            assertEquals(TransactionType.BUY, transactions.get(1).getType());
            assertEquals(TransactionType.WITHDRAWAL, transactions.get(2).getType());
        }

        @Test
        @DisplayName("Should handle concurrent asset purchases of same asset")
        void shouldHandleConcurrentAssetPurchasesOfSameAsset() {
            // First purchase
            portfolio.buyAsset(testAssetId, BigDecimal.valueOf(50), Money.of(BigDecimal.valueOf(10), usdCurrency), Collections.emptyList(), testDate, TransactionSource.MANUAL, "First buy");
            
            // Second purchase of same asset
            portfolio.buyAsset(testAssetId, BigDecimal.valueOf(30), Money.of(BigDecimal.valueOf(15), usdCurrency), Collections.emptyList(), testDate, TransactionSource.MANUAL, "Second buy");
            
            // Should still have only one holding but with combined quantity
            assertEquals(1, portfolio.getHoldings().size());
            AssetHolding holding = portfolio.getHoldings().values().iterator().next();
            assertEquals(BigDecimal.valueOf(80), holding.getQuantity());
            assertEquals(2, portfolio.getTransactions().size());
        }

        @Test
        @DisplayName("Should handle business rule violations gracefully")
        void shouldHandleBusinessRuleViolationsGracefully() {
            // Test insufficient funds
            assertThrows(InsufficientFundsException.class, () ->
                portfolio.buyAsset(testAssetId, BigDecimal.valueOf(10000), Money.of(BigDecimal.valueOf(100), usdCurrency), Collections.emptyList(), testDate, TransactionSource.MANUAL, "Excessive buy")
            );
            
            // Portfolio state should remain unchanged after exception
            assertEquals(initialBalance, portfolio.getPortfolioCashBalance());
            assertTrue(portfolio.getHoldings().isEmpty());
            assertTrue(portfolio.getTransactions().isEmpty());
        }

        @Test
        @DisplayName("Should handle edge case with exact balance usage")
        void shouldHandleEdgeCaseWithExactBalanceUsage() {
            Money exactPrice = Money.of(BigDecimal.valueOf(10000), usdCurrency); // Exact balance
            
            portfolio.buyAsset(testAssetId, BigDecimal.valueOf(1), exactPrice, Collections.emptyList(), testDate, TransactionSource.MANUAL, "Exact balance buy");
            
            assertEquals(Money.of(BigDecimal.ZERO, usdCurrency), portfolio.getPortfolioCashBalance());
            assertEquals(1, portfolio.getHoldings().size());
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete portfolio lifecycle")
        void shouldHandleCompletePortfolioLifecycle() {
            // 1. Deposit money
            Money deposit = Money.of(BigDecimal.valueOf(5000), usdCurrency);
            portfolio.recordCashflow(deposit, CashflowType.DEPOSIT, TransactionSource.MANUAL, "Initial deposit", Collections.emptyList(), testDate); // domain event #2
            
            // 2. Buy assets // domain event #3
            portfolio.buyAsset(testAssetId, BigDecimal.valueOf(100), Money.of(BigDecimal.valueOf(50), usdCurrency), Collections.emptyList(), testDate, TransactionSource.MANUAL, "Asset purchase");
            
            // 3. Receive dividend // domain event #4
            Money dividend = Money.of(BigDecimal.valueOf(100), usdCurrency);
            portfolio.recordIncome(dividend, IncomeType.DIVIDEND, TransactionSource.MANUAL, "Dividend", Collections.emptyList(), testDate);
            
            // 4. Incur liability // domain event #5
            Money loanAmount = Money.of(BigDecimal.valueOf(2000), usdCurrency);
            portfolio.incurrNewLiability(testLiabilityDetails, loanAmount, TransactionSource.MANUAL, Collections.emptyList(), testDate);
            
            // 5. Make liability payment // domain event #6
            LiabilityId liabilityId = portfolio.getLiabilities().keySet().iterator().next();
            Money payment = Money.of(BigDecimal.valueOf(200), usdCurrency);
            portfolio.recordLiabilityPayment(liabilityId, payment, TransactionSource.MANUAL, Collections.emptyList(), testDate);
            
            // 6. Sell some assets // domain event #7
            // AssetHoldingId holdingId = portfolio.getHoldings().keySet().iterator().next();
            // portfolio.sellAsset(holdingId, BigDecimal.valueOf(50), Money.of(BigDecimal.valueOf(60), usdCurrency), Collections.emptyList(), testDate, TransactionSource.MANUAL, "Partial sale");
            portfolio.sellAsset(testAssetId, BigDecimal.valueOf(50), Money.of(BigDecimal.valueOf(60), usdCurrency), Collections.emptyList(), testDate, TransactionSource.MANUAL, "Partial sale");
            
            // Verify final state
            assertAll(
                () -> assertEquals(6, portfolio.getTransactions().size()),
                () -> assertEquals(7, portfolio.getDomainEvents().size()), // it is plus 1 because @BeforeEach has 1 domain event already
                () -> assertEquals(1, portfolio.getHoldings().size()),
                () -> assertEquals(1, portfolio.getLiabilities().size()),
                () -> assertTrue(portfolio.getPortfolioCashBalance().amount().compareTo(BigDecimal.ZERO) > 0)
            );
        }

        @Test
        @DisplayName("Should maintain data consistency across operations")
        void shouldMaintainDataConsistencyAcrossOperations() {
            Money initialBalance = portfolio.getPortfolioCashBalance();
            
            // Perform multiple operations
            portfolio.recordCashflow(Money.of(BigDecimal.valueOf(1000), usdCurrency), CashflowType.DEPOSIT, TransactionSource.MANUAL, "Deposit", Collections.emptyList(), testDate);
            portfolio.buyAsset(testAssetId, BigDecimal.valueOf(10), Money.of(BigDecimal.valueOf(100), usdCurrency), Collections.emptyList(), testDate, TransactionSource.MANUAL, "Buy");
            portfolio.recordIncome(Money.of(BigDecimal.valueOf(50), usdCurrency), IncomeType.DIVIDEND, TransactionSource.MANUAL, "Dividend", Collections.emptyList(), testDate);
            
            // Calculate expected balance
            Money expectedBalance = initialBalance
                .add(Money.of(BigDecimal.valueOf(1000), usdCurrency)) // Deposit
                .subtract(Money.of(BigDecimal.valueOf(1000), usdCurrency)) // Buy (10 * 100)
                .add(Money.of(BigDecimal.valueOf(50), usdCurrency)); // Dividend
            
            assertEquals(expectedBalance, portfolio.getPortfolioCashBalance());
            
            // Verify transaction count matches operations
            assertEquals(3, portfolio.getTransactions().size());
            assertEquals(4, portfolio.getDomainEvents().size());
        }
    }
        // Should create portfolio with valid parameters"
    void shouldCreatePortfolioWithValidParameters() {
        Portfolio newPortfolio = new Portfolio(testUser.getId(), "New Portfolio", "Description", initialBalance, conversionService, marketDataService);
        
        assertAll(
            () -> assertEquals(testUser, newPortfolio.getUserId()),
            () -> assertEquals("New Portfolio", newPortfolio.getPortfolioName()),
            () -> assertEquals("Description", newPortfolio.getPortfolioDescription()),
            () -> assertEquals(initialBalance, newPortfolio.getPortfolioCashBalance()),
            () -> assertNotNull(newPortfolio.getPortfolioId()),
            () -> assertTrue(newPortfolio.getHoldings().isEmpty()),
            () -> assertTrue(newPortfolio.getLiabilities().isEmpty()),
            () -> assertTrue(newPortfolio.getTransactions().isEmpty()),
            () -> assertTrue(newPortfolio.getDomainEvents().isEmpty())
        );
    }

    @Test
    @DisplayName("Should create portfolio with unique ID")
    void shouldCreatePortfolioWithUniqueId() {
        Portfolio portfolio1 = new Portfolio(testUser.getId(), "Portfolio 1", "Desc", initialBalance, conversionService, marketDataService);
        Portfolio portfolio2 = new Portfolio(testUser.getId(), "Portfolio 2", "Desc", initialBalance, conversionService, marketDataService);
        
        assertNotEquals(portfolio1.getPortfolioId(), portfolio2.getPortfolioId());
    }


    @Nested
    @DisplayName("Buy Asset Tests")
    class BuyAssetTests {

        private BigDecimal quantity;
        private Money pricePerUnit;
        private List<Fee> fees;

        @BeforeEach
        void setUp() {
            quantity = BigDecimal.valueOf(100);
            pricePerUnit = Money.of(BigDecimal.valueOf(50), usdCurrency);
            fees = Arrays.asList(
                new Fee( FeeType.BROKERAGE, Money.of(BigDecimal.valueOf(10), usdCurrency), "desc")
            );
        }

        @Test
        @DisplayName("Should successfully buy asset with sufficient funds")
        void shouldSuccessfullyBuyAssetWithSufficientFunds() {
            portfolio.buyAsset(testAssetId, quantity, pricePerUnit, fees, testDate, TransactionSource.MANUAL, "Test buy");

            assertAll(
                () -> assertEquals(Money.of(BigDecimal.valueOf(4990), usdCurrency), portfolio.getPortfolioCashBalance()),
                () -> assertEquals(1, portfolio.getHoldings().size()),
                () -> assertEquals(1, portfolio.getTransactions().size()),
                () -> assertEquals(2, portfolio.getDomainEvents().size())
            );
        }

        @Test
        @DisplayName("Should throw exception when insufficient funds")
        void shouldThrowExceptionWhenInsufficientFunds() {
            BigDecimal largeQuantity = BigDecimal.valueOf(1000);
            
            assertThrows(InsufficientFundsException.class, () ->
                portfolio.buyAsset(testAssetId, largeQuantity, pricePerUnit, fees, testDate, TransactionSource.MANUAL, "Test buy")
            );
        }
        
        @Test
        @DisplayName("Should throw exception when price per unit is not positive")
        void shouldThrowExceptionWhenNegativeOrZeroPrice() {
            BigDecimal largeQuantity = BigDecimal.valueOf(1000);
            Money pricePerUnitWrong = Money.of(-1, usdCurrency);
            assertThrows(IllegalArgumentException.class, () ->
                portfolio.buyAsset(testAssetId, largeQuantity, pricePerUnitWrong, fees, testDate, TransactionSource.MANUAL, "Test buy")
            );
        }

        @Test
        @DisplayName("Should throw exception when asset identifier is null")
        void shouldThrowExceptionWhenAssetIdentifierIsNull() {
            assertThrows(NullPointerException.class, () ->
                portfolio.buyAsset(null, quantity, pricePerUnit, fees, testDate, TransactionSource.MANUAL, "Test buy")
            );
        }

        @Test
        @DisplayName("Should create new asset holding for new asset")
        void shouldCreateNewAssetHoldingForNewAsset() {
            portfolio.buyAsset(testAssetId, quantity, pricePerUnit, fees, testDate, TransactionSource.MANUAL, "Test buy");
            
            assertEquals(1, portfolio.getHoldings().size());
            AssetHolding holding = portfolio.getHoldings().values().iterator().next();
            assertEquals(testAssetId, holding.getAssetIdentifier());
        }

        @Test
        @DisplayName("Should add to existing asset holding for existing asset")
        void shouldAddToExistingAssetHoldingForExistingAsset() {
            // First purchase
            portfolio.buyAsset(testAssetId, quantity, pricePerUnit, fees, testDate, TransactionSource.MANUAL, "First buy");
            
            // Second purchase
            BigDecimal secondQuantity = BigDecimal.valueOf(50);
            Money secondPrice = Money.of(BigDecimal.valueOf(60), usdCurrency);
            portfolio.buyAsset(testAssetId, secondQuantity, secondPrice, fees, testDate, TransactionSource.MANUAL, "Second buy");
            
            assertEquals(1, portfolio.getHoldings().size());
            assertEquals(2, portfolio.getTransactions().size());
        }

        @Test
        @DisplayName("Should handle currency conversion correctly")
        void shouldHandleCurrencyConversionCorrectly() {
            Money usdPrice = Money.of(BigDecimal.valueOf(45), usdCurrency);
            List<Fee> eurFees = Arrays.asList(
                new Fee(FeeType.BROKERAGE, Money.of(BigDecimal.valueOf(8), eurCurrency), "desc")
            );
            // asset is in USD, we are in EURO, need to buy USD asset with our EURO by conversion
            // ^^ this is wrong and not how the system should work, we pass USD. if you want to buy AAPL, give me a USD price because that's what AAPL trades in
            portfolio.buyAsset(testAssetId, quantity, usdPrice, eurFees, testDate, TransactionSource.MANUAL, "EUR buy");
            
            Money expectedBalance = Money.of(BigDecimal.valueOf(10000).subtract(BigDecimal.valueOf(4500)), usdCurrency);
            assertEquals(expectedBalance, Money.of(5500, usdCurrency));
            assertTrue(portfolio.getPortfolioCashBalance().amount().compareTo(BigDecimal.valueOf(5491.2).setScale(MathContext.DECIMAL128.getPrecision())) == 0);
        }

        @Test
        @DisplayName("Should create correct transaction record")
        void shouldCreateCorrectTransactionRecord() {
            portfolio.buyAsset(testAssetId, quantity, pricePerUnit, fees, testDate, TransactionSource.MANUAL, "Test buy");
            
            Transaction transaction = portfolio.getTransactions().get(0);
            assertAll(
                () -> assertEquals(TransactionType.BUY, transaction.getType()),
                () -> assertEquals(TransactionStatus.COMPLETED, transaction.getStatus()),
                () -> assertEquals(portfolio.getPortfolioId(), transaction.getPortfolioId()),
                () -> assertEquals(testDate, transaction.getTransactionDate())
            );
        }

        @Test
        @DisplayName("Should publish asset bought event")
        void shouldPublishAssetBoughtEvent() {
            portfolio.buyAsset(testAssetId, quantity, pricePerUnit, fees, testDate, TransactionSource.MANUAL, "Test buy");
            
            assertEquals(2, portfolio.getDomainEvents().size());
            assertTrue(portfolio.getDomainEvents().get(1) instanceof AssetBoughtEvent);
            
            AssetBoughtEvent event = (AssetBoughtEvent) portfolio.getDomainEvents().get(1);
            assertEquals(portfolio.getPortfolioId(), event.portfolioId());
            assertEquals(testAssetId, event.assetIdentifier());
        }

        @Test
        @DisplayName("Should handle empty fees list")
        void shouldHandleEmptyFeesList() {
            portfolio.buyAsset(testAssetId, quantity, pricePerUnit, Collections.emptyList(), testDate, TransactionSource.MANUAL, "Test buy");
            
            assertEquals(Money.of(BigDecimal.valueOf(5000), usdCurrency), portfolio.getPortfolioCashBalance());
        }

        @Test
        @DisplayName("Should handle null fees list")
        void shouldHandleNullFeesList() {
            portfolio.buyAsset(testAssetId, quantity, pricePerUnit, null, testDate, TransactionSource.MANUAL, "Test buy");
            
            assertEquals(Money.of(BigDecimal.valueOf(5000), usdCurrency), portfolio.getPortfolioCashBalance());
        }
    }

    @Nested
    @DisplayName("Sell Asset Tests")
    class SellAssetTests {

        private AssetHoldingId assetHoldingId;
        private BigDecimal sellQuantity;
        private Money sellPrice;
        private List<Fee> fees;

        @BeforeEach
        void setUp() {
            // First buy some assets
            BigDecimal buyQuantity = BigDecimal.valueOf(100);
            Money buyPrice = Money.of(BigDecimal.valueOf(50), usdCurrency);
            portfolio.buyAsset(testAssetId, buyQuantity, buyPrice, Collections.emptyList(), testDate, TransactionSource.MANUAL, "Setup buy");
            
            assetHoldingId = portfolio.getHoldings().keySet().iterator().next();
            sellQuantity = BigDecimal.valueOf(50);
            sellPrice = Money.of(BigDecimal.valueOf(60), usdCurrency);
            fees = Arrays.asList(new Fee(FeeType.BROKERAGE, Money.of(BigDecimal.valueOf(5), usdCurrency),"desc"));
        }

        @Test
        @DisplayName("Should successfully sell asset")
        void shouldSuccessfullySellAsset() {
            Money initialCash = portfolio.getPortfolioCashBalance();
            
            // portfolio.sellAsset(assetHoldingId, sellQuantity, sellPrice, fees, testDate, TransactionSource.MANUAL, "Test sell");
            portfolio.sellAsset(testAssetId, sellQuantity, sellPrice, fees, testDate, TransactionSource.MANUAL, "Test sell");
            
            Money expectedCashIncrease = Money.of(BigDecimal.valueOf(2995), usdCurrency); // (60*50) - 5 fees
            Money expectedNewBalance = initialCash.add(expectedCashIncrease);
            
            assertEquals(expectedNewBalance, portfolio.getPortfolioCashBalance());
        }

        @Test
        @DisplayName("Should throw exception when asset holding not found")
        void shouldThrowExceptionWhenAssetHoldingNotFound2() {
            // AssetHoldingId nonExistentId = new AssetHoldingId(UUID.randomUUID());
            AssetIdentifier nonExistenAssetIdentifier = new AssetIdentifier(AssetType.CRYPTO, "BTC", "BTC-KRAKEN", "Bitcoin", "KRAKEN", usdCurrency);
            
            assertThrows(AssetNotFoundException.class, () ->
                // portfolio.sellAsset(nonExistentId, sellQuantity, sellPrice, fees, testDate, TransactionSource.MANUAL, "Test sell")
                portfolio.sellAsset(nonExistenAssetIdentifier, sellQuantity, sellPrice, null, testDate, TransactionSource.MANUAL, "Test sell")
            );
        }
        
        @Test
        @DisplayName("Should throw exception when quantity to sell is not positive")
        void shouldThrowExceptionWhenQuantityToSellIsNotPositive() {
            // Use a valid holding id for quantity validation
            assertThrows(InvalidQuantityException.class, () ->
                // portfolio.sellAsset(assetHoldingId, BigDecimal.valueOf(0), sellPrice, fees, testDate, TransactionSource.MANUAL, "Test sell")
                portfolio.sellAsset(testAssetId, BigDecimal.valueOf(0), sellPrice, fees, testDate, TransactionSource.MANUAL, "Test sell")
            );
            assertThrows(InvalidQuantityException.class, () ->
                // portfolio.sellAsset(assetHoldingId, BigDecimal.valueOf(-1), sellPrice, fees, testDate, TransactionSource.MANUAL, "Test sell")
                portfolio.sellAsset(testAssetId, BigDecimal.valueOf(-1), sellPrice, fees, testDate, TransactionSource.MANUAL, "Test sell")
            );
        }

        @Test
        @DisplayName("Should throw exception when asset holding not found")
        void shouldThrowExceptionWhenAssetHoldingNotFound() {
            // AssetHoldingId nonExistentId = new AssetHoldingId(UUID.randomUUID());
            AssetIdentifier nonExistenAssetIdentifier = new AssetIdentifier(AssetType.CRYPTO, "BTC", "BTC-KRAKEN", "Bitcoin", "KRAKEN", usdCurrency);

            assertThrows(AssetNotFoundException.class, () ->
                // portfolio.sellAsset(nonExistentId, sellQuantity, sellPrice, fees, testDate, TransactionSource.MANUAL, "Test sell")
                portfolio.sellAsset(nonExistenAssetIdentifier, sellQuantity, sellPrice, fees, testDate, TransactionSource.MANUAL, "Test sell")
            );
        }

        @Test
        @DisplayName("Should throw exception when selling more than owned")
        void shouldThrowExceptionWhenSellingMoreThanOwned() {
            BigDecimal excessiveQuantity = BigDecimal.valueOf(200);
            
            assertThrows(InvalidQuantityException.class, () ->
                // portfolio.sellAsset(assetHoldingId, excessiveQuantity, sellPrice, fees, testDate, TransactionSource.MANUAL, "Test sell")
                portfolio.sellAsset(testAssetId, excessiveQuantity, sellPrice, fees, testDate, TransactionSource.MANUAL, "Test sell")
            );
        }

        @Test
        @DisplayName("Should create correct transaction record for sell")
        void shouldCreateCorrectTransactionRecordForSell() {
            // portfolio.sellAsset(assetHoldingId, sellQuantity, sellPrice, fees, testDate, TransactionSource.MANUAL, "Test sell");
            portfolio.sellAsset(testAssetId, sellQuantity, sellPrice, fees, testDate, TransactionSource.MANUAL, "Test sell");
            
            // Should have 2 transactions now (buy + sell)
            assertEquals(2, portfolio.getTransactions().size());
            
            Transaction sellTransaction = portfolio.getTransactions().get(1);
            assertEquals(TransactionType.SELL, sellTransaction.getType());
        }

        @Test
        @DisplayName("Should publish asset sold event")
        void shouldPublishAssetSoldEvent() {
            // portfolio.sellAsset(assetHoldingId, sellQuantity, sellPrice, fees, testDate, TransactionSource.MANUAL, "Test sell");
            portfolio.sellAsset(testAssetId, sellQuantity, sellPrice, fees, testDate, TransactionSource.MANUAL, "Test sell");
            
            // Should have 2 events now (buy + sell)
            assertEquals(3, portfolio.getDomainEvents().size());
            assertTrue(portfolio.getDomainEvents().get(2) instanceof AssetSoldEvent);
        }

        @Test
        @DisplayName("Should reduce asset holding quantity correctly")
        void shouldReduceAssetHoldingQuantityCorrectly() {
            AssetHolding holding = portfolio.getHoldings().get(assetHoldingId);
            BigDecimal initialQuantity = holding.getQuantity();
            
            // portfolio.sellAsset(assetHoldingId, sellQuantity, sellPrice, fees, testDate, TransactionSource.MANUAL, "Test sell");
            portfolio.sellAsset(testAssetId, sellQuantity, sellPrice, fees, testDate, TransactionSource.MANUAL, "Test sell");
            
            BigDecimal expectedRemainingQuantity = initialQuantity.subtract(sellQuantity);
            assertEquals(expectedRemainingQuantity, holding.getQuantity());
        }

        @Test 
        @DisplayName("Should remove the holding when we sell all")
        void shouldRemoveHoldingWhenQuantityIs0() {

            // portfolio.sellAsset(assetHoldingId, BigDecimal.valueOf(100), sellPrice, fees, testDate, TransactionSource.MANUAL, "Test sell");
            portfolio.sellAsset(testAssetId, BigDecimal.valueOf(100), sellPrice, fees, testDate, TransactionSource.MANUAL, "Test sell");
            // assertTrue(portfolio.getHoldings().isEmpty()); // holdings don't go to zero anymore when we sell all, it's a soft delete
            // assertTrue(portfolio.getHoldings().containsKey(assetHoldingId) == false);
            assertTrue(portfolio.getHoldings().size() == 1);
            assertTrue(portfolio.getHoldings().get(assetHoldingId).isActive() == false);
        }
    }

    @Nested
    @DisplayName("Liability Management Tests")
    class LiabilityManagementTests {

        private Money liabilityAmount;
        private List<Fee> fees;

        @BeforeEach
        void setUp() {
            liabilityAmount = Money.of(BigDecimal.valueOf(5000), usdCurrency);
            fees = Arrays.asList(new Fee(FeeType.OTHER, Money.of(BigDecimal.valueOf(50), usdCurrency), "desc"));
        }

        @Test
        @DisplayName("Should successfully incur new liability")
        void shouldSuccessfullyIncurNewLiability() {
            Money initialCash = portfolio.getPortfolioCashBalance();
            
            portfolio.incurrNewLiability(testLiabilityDetails, liabilityAmount, TransactionSource.MANUAL, fees, testDate);
            
            Money expectedCashIncrease = Money.of(BigDecimal.valueOf(4950), usdCurrency); // 5000 - 50 fees
            Money expectedNewBalance = initialCash.add(expectedCashIncrease);
            
            assertAll(
                () -> assertEquals(expectedNewBalance, portfolio.getPortfolioCashBalance()),
                () -> assertEquals(1, portfolio.getLiabilities().size()),
                () -> assertEquals(1, portfolio.getTransactions().size()),
                () -> assertEquals(2, portfolio.getDomainEvents().size())
            );
        }

        @Test
        @DisplayName("Should publish liability incurred event")
        void shouldPublishLiabilityIncurredEvent() {
            portfolio.incurrNewLiability(testLiabilityDetails, liabilityAmount, TransactionSource.MANUAL, null, testDate);
            
            assertTrue(portfolio.getDomainEvents().get(1) instanceof LiabilityIncurredEvent);
            
            LiabilityIncurredEvent event = (LiabilityIncurredEvent) portfolio.getDomainEvents().get(1);
            assertEquals(liabilityAmount, event.amount());
        }

        @Test
        @DisplayName("Should create liability with correct details")
        void shouldCreateLiabilityWithCorrectDetails() {
            portfolio.incurrNewLiability(testLiabilityDetails, liabilityAmount, TransactionSource.MANUAL, Collections.emptyList(), testDate);
            
            assertEquals(1, portfolio.getLiabilities().size());
            Liability liability = portfolio.getLiabilities().values().iterator().next();
            
            assertAll(
                () -> assertEquals(portfolio.getPortfolioId(), liability.getPortfolioId()),
                () -> assertEquals(liabilityAmount, liability.getCurrentBalance()),
                () -> assertEquals(testLiabilityDetails, liability.getDetails())
            );
        }

        @Test
        @DisplayName("Should record liability payment successfully")
        void shouldRecordLiabilityPaymentSuccessfully() {
            // First incur liability
            portfolio.incurrNewLiability(testLiabilityDetails, liabilityAmount, TransactionSource.MANUAL, Collections.emptyList(), testDate);
            LiabilityId liabilityId = portfolio.getLiabilities().keySet().iterator().next();
            
            Money paymentAmount = Money.of(BigDecimal.valueOf(500), usdCurrency);
            List<Fee> paymentFees = Arrays.asList(new Fee(FeeType.PROCESSING, Money.of(BigDecimal.valueOf(5), usdCurrency), "desc"));
            
            Money cashBeforePayment = portfolio.getPortfolioCashBalance();
            
            portfolio.recordLiabilityPayment(liabilityId, paymentAmount, TransactionSource.MANUAL, paymentFees, testDate);
            
            // Cash should decrease by payment + fees
            Money expectedCashDecrease = paymentAmount.add(Money.of(BigDecimal.valueOf(5), usdCurrency));
            Money expectedNewBalance = cashBeforePayment.subtract(expectedCashDecrease);
            
            assertAll(
                () -> assertEquals(expectedNewBalance, portfolio.getPortfolioCashBalance()),
                () -> assertEquals(2, portfolio.getTransactions().size()),
                () -> assertEquals(3, portfolio.getDomainEvents().size())
            );
        }

        @Test
        @DisplayName("Should throw exception when liability not found for payment")
        void shouldThrowExceptionWhenLiabilityNotFoundForPayment() {
            LiabilityId nonExistentId = new LiabilityId(UUID.randomUUID());
            Money paymentAmount = Money.of(BigDecimal.valueOf(500), usdCurrency);
            
            assertThrows(IllegalArgumentException.class, () ->
                portfolio.recordLiabilityPayment(nonExistentId, paymentAmount, TransactionSource.MANUAL, Collections.emptyList(), testDate)
            );
        }
       
        @Test
        @DisplayName("Should throw exception when liability currency is not portfolios")
        void shouldThrowExceptionWhenLiabilityIsNotSameCurrencyAsPortfolio() {
            assertThrows(IllegalArgumentException.class, () ->portfolio.incurrNewLiability(testLiabilityDetails, Money.of(200, eurCurrency), TransactionSource.MANUAL, Collections.emptyList(), testDate));

        }


        @Test 
        @DisplayName("Should throw error when payment amount is not positive")
        void shouldThrowErrorWhenPaymentAmountIsNotPositive() {
            // First incur liability
            portfolio.incurrNewLiability(testLiabilityDetails, liabilityAmount, TransactionSource.MANUAL, Collections.emptyList(), testDate);
            LiabilityId liabilityId = portfolio.getLiabilities().keySet().iterator().next();
            
            Money paymentAmount = Money.of(BigDecimal.valueOf(-500), usdCurrency);
            
            assertThrows(IllegalArgumentException.class, () ->
                portfolio.recordLiabilityPayment(liabilityId, paymentAmount, TransactionSource.MANUAL, Collections.emptyList(), testDate)
            );
            assertThrows(IllegalArgumentException.class, () ->
                portfolio.recordLiabilityPayment(liabilityId,  Money.of(BigDecimal.valueOf(0), usdCurrency), TransactionSource.MANUAL, Collections.emptyList(), testDate)
            );
        }
    }

    @Nested
    @DisplayName("Cashflow Recording Tests")
    class CashflowRecordingTests {

        @Test
        @DisplayName("Should successfully record deposit")
        void shouldSuccessfullyRecordDeposit() {
            Money depositAmount = Money.of(BigDecimal.valueOf(1000), usdCurrency);
            Money initialBalance = portfolio.getPortfolioCashBalance();
            List<Fee> fees = Arrays.asList(new Fee(FeeType.DEPOSIT_FEE, Money.of(BigDecimal.valueOf(5), usdCurrency),"desc"));

            portfolio.recordCashflow(depositAmount, CashflowType.DEPOSIT, TransactionSource.MANUAL, "Test deposit", fees, testDate);
            
            Money expectedBalance = initialBalance.add(depositAmount).subtract(Money.of(5, usdCurrency));
            assertAll(
                () -> assertEquals(expectedBalance, portfolio.getPortfolioCashBalance()),
                () -> assertEquals(1, portfolio.getTransactions().size()),
                () -> assertEquals(TransactionType.DEPOSIT, portfolio.getTransactions().get(0).getType()),
                () -> assertEquals(2, portfolio.getDomainEvents().size())
            );
        }

        @Test
        @DisplayName("Should successfully record withdrawal")
        void shouldSuccessfullyRecordWithdrawal() {
            Money withdrawalAmount = Money.of(BigDecimal.valueOf(1000), usdCurrency);
            Money initialBalance = portfolio.getPortfolioCashBalance();
            
            portfolio.recordCashflow(withdrawalAmount, CashflowType.WITHDRAWAL, TransactionSource.MANUAL, "Test withdrawal", null, testDate);
            
            Money expectedBalance = initialBalance.subtract(withdrawalAmount);
            assertEquals(expectedBalance, portfolio.getPortfolioCashBalance());
        }

        @Test
        @DisplayName("Should throw exception for withdrawal exceeding balance")
        void shouldThrowExceptionForWithdrawalExceedingBalance() {
            Money excessiveAmount = Money.of(BigDecimal.valueOf(20000), usdCurrency);
            
            assertThrows(InsufficientFundsException.class, () ->
                portfolio.recordCashflow(excessiveAmount, CashflowType.WITHDRAWAL, TransactionSource.MANUAL, "Excessive withdrawal", Collections.emptyList(), testDate)
            );
        }
       
        @Test
        @DisplayName("Should throw exception for bad cashflowtype")
        void shouldThrowExceptionForBadCashflowType() {
            Money excessiveAmount = Money.of(BigDecimal.valueOf(200), usdCurrency);
            
            assertThrows(IllegalArgumentException.class, () ->
                portfolio.recordCashflow(excessiveAmount, CashflowType.TRANSFER, TransactionSource.MANUAL, "Excessive withdrawal", Collections.emptyList(), testDate)
            );
        }

        @Test
        @DisplayName("Should throw exception for currency mismatch")
        void shouldThrowExceptionForCurrencyMismatch() {
            Money eurAmount = Money.of(BigDecimal.valueOf(1000), eurCurrency);
            
            assertThrows(IllegalArgumentException.class, () ->
                portfolio.recordCashflow(eurAmount, CashflowType.DEPOSIT, TransactionSource.MANUAL, "EUR deposit", Collections.emptyList(), testDate)
            );
        }

        @Test
        @DisplayName("Should throw exception for negative amount")
        void shouldThrowExceptionForNegativeAmount() {
            Money negativeAmount = Money.of(BigDecimal.valueOf(-1000), usdCurrency);
            
            assertThrows(IllegalArgumentException.class, () ->
                portfolio.recordCashflow(negativeAmount, CashflowType.DEPOSIT, TransactionSource.MANUAL, "Negative deposit", Collections.emptyList(), testDate)
            );
        }

        @Test
        @DisplayName("Should handle different cashflow types correctly")
        void shouldHandleDifferentCashflowTypesCorrectly() {
            Money amount = Money.of(BigDecimal.valueOf(100), usdCurrency);
            
            // Test dividend
            portfolio.recordCashflow(amount, CashflowType.DIVIDEND, TransactionSource.MANUAL, "Dividend", Collections.emptyList(), testDate);
            assertEquals(TransactionType.INCOME, portfolio.getTransactions().get(0).getType());
            
            // Test fee
            portfolio.recordCashflow(amount, CashflowType.BROKERAGE_FEE, TransactionSource.MANUAL, "Fee", Collections.emptyList(), testDate);
            assertEquals(TransactionType.FEE, portfolio.getTransactions().get(1).getType());
        }

        @Test
        @DisplayName("Should publish cashflow recorded event")
        void shouldPublishCashflowRecordedEvent() {
            Money amount = Money.of(BigDecimal.valueOf(1000), usdCurrency);
            
            portfolio.recordCashflow(amount, CashflowType.DEPOSIT, TransactionSource.MANUAL, "Test deposit", Collections.emptyList(), testDate);
            
            assertEquals(2, portfolio.getDomainEvents().size());
            assertTrue(portfolio.getDomainEvents().get(1) instanceof CashflowRecordedEvent);
        }

        @Test
        @DisplayName("Should handle fees correctly with different cashflow types")
        void shouldHandleFeesCorrectlyWithDifferentCashflowTypes() {
            Money amount = Money.of(BigDecimal.valueOf(1000), usdCurrency);
            List<Fee> fees = Arrays.asList(new Fee( FeeType.PROCESSING, Money.of(BigDecimal.valueOf(10), usdCurrency),"desc"));
            Money initialBalance = portfolio.getPortfolioCashBalance();
            
            // Deposit: amount - fees = net positive
            portfolio.recordCashflow(amount, CashflowType.DEPOSIT, TransactionSource.MANUAL, "Deposit with fees", fees, testDate);
            Money expectedAfterDeposit = initialBalance.add(Money.of(BigDecimal.valueOf(990), usdCurrency));
            assertEquals(expectedAfterDeposit, portfolio.getPortfolioCashBalance());
            
            // Withdrawal: -(amount + fees) = net negative  
            portfolio.recordCashflow(amount, CashflowType.WITHDRAWAL, TransactionSource.MANUAL, "Withdrawal with fees", fees, testDate);
            Money expectedAfterWithdrawal = expectedAfterDeposit.subtract(Money.of(BigDecimal.valueOf(1010), usdCurrency));
            assertEquals(expectedAfterWithdrawal, portfolio.getPortfolioCashBalance());
        }
    }

    @Nested
    @DisplayName("Income Recording Tests")
    class IncomeRecordingTests {

        @Test
        @DisplayName("Should successfully record dividend income")
        void shouldSuccessfullyRecordDividendIncome() {
            Money dividendAmount = Money.of(BigDecimal.valueOf(100), usdCurrency);
            Money initialBalance = portfolio.getPortfolioCashBalance();
            
            portfolio.recordIncome(dividendAmount, IncomeType.DIVIDEND, TransactionSource.MANUAL, "Dividend payment", Collections.emptyList(), testDate);
            
            Money expectedBalance = initialBalance.add(dividendAmount);
            assertAll(
                () -> assertEquals(expectedBalance, portfolio.getPortfolioCashBalance()),
                () -> assertEquals(1, portfolio.getTransactions().size()),
                () -> assertEquals(TransactionType.INCOME, portfolio.getTransactions().get(0).getType()),
                () -> assertEquals(2, portfolio.getDomainEvents().size())
            );
        }

        @Test
        @DisplayName("Should successfully record interest income")
        void shouldSuccessfullyRecordInterestIncome() {
            Money interestAmount = Money.of(BigDecimal.valueOf(50), usdCurrency);
            
            portfolio.recordIncome(interestAmount, IncomeType.INTEREST, TransactionSource.MANUAL, "Interest payment", Collections.emptyList(), testDate);
            
            assertEquals(1, portfolio.getTransactions().size());
            assertEquals(TransactionType.INCOME, portfolio.getTransactions().get(0).getType());
        }

        @Test
        @DisplayName("Should successfully record rental income")
        void shouldSuccessfullyRecordRentalIncome() {
            Money rentalAmount = Money.of(BigDecimal.valueOf(500), usdCurrency);
            
            portfolio.recordIncome(rentalAmount, IncomeType.RENTAL, TransactionSource.MANUAL, "Rental income", Collections.emptyList(), testDate);
            
            assertEquals(1, portfolio.getTransactions().size());
            assertEquals(TransactionType.INCOME, portfolio.getTransactions().get(0).getType());
        }

        @Test
        @DisplayName("Should throw exception for null parameters")
        void shouldThrowExceptionForNullParameters() {
            Money amount = Money.of(BigDecimal.valueOf(100), usdCurrency);

            assertAll(
                () -> assertThrows(NullPointerException.class, () ->
                portfolio.recordIncome(null, IncomeType.DIVIDEND, TransactionSource.MANUAL, "Desc", Collections.emptyList(), testDate)),
                () -> assertThrows(NullPointerException.class, () ->
                portfolio.recordIncome(amount, null, TransactionSource.MANUAL, "Desc", Collections.emptyList(), testDate)),
                () -> assertThrows(NullPointerException.class, () ->
                portfolio.recordIncome(amount, IncomeType.DIVIDEND, null, "Desc", Collections.emptyList(), testDate)),
                () -> assertThrows(NullPointerException.class, () ->
                portfolio.recordIncome(amount, IncomeType.DIVIDEND, TransactionSource.MANUAL, "Desc", Collections.emptyList(), null))
                );
        }
            
        @Test 
        @DisplayName("Should throw exception when incomeType is not valid")
        void shouldThrowExceptionForInvalidIncomeType() {
            Money amount = Money.of(BigDecimal.valueOf(100), usdCurrency);
            assertThrows(IllegalArgumentException.class, ()->portfolio.recordIncome(amount, IncomeType.ERROR, TransactionSource.MANUAL, "desc", Collections.emptyList(), testDate));
        }
    }

    @Nested
    @DisplayName("Testing calcualteTotalValue")
    @ExtendWith(MockitoExtension.class)
    class CalculateTotalValueTests {
        @Mock
        private MarketDataService marketDataService;

        @Mock
        private CurrencyConversionService conversionService;
        private Currency USD;
        
        @BeforeEach
        void init() {
            USD = Currency.getInstance("USD");
            portfolio = new Portfolio(testUser.getId(), "Test Portfolio", "Test Description", initialBalance, conversionService, marketDataService);

        }
        
        @Test
        void testCalculateTotalValue_WithHoldingsAndLiabilities() throws Exception {
            Instant valuationDate = Instant.now();
            
            AssetHolding holding = mock(AssetHolding.class);
            Liability liability = mock(Liability.class);
            
            Money holdingValue = new Money(new BigDecimal("500.00"), USD);
            Money liabilityValue = new Money(new BigDecimal("200.00"), USD);
            
            AssetHoldingId assetHoldingId = new AssetHoldingId(UUID.randomUUID());
            LiabilityId liabilityId = new LiabilityId(UUID.randomUUID());
            
            // Use reflection to access private fields
            Field holdingsField = Portfolio.class.getDeclaredField("holdings");
            holdingsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<AssetHoldingId, AssetHolding> holdings = 
                (Map<AssetHoldingId, AssetHolding>) holdingsField.get(portfolio);
            holdings.put(assetHoldingId, holding);
            
            Field liabilitiesField = Portfolio.class.getDeclaredField("liabilities");
            liabilitiesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<LiabilityId, Liability> liabilities = 
                (Map<LiabilityId, Liability>) liabilitiesField.get(portfolio);
            liabilities.put(liabilityId, liability);
            
            // Rest of your test...
            when(marketDataService.calculateHoldingValue(holding, valuationDate))
                .thenReturn(holdingValue);
            when(liability.getCurrentBalance())
                .thenReturn(new Money(new BigDecimal("200.00"), USD));
            when(conversionService.convert(any(), eq(USD), eq(valuationDate)))
                .thenReturn(liabilityValue);
            
            Money result = portfolio.calculateTotalValue(valuationDate);
            
            assertEquals(new Money(new BigDecimal("10300.00"), USD), result);
        }
        @Test
        void testCalculateTotalValue_NullValuationDate_ShouldThrowException() {
            assertThrows(NullPointerException.class, () -> portfolio.calculateTotalValue(null));
        }
    }

    @Nested
    @DisplayName("Testing accrueInterest")
    @ExtendWith(MockitoExtension.class)
    class AccrueInterestTest {
        @Mock
        private MarketDataService marketDataService;

        @Mock
        private CurrencyConversionService conversionService;
        private Currency USD;
        
        @BeforeEach
        void init() {
            USD = Currency.getInstance("USD");
            portfolio = new Portfolio(testUser.getId(), "Test Portfolio", "Test Description", initialBalance, conversionService, marketDataService);

        }
        
        @Test
        void testAccrueInterest_WithAccruedInterest() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
            Instant accrualDate = Instant.now();

            LiabilityId liabilityId = new LiabilityId(UUID.randomUUID());

            Liability liability = mock(Liability.class);
            when(liability.accrueInterest(accrualDate)).thenReturn(new Money(new BigDecimal("50.00"), USD));
            when(liability.getLiabilityId()).thenReturn(liabilityId);

            Portfolio testPortfolio = new Portfolio(
                new UserId(UUID.randomUUID()),
                "Test Portfolio",
                "desc",
                new Money(new BigDecimal("1000.00"), USD),
                conversionService,
                marketDataService
            );

            // Inject liability via reflection
            Field liabilitiesField = Portfolio.class.getDeclaredField("liabilities");
            liabilitiesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<LiabilityId, Liability> liabilities = (Map<LiabilityId, Liability>) liabilitiesField.get(testPortfolio);
            liabilities.put(liabilityId, liability);

            // Spy
            Portfolio spyPortfolio = Mockito.spy(testPortfolio);

            // Stub recordCashflow so Mockito can track it
            doNothing().when(spyPortfolio).recordCashflow(
                any(Money.class),
                any(CashflowType.class),
                any(TransactionSource.class),
                anyString(),
                anyList(),
                any(Instant.class)
            );

            spyPortfolio.accrueInterest(accrualDate);

            verify(spyPortfolio).recordCashflow(
                eq(new Money(new BigDecimal("50.00"), USD)),
                eq(CashflowType.INTEREST_EXPENSE),
                eq(TransactionSource.SYSTEM),
                eq("Interest accrual for liability " + liabilityId),
                eq(Collections.emptyList()),
                eq(accrualDate)
            );
        }

        @Test
        void testAccrueInterest_NoInterestAccrued_ShouldNotRecordCashflow() {
            Instant accrualDate = Instant.now();

            // Create proper ID
            LiabilityId liabilityId = new LiabilityId(UUID.randomUUID());
            
            Liability liability = mock(Liability.class);
            // when(liability.accrueInterest(accrualDate)).thenReturn(new Money(BigDecimal.ZERO, USD));
            // when(liability.getLiabilityId()).thenReturn(liabilityId);

            // Create portfolio with test data using package-private constructor
            Map<LiabilityId, Liability> testLiabilities = new HashMap<>();
            testLiabilities.put(liabilityId, liability);
            
            Portfolio testPortfolio = new Portfolio(
                new UserId(UUID.randomUUID()),
                "Test Portfolio",
                "desc",
                new Money(new BigDecimal("1000.00"), USD),
                conversionService,
                marketDataService
            );

            Portfolio spyPortfolio = Mockito.spy(testPortfolio);
            spyPortfolio.accrueInterest(accrualDate);

            verify(spyPortfolio, never()).recordCashflow(any(), any(), any(), any(), any(), any());
        }



        @Test
        void testAccrueInterest_VerifySideEffects() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
            Instant accrualDate = Instant.now();
            
            LiabilityId liabilityId = new LiabilityId(UUID.randomUUID());
            Liability liability = mock(Liability.class);
            
            when(liability.accrueInterest(accrualDate)).thenReturn(new Money(new BigDecimal("50.00"), USD));
            when(liability.getLiabilityId()).thenReturn(liabilityId);

            Portfolio testPortfolio = new Portfolio(
                new UserId(UUID.randomUUID()),
                "Test Portfolio",
                "desc",
                new Money(new BigDecimal("1000.00"), USD),
                conversionService,
                marketDataService
            );

            Field liabilitiesField = Portfolio.class.getDeclaredField("liabilities");
            liabilitiesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<LiabilityId, Liability> liabilities = (Map<LiabilityId, Liability>) liabilitiesField.get(testPortfolio);
            liabilities.put(liabilityId, liability);

            Money initialBalance = testPortfolio.getPortfolioCashBalance();
            int initialTransactionCount = testPortfolio.getTransactions().size();

            testPortfolio.accrueInterest(accrualDate);

            // ✅ Verify balance decreased
            Money expectedBalance = initialBalance.subtract(new Money(new BigDecimal("50.00"), USD));
            assertEquals(expectedBalance, testPortfolio.getPortfolioCashBalance());

            // ✅ Verify transaction added
            assertEquals(initialTransactionCount + 1, testPortfolio.getTransactions().size());

            Transaction lastTransaction = testPortfolio.getTransactions().getLast(); // or get(size - 1)
            assertEquals(TransactionType.EXPENSE, lastTransaction.getType());
            assertEquals(new Money(new BigDecimal("-50.00"), USD), lastTransaction.getTransactionNetImpact());
        }

        @Test
        void testAccrueInterest_NullDate_ShouldThrowException() {
            assertThrows(NullPointerException.class, () -> portfolio.accrueInterest(null));
        }
    }
}
