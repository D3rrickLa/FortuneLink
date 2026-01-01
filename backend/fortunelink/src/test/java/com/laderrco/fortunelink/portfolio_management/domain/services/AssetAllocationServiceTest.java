package com.laderrco.fortunelink.portfolio_management.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

@ExtendWith(MockitoExtension.class)
public class AssetAllocationServiceTest {
    @Mock
    private PortfolioValuationService valuationService;

    @Mock
    private MarketDataService marketDataService;

    @InjectMocks
    private AssetAllocationService assetAllocationService;

    private Instant time;

    @BeforeEach
    void setUp() {
        time = Instant.now();
    }

    @Nested
    @DisplayName("calculateAllocationByType Tests")
    class CalculateAllocationByTypeTests {

        @Test
        @DisplayName("Should return empty map when portfolio value is zero")
        void shouldReturnEmptyMapWhenPortfolioValueIsZero() {
            // Arrange
            Portfolio portfolio = createMockPortfolio();
            Money zeroMoney = Money.of(BigDecimal.ZERO, ValidatedCurrency.USD);

            lenient()
                    .when(valuationService.calculateAssetValue(any(Asset.class), any(ValidatedCurrency.class),
                            eq(time)))
                    .thenReturn(zeroMoney);

            // Act
            Map<AssetType, Money> result = assetAllocationService
                    .calculateAllocationByType(portfolio, time); // this as well

            // Assert
            assertTrue(result.isEmpty());
            // NOT NEEDED ANYMORE
            // verify(valuationService).calculateTotalValue(portfolio, marketDataService,
            // time);
        }

        @Test
        @DisplayName("Should calculate correct allocation for single asset type")
        void shouldCalculateCorrectAllocationForSingleAssetType() {
            // Arrange
            Portfolio portfolio = createPortfolioWithSingleAssetType();
            // Ensure this matches what your helper actually puts in the portfolio!
            ValidatedCurrency usd = ValidatedCurrency.USD;
            Money stockValue = Money.of(new BigDecimal("10000.00"), usd);

            // Use any() for the currency and time to avoid precision/null mismatches
            when(valuationService.calculateAssetValue(any(Asset.class), any(), any()))
                    .thenReturn(stockValue);

            // Act
            Map<AssetType, Money> result = assetAllocationService.calculateAllocationByType(portfolio, time);

            // Assert
            assertNotNull(result);
            assertEquals(stockValue, result.get(AssetType.STOCK));
        }

        @Test
        @DisplayName("Service should group values by AssetType correctly")
        void serviceShouldGroupByAssetType() {
            // Act
            Portfolio portfolio = createPortfolioWithSingleAssetType();
            Money stockValue = Money.of(new BigDecimal("10000.00"), ValidatedCurrency.USD);
            when(valuationService.calculateAssetValue(any(Asset.class), any(), any()))
                    .thenReturn(stockValue);
            Map<AssetType, Money> result = assetAllocationService
                    .calculateAllocationByType(portfolio, time);

            // Assert
            assertEquals(Money.of(10000, "USD"), result.get(AssetType.STOCK));
        }

