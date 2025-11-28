package com.laderrco.fortunelink.portfolio_management.domain.models.entities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.UnsupportedTransactionTypeException;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.shared.valueobjects.Money;

class AccountTest {

    @Mock
    private MarketDataService marketDataService;

    private AccountId accountId;
    private String accountName;
    private AccountType accountType;
    private ValidatedCurrency baseCurrency;
    private Money initialCashBalance;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        accountId = mock(AccountId.class);
        accountName = "Investment Account";
        accountType = AccountType.INVESTMENT;
        baseCurrency = ValidatedCurrency.USD;
        initialCashBalance = Money.of(BigDecimal.valueOf(10000), ValidatedCurrency.USD);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create valid account with all required fields")
        void shouldCreateValidAccount() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            assertNotNull(account);
            assertEquals(accountId, account.getAccountId());
            assertEquals(accountName, account.getName());
            assertEquals(accountType, account.getAccountType());
            assertEquals(baseCurrency, account.getBaseCurrency());
            assertEquals(initialCashBalance, account.getCashBalance());
            assertEquals(1, account.getVersion());
            assertNotNull(account.getAssets());
            assertNotNull(account.getTransactions());
            assertTrue(account.getAssets().isEmpty());
            assertTrue(account.getTransactions().isEmpty());
        }

        @Test
        @DisplayName("Should initialize empty lists when assets and transactions are null")
        void shouldInitializeEmptyLists() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            assertNotNull(account.getAssets());
            assertNotNull(account.getTransactions());
            assertTrue(account.getAssets().isEmpty());
            assertTrue(account.getTransactions().isEmpty());
        }

        @Test
        @DisplayName("Should create defensive copies of lists")
        void shouldCreateDefensiveCopies() {
            List<Asset> assets = new ArrayList<>();
            List<Transaction> transactions = new ArrayList<>();

            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                assets,
                transactions
            );

            // Modifying original lists should not affect account
            assets.add(mock(Asset.class));
            transactions.add(mock(Transaction.class));

            assertTrue(account.getAssets().isEmpty());
            assertTrue(account.getTransactions().isEmpty());
        }

        @Test
        @DisplayName("Should throw exception when accountId is null")
        void shouldThrowExceptionWhenAccountIdIsNull() {
            assertThrows(NullPointerException.class, () ->
                new Account(
                    null,
                    accountName,
                    accountType,
                    baseCurrency,
                    initialCashBalance,
                    null,
                    null
                )
            );
        }

        @Test
        @DisplayName("Should throw exception when name is null")
        void shouldThrowExceptionWhenNameIsNull() {
            assertThrows(NullPointerException.class, () ->
                new Account(
                    accountId,
                    null,
                    accountType,
                    baseCurrency,
                    initialCashBalance,
                    null,
                    null
                )
            );
        }

        @Test
        @DisplayName("Should throw exception when accountType is null")
        void shouldThrowExceptionWhenAccountTypeIsNull() {
            assertThrows(NullPointerException.class, () ->
                new Account(
                    accountId,
                    accountName,
                    null,
                    baseCurrency,
                    initialCashBalance,
                    null,
                    null
                )
            );
        }

        @Test
        @DisplayName("Should throw exception when baseCurrency is null")
        void shouldThrowExceptionWhenBaseCurrencyIsNull() {
            assertThrows(NullPointerException.class, () ->
                new Account(
                    accountId,
                    accountName,
                    accountType,
                    null,
                    initialCashBalance,
                    null,
                    null
                )
            );
        }

        @Test
        @DisplayName("Should throw exception when cashBalance is null")
        void shouldThrowExceptionWhenCashBalanceIsNull() {
            assertThrows(NullPointerException.class, () ->
                new Account(
                    accountId,
                    accountName,
                    accountType,
                    baseCurrency,
                    null,
                    null,
                    null
                )
            );
        }
    }

    @Nested
    @DisplayName("Deposit Tests")
    class DepositTests {

        @Test
        @DisplayName("Should deposit money successfully")
        void shouldDepositMoneySuccessfully() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(1000), ValidatedCurrency.USD),
                null,
                null
            );

            Money depositAmount = Money.of(BigDecimal.valueOf(500), ValidatedCurrency.USD);
            int initialVersion = account.getVersion();

            account.deposit(depositAmount);

            assertEquals(new BigDecimal("1500").setScale(Precision.getMoneyPrecision()), account.getCashBalance().amount());
            assertEquals(initialVersion + 1, account.getVersion());
        }

        @Test
        @DisplayName("Should throw exception when depositing with different currency")
        void shouldThrowExceptionWhenDepositingDifferentCurrency() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            Money cadDeposit = Money.of(BigDecimal.valueOf(500), ValidatedCurrency.CAD);

            assertThrows(IllegalArgumentException.class, () ->
                account.deposit(cadDeposit)
            );
        }

        @Test
        @DisplayName("Should throw exception when depositing zero amount")
        void shouldThrowExceptionWhenDepositingZero() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            Money zeroDeposit = Money.of(BigDecimal.ZERO, ValidatedCurrency.USD);

            assertThrows(IllegalArgumentException.class, () ->
                account.deposit(zeroDeposit)
            );
        }

        @Test
        @DisplayName("Should throw exception when depositing negative amount")
        void shouldThrowExceptionWhenDepositingNegative() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            Money negativeDeposit = Money.of(BigDecimal.valueOf(-100), ValidatedCurrency.USD);

            assertThrows(IllegalArgumentException.class, () ->
                account.deposit(negativeDeposit)
            );
        }

        @Test
        @DisplayName("Should throw exception when deposit is null")
        void shouldThrowExceptionWhenDepositIsNull() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            assertThrows(NullPointerException.class, () ->
                account.deposit(null)
            );
        }
    }

    @Nested
    @DisplayName("Withdraw Tests")
    class WithdrawTests {

        @Test
        @DisplayName("Should withdraw money successfully")
        void shouldWithdrawMoneySuccessfully() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(1000), ValidatedCurrency.USD),
                null,
                null
            );

            Money withdrawAmount = Money.of(BigDecimal.valueOf(300), ValidatedCurrency.USD);
            int initialVersion = account.getVersion();

            account.withdraw(withdrawAmount);

            assertEquals(new BigDecimal("700").setScale(Precision.getMoneyPrecision()), account.getCashBalance().amount());
            assertEquals(initialVersion + 1, account.getVersion());
        }

        @Test
        @DisplayName("Should throw exception when withdrawing with different currency")
        void shouldThrowExceptionWhenWithdrawingDifferentCurrency() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            Money cadWithdraw = Money.of(BigDecimal.valueOf(500), ValidatedCurrency.CAD);

            assertThrows(CurrencyMismatchException.class, () ->
                account.withdraw(cadWithdraw)
            );
        }

        @Test
        @DisplayName("Should throw exception when withdrawing more than balance")
        void shouldThrowExceptionWhenInsufficientFunds() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(500), ValidatedCurrency.USD),
                null,
                null
            );

            Money largeWithdraw = Money.of(BigDecimal.valueOf(1000), ValidatedCurrency.USD);

            assertThrows(InsufficientFundsException.class, () ->
                account.withdraw(largeWithdraw)
            );
        }

        @Test
        @DisplayName("Should throw exception when withdrawing zero amount")
        void shouldThrowExceptionWhenWithdrawingZero() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            Money zeroWithdraw = Money.of(BigDecimal.ZERO, ValidatedCurrency.USD);

            assertThrows(IllegalArgumentException.class, () ->
                account.withdraw(zeroWithdraw)
            );
        }

        @Test
        @DisplayName("Should throw exception when withdrawing negative amount")
        void shouldThrowExceptionWhenWithdrawingNegative() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            Money negativeWithdraw = Money.of(BigDecimal.valueOf(-100), ValidatedCurrency.USD);

            assertThrows(IllegalArgumentException.class, () ->
                account.withdraw(negativeWithdraw)
            );
        }

        @Test
        @DisplayName("Should throw exception when withdraw is null")
        void shouldThrowExceptionWhenWithdrawIsNull() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            assertThrows(NullPointerException.class, () ->
                account.withdraw(null)
            );
        }
    }

    @Nested
    @DisplayName("Add Asset Tests")
    class AddAssetTests {

        @Test
        @DisplayName("Should add asset successfully")
        void shouldAddAssetSuccessfully() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            Asset asset = createMockAsset("AAPL");
            int initialVersion = account.getVersion();

            account.addAsset(asset);

            assertEquals(1, account.getAssets().size());
            assertTrue(account.getAssets().contains(asset));
            assertEquals(initialVersion + 1, account.getVersion());
        }

        @Test
        @DisplayName("Should throw exception when adding duplicate asset")
        void shouldThrowExceptionWhenAddingDuplicateAsset() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            Asset asset1 = createMockAsset("AAPL");
            Asset asset2 = createMockAsset("AAPL"); // Same identifier

            account.addAsset(asset1);

            assertThrows(IllegalStateException.class, () ->
                account.addAsset(asset2)
            );
        }

        @Test
        @DisplayName("Should throw exception when asset is null")
        void shouldThrowExceptionWhenAssetIsNull() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            assertThrows(NullPointerException.class, () ->
                account.addAsset(null)
            );
        }

        private Asset createMockAsset(String symbol) {
            Asset asset = mock(Asset.class);
            AssetIdentifier identifier = mock(AssetIdentifier.class);
            when(identifier.getPrimaryId()).thenReturn(symbol);
            when(asset.getAssetIdentifier()).thenReturn(identifier);
            return asset;
        }
    }

    @Nested
    @DisplayName("Remove Asset Tests")
    class RemoveAssetTests {

        @Test
        @DisplayName("Should remove asset successfully")
        void shouldRemoveAssetSuccessfully() {
            AssetId assetId = mock(AssetId.class);
            Asset asset = mock(Asset.class);
            when(asset.getAssetId()).thenReturn(assetId);

            List<Asset> assets = new ArrayList<>();
            assets.add(asset);

            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                assets,
                null
            );

            int initialVersion = account.getVersion();
            account.removeAsset(assetId);

            assertTrue(account.getAssets().isEmpty());
            assertEquals(initialVersion + 1, account.getVersion());
        }

        @Test
        @DisplayName("Should not throw exception when removing non-existent asset")
        void shouldNotThrowWhenRemovingNonExistentAsset() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            AssetId nonExistentId = mock(AssetId.class);

            assertDoesNotThrow(() -> account.removeAsset(nonExistentId));
        }

        @Test
        @DisplayName("Should throw exception when assetId is null")
        void shouldThrowExceptionWhenAssetIdIsNull() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            assertThrows(NullPointerException.class, () ->
                account.removeAsset(null)
            );
        }
    }

    //TODO These tests technically don't need to exists because this can never happen
    @Nested
    @DisplayName("Update Asset Tests")
    class UpdateAssetTests {

        @Test
        @DisplayName("Should update asset successfully")
        void shouldUpdateAssetSuccessfully() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(10000), ValidatedCurrency.USD),
                null,
                null
            );

            // Create a BUY transaction that creates the asset
            // AssetId assetId = mock(AssetId.class);
            AssetIdentifier assetIdentifier = mock(AssetIdentifier.class);
            when(assetIdentifier.getAssetType()).thenReturn(AssetType.STOCK);
            
            Transaction buyTransaction = mock(Transaction.class);
            TransactionId txId = mock(TransactionId.class);
            Money cost = Money.of(BigDecimal.valueOf(5000), ValidatedCurrency.USD);
            
            when(buyTransaction.getTransactionId()).thenReturn(txId);
            when(buyTransaction.getTransactionType()).thenReturn(TransactionType.BUY);
            when(buyTransaction.getAssetIdentifier()).thenReturn(assetIdentifier);
            when(buyTransaction.getQuantity()).thenReturn(BigDecimal.valueOf(100));
            when(buyTransaction.calculateTotalCost()).thenReturn(cost);
            when(buyTransaction.getTransactionDate()).thenReturn(Instant.now());
            
            // Record the transaction to create the asset
            account.recordTransaction(buyTransaction);
            assertEquals(Money.of(new BigDecimal("5000").setScale(Precision.getMoneyPrecision()), ValidatedCurrency.USD), account.getTransactions().get(0).calculateTotalCost());
            
            assertEquals(1, account.getAssets().size());
            
            // Now update the transaction (simulating price correction, for example)
            Transaction updatedTransaction = mock(Transaction.class);
            Money newCost = Money.of(BigDecimal.valueOf(5500), ValidatedCurrency.USD);
            
            when(updatedTransaction.getTransactionId()).thenReturn(txId);
            when(updatedTransaction.getTransactionType()).thenReturn(TransactionType.BUY);
            when(updatedTransaction.getAssetIdentifier()).thenReturn(assetIdentifier);
            when(updatedTransaction.getQuantity()).thenReturn(BigDecimal.valueOf(100));
            when(updatedTransaction.calculateTotalCost()).thenReturn(newCost);
            when(updatedTransaction.getTransactionDate()).thenReturn(Instant.now());
            
            account.updateTransaction(txId, updatedTransaction);
            
            // Asset should still exist with updated cost basis
            assertEquals(1, account.getAssets().size());
            assertEquals(Money.of(new BigDecimal("5500").setScale(Precision.getMoneyPrecision()), ValidatedCurrency.USD), account.getTransactions().get(0).calculateTotalCost());
        }

        @Test
        @DisplayName("Should throw exception when asset IDs don't match")
        void shouldThrowExceptionWhenAssetIdsDontMatch() {
            AssetId assetId1 = mock(AssetId.class);
            AssetId assetId2 = mock(AssetId.class);
            
            Asset originalAsset = mock(Asset.class);
            Asset updatedAsset = mock(Asset.class);
            
            when(originalAsset.getAssetId()).thenReturn(assetId1);
            when(updatedAsset.getAssetId()).thenReturn(assetId2);

            List<Asset> assets = new ArrayList<>();
            assets.add(originalAsset);

            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                assets,
                null
            );

            assertThrows(IllegalArgumentException.class, () ->
                account.updateAsset(assetId1, updatedAsset)
            );
        }

        @Test
        @DisplayName("Should throw exception when asset doesn't exist")
        void shouldThrowExceptionWhenAssetDoesntExist() {
            AssetId assetId = mock(AssetId.class);
            Asset updatedAsset = mock(Asset.class);
            when(updatedAsset.getAssetId()).thenReturn(assetId);

            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            assertThrows(IllegalStateException.class, () ->
                account.updateAsset(assetId, updatedAsset)
            );
        }

        @Test
        @DisplayName("Should throw exception when parameters are null")
        void shouldThrowExceptionWhenParametersAreNull() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            AssetId assetId = mock(AssetId.class);
            Asset asset = mock(Asset.class);

            assertThrows(NullPointerException.class, () ->
                account.updateAsset(null, asset)
            );

            assertThrows(NullPointerException.class, () ->
                account.updateAsset(assetId, null)
            );
        }
    }

    @Nested
    @DisplayName("Transaction Management Tests")
    class TransactionManagementTests {

        @Test
        @DisplayName("Should add transaction successfully")
        void shouldAddTransactionSuccessfully() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            Transaction transaction = createMockTransaction(TransactionType.DEPOSIT);
            account.addTransaction(transaction);

            assertEquals(1, account.getTransactions().size());
            assertTrue(account.getTransactions().contains(transaction));
        }

        @Test
        @DisplayName("Should throw exception when adding duplicate transaction")
        void shouldThrowExceptionWhenAddingDuplicateTransaction() {
            Transaction transaction1 = createMockTransaction(TransactionType.DEPOSIT);
            Transaction transaction2 = createMockTransaction(TransactionType.DEPOSIT);

            List<Transaction> transactions = new ArrayList<>();
            transactions.add(transaction1);

            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                transactions
            );
            
            // need to have the same 'id'
            TransactionId testTransactionId = TransactionId.randomId();
            when(transaction1.getTransactionId()).thenReturn(testTransactionId);
            when(transaction2.getTransactionId()).thenReturn(testTransactionId);
            assertThrows(IllegalStateException.class, () ->
                account.addTransaction(transaction2)
            );
        }

        @Test
        @DisplayName("Should remove transaction successfully")
        void shouldRemoveTransactionSuccessfully() {
            TransactionId transactionId = mock(TransactionId.class);
            Transaction transaction = mock(Transaction.class);
            when(transaction.getTransactionId()).thenReturn(transactionId);
            when(transaction.getTransactionType()).thenReturn(TransactionType.DEPOSIT);
            when(transaction.getTransactionDate()).thenReturn(Instant.now());

            List<Transaction> transactions = new ArrayList<>();
            transactions.add(transaction);

            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                transactions
            );

            account.removeTransaction(transactionId);

            assertTrue(account.getTransactions().isEmpty());
        }

        @Test
        @DisplayName("Should update transaction successfully")
        void shouldUpdateTransactionSuccessfully() {
            TransactionId transactionId = mock(TransactionId.class);
            Transaction originalTransaction = mock(Transaction.class);
            Transaction updatedTransaction = mock(Transaction.class);
            
            when(originalTransaction.getTransactionId()).thenReturn(transactionId);
            when(updatedTransaction.getTransactionId()).thenReturn(transactionId);
            when(originalTransaction.getTransactionType()).thenReturn(TransactionType.DEPOSIT);
            when(updatedTransaction.getTransactionType()).thenReturn(TransactionType.DEPOSIT);
            when(originalTransaction.getTransactionDate()).thenReturn(Instant.now());
            when(updatedTransaction.getTransactionDate()).thenReturn(Instant.now());
            when(updatedTransaction.getPricePerUnit()).thenReturn(new Money(BigDecimal.valueOf(100), baseCurrency));
            List<Transaction> transactions = new ArrayList<>();
            transactions.add(originalTransaction);

            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                transactions
            );

            account.updateTransaction(transactionId, updatedTransaction);

            assertTrue(account.getTransactions().contains(updatedTransaction));
            assertFalse(account.getTransactions().contains(originalTransaction));
        }

        private Transaction createMockTransaction(TransactionType type) {
            Transaction transaction = mock(Transaction.class);
            TransactionId transactionId = mock(TransactionId.class);
            when(transaction.getTransactionId()).thenReturn(transactionId);
            when(transaction.getTransactionType()).thenReturn(type);
            when(transaction.getTransactionDate()).thenReturn(Instant.now());
            return transaction;
        }
    }

    @Nested
    @DisplayName("Get Asset Tests")
    class GetAssetTests {

        @Test
        @DisplayName("Should get asset by identifier successfully")
        void shouldGetAssetByIdentifier() {
            AssetIdentifier identifier = mock(AssetIdentifier.class);
            Asset asset = mock(Asset.class);
            when(asset.getAssetIdentifier()).thenReturn(identifier);

            List<Asset> assets = new ArrayList<>();
            assets.add(asset);

            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                assets,
                null
            );

            Asset foundAsset = account.getAsset(identifier);

            assertEquals(asset, foundAsset);
        }

        @Test
        @DisplayName("Should throw exception when asset not found")
        void shouldThrowExceptionWhenAssetNotFound() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            AssetIdentifier identifier = mock(AssetIdentifier.class);

            assertThrows(AssetNotFoundException.class, () ->
                account.getAsset(identifier)
            );
        }

        @Test
        @DisplayName("Should throw exception when identifier is null")
        void shouldThrowExceptionWhenIdentifierIsNull() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            assertThrows(NullPointerException.class, () ->
                account.getAsset(null)
            );
        }
    }

    @Nested
    @DisplayName("Get Transaction Tests")
    class GetTransactionTests {

        @Test
        @DisplayName("Should get transaction by id successfully")
        void shouldGetTransactionById() {
            TransactionId transactionId = mock(TransactionId.class);
            Transaction transaction = mock(Transaction.class);
            when(transaction.getTransactionId()).thenReturn(transactionId);

            List<Transaction> transactions = new ArrayList<>();
            transactions.add(transaction);

            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                transactions
            );

            Transaction foundTransaction = account.getTransaction(transactionId);

            assertEquals(transaction, foundTransaction);
        }

        @Test
        @DisplayName("Should throw exception when transaction not found")
        void shouldThrowExceptionWhenTransactionNotFound() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            TransactionId transactionId = mock(TransactionId.class);

            assertThrows(IllegalArgumentException.class, () ->
                account.getTransaction(transactionId)
            );
        }

        @Test
        @DisplayName("Should throw exception when transaction id is null")
        void shouldThrowExceptionWhenTransactionIdIsNull() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            assertThrows(NullPointerException.class, () ->
                account.getTransaction(null)
            );
        }
    }

    @Nested
    @DisplayName("Calculate Total Value Tests")
    class CalculateTotalValueTests {

        @Test
        @DisplayName("Should calculate total value with cash only")
        void shouldCalculateTotalValueWithCashOnly() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(5000), ValidatedCurrency.USD),
                null,
                null
            );

            Money totalValue = account.calculateTotalValue(marketDataService);

            assertEquals(new BigDecimal("5000").setScale(Precision.getMoneyPrecision()), totalValue.amount());
        }

        @Test
        @DisplayName("Should calculate total value with cash and assets")
        void shouldCalculateTotalValueWithCashAndAssets() {
            AssetIdentifier identifier = mock(AssetIdentifier.class);
            Asset asset = mock(Asset.class);
            when(asset.getAssetIdentifier()).thenReturn(identifier);
            
            Money assetValue = Money.of(BigDecimal.valueOf(3000), ValidatedCurrency.USD);
            Money currentPrice = Money.of(BigDecimal.valueOf(100), ValidatedCurrency.USD);
            
            when(marketDataService.getCurrentPrice(identifier)).thenReturn(currentPrice);
            when(asset.calculateCurrentValue(currentPrice)).thenReturn(assetValue);

            List<Asset> assets = new ArrayList<>();
            assets.add(asset);

            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(2000), ValidatedCurrency.USD),
                assets,
                null
            );

            Money totalValue = account.calculateTotalValue(marketDataService);

            assertEquals(new BigDecimal("5000").setScale(Precision.getMoneyPrecision()), totalValue.amount());
            verify(marketDataService, times(1)).getCurrentPrice(identifier);
        }

        @Test
        @DisplayName("Should calculate total value with multiple assets")
        void shouldCalculateTotalValueWithMultipleAssets() {
            AssetIdentifier identifier1 = mock(AssetIdentifier.class);
            AssetIdentifier identifier2 = mock(AssetIdentifier.class);
            
            Asset asset1 = mock(Asset.class);
            Asset asset2 = mock(Asset.class);
            
            when(asset1.getAssetIdentifier()).thenReturn(identifier1);
            when(asset2.getAssetIdentifier()).thenReturn(identifier2);
            
            Money price1 = Money.of(BigDecimal.valueOf(100), ValidatedCurrency.USD);
            Money price2 = Money.of(BigDecimal.valueOf(50), ValidatedCurrency.USD);
            
            Money value1 = Money.of(BigDecimal.valueOf(2000), ValidatedCurrency.USD);
            Money value2 = Money.of(BigDecimal.valueOf(1000), ValidatedCurrency.USD);
            
            when(marketDataService.getCurrentPrice(identifier1)).thenReturn(price1);
            when(marketDataService.getCurrentPrice(identifier2)).thenReturn(price2);
            when(asset1.calculateCurrentValue(price1)).thenReturn(value1);
            when(asset2.calculateCurrentValue(price2)).thenReturn(value2);

            List<Asset> assets = new ArrayList<>();
            assets.add(asset1);
            assets.add(asset2);

            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(1500), ValidatedCurrency.USD),
                assets,
                null
            );

            Money totalValue = account.calculateTotalValue(marketDataService);

            // Cash: 1500 + Asset1: 2000 + Asset2: 1000 = 4500
            assertEquals(new BigDecimal("4500").setScale(Precision.getMoneyPrecision()), totalValue.amount());
        }
    }

    @Nested
    @DisplayName("Record Transaction Tests")
    class RecordTransactionTests {

        @Test
        @DisplayName("Should record DEPOSIT transaction")
        void shouldRecordDepositTransaction() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(1000), ValidatedCurrency.USD),
                null,
                null
            );

            TransactionId txId = mock(TransactionId.class);
            Transaction transaction = mock(Transaction.class);
            Money depositAmount = Money.of(BigDecimal.valueOf(500), ValidatedCurrency.USD);

            when(transaction.getTransactionId()).thenReturn(txId);
            when(transaction.getTransactionType()).thenReturn(TransactionType.DEPOSIT);
            when(transaction.getPricePerUnit()).thenReturn(depositAmount);
            when(transaction.getTransactionDate()).thenReturn(Instant.now());

            account.recordTransaction(transaction);

            assertEquals(1, account.getTransactions().size());
            assertEquals(new BigDecimal("1500").setScale(Precision.getMoneyPrecision()), account.getCashBalance().amount());
        }

        @Test
        @DisplayName("Should record WITHDRAWAL transaction")
        void shouldRecordWithdrawalTransaction() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(1000), ValidatedCurrency.USD),
                null,
                null
            );

            TransactionId txId = mock(TransactionId.class);
            Transaction transaction = mock(Transaction.class);
            Money withdrawAmount = Money.of(BigDecimal.valueOf(300), ValidatedCurrency.USD);

            when(transaction.getTransactionId()).thenReturn(txId);
            when(transaction.getTransactionType()).thenReturn(TransactionType.WITHDRAWAL);
            when(transaction.getPricePerUnit()).thenReturn(withdrawAmount);
            when(transaction.getTransactionDate()).thenReturn(Instant.now());

            account.recordTransaction(transaction);

            assertEquals(1, account.getTransactions().size());
            assertEquals(new BigDecimal("700").setScale(Precision.getMoneyPrecision()), account.getCashBalance().amount());
        }

        @Test
        @DisplayName("Should record BUY transaction")
        void shouldRecordBuyTransaction() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(10000), ValidatedCurrency.USD),
                null,
                null
            );

            TransactionId txId = mock(TransactionId.class);
            Transaction transaction = mock(Transaction.class);
            AssetIdentifier assetIdentifier = mock(AssetIdentifier.class);
            Money totalCost = Money.of(BigDecimal.valueOf(5000), ValidatedCurrency.USD);

            when(transaction.getTransactionId()).thenReturn(txId);
            when(transaction.getTransactionType()).thenReturn(TransactionType.BUY);
            when(transaction.getAssetIdentifier()).thenReturn(assetIdentifier);
            when(transaction.getQuantity()).thenReturn(BigDecimal.valueOf(100));
            when(transaction.calculateTotalCost()).thenReturn(totalCost);
            when(transaction.getTransactionDate()).thenReturn(Instant.now());
            when(assetIdentifier.getAssetType()).thenReturn(AssetType.STOCK);

            account.recordTransaction(transaction);

            assertEquals(1, account.getTransactions().size());
            assertEquals(1, account.getAssets().size());
            assertEquals(new BigDecimal("5000").setScale(Precision.getMoneyPrecision()), account.getCashBalance().amount());
        }

        @Test
        @DisplayName("Should record SELL transaction")
        void shouldRecordSellTransaction() {
            // First create an asset position
            AssetIdentifier assetIdentifier = mock(AssetIdentifier.class);
            Asset asset = mock(Asset.class);
            AssetId assetId = mock(AssetId.class);

            when(asset.getAssetId()).thenReturn(assetId);
            when(asset.getAssetIdentifier()).thenReturn(assetIdentifier);
            when(asset.getQuantity()).thenReturn(BigDecimal.valueOf(100));
            when(asset.getCostBasis()).thenReturn(Money.of(BigDecimal.valueOf(5000), ValidatedCurrency.USD));

            List<Asset> assets = new ArrayList<>();
            assets.add(asset);

            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(5000), ValidatedCurrency.USD),
                assets,
                null
            );

            TransactionId txId = mock(TransactionId.class);
            Transaction transaction = mock(Transaction.class);
            Money netProceeds = Money.of(BigDecimal.valueOf(6000), ValidatedCurrency.USD);

            when(transaction.getTransactionId()).thenReturn(txId);
            when(transaction.getTransactionType()).thenReturn(TransactionType.SELL);
            when(transaction.getAssetIdentifier()).thenReturn(assetIdentifier);
            when(transaction.getQuantity()).thenReturn(BigDecimal.valueOf(100));
            when(transaction.calculateNetAmount()).thenReturn(netProceeds);
            when(transaction.getTransactionDate()).thenReturn(Instant.now());

            account.recordTransaction(transaction);

            assertEquals(1, account.getTransactions().size());
            assertEquals(new BigDecimal("11000").setScale(Precision.getMoneyPrecision()), account.getCashBalance().amount());
        }

        @Test
        @DisplayName("Should record DIVIDEND transaction")
        void shouldRecordDividendTransaction() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(10000), ValidatedCurrency.USD),
                null,
                null
            );

            TransactionId txId = mock(TransactionId.class);
            Transaction transaction = mock(Transaction.class);
            Money dividendAmount = Money.of(BigDecimal.valueOf(250), ValidatedCurrency.USD);

            when(transaction.getTransactionId()).thenReturn(txId);
            when(transaction.getTransactionType()).thenReturn(TransactionType.DIVIDEND);
            when(transaction.getPricePerUnit()).thenReturn(dividendAmount);
            when(transaction.getTransactionDate()).thenReturn(Instant.now());

            account.recordTransaction(transaction);

            assertEquals(1, account.getTransactions().size());
            assertEquals(new BigDecimal("10250").setScale(Precision.getMoneyPrecision()), account.getCashBalance().amount());
        }

        @Test
        @DisplayName("Should record INTEREST transaction")
        void shouldRecordInterestTransaction() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(10000), ValidatedCurrency.USD),
                null,
                null
            );

            TransactionId txId = mock(TransactionId.class);
            Transaction transaction = mock(Transaction.class);
            Money interestAmount = Money.of(BigDecimal.valueOf(50), ValidatedCurrency.USD);

            when(transaction.getTransactionId()).thenReturn(txId);
            when(transaction.getTransactionType()).thenReturn(TransactionType.INTEREST);
            when(transaction.getPricePerUnit()).thenReturn(interestAmount);
            when(transaction.getTransactionDate()).thenReturn(Instant.now());

            account.recordTransaction(transaction);

            assertEquals(1, account.getTransactions().size());
            assertEquals(new BigDecimal("10050").setScale(Precision.getMoneyPrecision()), account.getCashBalance().amount());
        }

        @Test
        @DisplayName("Should record FEE transaction")
        void shouldRecordFeeTransaction() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(10000), ValidatedCurrency.USD),
                null,
                null
            );

            TransactionId txId = mock(TransactionId.class);
            Transaction transaction = mock(Transaction.class);
            Money feeAmount = Money.of(BigDecimal.valueOf(15), ValidatedCurrency.USD);

            when(transaction.getTransactionId()).thenReturn(txId);
            when(transaction.getTransactionType()).thenReturn(TransactionType.FEE);
            when(transaction.getPricePerUnit()).thenReturn(feeAmount);
            when(transaction.getTransactionDate()).thenReturn(Instant.now());

            account.recordTransaction(transaction);

            assertEquals(1, account.getTransactions().size());
            assertEquals(new BigDecimal("9985").setScale(Precision.getMoneyPrecision()), account.getCashBalance().amount());
        }

        @Test
        @DisplayName("Should record TRANSFER_IN cash transaction")
        void shouldRecordTransferInCashTransaction() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(5000), ValidatedCurrency.USD),
                null,
                null
            );

            TransactionId txId = mock(TransactionId.class);
            Transaction transaction = mock(Transaction.class);
            Money transferAmount = Money.of(BigDecimal.valueOf(2000), ValidatedCurrency.USD);

            when(transaction.getTransactionId()).thenReturn(txId);
            when(transaction.getTransactionType()).thenReturn(TransactionType.TRANSFER_IN);
            when(transaction.getAssetIdentifier()).thenReturn(null);
            when(transaction.getPricePerUnit()).thenReturn(transferAmount);
            when(transaction.getTransactionDate()).thenReturn(Instant.now());

            account.recordTransaction(transaction);

            assertEquals(1, account.getTransactions().size());
            assertEquals(new BigDecimal("7000").setScale(Precision.getMoneyPrecision()), account.getCashBalance().amount());
        }

        @Test
        @DisplayName("Should record TRANSFER_OUT cash transaction")
        void shouldRecordTransferOutCashTransaction() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(5000), ValidatedCurrency.USD),
                null,
                null
            );

            TransactionId txId = mock(TransactionId.class);
            Transaction transaction = mock(Transaction.class);
            Money transferAmount = Money.of(BigDecimal.valueOf(2000), ValidatedCurrency.USD);

            when(transaction.getTransactionId()).thenReturn(txId);
            when(transaction.getTransactionType()).thenReturn(TransactionType.TRANSFER_OUT);
            when(transaction.getAssetIdentifier()).thenReturn(null);
            when(transaction.getPricePerUnit()).thenReturn(transferAmount);
            when(transaction.getTransactionDate()).thenReturn(Instant.now());

            account.recordTransaction(transaction);

            assertEquals(1, account.getTransactions().size());
            assertEquals(new BigDecimal("3000").setScale(Precision.getMoneyPrecision()), account.getCashBalance().amount());
        }

        @Test
        @DisplayName("Should throw exception for null transaction")
        void shouldThrowExceptionForNullTransaction() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                initialCashBalance,
                null,
                null
            );

            assertThrows(NullPointerException.class, () ->
                account.recordTransaction(null)
            );
        }
    }

    @Nested
    @DisplayName("Apply Transaction Business Logic Tests")
    class ApplyTransactionBusinessLogicTests {

        @Test
        @DisplayName("Should accumulate quantity when buying same asset twice")
        void shouldAccumulateQuantityWhenBuyingSameAssetTwice() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(20000), ValidatedCurrency.USD),
                null,
                null
            );

            AssetIdentifier assetIdentifier = mock(AssetIdentifier.class);
            when(assetIdentifier.getAssetType()).thenReturn(AssetType.STOCK);

            // First buy
            TransactionId txId1 = mock(TransactionId.class);
            Transaction transaction1 = mock(Transaction.class);
            Money cost1 = Money.of(BigDecimal.valueOf(5000), ValidatedCurrency.USD);

            when(transaction1.getTransactionId()).thenReturn(txId1);
            when(transaction1.getTransactionType()).thenReturn(TransactionType.BUY);
            when(transaction1.getAssetIdentifier()).thenReturn(assetIdentifier);
            when(transaction1.getQuantity()).thenReturn(BigDecimal.valueOf(100));
            when(transaction1.calculateTotalCost()).thenReturn(cost1);
            when(transaction1.getTransactionDate()).thenReturn(Instant.now());

            account.recordTransaction(transaction1);

            assertEquals(1, account.getAssets().size());
            assertEquals(new BigDecimal("15000").setScale(Precision.getMoneyPrecision()), account.getCashBalance().amount());

            // Second buy
            TransactionId txId2 = mock(TransactionId.class);
            Transaction transaction2 = mock(Transaction.class);
            Money cost2 = Money.of(BigDecimal.valueOf(6000), ValidatedCurrency.USD);

            when(transaction2.getTransactionId()).thenReturn(txId2);
            when(transaction2.getTransactionType()).thenReturn(TransactionType.BUY);
            when(transaction2.getAssetIdentifier()).thenReturn(assetIdentifier);
            when(transaction2.getQuantity()).thenReturn(BigDecimal.valueOf(100));
            when(transaction2.calculateTotalCost()).thenReturn(cost2);
            when(transaction2.getTransactionDate()).thenReturn(Instant.now().plusSeconds(1));

            account.recordTransaction(transaction2);

            assertEquals(1, account.getAssets().size());
            assertEquals(new BigDecimal("9000").setScale(Precision.getMoneyPrecision()), account.getCashBalance().amount());
        }

                @Test
        @DisplayName("Should apply transfer in logic to account")
        void ShouldAddNewAssetFromTransfer() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(10000), ValidatedCurrency.USD),
                null,
                null
            );

            AssetIdentifier assetIdentifier = mock(AssetIdentifier.class);
            when(assetIdentifier.getPrimaryId()).thenReturn("AAPL");

            TransactionId txId = mock(TransactionId.class);
            Transaction transaction = mock(Transaction.class);

            when(transaction.getTransactionId()).thenReturn(txId);
            when(transaction.getTransactionType()).thenReturn(TransactionType.TRANSFER_IN);
            when(transaction.getAssetIdentifier()).thenReturn(assetIdentifier);
            when(transaction.getAssetIdentifier().getAssetType()).thenReturn(AssetType.ETF);
            when(transaction.getQuantity()).thenReturn(BigDecimal.valueOf(50));
            when(transaction.calculateNetAmount()).thenReturn(Money.of(2000, "USD"));
            when(transaction.calculateTotalCost()).thenReturn(Money.of(20, "USD"));
            when(transaction.getTransactionDate()).thenReturn(Instant.now());

            account.recordTransaction(transaction);

            assertEquals(1, account.getAssets().size());
        }

        @Test
        @DisplayName("Should apply transfer out logic to account")
        void ShouldRemoveAssetFromTransferOut() {
            Asset testAAPLAsset = mock(Asset.class);
            when(testAAPLAsset.getAssetIdentifier()).thenReturn(new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null));
            when(testAAPLAsset.getQuantity()).thenReturn(BigDecimal.valueOf(50));
            
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(10000), ValidatedCurrency.USD),
                List.of(testAAPLAsset),
                null
            );

            AssetIdentifier assetIdentifier = mock(AssetIdentifier.class);
            when(assetIdentifier.getPrimaryId()).thenReturn("AAPL");

            TransactionId txId = mock(TransactionId.class);
            Transaction transaction = mock(Transaction.class);

            when(transaction.getTransactionId()).thenReturn(txId);
            when(transaction.getTransactionType()).thenReturn(TransactionType.TRANSFER_OUT);
            when(transaction.getAssetIdentifier()).thenReturn(new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null));
            when(transaction.getQuantity()).thenReturn(BigDecimal.valueOf(50));
            when(transaction.calculateNetAmount()).thenReturn(Money.of(2000, "USD"));
            when(transaction.calculateTotalCost()).thenReturn(Money.of(20, "USD"));
            when(transaction.getTransactionDate()).thenReturn(Instant.now());

            account.recordTransaction(transaction);
            assertEquals(0, account.getAssets().size());

        }

        @Test
        @DisplayName("Should throw AssetNotFoundException when selling non-existent asset")
        void shouldThrowExceptionWhenSellingNonExistentAsset() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(10000), ValidatedCurrency.USD),
                null,
                null
            );

            AssetIdentifier assetIdentifier = mock(AssetIdentifier.class);
            when(assetIdentifier.getPrimaryId()).thenReturn("AAPL");

            TransactionId txId = mock(TransactionId.class);
            Transaction transaction = mock(Transaction.class);
            Money proceeds = Money.of(BigDecimal.valueOf(5000), ValidatedCurrency.USD);

            when(transaction.getTransactionId()).thenReturn(txId);
            when(transaction.getTransactionType()).thenReturn(TransactionType.SELL);
            when(transaction.getAssetIdentifier()).thenReturn(assetIdentifier);
            when(transaction.getQuantity()).thenReturn(BigDecimal.valueOf(50));
            when(transaction.calculateNetAmount()).thenReturn(proceeds);
            when(transaction.getTransactionDate()).thenReturn(Instant.now());

            assertThrows(AssetNotFoundException.class, () ->
                account.recordTransaction(transaction)
            );
        }

        @Test
        @DisplayName("Should throw UnsupportedTransactionTypeException when given a non-functional transaction type")
        void shouldThrowExceptionWhenApplyTransactionIsGivenNonStandardInput() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(10000), ValidatedCurrency.USD),
                null,
                null
            );

            AssetIdentifier assetIdentifier = mock(AssetIdentifier.class);
            when(assetIdentifier.getPrimaryId()).thenReturn("AAPL");

            TransactionId txId = mock(TransactionId.class);
            Transaction transaction = mock(Transaction.class);
            Money proceeds = Money.of(BigDecimal.valueOf(5000), ValidatedCurrency.USD);

            when(transaction.getTransactionId()).thenReturn(txId);
            when(transaction.getTransactionType()).thenReturn(TransactionType.OTHER);
            when(transaction.getAssetIdentifier()).thenReturn(assetIdentifier);
            when(transaction.getQuantity()).thenReturn(BigDecimal.valueOf(50));
            when(transaction.calculateNetAmount()).thenReturn(proceeds);
            when(transaction.getTransactionDate()).thenReturn(Instant.now());

            assertThrows(UnsupportedTransactionTypeException.class, () ->
                account.recordTransaction(transaction)
            );
        }


    }

    @Nested
    @DisplayName("Recalculate State Tests")
    class RecalculateStateTests {

        @Test
        @DisplayName("Should recalculate state when transaction is removed")
        void shouldRecalculateStateWhenTransactionRemoved() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(10000), ValidatedCurrency.USD),
                null,
                null
            );

            // Add deposit transaction
            TransactionId txId = mock(TransactionId.class);
            Transaction transaction = mock(Transaction.class);
            Money depositAmount = Money.of(BigDecimal.valueOf(5000), ValidatedCurrency.USD);

            when(transaction.getTransactionId()).thenReturn(txId);
            when(transaction.getTransactionType()).thenReturn(TransactionType.DEPOSIT);
            when(transaction.getPricePerUnit()).thenReturn(depositAmount);
            when(transaction.getTransactionDate()).thenReturn(Instant.now());

            account.recordTransaction(transaction);
            assertEquals(new BigDecimal("15000").setScale(Precision.getMoneyPrecision()), account.getCashBalance().amount());

            // Remove transaction - should recalculate
            account.removeTransaction(txId);
            assertEquals(new BigDecimal("0").setScale(Precision.getMoneyPrecision()), account.getCashBalance().amount());
        }

        @Test
        @DisplayName("Should recalculate state when transaction is updated")
        void shouldRecalculateStateWhenTransactionUpdated() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(10000), ValidatedCurrency.USD),
                null,
                null
            );

            // Original transaction
            TransactionId txId = mock(TransactionId.class);
            Transaction originalTransaction = mock(Transaction.class);
            Money originalAmount = Money.of(BigDecimal.valueOf(5000), ValidatedCurrency.USD);

            when(originalTransaction.getTransactionId()).thenReturn(txId);
            when(originalTransaction.getTransactionType()).thenReturn(TransactionType.DEPOSIT);
            when(originalTransaction.getPricePerUnit()).thenReturn(originalAmount);
            when(originalTransaction.getTransactionDate()).thenReturn(Instant.now());

            account.recordTransaction(originalTransaction);
            assertEquals(new BigDecimal("15000").setScale(Precision.getMoneyPrecision()), account.getCashBalance().amount());

            // Updated transaction with different amount
            Transaction updatedTransaction = mock(Transaction.class);
            Money updatedAmount = Money.of(BigDecimal.valueOf(3000), ValidatedCurrency.USD);

            when(updatedTransaction.getTransactionId()).thenReturn(txId);
            when(updatedTransaction.getTransactionType()).thenReturn(TransactionType.DEPOSIT);
            when(updatedTransaction.getPricePerUnit()).thenReturn(updatedAmount);
            when(updatedTransaction.getTransactionDate()).thenReturn(Instant.now());

            account.updateTransaction(txId, updatedTransaction);
            assertEquals(new BigDecimal("3000").setScale(Precision.getMoneyPrecision()), account.getCashBalance().amount());
        }
    }

    @Nested
    @DisplayName("Version and Metadata Tests")
    class VersionAndMetadataTests {

        @Test
        @DisplayName("Should increment version on mutations")
        void shouldIncrementVersionOnMutations() {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(10000), ValidatedCurrency.USD),
                null,
                null
            );

            int initialVersion = account.getVersion();

            account.deposit(Money.of(BigDecimal.valueOf(500), ValidatedCurrency.USD));
            assertEquals(initialVersion + 1, account.getVersion());

            account.withdraw(Money.of(BigDecimal.valueOf(200), ValidatedCurrency.USD));
            assertEquals(initialVersion + 2, account.getVersion());

            Asset asset = mock(Asset.class);
            AssetIdentifier identifier = mock(AssetIdentifier.class);
            when(identifier.getPrimaryId()).thenReturn("AAPL");
            when(asset.getAssetIdentifier()).thenReturn(identifier);

            account.addAsset(asset);
            assertEquals(initialVersion + 3, account.getVersion());
        }

        @Test
        @DisplayName("Should update lastSystemInteraction on mutations")
        void shouldUpdateLastSystemInteractionOnMutations() throws InterruptedException {
            Account account = new Account(
                accountId,
                accountName,
                accountType,
                baseCurrency,
                Money.of(BigDecimal.valueOf(10000), ValidatedCurrency.USD),
                null,
                null
            );

            Instant firstInteraction = account.getLastSystemInteraction();
            
            Thread.sleep(10);
            
            account.deposit(Money.of(BigDecimal.valueOf(500), ValidatedCurrency.USD));
            Instant secondInteraction = account.getLastSystemInteraction();
            
            assertTrue(secondInteraction.isAfter(firstInteraction) || 
                       secondInteraction.equals(firstInteraction));
        }
    }
}