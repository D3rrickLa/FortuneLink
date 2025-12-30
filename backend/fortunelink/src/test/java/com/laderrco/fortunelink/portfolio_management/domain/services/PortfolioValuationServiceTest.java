package com.laderrco.fortunelink.portfolio_management.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.MarketDataNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CryptoIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class PortfolioValuationServiceTest {
    private PortfolioValuationService valuationService;
    private ValidatedCurrency ValidatedCurrencyCAD = ValidatedCurrency.CAD;
    private AccountId accountId1;
    private AccountId accountId2;
    private PortfolioId portfolioId1;
    private UserId userId1;
    private AssetId assetId1;
    
    @Mock
    private MarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        valuationService = new PortfolioValuationService();
        accountId1 = AccountId.randomId();
        accountId2 = AccountId.randomId();
        portfolioId1 = PortfolioId.randomId();
        userId1 = UserId.randomId();
        assetId1 = AssetId.randomId();
    }

    @Test
    @DisplayName("Should calculate total portfolio value with single account and single asset")
    void calculateTotalValue_SingleAccountSingleAsset() {
        // Arrange
        MarketIdentifier appleSymbol = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);
        ValidatedCurrency usd = ValidatedCurrency.USD;
        
        Asset appleStock = Asset.builder()
            .assetId(assetId1)
            .assetIdentifier(appleSymbol)
            .quantity(new BigDecimal("10"))
            .currency(usd)
            .costBasis(new Money(new BigDecimal("1500"), usd))
            .acquiredOn(LocalDateTime.now().minusMonths(6).toInstant(ZoneOffset.UTC))
            .lastSystemInteraction(Instant.MIN)
            .build();

        Account investmentAccount = Account.builder()
            .accountId(accountId1)
            .name("Investment Account")
            .accountType(AccountType.NON_REGISTERED)
            .baseCurrency(usd)
            .cashBalance(new Money(new BigDecimal("500"), usd))
            .assets(List.of(appleStock))
            .systemCreationDate(Instant.now())
            .lastSystemInteraction(Instant.now())
            .build();

        Portfolio portfolio = Portfolio.builder()
            .portfolioId(portfolioId1)
            .userId(userId1)
            .accounts(List.of(investmentAccount))
            .portfolioCurrencyPreference(usd)
            .systemCreationDate(Instant.now())
            .lastUpdatedAt(Instant.now())
            .build();

        // Mock market data service to return current price
        Money currentPrice = new Money(new BigDecimal("180"), usd);
        when(marketDataService.getCurrentPrice(appleSymbol))
            .thenReturn(currentPrice);

        // Act
        Money totalValue = valuationService.calculateTotalValue(portfolio, marketDataService, Instant.now());

        // Assert
        // 10 shares * $180 = $1800 + $500 cash = $2300
        Money expectedValue = new Money(new BigDecimal("2300"), usd);
        assertEquals(expectedValue, totalValue);
        
        verify(marketDataService, times(1)).getCurrentPrice(appleSymbol);
    }

    @Test
    @DisplayName("Should calculate total portfolio value with single account and single asset")
    void calculateTotalAssets_SingleAccountSingleAsset() {
        // Arrange
        MarketIdentifier appleSymbol = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);
        ValidatedCurrency usd = ValidatedCurrency.USD;
        
        Asset appleStock = Asset.builder()
            .assetId(assetId1)
            .assetIdentifier(appleSymbol)
            .quantity(new BigDecimal("10"))
            .currency(usd)
            .costBasis(new Money(new BigDecimal("1500"), usd))
            .acquiredOn(LocalDateTime.now().minusMonths(6).toInstant(ZoneOffset.UTC))
            .lastSystemInteraction(Instant.MIN)
            .build();

        Account investmentAccount = Account.builder()
            .accountId(accountId1)
            .name("Investment Account")
            .accountType(AccountType.NON_REGISTERED)
            .baseCurrency(usd)
            .cashBalance(new Money(new BigDecimal("500"), usd))
            .assets(List.of(appleStock))
            .systemCreationDate(Instant.now())
            .lastSystemInteraction(Instant.now())
            .build();

        Portfolio portfolio = Portfolio.builder()
            .portfolioId(portfolioId1)
            .userId(userId1)
            .accounts(List.of(investmentAccount))
            .portfolioCurrencyPreference(usd)
            .systemCreationDate(Instant.now())
            .lastUpdatedAt(Instant.now())
            .build();

        // Mock market data service to return current price
        Money currentPrice = new Money(new BigDecimal("180"), usd);
        when(marketDataService.getCurrentPrice(appleSymbol))
            .thenReturn(currentPrice);

        // Act
        Money totalValue = valuationService.calculateTotalValue(portfolio, marketDataService, Instant.now()); // this was total assets before

        // Assert
        // 10 shares * $180 = $1800 + $500 cash = $2300
        Money expectedValue = new Money(new BigDecimal("2300"), usd);
        assertEquals(expectedValue, totalValue);
        
        verify(marketDataService, times(1)).getCurrentPrice(appleSymbol);
    }

    @Test
    @DisplayName("Should calculate portfolio value with multiple accounts and assets")
    void calculateTotalValue_MultipleAccountsAndAssets() {
        // Arrange
        ValidatedCurrency cad = ValidatedCurrency.CAD;
        
        // First account with two assets
        MarketIdentifier shopifySymbol = new MarketIdentifier("SHOP", null, AssetType.STOCK, "Shopify", "CAD", null);
        MarketIdentifier ryCSymbol = new MarketIdentifier("RY", null, AssetType.STOCK, "Royal Bank of Canada", "CAD", null);
        
        Asset shopifyStock = createAsset("asset-1", shopifySymbol, "50", ValidatedCurrencyCAD);
        Asset royalBankStock = createAsset("asset-2", ryCSymbol, "100", ValidatedCurrencyCAD);
        
        Account tfsaAccount = Account.builder()
            .accountId(accountId1)
            .name("TFSA")
            .accountType(AccountType.TFSA)
            .baseCurrency(cad)
            .cashBalance(new Money(new BigDecimal("1000"), cad))
            .assets(List.of(shopifyStock, royalBankStock))
            .systemCreationDate(Instant.now())
            .lastSystemInteraction(Instant.now())
            .build();

        // Second account with one asset
        MarketIdentifier vfvSymbol = new MarketIdentifier("VFV", null, AssetType.ETF, "Vanguard S&P 500 ETF", "CAD", null);
        Asset vfvEtf = createAsset("asset-3", vfvSymbol, "200", ValidatedCurrencyCAD);
        
        Account rrspAccount = Account.builder()
            .accountId(accountId2)
            .name("RRSP")
            .accountType(AccountType.RRSP)
            .baseCurrency(cad)
            .cashBalance(new Money(new BigDecimal("2000"), cad))
            .assets(List.of(vfvEtf))
            .systemCreationDate(Instant.now())
            .lastSystemInteraction(Instant.MAX)
            .build();

        Portfolio portfolio = Portfolio.builder()
            .portfolioId(portfolioId1)
            .userId(UserId.randomId())
            .accounts(List.of(tfsaAccount, rrspAccount))
            .portfolioCurrencyPreference(cad)
            .systemCreationDate(Instant.now())
            .lastUpdatedAt(Instant.now())
            .build();

        // Mock market prices
        when(marketDataService.getCurrentPrice(shopifySymbol))
            .thenReturn(new Money(new BigDecimal("80"), cad));
        when(marketDataService.getCurrentPrice(ryCSymbol))
            .thenReturn(new Money(new BigDecimal("120"), cad));
        when(marketDataService.getCurrentPrice(vfvSymbol))
            .thenReturn(new Money(new BigDecimal("100"), cad));

        // Act
        Money totalValue = valuationService.calculateTotalValue(portfolio, marketDataService, Instant.now());

        // Assert
        // TFSA: (50 * $80) + (100 * $120) + $1000 = $4000 + $12000 + $1000 = $17000
        // RRSP: (200 * $100) + $2000 = $20000 + $2000 = $22000
        // Total: $39000
        Money expectedValue = new Money(new BigDecimal("39000"), cad);
        assertEquals(expectedValue, totalValue);
        
        verify(marketDataService, times(1)).getCurrentPrice(shopifySymbol);
        verify(marketDataService, times(1)).getCurrentPrice(ryCSymbol);
        verify(marketDataService, times(1)).getCurrentPrice(vfvSymbol);
    }

    @Test
    @DisplayName("Should handle portfolio with only cash and no assets")
    void calculateTotalValue_OnlyCashNoAssets() {
        // Arrange
        ValidatedCurrency usd = ValidatedCurrency.USD;
        
        Account cashAccount = Account.builder()
            .accountId(accountId1)
            .name("Chequing Account")
            .accountType(AccountType.CHEQUING)
            .baseCurrency(usd)
            .cashBalance(new Money(new BigDecimal("5000"), usd))
            .systemCreationDate(Instant.now())
            .assets(List.of())
            .systemCreationDate(Instant.now())
            .lastSystemInteraction(Instant.now())
            .build();

        Portfolio portfolio = Portfolio.builder()
            .portfolioId(portfolioId1)
            .userId(UserId.randomId())
            .accounts(List.of(cashAccount))
            .portfolioCurrencyPreference(usd)
            .systemCreationDate(Instant.now())
            .lastUpdatedAt(Instant.now())
            .build();

        // Act
        Money totalValue = valuationService.calculateTotalValue(portfolio, marketDataService, Instant.now());

        // Assert
        Money expectedValue = new Money(new BigDecimal("5000"), usd);
        assertEquals(expectedValue, totalValue);
        
        // Should not call market data service when there are no assets
        verifyNoInteractions(marketDataService);
    }

    @Test
    @DisplayName("Should handle empty portfolio")
    void calculateTotalValue_EmptyPortfolio() {
        // Arrange
        Portfolio portfolio = Portfolio.builder()
            .portfolioId(portfolioId1)
            .userId(UserId.randomId())
            .accounts(List.of())
            .portfolioCurrencyPreference(ValidatedCurrency.USD)
            .systemCreationDate(Instant.now())
            .lastUpdatedAt(Instant.now())
            .build();

        // Act
        Money totalValue = valuationService.calculateTotalValue(portfolio, marketDataService, Instant.now());

        // Assert
        Money expectedValue = Money.ZERO(ValidatedCurrency.USD);
        assertEquals(expectedValue, totalValue);
        
        verifyNoInteractions(marketDataService);
    }

    @Test
    @DisplayName("Should calculate account value correctly")
    void calculateAccountValue_Success() {
        // Arrange
        ValidatedCurrency usd = ValidatedCurrency.USD;
        MarketIdentifier teslaSymbol = new MarketIdentifier("TSLA", null, AssetType.STOCK, "Tesla", "USD", null);
        
        Asset teslaStock = createAsset("asset-1", teslaSymbol, "25", ValidatedCurrency.USD);
        
        Account account = Account.builder()
            .accountId(accountId1)
            .name("Investment Account")
            .accountType(AccountType.INVESTMENT)
            .baseCurrency(usd)
            .cashBalance(new Money(new BigDecimal("3000"), usd))
            .assets(List.of(teslaStock))
            .systemCreationDate(Instant.now())
            .lastSystemInteraction(Instant.now())
            .build();

        when(marketDataService.getCurrentPrice(teslaSymbol))
            .thenReturn(new Money(new BigDecimal("250"), usd));

        // Act
        Money accountValue = valuationService.calculateAccountValue(account, marketDataService, Instant.now());

        // Assert
        // 25 shares * $250 + $3000 cash = $9250
        Money expectedValue = new Money(new BigDecimal("9250"), usd);
        assertEquals(expectedValue, accountValue);
    }

    @Test
    @DisplayName("Should calculate asset value correctly")
    void calculateAssetValue_Success() {
        // Arrange
        ValidatedCurrency usd = ValidatedCurrency.USD;
        CryptoIdentifier bitcoinSymbol = new CryptoIdentifier("BTC", "Bitcoin", AssetType.CRYPTO, "USD", null);
        
        Asset bitcoin = Asset.builder()
            .assetId(assetId1)
            .assetIdentifier(bitcoinSymbol)
            .quantity(new BigDecimal("0.5"))
            .costBasis(new Money(new BigDecimal("20000"), usd))
            .currency(usd)
            .acquiredOn(LocalDateTime.now().minusYears(1).toInstant(ZoneOffset.UTC))
            .lastSystemInteraction(Instant.now())
            .build();

        when(marketDataService.getCurrentPrice(bitcoinSymbol))
            .thenReturn(new Money(new BigDecimal("50000"), usd));

        // Act
        Money assetValue = valuationService.calculateAssetValue(bitcoin, marketDataService, Instant.now());

        // Assert
        // 0.5 BTC * $50000 = $25000
        Money expectedValue = new Money(new BigDecimal("25000"), usd);
        assertEquals(expectedValue, assetValue);
    }

    @Test
    @DisplayName("Should throw exception when market data service fails")
    void calculateAssetValue_MarketDataServiceFails() {
        // Arrange
        MarketIdentifier symbol = new MarketIdentifier("INVALID", null, AssetType.STOCK, "INVALID", "INVALID", null);
        Asset asset = createAsset("asset-1", symbol, "10", ValidatedCurrencyCAD);

        when(marketDataService.getCurrentPrice(symbol))
            .thenThrow(new MarketDataNotFoundException("Price not found for symbol: INVALID"));

        // Act & Assert
        assertThrows(MarketDataNotFoundException.class, () -> {
            valuationService.calculateAssetValue(asset, marketDataService, Instant.now());
        });
    }

    @Test
    @DisplayName("Should call getHistoricalPrice when asOfDate is in the past")
    void testCalculateAssetValue_HistoricalPath() {
        // 1. Setup
        PortfolioValuationService service = new PortfolioValuationService();
        MarketDataService marketDataService = mock(MarketDataService.class);
        
        Asset mockAsset = mock(Asset.class);
        AssetIdentifier assetId = mock(AssetIdentifier.class);
        
        // Set a date well in the past (e.g., 10 days ago)
        Instant historicalInstant = Instant.now().minus(Duration.ofDays(10));
        LocalDateTime expectedLdt = LocalDateTime.ofInstant(historicalInstant, ZoneOffset.UTC);
        
        when(mockAsset.getAssetIdentifier()).thenReturn(assetId);
        when(mockAsset.getQuantity()).thenReturn(new BigDecimal("10"));
        
        // Mock the historical price response
        Money historicalPrice = Money.of(150.00, "CAD");
        when(marketDataService.getHistoricalPrice(eq(assetId), eq(expectedLdt)))
            .thenReturn(historicalPrice);

        // 2. Execute
        Money result = service.calculateAssetValue(mockAsset, marketDataService, historicalInstant);

        // 3. Verify
        // Ensure the result is Price (150) * Quantity (10) = 1500
        assertEquals(new BigDecimal("1500.00"), result.amount().setScale(2, RoundingMode.HALF_UP));
        
        // Verify that the historical method was the one actually called
        verify(marketDataService, times(1)).getHistoricalPrice(any(), any());
        verify(marketDataService, never()).getCurrentPrice(any());
    }
    
    @Nested
    @DisplayName("Tests for current price path (the 'if' branch)")
    class CurrentPriceTests {

        @Test
        @DisplayName("Should call getCurrentPrice when asOfDate is null")
        void testCalculateAssetValue_NullDate() {
            PortfolioValuationService service = new PortfolioValuationService();
            MarketDataService marketDataService = mock(MarketDataService.class);
            Asset mockAsset = mock(Asset.class);
            AssetIdentifier assetId = mock(AssetIdentifier.class);

            when(mockAsset.getAssetIdentifier()).thenReturn(assetId);
            when(mockAsset.getQuantity()).thenReturn(new BigDecimal("5"));
            when(marketDataService.getCurrentPrice(assetId)).thenReturn(Money.of(100, "CAD"));

            // Act: Pass null for asOfDate
            Money result = service.calculateAssetValue(mockAsset, marketDataService, null);

            // Assert: 100 * 5 = 500
            assertEquals(new BigDecimal("500.00"), result.amount().setScale(2, RoundingMode.HALF_UP));
            verify(marketDataService).getCurrentPrice(assetId);
            verify(marketDataService, never()).getHistoricalPrice(any(), any());
        }

        @Test
        @DisplayName("Should call getCurrentPrice when asOfDate is very recent (e.g., 1 second ago)")
        void testCalculateAssetValue_RecentDate() {
            PortfolioValuationService service = new PortfolioValuationService();
            MarketDataService marketDataService = mock(MarketDataService.class);
            Asset mockAsset = mock(Asset.class);
            AssetIdentifier assetId = mock(AssetIdentifier.class);

            // Set date to 1 second ago (well within the 5-second threshold)
            Instant recentInstant = Instant.now().minusSeconds(1);

            when(mockAsset.getAssetIdentifier()).thenReturn(assetId);
            when(mockAsset.getQuantity()).thenReturn(new BigDecimal("2"));
            when(marketDataService.getCurrentPrice(assetId)).thenReturn(Money.of(50, "CAD"));

            // Act
            service.calculateAssetValue(mockAsset, marketDataService, recentInstant);

            // Assert
            verify(marketDataService).getCurrentPrice(assetId);
        }
    }

    // Helper method to create assets for tests
    private Asset createAsset(String id, AssetIdentifier symbol, String quantity, ValidatedCurrency currency) {
        return Asset.builder()
            .assetId(AssetId.randomId())
            .assetIdentifier(symbol)
            .currency(currency)
            .quantity(new BigDecimal(quantity))
            .costBasis(new Money(BigDecimal.ZERO, ValidatedCurrencyCAD))
            .acquiredOn(Instant.now())
            .lastSystemInteraction(Instant.now())
            .build();
    }
}