        @Test
        @DisplayName("Should calculate correct allocation for multiple asset types")
        void shouldCalculateCorrectAllocationForMultipleAssetTypes() {
            // 1. Arrange - Use a real Portfolio object (not a mock) to avoid stubbing
            // confusion
            Portfolio portfolio = Portfolio.createNew(UserId.randomId(), ValidatedCurrency.USD);

            Asset stockAsset = createAsset(AssetType.STOCK);
            Asset etfAsset = createAsset(AssetType.ETF);
            Asset cryptoAsset = createAsset(AssetType.CRYPTO);

            Account tfsaAccount = createAccount(AccountType.TFSA, List.of(stockAsset, etfAsset, cryptoAsset));
            portfolio.addAccount(tfsaAccount);

            // 2. Stubbing - Use any() for Currency if you want to be safe, or ensure it's
            // USD
            // Using eq(ValidatedCurrency.USD) only works if
            // portfolio.getPortfolioCurrencyPreference() is USD
            when(valuationService.calculateAssetValue(eq(stockAsset), any(), eq(time)))
                    .thenReturn(Money.of(new BigDecimal("6000.00"), ValidatedCurrency.USD));

            when(valuationService.calculateAssetValue(eq(etfAsset), any(), eq(time)))
                    .thenReturn(Money.of(new BigDecimal("3000.00"), ValidatedCurrency.USD));

            when(valuationService.calculateAssetValue(eq(cryptoAsset), any(), eq(time)))
                    .thenReturn(Money.of(new BigDecimal("1000.00"), ValidatedCurrency.USD));

            // 3. Act
            Map<AssetType, Money> result = assetAllocationService.calculateAllocationByType(portfolio, time);

            // 4. Assert
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals(new BigDecimal("6000.00"), result.get(AssetType.STOCK).amount().setScale(2));
            assertEquals(new BigDecimal("3000.00"), result.get(AssetType.ETF).amount().setScale(2));
            assertEquals(new BigDecimal("1000.00"), result.get(AssetType.CRYPTO).amount().setScale(2));
        }

        @Test
        @DisplayName("Should aggregate multiple assets of same type correctly")
        void shouldAggregateMultipleAssetsOfSameType() {
            // Arrange
            Portfolio portfolio = createPortfolioWithMultipleAssetsOfSameType();

            when(valuationService.calculateAssetValue(any(Asset.class), any(), eq(time)))
                    .thenReturn(Money.of(new BigDecimal("2500.00"), ValidatedCurrency.USD));

            // Act
            Map<AssetType, Money> result = assetAllocationService
                    .calculateAllocationByType(portfolio, time);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertTrue(result.containsKey(AssetType.STOCK));
            // // 4 assets * 2500 = 10000, so 100%
            // assertEquals(new
            // BigDecimal("100.00").setScale(Precision.PERCENTAGE.getDecimalPlaces()),
            // result.get(AssetType.STOCK).toPercentage());
            assertEquals(Money.of(10000, "USD"), result.get(AssetType.STOCK));
        }

