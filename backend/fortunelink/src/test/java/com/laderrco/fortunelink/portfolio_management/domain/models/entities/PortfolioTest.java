package com.laderrco.fortunelink.portfolio_management.domain.models.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CryptoIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;



@ExtendWith(MockitoExtension.class)
@DisplayName("Portfolio Tests")
class PortfolioTest {

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private ExchangeRateService exchangeRateService;

    private UserId userId;
    private ValidatedCurrency portfolioCurrency;
    private TransactionId transactionId1;
    private AssetId assetId1;


    @BeforeEach
    void setUp() {
        userId = UserId.randomId();
        portfolioCurrency = ValidatedCurrency.CAD;
        transactionId1 = TransactionId.randomId();
        assetId1 = AssetId.randomId();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create portfolio with valid parameters")
        void shouldCreatePortfolioWithValidParameters() {
            // When
            Portfolio portfolio = new Portfolio(userId, portfolioCurrency);

            // Then
            assertThat(portfolio).isNotNull();
            assertThat(portfolio.getUserId()).isEqualTo(userId);
            assertThat(portfolio.getPortfolioCurrency()).isEqualTo(portfolioCurrency);
            assertThat(portfolio.getAccounts()).isEmpty();
            assertThat(portfolio.getPortfolioId()).isNotNull();
            assertThat(portfolio.getSystemCreationDate()).isNotNull();
            assertThat(portfolio.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when userId is null")
        void shouldThrowExceptionWhenUserIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> new Portfolio(null, portfolioCurrency))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw exception when portfolioCurrency is null")
        void shouldThrowExceptionWhenPortfolioCurrencyIsNull() {
            // When & Then
            assertThatThrownBy(() -> new Portfolio(userId, null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Account Management Tests")
    class AccountManagementTests {

        private Portfolio portfolio;
        private Account account1;
        private Account account2;

        @BeforeEach
        void setUp() {
            portfolio = new Portfolio(userId, portfolioCurrency);
            account1 = Account.builder()
                .accountId(AccountId.randomId())
                .name("TFSA Account")
                .accountType(AccountType.TFSA)
                .baseCurrency(ValidatedCurrency.CAD)
                .assets(new ArrayList<>())
                .build();
            
            account2 = Account.builder()
                .accountId(AccountId.randomId())
                .name("RRSP Account")
                .accountType(AccountType.RRSP)
                .baseCurrency(ValidatedCurrency.CAD)
                .assets(new ArrayList<>())
                .build();
        }

        @Test
        @DisplayName("Should successfully add account to portfolio")
        void shouldAddAccountToPortfolio() {
            // When
            portfolio.addAccount(account1);

            // Then
            assertThat(portfolio.getAccounts()).hasSize(1);
            assertThat(portfolio.getAccounts()).contains(account1);
        }

        @Test
        @DisplayName("Should add multiple accounts to portfolio")
        void shouldAddMultipleAccountsToPortfolio() {
            // When
            portfolio.addAccount(account1);
            portfolio.addAccount(account2);

            // Then
            assertThat(portfolio.getAccounts()).hasSize(2);
            assertThat(portfolio.getAccounts()).containsExactly(account1, account2);
        }

        @Test
        @DisplayName("Should throw exception when adding null account")
        void shouldThrowExceptionWhenAddingNullAccount() {
            // When & Then
            assertThatThrownBy(() -> portfolio.addAccount(null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw exception when adding duplicate account ID")
        void shouldThrowExceptionWhenAddingDuplicateAccountId() {
            // Given
            portfolio.addAccount(account1);

            // When & Then
            assertThatThrownBy(() -> portfolio.addAccount(account1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should throw exception when adding account with duplicate name")
        void shouldThrowExceptionWhenAddingAccountWithDuplicateName() {
            // Given
            portfolio.addAccount(account1);
            Account duplicateNameAccount = Account.builder()
                .accountId(AccountId.randomId())
                .name("TFSA Account") // Same name as account1
                .accountType(AccountType.TFSA)
                .baseCurrency(ValidatedCurrency.CAD)
                .assets(new ArrayList<>())
                .build();

            // When & Then
            assertThatThrownBy(() -> portfolio.addAccount(duplicateNameAccount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should update metadata when adding account")
        void shouldUpdateMetadataWhenAddingAccount() throws InterruptedException {
            // Given
            Instant initialUpdatedAt = portfolio.getUpdatedAt();
            Thread.sleep(10); // Ensure time difference

            // When
            portfolio.addAccount(account1);

            // Then
            assertThat(portfolio.getUpdatedAt()).isAfter(initialUpdatedAt);
        }

        @Test
        @DisplayName("Should successfully remove empty account")
        void shouldRemoveEmptyAccount() throws AccountNotFoundException {
            // Given
            portfolio.addAccount(account1);

            // When
            portfolio.removeAccount(account1.getAccountId());

            // Then
            assertThat(portfolio.getAccounts()).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when removing account with assets")
        void shouldThrowExceptionWhenRemovingAccountWithAssets() {
            // Given
            Asset asset = createTestAsset();
            account1.getAssets().add(asset);
            portfolio.addAccount(account1);

            // When & Then
            assertThatThrownBy(() -> portfolio.removeAccount(account1.getAccountId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("still contains assets");
        }

        @Test
        @DisplayName("Should throw exception when removing non-existent account")
        void shouldThrowExceptionWhenRemovingNonExistentAccount() {
            // Given
            AccountId nonExistentId = AccountId.randomId();

            // When & Then
            assertThatThrownBy(() -> portfolio.removeAccount(nonExistentId))
                .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when removing account with null ID")
        void shouldThrowExceptionWhenRemovingAccountWithNullId() {
            // When & Then
            assertThatThrownBy(() -> portfolio.removeAccount(null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Transaction Recording Tests")
    class TransactionRecordingTests {

        private Portfolio portfolio;
        private Account account;
        private Transaction transaction;

        @BeforeEach
        void setUp() {
            portfolio = new Portfolio(userId, portfolioCurrency);
            account = createTestAccount("TFSA", AccountType.TFSA);
            portfolio.addAccount(account);
            transaction = createTestTransaction();
        }

        @Test
        @DisplayName("Should successfully record transaction")
        void shouldRecordTransaction() throws Exception {
            // When
            portfolio.recordTransaction(account.getAccountId(), transaction);

            // Then
            List<Transaction> transactions = portfolio.getTransactionsForAccount(account.getAccountId());
            assertThat(transactions).hasSize(1);
            assertThat(transactions).contains(transaction);
        }

        @Test
        @DisplayName("Should throw exception when recording transaction to non-existent account")
        void shouldThrowExceptionWhenRecordingToNonExistentAccount() {
            // Given
            AccountId nonExistentId = AccountId.randomId();

            // When & Then
            assertThatThrownBy(() -> portfolio.recordTransaction(nonExistentId, transaction))
                .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("Should update metadata when recording transaction")
        void shouldUpdateMetadataWhenRecordingTransaction() throws Exception {
            // Given
            Instant initialUpdatedAt = portfolio.getUpdatedAt();
            Thread.sleep(10);

            // When
            portfolio.recordTransaction(account.getAccountId(), transaction);

            // Then
            assertThat(portfolio.getUpdatedAt()).isAfter(initialUpdatedAt);
        }
    }

    @Nested
    @DisplayName("Get Account Tests")
    class GetAccountTests {

        private Portfolio portfolio;
        private Account account;

        @BeforeEach
        void setUp() {
            portfolio = new Portfolio(userId, portfolioCurrency);
            account = createTestAccount("TFSA", AccountType.TFSA);
            portfolio.addAccount(account);
        }

        @Test
        @DisplayName("Should successfully get existing account")
        void shouldGetExistingAccount() throws AccountNotFoundException {
            // When
            Account retrievedAccount = portfolio.getAccount(account.getAccountId());

            // Then
            assertThat(retrievedAccount).isEqualTo(account);
        }

        @Test
        @DisplayName("Should throw exception when getting non-existent account")
        void shouldThrowExceptionWhenGettingNonExistentAccount() {
            // Given
            AccountId nonExistentId = AccountId.randomId();

            // When & Then
            assertThatThrownBy(() -> portfolio.getAccount(nonExistentId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("cannot be found");
        }
    }

    @Nested
    @DisplayName("Total Assets Calculation Tests")
    class TotalAssetsTests {

        private Portfolio portfolio;
        private Account account1;
        private Account account2;

        @BeforeEach
        void setUp() {
            portfolio = new Portfolio(userId, portfolioCurrency);
            account1 = createTestAccount("Account 1", AccountType.TFSA);
            account2 = createTestAccount("Account 2", AccountType.RRSP);
        }

        @Test
        @DisplayName("Should calculate total assets for empty portfolio")
        void shouldCalculateTotalAssetsForEmptyPortfolio() {
            // Given
            when(exchangeRateService.convert(any(Money.class), eq(portfolioCurrency)))
                .thenReturn(Money.ZERO(portfolioCurrency));

            // When
            Money totalAssets = portfolio.getTotalAssets(marketDataService, exchangeRateService);

            // Then
            assertThat(totalAssets.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should calculate total assets for portfolio with accounts")
        void shouldCalculateTotalAssetsForPortfolioWithAccounts() {
            // Given
            portfolio.addAccount(account1);
            portfolio.addAccount(account2);

            Money account1Value = Money.of(new BigDecimal("10000"), ValidatedCurrency.CAD);
            Money account2Value = Money.of(new BigDecimal("5000"), ValidatedCurrency.CAD);

            when(marketDataService.getCurrentPrice(any())).thenReturn(mock(Money.class));
            when(exchangeRateService.convert(any(Money.class), eq(portfolioCurrency)))
                .thenReturn(account1Value, account2Value);

            // When
            Money totalAssets = portfolio.getTotalAssets(marketDataService, exchangeRateService);

            // Then
            verify(exchangeRateService, times(2)).convert(any(Money.class), eq(portfolioCurrency));
            assertEquals(account2Value, totalAssets);
        }

        @Test
        @DisplayName("Should throw exception when marketDataService is null")
        void shouldThrowExceptionWhenMarketDataServiceIsNull() {
            // When & Then
            assertThatThrownBy(() -> portfolio.getTotalAssets(null, exchangeRateService))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("marketDataService");
        }

        @Test
        @DisplayName("Should throw exception when exchangeRateService is null")
        void shouldThrowExceptionWhenExchangeRateServiceIsNull() {
            // When & Then
            assertThatThrownBy(() -> portfolio.getTotalAssets(marketDataService, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("exchangeRateService");
        }
    }

    @Nested
    @DisplayName("Transaction History Tests")
    class TransactionHistoryTests {

        private Portfolio portfolio;
        private Account account;
        private Instant baseDate;
        private Transaction transaction1;
        private Transaction transaction2;
        private Transaction transaction3;

        @BeforeEach
        void setUp() {
            portfolio = new Portfolio(userId, portfolioCurrency);
            account = createTestAccount("TFSA", AccountType.TFSA);
            portfolio.addAccount(account);

            baseDate = Instant.now();
            transaction1 = createTransactionWithDate(baseDate.minus(3, ChronoUnit.DAYS));
            transaction2 = createTransactionWithDate(baseDate.minus(1, ChronoUnit.DAYS));
            transaction3 = createTransactionWithDate(baseDate);
        }

        @Test
        @DisplayName("Should get all transactions when startDate is null")
        void shouldGetAllTransactionsWhenStartDateIsNull() throws Exception {
            // Given
            portfolio.recordTransaction(account.getAccountId(), transaction1);
            portfolio.recordTransaction(account.getAccountId(), transaction2);
            portfolio.recordTransaction(account.getAccountId(), transaction3);

            // When
            List<Transaction> history = portfolio.getTransactionHistory(null, baseDate.plus(1, ChronoUnit.DAYS));

            // Then
            assertThat(history).hasSize(3);
            assertThat(history).containsExactly(transaction1, transaction2, transaction3);
        }

        @Test
        @DisplayName("Should get transactions within date range")
        void shouldGetTransactionsWithinDateRange() throws Exception {
            // Given
            portfolio.recordTransaction(account.getAccountId(), transaction1);
            portfolio.recordTransaction(account.getAccountId(), transaction2);
            portfolio.recordTransaction(account.getAccountId(), transaction3);

            Instant startDate = baseDate.minus(2, ChronoUnit.DAYS);
            Instant endDate = baseDate.minus(1, ChronoUnit.DAYS);

            // When
            List<Transaction> history = portfolio.getTransactionHistory(startDate, endDate);

            // Then
            assertThat(history).hasSize(1);
            assertThat(history).contains(transaction2);
        }

        @Test
        @DisplayName("Should return transactions in chronological order")
        void shouldReturnTransactionsInChronologicalOrder() throws Exception {
            // Given - Add transactions out of order
            portfolio.recordTransaction(account.getAccountId(), transaction3);
            portfolio.recordTransaction(account.getAccountId(), transaction1);
            portfolio.recordTransaction(account.getAccountId(), transaction2);

            // When
            List<Transaction> history = portfolio.getTransactionHistory(null, baseDate.plus(1, ChronoUnit.DAYS));

            // Then
            assertThat(history).containsExactly(transaction1, transaction2, transaction3);
        }

        @Test
        @DisplayName("Should throw exception when endDate is null")
        void shouldThrowExceptionWhenEndDateIsNull() {
            // When & Then
            assertThatThrownBy(() -> portfolio.getTransactionHistory(baseDate, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date cannot be null");
        }

        @Test
        @DisplayName("Should return empty list when no transactions in range")
        void shouldReturnEmptyListWhenNoTransactionsInRange() throws Exception {
            // Given
            portfolio.recordTransaction(account.getAccountId(), transaction1);

            Instant startDate = baseDate.plus(1, ChronoUnit.DAYS);
            Instant endDate = baseDate.plus(2, ChronoUnit.DAYS);

            // When
            List<Transaction> history = portfolio.getTransactionHistory(startDate, endDate);

            // Then
            assertThat(history).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Transactions For Account Tests")
    class GetTransactionsForAccountTests {

        private Portfolio portfolio;
        private Account account1;
        private Account account2;

        @BeforeEach
        void setUp() {
            portfolio = new Portfolio(userId, portfolioCurrency);
            account1 = createTestAccount("Account 1", AccountType.TFSA);
            account2 = createTestAccount("Account 2", AccountType.RRSP);
            portfolio.addAccount(account1);
            portfolio.addAccount(account2);
        }

        @Test
        @DisplayName("Should get transactions for specific account")
        void shouldGetTransactionsForSpecificAccount() throws Exception {
            // Given
            Transaction tx1 = createTestTransaction();
            Transaction tx2 = createTestTransaction();
            portfolio.recordTransaction(account1.getAccountId(), tx1);
            portfolio.recordTransaction(account2.getAccountId(), tx2);

            // When
            List<Transaction> account1Transactions = portfolio.getTransactionsForAccount(account1.getAccountId());

            // Then
            assertThat(account1Transactions).hasSize(1);
            assertThat(account1Transactions).contains(tx1);
            assertThat(account1Transactions).doesNotContain(tx2);
        }

        @Test
        @DisplayName("Should throw exception when account not found")
        void shouldThrowExceptionWhenAccountNotFound() {
            // Given
            AccountId nonExistentId = AccountId.randomId();

            // When & Then
            assertThatThrownBy(() -> portfolio.getTransactionsForAccount(nonExistentId))
                .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Get Transactions For Asset Tests")
    class GetTransactionsForAssetTests {

        private Portfolio portfolio;
        private Account account;
        private AssetIdentifier assetId1;
        private AssetIdentifier assetId2;

        @BeforeEach
        void setUp() {
            portfolio = new Portfolio(userId, portfolioCurrency);
            account = createTestAccount("TFSA", AccountType.TFSA);
            portfolio.addAccount(account);
            assetId1 = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);
            assetId2 = new CryptoIdentifier("BTC", "Bitcoin", AssetType.CRYPTO, "Bitcoin", null);
        }

        @Test
        @DisplayName("Should get transactions for specific asset")
        void shouldGetTransactionsForSpecificAsset() throws Exception {
            // Given
            Transaction tx1 = createTransactionWithAsset(assetId1);
            Transaction tx2 = createTransactionWithAsset(assetId2);
            Transaction tx3 = createTransactionWithAsset(assetId1);

            portfolio.recordTransaction(account.getAccountId(), tx1);
            portfolio.recordTransaction(account.getAccountId(), tx2);
            portfolio.recordTransaction(account.getAccountId(), tx3);

            // When
            List<Transaction> assetTransactions = portfolio.getTransactionsForAsset(assetId1);

            // Then
            assertThat(assetTransactions).hasSize(2);
            assertThat(assetTransactions).containsExactly(tx1, tx3);
        }

        @Test
        @DisplayName("Should return empty list when no transactions for asset")
        void shouldReturnEmptyListWhenNoTransactionsForAsset() {
            // Given
            AssetIdentifier unusedAsset = new MarketIdentifier("TSLA", null, AssetType.STOCK, "TESLA", "USD", null);

            // When
            List<Transaction> transactions = portfolio.getTransactionsForAsset(unusedAsset);

            // Then
            assertThat(transactions).isEmpty();
        }
    }

    @Nested
    @DisplayName("Query Transactions Tests")
    class QueryTransactionsTests {

        private Portfolio portfolio;
        private Account account1;
        private Account account2;
        private AssetIdentifier assetId;
        private Instant baseDate;

        @BeforeEach
        void setUp() {
            portfolio = new Portfolio(userId, portfolioCurrency);
            account1 = createTestAccount("Account 1", AccountType.TFSA);
            account2 = createTestAccount("Account 2", AccountType.RRSP);
            portfolio.addAccount(account1);
            portfolio.addAccount(account2);
            
            assetId = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);
            baseDate = Instant.now();
        }

        @Test
        @DisplayName("Should query transactions with all parameters")
        void shouldQueryTransactionsWithAllParameters() throws Exception {
            // Given
            Transaction tx1 = createTransactionWithAssetAndDate(assetId, baseDate.minus(1, ChronoUnit.DAYS));
            portfolio.recordTransaction(account1.getAccountId(), tx1);

            // When
            List<Transaction> results = portfolio.queryTransactions(
                account1.getAccountId(),
                assetId,
                baseDate.minus(2, ChronoUnit.DAYS),
                baseDate
            );

            // Then
            assertThat(results).hasSize(1);
            assertThat(results).contains(tx1);
        }

        @Test
        @DisplayName("Should query transactions with null parameters returning all")
        void shouldQueryTransactionsWithNullParameters() throws Exception {
            // Given
            Transaction tx1 = createTestTransaction();
            Transaction tx2 = createTestTransaction();
            portfolio.recordTransaction(account1.getAccountId(), tx1);
            portfolio.recordTransaction(account2.getAccountId(), tx2);

            // When
            List<Transaction> results = portfolio.queryTransactions(null, null, null, null);

            // Then
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("Should filter by account only")
        void shouldFilterByAccountOnly() throws Exception {
            // Given
            Transaction tx1 = createTestTransaction();
            Transaction tx2 = createTestTransaction();
            portfolio.recordTransaction(account1.getAccountId(), tx1);
            portfolio.recordTransaction(account2.getAccountId(), tx2);

            // When
            List<Transaction> results = portfolio.queryTransactions(account1.getAccountId(), null, null, null);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results).contains(tx1);
        }
    }

    @Nested
    @DisplayName("Net Worth Calculation Tests")
    class NetWorthTests {

        private Portfolio portfolio;

        @BeforeEach
        void setUp() {
            portfolio = new Portfolio(userId, portfolioCurrency);
        }

        @Test
        @DisplayName("Should calculate net worth equal to total assets")
        void shouldCalculateNetWorthEqualToTotalAssets() {
            // Given
            Money expectedValue = Money.of(new BigDecimal("50000"), portfolioCurrency);
            when(exchangeRateService.convert(any(Money.class), eq(portfolioCurrency)))
                .thenReturn(expectedValue);

            // When
            Money netWorth = portfolio.calculateNetWorth(marketDataService, exchangeRateService);

            // Then
            assertThat(netWorth).isEqualTo(expectedValue);
        }
    }

    // Helper methods
    private Account createTestAccount(String name, AccountType type) {
        return Account.builder()
            .accountId(AccountId.randomId())
            .name(name)
            .accountType(type)
            .baseCurrency(ValidatedCurrency.CAD)
            .assets(new ArrayList<>())
            .build();
    }

    private Asset createTestAsset() {
        return Asset.builder()
            .assetId(assetId1)
            .assetIdentifier(new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null))
            .quantity((new BigDecimal("10")))
            .costBasis(Money.of(new BigDecimal("1000"), ValidatedCurrency.CAD))
            .acquiredOn(Instant.now())
            .build();
    }

    private Transaction createTestTransaction() {
        return Transaction.builder()
            .transactionId(transactionId1)
            .transactionType(TransactionType.BUY)
            .assetIdentifier(new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null))
            .quantity((new BigDecimal("10")))
            .pricePerUnit((Money.of(new BigDecimal("150"), ValidatedCurrency.CAD)))
            .transactionDate(Instant.now())
            .build();
    }

    private Transaction createTransactionWithDate(Instant date) {
        return Transaction.builder()
            .transactionId(TransactionId.randomId())
            .transactionType(TransactionType.BUY)
            .assetIdentifier(new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null))
            .quantity((new BigDecimal("10")))
            .pricePerUnit((Money.of(new BigDecimal("150"), ValidatedCurrency.CAD)))
            .transactionDate(date)
            .build();
    }

    private Transaction createTransactionWithAsset(AssetIdentifier assetIdentifier) {
        return Transaction.builder()
            .transactionId(TransactionId.randomId())
            .transactionType(TransactionType.BUY)
            .assetIdentifier(assetIdentifier)
            .quantity((new BigDecimal("10")))
            .pricePerUnit((Money.of(new BigDecimal("150"), ValidatedCurrency.CAD)))
            .transactionDate(Instant.now())
            .build();
    }

    private Transaction createTransactionWithAssetAndDate(AssetIdentifier assetIdentifier, Instant date) {
        return Transaction.builder()
            .transactionId(TransactionId.randomId())
            .transactionType(TransactionType.BUY)
            .assetIdentifier(assetIdentifier)
            .quantity((new BigDecimal("10")))
            .pricePerUnit((Money.of(new BigDecimal("150"), ValidatedCurrency.CAD)))
            .transactionDate(date)
            .build();
    }
}