package com.laderrco.fortunelink.portfolio_management.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class AssetAllocationServiceTest {
    @Mock
    private PortfolioValuationService valuationService;
    
    @Mock
    private MarketDataService marketDataService;
    
    private AssetAllocationService assetAllocationService;

    private Instant time;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        assetAllocationService = new AssetAllocationService(valuationService);
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
            
            when(valuationService.calculateTotalValue(portfolio, marketDataService, time))
                .thenReturn(zeroMoney);
            
            // Act
            Map<AssetType, Money> result = assetAllocationService
                .calculateAllocationByType(portfolio, marketDataService, time); // this as well
            
            // Assert
            assertTrue(result.isEmpty());
            // NOT NEEDED ANYMORE
            // verify(valuationService).calculateTotalValue(portfolio, marketDataService, time);
        }
        
        @Test
        @DisplayName("Should calculate correct allocation for single asset type")
        void shouldCalculateCorrectAllocationForSingleAssetType() {
            // Arrange
            Portfolio portfolio = createPortfolioWithSingleAssetType();
            Money totalValue = Money.of(new BigDecimal("10000.00"), ValidatedCurrency.USD);
            Money stockValue = Money.of(new BigDecimal("10000.00"), ValidatedCurrency.USD);
            
            when(valuationService.calculateTotalValue(portfolio, marketDataService, time))
                .thenReturn(totalValue);
            when(valuationService.calculateAssetValue(any(Asset.class), eq(marketDataService), eq(time)))
                .thenReturn(stockValue);
            
            // Act
            Map<AssetType, Money> result = assetAllocationService
                .calculateAllocationByType(portfolio, marketDataService, time);
            
            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertTrue(result.containsKey(AssetType.STOCK));
            
            // Change: Assert the MONEY value, not the percentage
            assertEquals(stockValue, result.get(AssetType.STOCK));
        }

        @Test
        @DisplayName("Service should group values by AssetType correctly")
        void serviceShouldGroupByAssetType() {
            // Act
            Portfolio portfolio = createPortfolioWithSingleAssetType();
            Money totalValue = Money.of(new BigDecimal("10000.00"), ValidatedCurrency.USD);
            Money stockValue = Money.of(new BigDecimal("10000.00"), ValidatedCurrency.USD);
            when(valuationService.calculateTotalValue(portfolio, marketDataService, time))
                .thenReturn(totalValue);
            when(valuationService.calculateAssetValue(any(Asset.class), eq(marketDataService), eq(time)))
                .thenReturn(stockValue);
            Map<AssetType, Money> result = assetAllocationService
                .calculateAllocationByType(portfolio, marketDataService, time);
            
            // Assert
            assertEquals(Money.of(10000,"USD"), result.get(AssetType.STOCK));
        }
        
        
        @Test
        @DisplayName("Should calculate correct allocation for multiple asset types")
        void shouldCalculateCorrectAllocationForMultipleAssetTypes() {
            // Arrange
            Portfolio portfolio = createPortfolioWithMultipleAssetTypes();
            Money totalValue = Money.of(new BigDecimal("10000.00"), ValidatedCurrency.USD);
            
            Asset stockAsset = createAsset(AssetType.STOCK);
            Asset etfAsset = createAsset(AssetType.ETF);
            Asset cryptoAsset = createAsset(AssetType.CRYPTO);
            
            Account tfsaAccount = createAccount(AccountType.TFSA, List.of(stockAsset, etfAsset, cryptoAsset));
            portfolio.addAccount(tfsaAccount);



            when(valuationService.calculateTotalValue(portfolio, marketDataService, time))
                .thenReturn(totalValue);
            when(valuationService.calculateAssetValue(stockAsset, marketDataService,time))
                .thenReturn(Money.of(new BigDecimal("6000.00"), ValidatedCurrency.USD));
            when(valuationService.calculateAssetValue(etfAsset, marketDataService, time))
                .thenReturn(Money.of(new BigDecimal("3000.00"), ValidatedCurrency.USD));
            when(valuationService.calculateAssetValue(cryptoAsset, marketDataService, time))
                .thenReturn(Money.of(new BigDecimal("1000.00"), ValidatedCurrency.USD));
            
            // Act
            when(portfolio.getAccounts()).thenReturn(List.of(tfsaAccount));
            Map<AssetType, Money> result = assetAllocationService
                .calculateAllocationByType(portfolio, marketDataService, time);
            
            // Assert
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals(Money.of(6000, "USD"), result.get(AssetType.STOCK));
            assertEquals(Money.of(3000, "USD"), result.get(AssetType.ETF));
            assertEquals(Money.of(1000, "USD"), result.get(AssetType.CRYPTO));
        }
        
        @Test
        @DisplayName("Should aggregate multiple assets of same type correctly")
        void shouldAggregateMultipleAssetsOfSameType() {
            // Arrange
            Portfolio portfolio = createPortfolioWithMultipleAssetsOfSameType();
            Money totalValue = Money.of(new BigDecimal("10000.00"), ValidatedCurrency.USD);
            
            when(valuationService.calculateTotalValue(portfolio, marketDataService, time))
                .thenReturn(totalValue);
            when(valuationService.calculateAssetValue(any(Asset.class), eq(marketDataService), any(Instant.class)))
                .thenReturn(Money.of(new BigDecimal("2500.00"), ValidatedCurrency.USD));
            
            // Act
            Map<AssetType, Money> result = assetAllocationService
                .calculateAllocationByType(portfolio, marketDataService, time);
            
            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertTrue(result.containsKey(AssetType.STOCK));
            // // 4 assets * 2500 = 10000, so 100%
            // assertEquals(new BigDecimal("100.00").setScale(Precision.PERCENTAGE.getDecimalPlaces()), result.get(AssetType.STOCK).toPercentage());
            assertEquals(Money.of(10000, "USD"), result.get(AssetType.STOCK));
        }
        
        @Test
        @DisplayName("Should handle portfolio with empty accounts")
        void shouldHandlePortfolioWithEmptyAccounts() {
            // Arrange
            Portfolio portfolio = createEmptyPortfolio();
            Money totalValue = Money.of(BigDecimal.ZERO, ValidatedCurrency.USD);
            
            when(valuationService.calculateTotalValue(portfolio, marketDataService, time))
                .thenReturn(totalValue);
            
            // Act
            Map<AssetType, Money> result = assetAllocationService.calculateAllocationByType(portfolio, marketDataService, time); // this as well
            
            // Assert
            assertTrue(result.isEmpty());
        }
    }
    
    @Nested
    @DisplayName("calculateAllocationByAccount Tests")
    class CalculateAllocationByAccountTests {
        
        @Test
        @DisplayName("Should return empty map when portfolio value is zero")
        void shouldReturnEmptyMapWhenPortfolioValueIsZero() {
            // Arrange
            Portfolio portfolio = createMockPortfolio();
            Money zeroMoney = Money.of(BigDecimal.ZERO, ValidatedCurrency.USD);
            
            when(valuationService.calculateTotalValue(portfolio, marketDataService, time))
                .thenReturn(zeroMoney);
            
            // Act
            Map<AccountType, Money> result = assetAllocationService.calculateAllocationByAccount(portfolio, marketDataService, time);
            
            // Assert
            assertTrue(result.isEmpty());
        }
        
        @Test
        @DisplayName("Should calculate correct allocation for single account type")
        void shouldCalculateCorrectAllocationForSingleAccountType() {
            // Arrange
            Portfolio portfolio = createPortfolioWithSingleAccount();
            // Use your fixed 'time' variable instead of Instant.now()
            Instant testTime = time; 
            
            Money accountValue = Money.of(new BigDecimal("50000.00"), ValidatedCurrency.USD);
            
            // We don't need to mock calculateTotalValue anymore since the Service 
            // no longer uses it to calculate percentages!
            
            when(valuationService.calculateAccountValue(any(Account.class), eq(marketDataService), eq(testTime)))
                .thenReturn(accountValue);
            
            // Act
            Map<AccountType, Money> result = assetAllocationService
                .calculateAllocationByAccount(portfolio, marketDataService, testTime);
            
            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertTrue(result.containsKey(AccountType.TFSA));
            
            // Assert the MONEY value
            assertEquals(accountValue, result.get(AccountType.TFSA));
        }
        
        @Test
        @DisplayName("Should calculate correct monetary allocation for multiple account types")
        void shouldCalculateCorrectAllocationForMultipleAccountTypes() {
            // Arrange
            Portfolio portfolio = createPortfolioWithMultipleAccounts();
            
            Account tfsaAccount = createAccount(AccountType.TFSA);
            Account rrspAccount = createAccount(AccountType.RRSP);
            Account nonRegAccount = createAccount(AccountType.NON_REGISTERED);

            // Prepare expected Money values
            Money tfsaValue = Money.of(new BigDecimal("40000.00"), ValidatedCurrency.USD);
            Money rrspValue = Money.of(new BigDecimal("35000.00"), ValidatedCurrency.USD);
            Money nonRegValue = Money.of(new BigDecimal("25000.00"), ValidatedCurrency.USD);

            // Mocking the behavior - Use 'time' variable for consistency
            when(valuationService.calculateAccountValue(tfsaAccount, marketDataService, time))
                .thenReturn(tfsaValue);
            when(valuationService.calculateAccountValue(rrspAccount, marketDataService, time))
                .thenReturn(rrspValue);
            when(valuationService.calculateAccountValue(nonRegAccount, marketDataService, time))
                .thenReturn(nonRegValue);
            
            // Act
            when(portfolio.getAccounts()).thenReturn(List.of(tfsaAccount, rrspAccount, nonRegAccount));
            Map<AccountType, Money> result = assetAllocationService
                .calculateAllocationByAccount(portfolio, marketDataService, time);
            
            // Assert
            assertNotNull(result);
            assertEquals(3, result.size());
            
            // Check ground truth Money values
            assertEquals(tfsaValue, result.get(AccountType.TFSA));
            assertEquals(rrspValue, result.get(AccountType.RRSP));
            assertEquals(nonRegValue, result.get(AccountType.NON_REGISTERED));
        }
        
        @Test
        @DisplayName("Should aggregate multiple accounts of same type")
        void shouldAggregateMultipleAccountsOfSameType() {
            // Arrange
            Portfolio portfolio = createPortfolioWithMultipleAccountsOfSameType();
            Money totalValue = Money.of(new BigDecimal("100000.00"), ValidatedCurrency.USD);
            
            when(valuationService.calculateTotalValue(portfolio, marketDataService, time))
                .thenReturn(totalValue);
            when(valuationService.calculateAccountValue(any(Account.class), eq(marketDataService), any(Instant.class)))
                .thenReturn(Money.of(new BigDecimal("25000.00"), ValidatedCurrency.USD));
            
            // Act
            Map<AccountType, Money> result = assetAllocationService.calculateAllocationByAccount(portfolio, marketDataService, time);
            
            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertTrue(result.containsKey(AccountType.TFSA));
            // 4 accounts * 25000 = 100000
            // assertEquals(new BigDecimal("100.00").setScale(Precision.PERCENTAGE.getDecimalPlaces()), result.get(AccountType.TFSA).toPercentage());
            assertEquals(Money.of(100000, "USD"), result.get(AccountType.TFSA));
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
            Money zeroMoney = Money.of(BigDecimal.ZERO, ValidatedCurrency.USD);
            
            when(valuationService.calculateTotalValue(portfolio, marketDataService, time))
                .thenReturn(zeroMoney);
            
            // Act
            Map<ValidatedCurrency, Money> result = assetAllocationService
                .calculateAllocationByCurrency(portfolio, marketDataService, time);
            
            // Assert
            assertTrue(result.isEmpty());
        }
        
        @Test
        @DisplayName("Should calculate correct allocation for single currency")
        void shouldCalculateCorrectAllocationForSingleCurrency() {
            // Arrange
            Portfolio portfolio = createPortfolioWithSingleCurrency();
            Money accountValue = Money.of(new BigDecimal("75000.00"), ValidatedCurrency.USD);
            
            // Remove calculateTotalValue mock - no longer needed by service
            when(valuationService.calculateAccountValue(any(Account.class), eq(marketDataService), eq(time)))
                .thenReturn(accountValue);
            
            // Act
            Map<ValidatedCurrency, Money> result = assetAllocationService
                .calculateAllocationByCurrency(portfolio, marketDataService, time);
            
            // Assert
            assertEquals(1, result.size());
            assertEquals(accountValue, result.get(ValidatedCurrency.USD));
        }
        
        @Test
        @DisplayName("Should calculate correct allocation for multiple currencies")
        void shouldCalculateCorrectAllocationForMultipleCurrencies() {
            // Arrange
            Portfolio portfolio = createPortfolioWithMultipleCurrencies();
            Account usdAccount = createAccountWithCurrency(ValidatedCurrency.USD);
            Account cadAccount = createAccountWithCurrency(ValidatedCurrency.CAD);
            Account eurAccount = createAccountWithCurrency(ValidatedCurrency.EUR);
            
            Money usdVal = Money.of(new BigDecimal("50000.00"), ValidatedCurrency.USD);
            Money cadVal = Money.of(new BigDecimal("30000.00"), ValidatedCurrency.CAD);
            Money eurVal = Money.of(new BigDecimal("20000.00"), ValidatedCurrency.EUR);

            when(valuationService.calculateAccountValue(usdAccount, marketDataService, time)).thenReturn(usdVal);
            when(valuationService.calculateAccountValue(cadAccount, marketDataService, time)).thenReturn(cadVal);
            when(valuationService.calculateAccountValue(eurAccount, marketDataService, time)).thenReturn(eurVal);
            
            // Act
            when(portfolio.getAccounts()).thenReturn(List.of(usdAccount, cadAccount, eurAccount));
            Map<ValidatedCurrency, Money> result = assetAllocationService.calculateAllocationByCurrency(portfolio, marketDataService, time);
            
            // Assert
            assertEquals(3, result.size());
            assertEquals(usdVal, result.get(ValidatedCurrency.USD));
            assertEquals(cadVal, result.get(ValidatedCurrency.CAD));
            assertEquals(eurVal, result.get(ValidatedCurrency.EUR));
        }
        
        @Test
        @DisplayName("Should aggregate multiple accounts with same currency")
        void shouldAggregateMultipleAccountsWithSameCurrency() {
            // Arrange
            Portfolio portfolio = createPortfolioWithMultipleAccountsSameCurrency();
            Money totalValue = Money.of(new BigDecimal("100000.00"), ValidatedCurrency.USD);
            
            when(valuationService.calculateTotalValue(portfolio, marketDataService, time))
                .thenReturn(totalValue);
            when(valuationService.calculateAccountValue(any(Account.class), eq(marketDataService), any(Instant.class)))
                .thenReturn(Money.of(new BigDecimal("25000.00"), ValidatedCurrency.USD));
            
            // Act
            Map<ValidatedCurrency, Money> result = assetAllocationService.calculateAllocationByCurrency(portfolio, marketDataService, time); // same here
            
            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertTrue(result.containsKey(ValidatedCurrency.USD));
            // assertEquals(new BigDecimal("100.00").setScale(Precision.PERCENTAGE.getDecimalPlaces()), result.get(ValidatedCurrency.USD).toPercentage());
            assertEquals(totalValue, result.get(ValidatedCurrency.USD));
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
                assetAllocationService.calculateAllocationByType(null, marketDataService, time);
            });
        }
        
        @Test
        @DisplayName("Should handle null market data service gracefully")
        void shouldHandleNullMarketDataService() {
            // Arrange
            Portfolio portfolio = createMockPortfolio();
            
            // Act & Assert
            assertThrows(NullPointerException.class, () -> {
                assetAllocationService.calculateAllocationByType(portfolio, null, time);
            });
        }

        @Test
        @DisplayName("Should handle null as of date service gracefully")
        void shouldHandleNullAsOfDateService() {
            // Arrange
            Portfolio portfolio = createMockPortfolio();
            
            // Act & Assert
            assertThrows(NullPointerException.class, () -> {
                assetAllocationService.calculateAllocationByType(portfolio, marketDataService, null);
            });
        }
        
        @Test
        @DisplayName("Should handle very small allocation percentages")
        void shouldHandleVerySmallAllocationPercentages() {
            // Arrange
            Portfolio portfolio = createPortfolioWithVerySmallAllocations();
            Money totalValue = Money.of(new BigDecimal("1000000.00"), ValidatedCurrency.USD);
            
            when(valuationService.calculateTotalValue(portfolio, marketDataService, time))
                .thenReturn(totalValue);
            when(valuationService.calculateAssetValue(any(Asset.class), eq(marketDataService), any(Instant.class)))
                .thenReturn(Money.of(new BigDecimal("1.00"), ValidatedCurrency.USD));
            
            // Act
            Map<AssetType, Money> result = assetAllocationService.calculateAllocationByType(portfolio, marketDataService, time); // this as well
            
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
    
    private Portfolio createPortfolioWithSingleAccount() {
        Portfolio portfolio = mock(Portfolio.class);
        Account account = createAccount(AccountType.TFSA);
        when(portfolio.getAccounts()).thenReturn(List.of(account));
        return portfolio;
    }
    
    private Portfolio createPortfolioWithMultipleAccounts() {
        Portfolio portfolio = mock(Portfolio.class);
        Account tfsaAccount = createAccount(AccountType.TFSA);
        Account rrspAccount = createAccount(AccountType.RRSP);
        Account nonRegAccount = createAccount(AccountType.NON_REGISTERED);
        when(portfolio.getAccounts()).thenReturn(List.of(tfsaAccount, rrspAccount, nonRegAccount));
        return portfolio;
    }
    
    private Portfolio createPortfolioWithMultipleAccountsOfSameType() {
        Portfolio portfolio = mock(Portfolio.class);
        Account account1 = createAccount(AccountType.TFSA);
        Account account2 = createAccount(AccountType.TFSA);
        Account account3 = createAccount(AccountType.TFSA);
        Account account4 = createAccount(AccountType.TFSA);
        when(portfolio.getAccounts()).thenReturn(List.of(account1, account2, account3, account4));
        return portfolio;
    }
    
    private Portfolio createPortfolioWithSingleCurrency() {
        Portfolio portfolio = mock(Portfolio.class);
        Account account = createAccountWithCurrency(ValidatedCurrency.USD);
        when(portfolio.getAccounts()).thenReturn(List.of(account));
        return portfolio;
    }
    
    private Portfolio createPortfolioWithMultipleCurrencies() {
        Portfolio portfolio = mock(Portfolio.class);
        Account usdAccount = createAccountWithCurrency(ValidatedCurrency.USD);
        Account cadAccount = createAccountWithCurrency(ValidatedCurrency.CAD);
        Account eurAccount = createAccountWithCurrency(ValidatedCurrency.EUR);
        when(portfolio.getAccounts()).thenReturn(List.of(usdAccount, cadAccount, eurAccount));
        return portfolio;
    }
    
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
        when(account.getAccountType()).thenReturn(type);
        when(account.getBaseCurrency()).thenReturn(ValidatedCurrency.USD);
        return account;
    }

    private Account createAccount(AccountType type, List<Asset> assets) {
        Account account = mock(Account.class);
        when(account.getAccountType()).thenReturn(type);
        when(account.getBaseCurrency()).thenReturn(ValidatedCurrency.USD);
        when(account.getAssets()).thenReturn(assets);
        return account;
    }
    
    private Account createAccountWithCurrency(ValidatedCurrency ValidatedCurrency) {
        Account account = mock(Account.class);
        when(account.getBaseCurrency()).thenReturn(ValidatedCurrency);
        return account;
    }
    
    private Asset createAsset(AssetType type) {
        Asset asset = mock(Asset.class);
        AssetIdentifier assetIdentifier = mock(AssetIdentifier.class);
        when(asset.getAssetIdentifier()).thenReturn(assetIdentifier);
        when(assetIdentifier.getAssetType()).thenReturn(type);
        return asset;
    }
}