        @Test
        @DisplayName("Should handle portfolio with empty accounts")
        void shouldHandlePortfolioWithEmptyAccounts() {
            // Arrange
            Portfolio portfolio = createEmptyPortfolio();

            // Act
            Map<AssetType, Money> result = assetAllocationService.calculateAllocationByType(portfolio, time);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should exclude assets with zero or negative value from allocation map")
        void shouldExcludeNonPositiveValuesFromAllocation() {
            // 1. Arrange
            Portfolio portfolio = createPortfolioWithMultipleAssetTypes();
            // This helper likely adds STOCK, ETF, CRYPTO assets

            ValidatedCurrency usd = ValidatedCurrency.USD;
            // Ensure the portfolio has a currency preference to avoid the null mismatch
            // error
            when(portfolio.getPortfolioCurrencyPreference()).thenReturn(usd);

            // Mock an asset with ZERO value
            Money zeroValue = Money.of(BigDecimal.ZERO, usd);
            // Mock an asset with NEGATIVE value (e.g., a short position or data error)
            Money negativeValue = Money.of(new BigDecimal("-100.00"), usd);

            // Stub all calls to return non-positive values
            // We use any(Asset.class) to cover all assets in the portfolio
            when(valuationService.calculateAssetValue(any(Asset.class), eq(usd), eq(time)))
                    .thenReturn(zeroValue) // First call returns 0
                    .thenReturn(negativeValue) // Second call returns -100
                    .thenReturn(zeroValue); // Third call returns 0

            // 2. Act
            Map<AssetType, Money> result = assetAllocationService.calculateAllocationByType(portfolio, time);

            // 3. Assert
            // Because 0 is not > 0 and -100 is not > 0, nothing should be merged into the
            // map
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Map should be empty when all asset values are zero or negative");

            // Optional: Verify the valuation service was actually called for the assets
            verify(valuationService, atLeastOnce()).calculateAssetValue(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("calculateAllocationByAccount Tests")
    class CalculateAllocationByAccountTests {

        @Test
        @DisplayName("Should return empty map when portfolio has no accounts")
        void shouldReturnEmptyMapWhenPortfolioIsEmpty() {
            // Arrange - Use a real object to avoid stubbing getAccounts()
            Portfolio portfolio = Portfolio.createNew(UserId.randomId(), ValidatedCurrency.USD);

            // Act
            Map<AccountType, Money> result = assetAllocationService.calculateAllocationByAccount(portfolio, time);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should calculate correct allocation for multiple account types")
        void shouldCalculateCorrectAllocationForMultipleAccountTypes() {
            // Arrange
            Portfolio portfolio = mock(Portfolio.class);
            // CRITICAL: Stub the currency preference
            when(portfolio.getPortfolioCurrencyPreference()).thenReturn(ValidatedCurrency.USD);

            // Create specific mocks for this test to ensure identity matching
            Account tfsa = createAccount(AccountType.TFSA);
            Account rrsp = createAccount(AccountType.RRSP);

            when(portfolio.getAccounts()).thenReturn(List.of(tfsa, rrsp));

            Money tfsaVal = Money.of(new BigDecimal("40000.00"), ValidatedCurrency.USD);
            Money rrspVal = Money.of(new BigDecimal("35000.00"), ValidatedCurrency.USD);

            // Use any() for the currency to be safe against nulls/mismatches
            when(valuationService.calculateAccountValue(eq(tfsa), any(), eq(time))).thenReturn(tfsaVal);
            when(valuationService.calculateAccountValue(eq(rrsp), any(), eq(time))).thenReturn(rrspVal);

            // Act
            Map<AccountType, Money> result = assetAllocationService.calculateAllocationByAccount(portfolio, time);

            // Assert
            assertEquals(2, result.size());
            assertEquals(tfsaVal, result.get(AccountType.TFSA));
            assertEquals(rrspVal, result.get(AccountType.RRSP));
        }

        @Test
        @DisplayName("Should aggregate multiple accounts of same type")
        void shouldAggregateMultipleAccountsOfSameType() {
            // Arrange
            Portfolio portfolio = mock(Portfolio.class);
            when(portfolio.getPortfolioCurrencyPreference()).thenReturn(ValidatedCurrency.USD);

            // Two different account instances, same type
            Account tfsa1 = createAccount(AccountType.TFSA);
            Account tfsa2 = createAccount(AccountType.TFSA);

            when(portfolio.getAccounts()).thenReturn(List.of(tfsa1, tfsa2));

            Money val1 = Money.of(new BigDecimal("25000.00"), ValidatedCurrency.USD);
            Money val2 = Money.of(new BigDecimal("25000.00"), ValidatedCurrency.USD);

            when(valuationService.calculateAccountValue(eq(tfsa1), any(), eq(time))).thenReturn(val1);
            when(valuationService.calculateAccountValue(eq(tfsa2), any(), eq(time))).thenReturn(val2);

            // Act
            Map<AccountType, Money> result = assetAllocationService.calculateAllocationByAccount(portfolio, time);

            // Assert
            assertEquals(1, result.size());
            // 25000 + 25000 = 50000
            assertEquals(new BigDecimal("50000.00"), result.get(AccountType.TFSA).amount().setScale(2));
        }
    }

    @Nested
    @DisplayName("calculateAllocationByCurrency Tests")
    class CalculateAllocationByCurrencyTests {

        @Test
        @DisplayName("Should return empty map when portfolio value is zero")
        void shouldReturnEmptyMapWhenPortfolioValueIsZero() {
            // Arrange
            Portfolio portfolio = createMockPortfolio();

            // Act
            Map<ValidatedCurrency, Money> result = assetAllocationService
                    .calculateAllocationByCurrency(portfolio, time);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should calculate correct allocation for single currency")
        void shouldCalculateCorrectAllocationForSingleCurrency() {
            // Arrange
            Portfolio portfolio = createPortfolioWithSingleCurrency();
            Account account = portfolio.getAccounts().get(0); // Get the mock from your helper

            Money expectedValue = Money.of(new BigDecimal("75000.00"), ValidatedCurrency.USD);

            // Stub the CASH instead of the AccountValue
            when(account.getCashBalance()).thenReturn(expectedValue);
            when(account.getAssets()).thenReturn(List.of()); // Ensure no assets to avoid NPEs

            // Act
            Map<ValidatedCurrency, Money> result = assetAllocationService
                    .calculateAllocationByCurrency(portfolio, time);

            // Assert
            assertEquals(1, result.size());
            assertEquals(expectedValue, result.get(ValidatedCurrency.USD));
        }

        @Test
        @DisplayName("Should calculate correct allocation for multiple currencies")
        void shouldCalculateCorrectAllocationForMultipleCurrencies() {
            // 1. Arrange
            Portfolio portfolio = mock(Portfolio.class);
            Account account = mock(Account.class);

            // Create assets with different identifiers
            Asset usdStock = createAsset(AssetType.STOCK); // Should return a mock asset
            Asset cadStock = createAsset(AssetType.STOCK);

            // Mock Cash Balance (Crucial because your code now processes cash)
            when(account.getCashBalance()).thenReturn(Money.ZERO("USD"));
            when(account.getAssets()).thenReturn(List.of(usdStock, cadStock));
            when(portfolio.getAccounts()).thenReturn(List.of(account));

            // 2. Mock MarketDataService for Native Currency
            when(marketDataService.getTradingCurrency(usdStock.getAssetIdentifier()))
                    .thenReturn(ValidatedCurrency.USD);
            when(marketDataService.getTradingCurrency(cadStock.getAssetIdentifier()))
                    .thenReturn(ValidatedCurrency.CAD);

            // 3. Mock ValuationService for Asset Values
            Money usdVal = Money.of(new BigDecimal("500.00"), ValidatedCurrency.USD);
            Money cadVal = Money.of(new BigDecimal("700.00"), ValidatedCurrency.CAD);

            when(valuationService.calculateAssetValue(eq(usdStock), eq(ValidatedCurrency.USD), eq(time)))
                    .thenReturn(usdVal);
            when(valuationService.calculateAssetValue(eq(cadStock), eq(ValidatedCurrency.CAD), eq(time)))
                    .thenReturn(cadVal);

            // 4. Act
            Map<ValidatedCurrency, Money> result = assetAllocationService.calculateAllocationByCurrency(portfolio,
                    time);

            // 5. Assert
            assertEquals(2, result.size());
            assertEquals(usdVal, result.get(ValidatedCurrency.USD));
            assertEquals(cadVal, result.get(ValidatedCurrency.CAD));
        }

        @Test
        @DisplayName("Should aggregate multiple accounts with same currency")
        void shouldAggregateMultipleAccountsWithSameCurrency() {
            // 1. Arrange
            Portfolio portfolio = createPortfolioWithMultipleAccountsSameCurrency();

            // We expect 100,000 total (4 accounts x 25,000 each)
            Money expectedTotal = Money.of(new BigDecimal("100000.00"), ValidatedCurrency.USD);
            Money perAccountCash = Money.of(new BigDecimal("25000.00"), ValidatedCurrency.USD);

            // 2. Stub the accounts to return the 25k as CASH (since that's the easiest
            // path)
            for (Account account : portfolio.getAccounts()) {
                // The code calls getCashBalance() first
                when(account.getCashBalance()).thenReturn(perAccountCash);
                // And it loops through assets, so return an empty list to avoid crashes
                when(account.getAssets()).thenReturn(List.of());
            }

            // 3. Act
            Map<ValidatedCurrency, Money> result = assetAllocationService
                    .calculateAllocationByCurrency(portfolio, time);

            // 4. Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            // This will now be 25,000 + 25,000 + 25,000 + 25,000 = 100,000
            assertEquals(expectedTotal, result.get(ValidatedCurrency.USD));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null portfolio gracefully")
        void shouldHandleNullPortfolio() {
            // Act & Assert
            assertThrows(NullPointerException.class, () -> {
                assetAllocationService.calculateAllocationByType(null, time);
            });
        }

        @Test
        @DisplayName("Should handle null as of date service gracefully")
        void shouldHandleNullAsOfDateService() {
            // Arrange
            Portfolio portfolio = createMockPortfolio();

            // Act & Assert
            assertThrows(NullPointerException.class, () -> {
                assetAllocationService.calculateAllocationByType(portfolio, null);
            });
        }

        @Test
        @DisplayName("Should handle very small allocation percentages")
        void shouldHandleVerySmallAllocationPercentages() {
            // Arrange
            Portfolio portfolio = createPortfolioWithVerySmallAllocations();

            when(valuationService.calculateAssetValue(any(Asset.class), any(), eq(time)))
                    .thenReturn(Money.of(new BigDecimal("1.00"), ValidatedCurrency.USD));

            // Act
            Map<AssetType, Money> result = assetAllocationService.calculateAllocationByType(portfolio, time);

            // Assert
            assertNotNull(result);
            assertFalse(result.isEmpty());
            // Verify precision is maintained for small percentages
            assertTrue(result.values().stream()
                    .allMatch(p -> p.amount().compareTo(BigDecimal.ZERO) > 0));
        }
    }

    // Helper methods for creating test data
    private Portfolio createMockPortfolio() {
        return mock(Portfolio.class);
    }

    private Portfolio createEmptyPortfolio() {
        Portfolio portfolio = mock(Portfolio.class);
        when(portfolio.getAccounts()).thenReturn(List.of());
        return portfolio;
    }

    private Portfolio createPortfolioWithSingleAssetType() {
        Portfolio portfolio = mock(Portfolio.class);
        Account account = createAccount(AccountType.TFSA);
        Asset asset = createAsset(AssetType.STOCK);
        when(account.getAssets()).thenReturn(List.of(asset));
        when(portfolio.getAccounts()).thenReturn(List.of(account));
        return portfolio;
    }

    private Portfolio createPortfolioWithMultipleAssetTypes() {
        Portfolio portfolio = mock(Portfolio.class);
        Account account = createAccount(AccountType.TFSA);
        Asset stockAsset = createAsset(AssetType.STOCK);
        Asset etfAsset = createAsset(AssetType.ETF);
        Asset cryptoAsset = createAsset(AssetType.CRYPTO);
        when(account.getAssets()).thenReturn(List.of(stockAsset, etfAsset, cryptoAsset));
        when(portfolio.getAccounts()).thenReturn(List.of(account));
        return portfolio;
    }

    private Portfolio createPortfolioWithMultipleAssetsOfSameType() {
        Portfolio portfolio = mock(Portfolio.class);
        Account account = createAccount(AccountType.TFSA);
        Asset asset1 = createAsset(AssetType.STOCK);
        Asset asset2 = createAsset(AssetType.STOCK);
        Asset asset3 = createAsset(AssetType.STOCK);
        Asset asset4 = createAsset(AssetType.STOCK);
        when(account.getAssets()).thenReturn(List.of(asset1, asset2, asset3, asset4));
        when(portfolio.getAccounts()).thenReturn(List.of(account));
        return portfolio;
    }

    // private Portfolio createPortfolioWithSingleAccount() {
    //     Portfolio portfolio = mock(Portfolio.class);
    //     Account account = createAccount(AccountType.TFSA);
    //     when(portfolio.getAccounts()).thenReturn(List.of(account));
    //     return portfolio;
    // }

    // private Portfolio createPortfolioWithMultipleAccounts() {
    //     Portfolio portfolio = mock(Portfolio.class);
    //     Account tfsaAccount = createAccount(AccountType.TFSA);
    //     Account rrspAccount = createAccount(AccountType.RRSP);
    //     Account nonRegAccount = createAccount(AccountType.NON_REGISTERED);
    //     when(portfolio.getAccounts()).thenReturn(List.of(tfsaAccount, rrspAccount, nonRegAccount));
    //     return portfolio;
    // }

    // private Portfolio createPortfolioWithMultipleAccountsOfSameType() {
    //     Portfolio portfolio = mock(Portfolio.class);
    //     Account account1 = createAccount(AccountType.TFSA);
    //     Account account2 = createAccount(AccountType.TFSA);
    //     Account account3 = createAccount(AccountType.TFSA);
    //     Account account4 = createAccount(AccountType.TFSA);
    //     when(portfolio.getAccounts()).thenReturn(List.of(account1, account2, account3, account4));
    //     return portfolio;
    // }

    private Portfolio createPortfolioWithSingleCurrency() {
        Portfolio portfolio = mock(Portfolio.class);
        Account account = createAccountWithCurrency(ValidatedCurrency.USD);
        when(portfolio.getAccounts()).thenReturn(List.of(account));
        return portfolio;
    }

    // private Portfolio createPortfolioWithMultipleCurrencies() {
    //     Portfolio portfolio = mock(Portfolio.class);
    //     Account usdAccount = createAccountWithCurrency(ValidatedCurrency.USD);
    //     Account cadAccount = createAccountWithCurrency(ValidatedCurrency.CAD);
    //     Account eurAccount = createAccountWithCurrency(ValidatedCurrency.EUR);
    //     when(portfolio.getAccounts()).thenReturn(List.of(usdAccount, cadAccount, eurAccount));
    //     return portfolio;
    // }

    private Portfolio createPortfolioWithMultipleAccountsSameCurrency() {
        Portfolio portfolio = mock(Portfolio.class);
        Account account1 = createAccountWithCurrency(ValidatedCurrency.USD);
        Account account2 = createAccountWithCurrency(ValidatedCurrency.USD);
        Account account3 = createAccountWithCurrency(ValidatedCurrency.USD);
        Account account4 = createAccountWithCurrency(ValidatedCurrency.USD);
        when(portfolio.getAccounts()).thenReturn(List.of(account1, account2, account3, account4));
        return portfolio;
    }

    private Portfolio createPortfolioWithVerySmallAllocations() {
        Portfolio portfolio = mock(Portfolio.class);
        Account account = createAccount(AccountType.TFSA);
        Asset asset = createAsset(AssetType.CRYPTO);
        when(account.getAssets()).thenReturn(List.of(asset));
        when(portfolio.getAccounts()).thenReturn(List.of(account));
        return portfolio;
    }

    private Account createAccount(AccountType type) {
        Account account = mock(Account.class);
        lenient().when(account.getAccountType()).thenReturn(type);
        lenient().when(account.getBaseCurrency()).thenReturn(ValidatedCurrency.USD);
        return account;
    }

    private Account createAccount(AccountType type, List<Asset> assets) {
        Account account = mock(Account.class);
        lenient().when(account.getAccountType()).thenReturn(type);
        lenient().when(account.getBaseCurrency()).thenReturn(ValidatedCurrency.USD);
        lenient().when(account.getAssets()).thenReturn(assets);
        return account;
    }

    private Account createAccountWithCurrency(ValidatedCurrency ValidatedCurrency) {
        Account account = mock(Account.class);
        lenient().when(account.getBaseCurrency()).thenReturn(ValidatedCurrency);
        // CRITICAL: Cash must not be null!
        lenient().when(account.getCashBalance()).thenReturn(Money.of(BigDecimal.ZERO, ValidatedCurrency));
        // CRITICAL: Assets must not be null!
        lenient().when(account.getAssets()).thenReturn(List.of());
        return account;
    }

    private Asset createAsset(AssetType type) {
        Asset asset = mock(Asset.class);
        AssetIdentifier assetIdentifier = mock(AssetIdentifier.class);
        lenient().when(asset.getAssetIdentifier()).thenReturn(assetIdentifier);
        lenient().when(assetIdentifier.getAssetType()).thenReturn(type);
        return asset;
    }
}
