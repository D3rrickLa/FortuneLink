package com.laderrco.fortunelink.portfolio_management.domain.models.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CashIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CryptoIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
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
    private AccountId accountId1;


    @BeforeEach
    void setUp() {
        userId = UserId.randomId();
        portfolioCurrency = ValidatedCurrency.CAD;
        transactionId1 = TransactionId.randomId();
        assetId1 = AssetId.randomId();
        accountId1 = AccountId.randomId();
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
            assertThat(portfolio.getPortfolioCurrencyPreference()).isEqualTo(portfolioCurrency);
            assertThat(portfolio.getAccounts()).isEmpty();
            assertThat(portfolio.getPortfolioId()).isNotNull();
            assertThat(portfolio.getSystemCreationDate()).isNotNull();
            assertThat(portfolio.getLastUpdatedAt()).isNotNull();
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

        @Test 
        @DisplayName("Should create portfolio using repo specific constructor")
        void testRepoConstructorIsSucess() {
            List<Account> accounts = List.of(createTestAccount("TEST 1", AccountType.CHEQUING), createTestAccount("RRSP", AccountType.INVESTMENT));
            Portfolio portfolio = new Portfolio(PortfolioId.randomId(), userId, portfolioCurrency, accounts, Instant.now(), Instant.now());
            assertEquals(accounts, portfolio.getAccounts());
            assertEquals(portfolioCurrency, portfolio.getPortfolioCurrencyPreference());
        }
    }

    @Nested
    @DisplayName("Random Misc. Tests")
    public class InnerPortfolioTest {
        @Test
        void testUpdatingCurrencyPref() {
            Portfolio portfolio = new Portfolio(userId, portfolioCurrency);
            ValidatedCurrency newCur = ValidatedCurrency.GBP;
            portfolio.updateCurrencyPreference(newCur);
            assertEquals(newCur, portfolio.getPortfolioCurrencyPreference());
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
                .cashBalance(Money.ZERO("CAD"))
                .assets(new ArrayList<>())
                .systemCreationDate(Instant.now())
                .lastSystemInteraction(Instant.now())
                .isActive(true)
                .build();
            
            account2 = Account.builder()
                .accountId(AccountId.randomId())
                .name("RRSP Account")
                .accountType(AccountType.RRSP)
                .baseCurrency(ValidatedCurrency.CAD)
                .cashBalance(Money.ZERO("CAD"))
                .assets(new ArrayList<>())
                .systemCreationDate(Instant.now())
                .lastSystemInteraction(Instant.now())
                .isActive(true)
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
                .cashBalance(Money.ZERO("CAD"))
                .assets(new ArrayList<>())
                .systemCreationDate(Instant.now())
                .lastSystemInteraction(Instant.now())
                .build();

            // When & Then
            assertThatThrownBy(() -> portfolio.addAccount(duplicateNameAccount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should update metadata when adding account")
        void shouldUpdateMetadataWhenAddingAccount() throws InterruptedException {
            // Given
            Instant initialUpdatedAt = portfolio.getLastUpdatedAt();
            Thread.sleep(10); // Ensure time difference

            // When
            portfolio.addAccount(account1);

            // Then
            assertThat(portfolio.getLastUpdatedAt()).isAfter(initialUpdatedAt);
        }

        @Test
        @DisplayName("Should successfully remove empty account")
        void shouldRemoveEmptyAccount() throws AccountNotFoundException {
            // Given
            portfolio.addAccount(account1);
            account1.getAssets().removeIf(a -> true);
            
            // When
            account1.close();
            portfolio.removeAccount(account1.getAccountId());

            // Then
            assertThat(portfolio.getAccounts()).isEmpty();
            assertFalse(portfolio.containsAccounts());
        }

        @Test
        @DisplayName("Should throw exception when removing without closing the account first")
        void shouldThrowExceptionWhenRemovingIfAccountIsActive() {
            Asset asset = createTestAsset();
            account1.getAssets().add(asset);
            portfolio.addAccount(account1);

            assertThatThrownBy(() -> portfolio.removeAccount(account1.getAccountId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Account cannot be removed, please close the account first");
        }

        @Test
        void shouldThrowExceptionWhenRemovingAccountWithTransactionsInIt() {
            Transaction transaction = createTestTransaction();
            portfolio.addAccount(account1);
            portfolio.recordTransaction(account1.getAccountId(), transaction);
            account1.getAssets().clear();
            portfolio.closeAccount(account1.getAccountId());

            assertThatThrownBy(() -> portfolio.removeAccount(account1.getAccountId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot remove account");

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
            List<Transaction> transactions = portfolio.getTransactionsFromAccount(account.getAccountId());
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
            Instant initialUpdatedAt = portfolio.getLastUpdatedAt();
            Thread.sleep(10);

            // When
            portfolio.recordTransaction(account.getAccountId(), transaction);

            // Then
            assertThat(portfolio.getLastUpdatedAt()).isAfter(initialUpdatedAt);
        }

        @Test
        @DisplayName("Should update transaction successfully")
        void testUpdateTransactionShouldBeSuccessful() {
            portfolio.recordTransaction(account.getAccountId(), transaction);
            Transaction updatedTransaction = Transaction.builder()
                .transactionId(transaction.getTransactionId())
                .accountId(account.getAccountId())
                .transactionType(TransactionType.BUY)
                .assetIdentifier(new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null))
                .quantity((new BigDecimal("100")))
                .pricePerUnit((Money.of(new BigDecimal("350"), ValidatedCurrency.CAD)))
                .transactionDate(Instant.now())
                .notes("something here 2")
                .build();

            portfolio.updateTransaction(account.getAccountId(), transaction.getTransactionId(), updatedTransaction);
            assertEquals(account.getTransaction(transaction.getTransactionId()), updatedTransaction);
        }

        @Test
        @DisplayName("Should remove transaction successfully")
        void testRemoveTransactionShouldBeSuccessful() {
            portfolio.recordTransaction(account.getAccountId(), transaction);
            assertTrue(portfolio.containsAccounts());
            assertTrue(portfolio.getTransactionCount() == 1);
            
            portfolio.removeTransaction(account.getAccountId(), transaction.getTransactionId());
            assertTrue(portfolio.getTransactionCount() == 0);
            
        }
    }

    @Nested
    @DisplayName("Correcting Asset Ticker Tests")
    public class CorrectAssetTickerTests {
        private Portfolio portfolio;
        private Account account1;

        @BeforeEach
        void setUp() {
            portfolio = new Portfolio(userId, ValidatedCurrency.USD);
            account1 = Account.builder()
                .accountId(AccountId.randomId())
                .name("TFSA Account")
                .accountType(AccountType.TFSA)
                .baseCurrency(ValidatedCurrency.USD)
                .cashBalance(Money.ZERO("USD"))
                .assets(new ArrayList<>())
                .systemCreationDate(Instant.now())
                .lastSystemInteraction(Instant.now())
                .isActive(true)
                .build();
        }

        @Test
        @DisplayName("Should swap wrong ticker for correct ticker via transactions when quantity > 0")
        void shouldSwapTickersViaTransactions() {
            // Given
            AssetIdentifier wrongTicker = new MarketIdentifier("FB", null, AssetType.STOCK, "Facebook", "USD", null); // Old Facebook ticker
            AssetIdentifier correctTicker = new MarketIdentifier("META", null, AssetType.STOCK, "META", "USD", null);
            BigDecimal quantity = new BigDecimal("10");
            
            // Setup an account with the "wrong" asset already in it
            // Note: Assuming recordTransaction or a setup method adds the initial asset
            Asset initialAsset = Asset.builder()
                .assetId(assetId1)
                .assetIdentifier(wrongTicker)
                .currency(ValidatedCurrency.USD)
                .quantity(quantity)
                .costBasis(Money.of(new BigDecimal("1000"), ValidatedCurrency.USD))
                .acquiredOn(Instant.now())
                .lastSystemInteraction(Instant.now())
                .build();

            account1.addAsset(initialAsset); 
            portfolio.addAccount(account1);

            // When
            portfolio.correctAssetTicker(account1.getAccountId(), wrongTicker, correctTicker);

            // Then
            // 1. The old asset should be gone (or have 0 quantity depending on your logic)
            assertThat(account1.getAssets()).noneMatch(a -> a.getAssetIdentifier().equals(wrongTicker));
            
            // 2. The new asset should exist with the correct quantity
            Asset newAsset = account1.getAsset(correctTicker);
            assertThat(newAsset.getQuantity()).isEqualByComparingTo(quantity);

            // 3. Two correction transactions should have been recorded
            List<Transaction> history = account1.getTransactions();
            assertThat(history).hasSize(2);
            assertThat(history.get(0).getTransactionType()).isEqualTo(TransactionType.SELL);
            assertThat(history.get(1).getTransactionType()).isEqualTo(TransactionType.BUY);
            assertThat(history.get(1).getAssetIdentifier()).isEqualTo(correctTicker);
        }

        @Test
        @DisplayName("Should simply remove asset if quantity is zero")
        void shouldRemoveAssetDirectlyIfQuantityIsZero() {
            // Given
            AssetIdentifier wrongTicker = new CashIdentifier("USD");
            AssetIdentifier correctTicker = new CashIdentifier("JPY");
            
            // Asset exists but quantity is 0
            Asset zeroAsset = Asset.builder()
                .assetId(assetId1)
                .assetIdentifier(wrongTicker)
                .currency(ValidatedCurrency.USD)
                .quantity(BigDecimal.ZERO)
                .costBasis(Money.of(new BigDecimal("0"), ValidatedCurrency.USD))
                .acquiredOn(Instant.now())
                .lastSystemInteraction(Instant.now())
                .build();

            account1.addAsset(zeroAsset);
            portfolio.addAccount(account1);

            // When
            portfolio.correctAssetTicker(account1.getAccountId(), wrongTicker, correctTicker);

            // Then
            assertThat(account1.getAssets()).isEmpty();
            assertThat(account1.getTransactions()).isEmpty(); // No transactions should be recorded for 0 quantity
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
                .hasMessageContaining("not found in this portfolio");
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
            // this is NOT needed
            // when(exchangeRateService.convert(any(Money.class), any(ValidatedCurrency.class))).thenReturn(Money.ZERO(portfolioCurrency));

            // When
            Money totalAssets = portfolio.getAssetsTotalValue(marketDataService, exchangeRateService);

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

            // when(marketDataService.getCurrentPrice(any())).thenReturn(mock(Money.class));
            when(exchangeRateService.convert(any(Money.class), eq(portfolioCurrency)))
                .thenReturn(account1Value, account2Value);

            // When
            Money totalAssets = portfolio.getAssetsTotalValue(marketDataService, exchangeRateService);

            // Then
            verify(exchangeRateService, times(2)).convert(any(Money.class), eq(portfolioCurrency));
            assertEquals(account2Value.add(account1Value), totalAssets);
        }

        @Test
        @DisplayName("Should throw exception when marketDataService is null")
        void shouldThrowExceptionWhenMarketDataServiceIsNull() {
            // When & Then
            assertThatThrownBy(() -> portfolio.getAssetsTotalValue(null, exchangeRateService))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("marketDataService");
        }

        @Test
        @DisplayName("Should throw exception when exchangeRateService is null")
        void shouldThrowExceptionWhenExchangeRateServiceIsNull() {
            // When & Then
            assertThatThrownBy(() -> portfolio.getAssetsTotalValue(marketDataService, null))
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
                .isInstanceOf(NullPointerException.class)
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

        @Test
        @DisplayName("Should return all transactions up to endDate when startDate is null")
        void shouldReturnTransactionsUpToEndDateWhenStartDateIsNull() {
            // Given
            Instant farPast = Instant.parse("2020-01-01T00:00:00Z");
            Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
            Instant today = Instant.now();
            Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS);

            // Setup: Two different accounts
            Account accountA = Account.builder()
                .accountId(AccountId.randomId())
                .name("Account A")
                .accountType(AccountType.INVESTMENT)
                .baseCurrency(ValidatedCurrency.USD)
                .cashBalance(Money.ZERO("USD"))
                .assets(new ArrayList<>())
                .systemCreationDate(Instant.now())
                .lastSystemInteraction(Instant.now())
                .build();

            Account accountB = Account.builder()
                .accountId(AccountId.randomId())
                .name("Account B")
                .accountType(AccountType.INVESTMENT)
                .baseCurrency(ValidatedCurrency.USD)
                .cashBalance(Money.ZERO("USD"))
                .assets(new ArrayList<>())
                .systemCreationDate(Instant.now())
                .lastSystemInteraction(Instant.now())
                .build();

            portfolio.addAccount(accountA);
            portfolio.addAccount(accountB);

            // Transaction 1: Far past (Account A)
            Transaction tx1 = createTx(new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null), farPast);
            portfolio.recordTransaction(accountA.getAccountId(), tx1);

            // Transaction 2: Yesterday (Account B)
            Transaction tx2 = createTx(new MarketIdentifier("TSLA", null, AssetType.STOCK, "Tesla", "USD", null), yesterday);
            portfolio.recordTransaction(accountB.getAccountId(), tx2);

            // Transaction 3: Tomorrow (Should be excluded)
            Transaction tx3 = createTx(new MarketIdentifier("GOOGL", null, AssetType.STOCK, "Google", "USD", null), tomorrow);
            portfolio.recordTransaction(accountA.getAccountId(), tx3);

            // When: Querying with startDate = null and endDate = today
            List<Transaction> history = portfolio.getTransactionHistory(null, today);

            // Then
            // 1. Should only contain tx1 and tx2 (because tx3 is after 'today')
            assertThat(history).hasSize(2);
            
            // 2. Should be sorted chronologically (tx1 then tx2)
            assertThat(history).containsExactly(tx1, tx2);
            
            // 3. Verify specifically that the earliest transaction was included
            assertThat(history).extracting(Transaction::getTransactionDate)
                .contains(farPast);
        }
    }

    @Nested
    @DisplayName("Get Transactions For Account Tests")
    class getTransactionsFromAccountTests {

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
            List<Transaction> account1Transactions = portfolio.getTransactionsFromAccount(account1.getAccountId());

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
            assertThatThrownBy(() -> portfolio.getTransactionsFromAccount(nonExistentId))
                .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Get Transactions For Asset Tests")
    class getTransactionsFromAssetTests {

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
            List<Transaction> assetTransactions = portfolio.getTransactionsFromAsset(assetId1);

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
            List<Transaction> transactions = portfolio.getTransactionsFromAsset(unusedAsset);

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

        private Instant now = Instant.now();
        private Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        private Instant tomorrow = now.plus(1, ChronoUnit.DAYS);
        private AccountId accountId;

        @BeforeEach
        void setUp() {
            portfolio = new Portfolio(userId, ValidatedCurrency.USD);
            account1 = Account.builder()
                .accountId(AccountId.randomId())
                .name("TFSA Account")
                .accountType(AccountType.TFSA)
                .baseCurrency(ValidatedCurrency.USD)
                .cashBalance(Money.ZERO("USD"))
                .assets(new ArrayList<>())
                .systemCreationDate(Instant.now())
                .lastSystemInteraction(Instant.now())
                .isActive(true)
                .build();
            
            account2 = Account.builder()
                .accountId(AccountId.randomId())
                .name("RRSP Account")
                .accountType(AccountType.RRSP)
                .baseCurrency(ValidatedCurrency.USD)
                .cashBalance(Money.ZERO("USD"))
                .assets(new ArrayList<>())
                .systemCreationDate(Instant.now())
                .lastSystemInteraction(Instant.now())
                .isActive(true)
                .build();
            portfolio.addAccount(account1);
            portfolio.addAccount(account2);
            accountId = account1.getAccountId();
            
            assetId = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);
            baseDate = Instant.now();
        }

        @Test
        @DisplayName("Should query transactions with all parameters")
        void shouldQueryTransactionsWithAllParameters() throws Exception {
            // Given
            Transaction tx1 = Transaction.builder()
                .transactionId(TransactionId.randomId())
                .accountId(AccountId.randomId())
                .transactionType(TransactionType.BUY)
                .assetIdentifier(assetId)
                .quantity((new BigDecimal("10")))
                .pricePerUnit((Money.of(new BigDecimal("150"), ValidatedCurrency.USD)))
                .transactionDate(baseDate.minus(1, ChronoUnit.DAYS))
                .notes("something here")
                .build();
            
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
            Transaction tx1 = Transaction.builder()
                .transactionId(transactionId1)
                .accountId(accountId1)
                .transactionType(TransactionType.BUY)
                .assetIdentifier(new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null))
                .quantity((new BigDecimal("10")))
                .pricePerUnit((Money.of(new BigDecimal("150"), ValidatedCurrency.USD)))
                .transactionDate(Instant.now())
                .notes("something here")
                .build();
                
            Transaction tx2 = Transaction.builder()
                .transactionId(transactionId1)
                .accountId(accountId1)
                .transactionType(TransactionType.BUY)
                .assetIdentifier(new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null))
                .quantity((new BigDecimal("10")))
                .pricePerUnit((Money.of(new BigDecimal("150"), ValidatedCurrency.USD)))
                .transactionDate(Instant.now())
                .notes("something here")
                .build();

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
            Transaction tx1 = Transaction.builder()
                .transactionId(transactionId1)
                .accountId(accountId1)
                .transactionType(TransactionType.BUY)
                .assetIdentifier(new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null))
                .quantity((new BigDecimal("10")))
                .pricePerUnit((Money.of(new BigDecimal("150"), ValidatedCurrency.USD)))
                .transactionDate(Instant.now())
                .notes("something here")
                .build();
                
            Transaction tx2 = Transaction.builder()
                .transactionId(transactionId1)
                .accountId(accountId1)
                .transactionType(TransactionType.BUY)
                .assetIdentifier(new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null))
                .quantity((new BigDecimal("10")))
                .pricePerUnit((Money.of(new BigDecimal("150"), ValidatedCurrency.USD)))
                .transactionDate(Instant.now())
                .notes("something here")
                .build();

            portfolio.recordTransaction(account1.getAccountId(), tx1);
            portfolio.recordTransaction(account2.getAccountId(), tx2);

            // When
            List<Transaction> results = portfolio.queryTransactions(account1.getAccountId(), null, null, null);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results).contains(tx1);
        }

        @Test
        @DisplayName("Should filter by AssetIdentifier and ignore others")
        void shouldFilterByAssetIdentifier() {
            // Given: Account with two different assets
            AssetIdentifier apple = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);
            AssetIdentifier tesla = new MarketIdentifier("TSLA", null, AssetType.STOCK, "Tesla", "USD", null);
            
            portfolio.recordTransaction(accountId, createTx(apple, now));
            portfolio.recordTransaction(accountId, createTx(tesla, now));

            // When: Querying specifically for Apple
            List<Transaction> results = portfolio.queryTransactions(accountId, apple, null, null);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getAssetIdentifier()).isEqualTo(apple);
        }

        @Test
        @DisplayName("Should return all assets when AssetIdentifier is null")
        void shouldReturnAllAssetsWhenIdentifierIsNull() {
            portfolio.recordTransaction(accountId, createTx(new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null), now));
            portfolio.recordTransaction(accountId, createTx(new MarketIdentifier("STLA", null, AssetType.STOCK, "Tesla", "USD", null), now));

            List<Transaction> results = portfolio.queryTransactions(accountId, null, null, null);

            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("Should respect inclusive date boundaries (Edge Cases)")
        void shouldRespectDateBoundaries() {
            AssetIdentifier id = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);;
            
            // 1. Transaction exactly on the startDate
            portfolio.recordTransaction(accountId, createTx(id, yesterday)); 
            // 2. Transaction exactly on the endDate
            portfolio.recordTransaction(accountId, createTx(id, tomorrow));
            // 3. Transaction outside range (too early)
            portfolio.recordTransaction(accountId, createTx(id, yesterday.minusSeconds(10)));

            // When: Querying the exact window
            List<Transaction> results = portfolio.queryTransactions(accountId, id, yesterday, tomorrow);

            // Then: Should find the 2 transactions on the boundaries
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("Should filter using only startDate or only endDate")
        void shouldFilterWithOpenEndedRange() {
            AssetIdentifier id = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);;
            portfolio.recordTransaction(accountId, createTx(id, yesterday));
            portfolio.recordTransaction(accountId, createTx(id, tomorrow));

            // Only start date provided
            List<Transaction> sinceYesterday = portfolio.queryTransactions(accountId, id, now, null);
            assertThat(sinceYesterday).hasSize(1);
            assertThat(sinceYesterday.get(0).getTransactionDate()).isEqualTo(tomorrow);

            // Only end date provided
            List<Transaction> untilNow = portfolio.queryTransactions(accountId, id, null, now);
            assertThat(untilNow).hasSize(1);
            assertThat(untilNow.get(0).getTransactionDate()).isEqualTo(yesterday);
        }
    }

    // Helper methods
    private Account createTestAccount(String name, AccountType type) {
        return Account.builder()
            .accountId(AccountId.randomId())
            .name(name)
            .accountType(type)
            .baseCurrency(ValidatedCurrency.CAD)
            .cashBalance(Money.ZERO("CAD"))
            .assets(new ArrayList<>())
            .systemCreationDate(Instant.now())
            .lastSystemInteraction(Instant.now())
            .build();
    }

    private Asset createTestAsset() {
        return Asset.builder()
            .assetId(assetId1)
            .assetIdentifier(new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null))
            .currency(ValidatedCurrency.CAD)
            .quantity((new BigDecimal("10")))
            .costBasis(Money.of(new BigDecimal("1000"), ValidatedCurrency.CAD))
            .acquiredOn(Instant.now())
            .lastSystemInteraction(Instant.now())
            .build();
    }

    private Transaction createTestTransaction() {
        return Transaction.builder()
            .transactionId(transactionId1)
            .accountId(accountId1)
            .transactionType(TransactionType.BUY)
            .assetIdentifier(new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null))
            .quantity((new BigDecimal("10")))
            .pricePerUnit((Money.of(new BigDecimal("150"), ValidatedCurrency.CAD)))
            .transactionDate(Instant.now())
            .notes("something here")
            .build();
    }

    private Transaction createTransactionWithDate(Instant date) {
        return Transaction.builder()
            .transactionId(TransactionId.randomId())
            .accountId(AccountId.randomId())
            .transactionType(TransactionType.BUY)
            .assetIdentifier(new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null))
            .quantity((new BigDecimal("10")))
            .pricePerUnit((Money.of(new BigDecimal("150"), ValidatedCurrency.CAD)))
            .transactionDate(date)
            .notes("something here")
            .build();
    }

    private Transaction createTransactionWithAsset(AssetIdentifier assetIdentifier) {
        return Transaction.builder()
            .transactionId(TransactionId.randomId())
            .accountId(AccountId.randomId())
            .transactionType(TransactionType.BUY)
            .assetIdentifier(assetIdentifier)
            .quantity((new BigDecimal("10")))
            .pricePerUnit((Money.of(new BigDecimal("150"), ValidatedCurrency.CAD)))
            .transactionDate(Instant.now())
            .notes("something here")
            .build();
    }

    // private Transaction createTransactionWithAssetAndDate(AssetIdentifier assetIdentifier, Instant date) {
    //     return Transaction.builder()
    //         .transactionId(TransactionId.randomId())
    //         .transactionType(TransactionType.BUY)
    //         .assetIdentifier(assetIdentifier)
    //         .quantity((new BigDecimal("10")))
    //         .pricePerUnit((Money.of(new BigDecimal("150"), ValidatedCurrency.CAD)))
    //         .transactionDate(date)
    //         .notes("something here")
    //         .build();
    // }

    private Transaction createTx(AssetIdentifier assetIdentifier, Instant time) {
        return new Transaction(
            TransactionId.randomId(),
            AccountId.randomId(),
            TransactionType.BUY,
            assetIdentifier,
            BigDecimal.ONE,               // Quantity
            Money.of(10, "USD"),               // Price
            null,                    // Fee
            time,                         // The date we are testing
            "Query Test Transaction"
        );
    }
}