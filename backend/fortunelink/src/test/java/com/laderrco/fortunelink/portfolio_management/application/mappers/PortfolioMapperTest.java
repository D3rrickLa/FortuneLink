package com.laderrco.fortunelink.portfolio_management.application.mappers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laderrco.fortunelink.portfolio_management.application.responses.AccountResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.AssetResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.PortfolioResponse;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CashIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CryptoIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioMapperTest {

    @Mock
    private ExchangeRateService exchangeRateService;
    
    @Mock
    private MarketDataService marketDataService;

    private PortfolioMapper mapper;
    
    private static final ValidatedCurrency USD = ValidatedCurrency.USD;
    private static final ValidatedCurrency CAD = ValidatedCurrency.CAD;
    private static final Instant NOW = Instant.now();
    private PortfolioId portfolioId;
    private AccountId accountId;

    @BeforeEach
    void setUp() {
        mapper = new PortfolioMapper(exchangeRateService);
        portfolioId = PortfolioId.randomId();
        accountId = AccountId.randomId();
    }

    @Nested
    @DisplayName("toResponse() Tests")
    class ToResponseTests {

        @Test
        @DisplayName("Should return null when portfolio is null")
        void shouldReturnNullForNullPortfolio() {
            // Act
            PortfolioResponse response = mapper.toResponse(null, marketDataService);

            // Assert
            assertNull(response);
            verifyNoInteractions(marketDataService, exchangeRateService);
        }

        @Test
        @DisplayName("Should map portfolio with single account correctly")
        void shouldMapPortfolioWithSingleAccount() {
            // Arrange
            Portfolio portfolio = createTestPortfolio();
            Money totalValue = Money.of(10000, "USD");
            
            when(portfolio.getAssetsTotalValue(marketDataService, exchangeRateService))
                .thenReturn(totalValue);
            

            // Act
            PortfolioResponse response = mapper.toResponse(portfolio, marketDataService);

            // Assert
            assertNotNull(response);
            assertEquals(portfolio.getPortfolioId(), response.portfolioId());
            assertEquals(portfolio.getUserId(), response.userId());
            assertEquals(totalValue, response.totalValue());
            assertEquals(1, response.accounts().size());
            verify(portfolio).getAssetsTotalValue(marketDataService, exchangeRateService);
        }

        @Test
        @DisplayName("Should map portfolio with multiple accounts")
        void shouldMapPortfolioWithMultipleAccounts() {
            // Arrange
            Account account1 = createTestAccount("TFSA", AccountType.TFSA, USD);
            Account account2 = createTestAccount("RRSP", AccountType.RRSP, CAD);
            
            Portfolio portfolio = mock(Portfolio.class);
            when(portfolio.getAccounts()).thenReturn(Arrays.asList(account1, account2));
            when(portfolio.getAssetsTotalValue(any(), any())).thenReturn(Money.of(20000, "USD"));
            when(portfolio.getTransactionCount()).thenReturn(10L);
            when(portfolio.getPortfolioId()).thenReturn(portfolioId);
            when(portfolio.getSystemCreationDate()).thenReturn(NOW);
            when(portfolio.getLastUpdatedAt()).thenReturn(NOW);
            when(portfolio.getUserId()).thenReturn(UserId.randomId());
            
            when(account1.calculateTotalValue(marketDataService)).thenReturn(Money.of(10000, "USD"));
            when(account2.calculateTotalValue(marketDataService)).thenReturn(Money.of(10000, "CAD"));

            // Act
            PortfolioResponse response = mapper.toResponse(portfolio, marketDataService);

            // Assert
            assertNotNull(response);
            assertEquals(2, response.accounts().size());
            
            AccountResponse acc1Response = response.accounts().get(0);
            assertEquals("TFSA", acc1Response.name());
            assertEquals(AccountType.TFSA, acc1Response.type());
            
            AccountResponse acc2Response = response.accounts().get(1);
            assertEquals("RRSP", acc2Response.name());
            assertEquals(AccountType.RRSP, acc2Response.type());
        }

        @Test
        @DisplayName("Should handle portfolio with empty accounts list")
        void shouldHandlePortfolioWithNoAccounts() {
            // Arrange
            Portfolio portfolio = mock(Portfolio.class);
            when(portfolio.getAccounts()).thenReturn(Collections.emptyList());
            when(portfolio.getAssetsTotalValue(any(), any())).thenReturn(Money.ZERO("USD"));
            when(portfolio.getPortfolioId()).thenReturn(portfolioId);
            when(portfolio.getUserId()).thenReturn(UserId.randomId());
            when(portfolio.getSystemCreationDate()).thenReturn(NOW);
            when(portfolio.getLastUpdatedAt()).thenReturn(NOW);

            // Act
            PortfolioResponse response = mapper.toResponse(portfolio, marketDataService);

            // Assert
            assertNotNull(response);
            assertTrue(response.accounts().isEmpty());
            assertEquals(Money.ZERO("USD"), response.totalValue());
        }

        @Test
        @DisplayName("Should preserve timestamps and transaction count")
        void shouldPreserveMetadata() {
            // Arrange
            Portfolio portfolio = createTestPortfolio();
            Instant createdDate = Instant.parse("2024-01-01T00:00:00Z");
            Instant updatedDate = Instant.parse("2024-01-15T00:00:00Z");
            
            when(portfolio.getSystemCreationDate()).thenReturn(createdDate);
            when(portfolio.getLastUpdatedAt()).thenReturn(updatedDate);
            when(portfolio.getTransactionCount()).thenReturn(25L);
            when(portfolio.getAssetsTotalValue(any(), any())).thenReturn(Money.of(10000, "USD"));

            // Act
            PortfolioResponse response = mapper.toResponse(portfolio, marketDataService);

            // Assert
            assertEquals(createdDate, response.createDate());
            assertEquals(updatedDate, response.lastUpdated());
            assertEquals(25L, response.transactionCount());
        }
    }

    @Nested
    @DisplayName("toAccountResponse() Tests")
    class ToAccountResponseTests {

        @Test
        @DisplayName("Should return null when account is null")
        void shouldReturnNullForNullAccount() {
            // Act
            AccountResponse response = mapper.toAccountResponse(null, marketDataService);

            // Assert
            assertNull(response);
            verifyNoInteractions(marketDataService);
        }

        @Test
        @DisplayName("Should map account with assets correctly")
        void shouldMapAccountWithAssets() {
            // Arrange
            Account account = createTestAccount("My TFSA", AccountType.TFSA, USD);
            Money totalValue = Money.of(15000, "USD");
            
            when(account.calculateTotalValue(marketDataService)).thenReturn(totalValue);

            // Act
            AccountResponse response = mapper.toAccountResponse(account, marketDataService);

            // Assert
            assertNotNull(response);
            assertEquals(account.getAccountId(), response.accountId());
            assertEquals("My TFSA", response.name());
            assertEquals(AccountType.TFSA, response.type());
            assertEquals(USD, response.baseCurrency());
            assertEquals(totalValue, response.totalValue());
            verify(account).calculateTotalValue(marketDataService);
        }

        @Test
        @DisplayName("Should calculate cash balance correctly")
        void shouldCalculateCashBalance() {
            // Arrange
            Asset cashAsset1 = createCashAsset(AssetId.randomId(), new BigDecimal("5000"), USD);
            Asset cashAsset2 = createCashAsset(AssetId.randomId(), new BigDecimal("3000"), USD);
            Asset stockAsset = createStockAsset(AssetId.randomId(), "AAPL", new BigDecimal("10000"), USD);
            
            Account account = mock(Account.class);
            when(account.getAssets()).thenReturn(Arrays.asList(cashAsset1, cashAsset2, stockAsset));
            when(account.getAccountId()).thenReturn(accountId);
            when(account.getName()).thenReturn("Test Account");
            when(account.getAccountType()).thenReturn(AccountType.NON_REGISTERED);
            when(account.getBaseCurrency()).thenReturn(USD);
            when(account.calculateTotalValue(marketDataService)).thenReturn(Money.of(18000, "USD"));
            when(account.getSystemCreationDate()).thenReturn(NOW);

            // Act
            AccountResponse response = mapper.toAccountResponse(account, marketDataService);

            // Assert
            assertNotNull(response);
            // Cash balance should be 5000 + 3000 = 8000
            assertEquals(Money.of(8000, "USD"), response.cashBalance());
            // Total value includes stocks too
            assertEquals(Money.of(18000, "USD"), response.totalValue());
        }

        @Test
        @DisplayName("Should handle account with no cash assets")
        void shouldHandleAccountWithNoCash() {
            // Arrange
            Asset stockAsset = createStockAsset(AssetId.randomId(), "AAPL", new BigDecimal("10000"), USD);
            
            Account account = mock(Account.class);
            when(account.getAssets()).thenReturn(Collections.singletonList(stockAsset));
            when(account.getAccountId()).thenReturn(accountId);
            when(account.getName()).thenReturn("Stocks Only");
            when(account.getAccountType()).thenReturn(AccountType.INVESTMENT);
            when(account.getBaseCurrency()).thenReturn(USD);
            when(account.calculateTotalValue(marketDataService)).thenReturn(Money.of(10000, "USD"));
            when(account.getSystemCreationDate()).thenReturn(NOW);

            // Act
            AccountResponse response = mapper.toAccountResponse(account, marketDataService);

            // Assert
            assertNotNull(response);
            assertEquals(Money.ZERO(USD), response.cashBalance());
            assertEquals(Money.of(10000, "USD"), response.totalValue());
        }

        @Test
        @DisplayName("Should handle account with empty assets list")
        void shouldHandleEmptyAccount() {
            // Arrange
            Account account = mock(Account.class);
            when(account.getAssets()).thenReturn(Collections.emptyList());
            when(account.getAccountId()).thenReturn(accountId);
            when(account.getName()).thenReturn("Empty Account");
            when(account.getAccountType()).thenReturn(AccountType.CHEQUING);
            when(account.getBaseCurrency()).thenReturn(USD);
            when(account.calculateTotalValue(marketDataService)).thenReturn(Money.ZERO("USD"));
            when(account.getSystemCreationDate()).thenReturn(NOW);

            // Act
            AccountResponse response = mapper.toAccountResponse(account, marketDataService);

            // Assert
            assertNotNull(response);
            assertEquals(Money.ZERO(USD), response.cashBalance());
            assertEquals(Money.ZERO("USD"), response.totalValue());
        }
    }

    @Nested
    @DisplayName("toAssetResponse() Tests")
    class ToAssetResponseTests {

        @Test
        @DisplayName("Should return null when asset is null")
        void shouldReturnNullForNullAsset() {
            // Act
            AssetResponse response = PortfolioMapper.toAssetResponse(null, Money.of(150, "USD"));

            // Assert
            assertNull(response);
        }

        @Test
        @DisplayName("Should map asset with current price correctly")
        void shouldMapAssetWithPrice() {
            // Arrange
            Asset asset = createStockAsset(AssetId.randomId(), "AAPL", new BigDecimal("10000"), USD);
            Money currentPrice = Money.of(175, "USD");
            Money currentValue = Money.of(17500, "USD");
            Money unrealizedGain = Money.of(7500, "USD");
            
            when(asset.calculateCurrentValue(currentPrice)).thenReturn(currentValue);
            when(asset.calculateUnrealizedGainLoss(currentPrice)).thenReturn(unrealizedGain);
            when(asset.getCostBasis()).thenReturn(Money.of(10000, "USD"));

            // Act
            AssetResponse response = PortfolioMapper.toAssetResponse(asset, currentPrice);

            // Assert
            assertNotNull(response);
            assertEquals(asset.getAssetId(), response.assetId());
            assertEquals("AAPL", response.symbol());
            assertEquals(AssetType.STOCK, response.type());
            assertEquals(currentPrice, response.currentPrice());
            assertEquals(currentValue, response.currentValue());
            assertEquals(unrealizedGain, response.unrealizedGain());
            
            // Gain percentage should be 75% (7500 / 10000 * 100)
            assertEquals(new BigDecimal("75.00"), response.unrealizedGainPercentage().value().setScale(2));
        }

        @Test
        @DisplayName("Should handle null current price gracefully")
        void shouldHandleNullPrice() {
            // Arrange
            Asset asset = createStockAsset(AssetId.randomId(), "AAPL", new BigDecimal("10000"), USD);
            when(asset.getCurrency()).thenReturn(USD);
            when(asset.getCostPerUnit()).thenReturn(Money.of(0, "USD"));
            when(asset.getCostBasis()).thenReturn(Money.of(0, "USD"));
            // Act
            AssetResponse response = PortfolioMapper.toAssetResponse(asset, null);

            // Assert
            assertNotNull(response);
            assertEquals(Money.ZERO(USD), response.currentValue());
            assertEquals(Money.ZERO(USD), response.unrealizedGain());
            assertEquals(Percentage.ZERO, response.unrealizedGainPercentage());
            assertEquals(Money.ZERO(USD), response.currentPrice());
        }

        @Test
        @DisplayName("Should calculate negative gain percentage correctly")
        void shouldHandleNegativeGain() {
            // Arrange
            Asset asset = createStockAsset(AssetId.randomId(), "AAPL", new BigDecimal("10000"), USD);
            Money currentPrice = Money.of(80, "USD");
            Money currentValue = Money.of(8000, "USD");
            Money unrealizedGain = Money.of(-2000, "USD");
            
            lenient().when(asset.calculateCurrentValue(currentPrice)).thenReturn(currentValue);
            lenient().when(asset.calculateUnrealizedGainLoss(currentPrice)).thenReturn(unrealizedGain);
            lenient().when(asset.getCostBasis()).thenReturn(Money.of(10000, "USD"));

            // Act
            AssetResponse response = PortfolioMapper.toAssetResponse(asset, currentPrice);

            // Assert
            assertNotNull(response);
            assertEquals(unrealizedGain, response.unrealizedGain());
            // Loss percentage should be -20% (-2000 / 10000 * 100)
            assertEquals(new BigDecimal("-20.00"), response.unrealizedGainPercentage().value().setScale(2));
        }

        @Test
        @DisplayName("Should handle zero cost basis")
        void shouldHandleZeroCostBasis() {
            // Arrange
            Asset asset = createStockAsset(AssetId.randomId(), "FREE", BigDecimal.ZERO, USD);
            Money currentPrice = Money.of(100, "USD");
            Money currentValue = Money.of(10000, "USD");
            Money unrealizedGain = Money.of(10000, "USD");
            
            when(asset.calculateCurrentValue(any())).thenReturn(currentValue);
            when(asset.calculateUnrealizedGainLoss(any())).thenReturn(unrealizedGain);
            when(asset.getCostBasis()).thenReturn(Money.ZERO("USD"));

            // Act
            AssetResponse response = PortfolioMapper.toAssetResponse(asset, currentPrice);

            // Assert
            assertNotNull(response);
            // Percentage should be 0 when cost basis is 0 (avoid division by zero)
            assertEquals(Percentage.ZERO, response.unrealizedGainPercentage());
        }

        @Test
        @DisplayName("Should map all asset fields correctly")
        void shouldMapAllAssetFields() {
            // Arrange
            AssetId assetId = AssetId.randomId();
            AssetIdentifier identifier =  new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);;
            BigDecimal quantity = new BigDecimal("100");
            Money costBasis = Money.of(10000, "USD");
            Money costPerUnit = Money.of(100, "USD");
            LocalDateTime acquiredOn = LocalDateTime.of(2024, 1, 1, 0, 0);
            Instant lastInteraction = Instant.parse("2024-01-15T10:00:00Z");
            
            Asset asset = mock(Asset.class);
            when(asset.getAssetId()).thenReturn(assetId);
            when(asset.getAssetIdentifier()).thenReturn(identifier);
            when(asset.getQuantity()).thenReturn(quantity);
            when(asset.getCostBasis()).thenReturn(costBasis);
            when(asset.getCostPerUnit()).thenReturn(costPerUnit);
            when(asset.getAcquiredOn()).thenReturn(acquiredOn.toInstant(ZoneOffset.UTC));
            when(asset.getLastSystemInteraction()).thenReturn(lastInteraction);
            // when(asset.getCurrency()).thenReturn(USD);
            
            Money currentPrice = Money.of(100, "USD");
            when(asset.calculateCurrentValue(currentPrice)).thenReturn(Money.of(11000, "USD"));
            when(asset.calculateUnrealizedGainLoss(currentPrice)).thenReturn(Money.of(1000, "USD"));

            // Act
            AssetResponse response = PortfolioMapper.toAssetResponse(asset, currentPrice);

            // Assert
            assertEquals(assetId, response.assetId());
            assertEquals("AAPL", response.symbol());
            assertEquals(AssetType.STOCK, response.type());
            assertEquals(quantity, response.quantity());
            assertEquals(costBasis, response.costBasis());
            assertEquals(costPerUnit, response.currentPrice());
            assertEquals(acquiredOn.toInstant(ZoneOffset.UTC), response.acquiredDate());
            assertEquals(lastInteraction, response.lastUpdated());
        }

        @Test
        @DisplayName("Should handle crypto asset")
        void shouldHandleCryptoAsset() {
            // Arrange
            Asset asset = createCryptoAsset(AssetId.randomId(), "BTC", new BigDecimal("50000"), USD);
            Money currentPrice = Money.of(60000, "USD");
            Money currentValue = Money.of(60000, "USD");
            Money unrealizedGain = Money.of(10000, "USD");
            
            lenient().when(asset.calculateCurrentValue(currentPrice)).thenReturn(currentValue);
            lenient().when(asset.calculateUnrealizedGainLoss(currentPrice)).thenReturn(unrealizedGain);
            lenient().when(asset.getCostBasis()).thenReturn(Money.of(50000, "USD"));

            // Act
            AssetResponse response = PortfolioMapper.toAssetResponse(asset, currentPrice);

            // Assert
            assertNotNull(response);
            assertEquals(AssetType.CRYPTO, response.type());
            assertEquals("BTC", response.symbol());
            assertEquals(new BigDecimal("20.00"), response.unrealizedGainPercentage().value().setScale(2));
        }
    }

    // ========== Test Helper Methods ==========

    private Portfolio createTestPortfolio() {
        Portfolio portfolio = mock(Portfolio.class);
        Account account = createTestAccount("Test Account", AccountType.TFSA, USD);
        
        // Mock the account's calculateTotalValue to return a non-null value
        Money accountValue = Money.of(10000, "USD");
        when(account.calculateTotalValue(any(MarketDataService.class))).thenReturn(accountValue);
        
        when(portfolio.getAccounts()).thenReturn(Collections.singletonList(account));
        when(portfolio.getPortfolioId()).thenReturn(PortfolioId.randomId());
        when(portfolio.getUserId()).thenReturn(UserId.randomId());
        when(portfolio.getTransactionCount()).thenReturn(5L);
        when(portfolio.getSystemCreationDate()).thenReturn(NOW);
        when(portfolio.getLastUpdatedAt()).thenReturn(NOW);
        
        return portfolio;
    }

    private Account createTestAccount(String name, AccountType type, ValidatedCurrency currency) {
        Account account = mock(Account.class);
        when(account.getAccountId()).thenReturn(AccountId.randomId());
        when(account.getName()).thenReturn(name);
        when(account.getAccountType()).thenReturn(type);
        when(account.getBaseCurrency()).thenReturn(currency);
        when(account.getAssets()).thenReturn(Collections.emptyList());
        when(account.getSystemCreationDate()).thenReturn(NOW);
        
        return account;
    }

    private Asset createCashAsset(AssetId id, BigDecimal amount, ValidatedCurrency currency) {
        Asset asset = mock(Asset.class);
        AssetIdentifier identifier = new CashIdentifier("USD");
        
        lenient().when(asset.getAssetId()).thenReturn(id);
        lenient().when(asset.getAssetIdentifier()).thenReturn(identifier);
        lenient().when(asset.getCostBasis()).thenReturn(Money.of(amount, currency));
        lenient().when(asset.getCurrency()).thenReturn(currency);
        
        return asset;
    }

    private Asset createStockAsset(AssetId id, String symbol, BigDecimal costBasis, ValidatedCurrency currency) {
        Asset asset = mock(Asset.class);
        AssetIdentifier identifier = new MarketIdentifier(symbol, null, AssetType.STOCK, "STOCK NAME", currency.getSymbol(), null);
        
        lenient().when(asset.getAssetId()).thenReturn(id);
        lenient().when(asset.getAssetIdentifier()).thenReturn(identifier);
        lenient().when(asset.getCostBasis()).thenReturn(Money.of(costBasis, currency));
        lenient().// when(asset.getCurrency()).thenReturn(currency);
        when(asset.getQuantity()).thenReturn(new BigDecimal("100"));
        lenient().when(asset.getCostPerUnit()).thenReturn(Money.of(costBasis.divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP), currency));
        lenient().when(asset.getAcquiredOn()).thenReturn(Instant.now());
        lenient().when(asset.getLastSystemInteraction()).thenReturn(Instant.now());
        
        return asset;
    }

    private Asset createCryptoAsset(AssetId id, String symbol, BigDecimal costBasis, ValidatedCurrency currency) {
        Asset asset = mock(Asset.class);
        AssetIdentifier identifier = new CryptoIdentifier(symbol, "CRYPTO NAME", AssetType.CRYPTO, symbol, null);
        
        when(asset.getAssetId()).thenReturn(id);
        when(asset.getAssetIdentifier()).thenReturn(identifier);
        when(asset.getCostBasis()).thenReturn(Money.of(costBasis, currency));
        lenient().when(asset.getCurrency()).thenReturn(currency);
        when(asset.getQuantity()).thenReturn(BigDecimal.ONE);
        when(asset.getCostPerUnit()).thenReturn(Money.of(costBasis, currency));
        when(asset.getAcquiredOn()).thenReturn(Instant.now());
        when(asset.getLastSystemInteraction()).thenReturn(Instant.now());
        
        return asset;
    }
}