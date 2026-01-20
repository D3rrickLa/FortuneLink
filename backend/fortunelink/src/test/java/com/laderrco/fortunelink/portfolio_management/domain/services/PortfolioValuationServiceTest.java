package com.laderrco.fortunelink.portfolio_management.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.classfile.ClassFile.Option;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.MarketDataException;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.ErrorType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CryptoIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class PortfolioValuationServiceTest {
    private ValidatedCurrency ValidatedCurrencyCAD = ValidatedCurrency.CAD;
    private AccountId accountId1;
    private AccountId accountId2;
    private PortfolioId portfolioId1;
    private UserId userId1;
    private AssetId assetId1;

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private PortfolioValuationService valuationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        valuationService = new PortfolioValuationService(marketDataService, exchangeRateService);
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
        when(marketDataService.getCurrentPrice(appleSymbol)).thenReturn(currentPrice);
        // when(exchangeRateService.convert(currentPrice, usd));

        // Act
        Money totalValue = valuationService.calculateTotalValue(portfolio, Instant.now());

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
        Instant evaluationTime = Instant.now();
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

        when(marketDataService.getCurrentPrice(appleSymbol)).thenReturn(currentPrice);
        // IMPORTANT: Return the actual value if converting to the same currency,
        // or just stub it to return the input amount for this test.
        when(exchangeRateService.convert(any(Money.class), eq(usd), any(Instant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0)); // Returns the input Money as is

        // Act
        Money totalValue = valuationService.calculateTotalValue(portfolio, evaluationTime); // this was total assets
                                                                                            // before

        // Assert
        // 10 shares * $180 = $1800 + $500 cash = $2300
        Money expectedValue = new Money(new BigDecimal("2300"), usd);
        assertEquals(expectedValue, totalValue);

        verify(marketDataService, times(1)).getCurrentPrice(appleSymbol);
    }

    @Test
    @DisplayName("Should calculate portfolio value with multiple accounts and assets")
    void calculateTotalValue_MultipleAccountsAndAssets() {
        // --- Arrange ---
        ValidatedCurrency cad = ValidatedCurrency.CAD;

        // Identifiers
        MarketIdentifier shopifySymbol = new MarketIdentifier("SHOP", null, AssetType.STOCK, "Shopify", "CAD", null);
        MarketIdentifier ryCSymbol = new MarketIdentifier("RY", null, AssetType.STOCK, "Royal Bank of Canada", "CAD",
                null);
        MarketIdentifier vfvSymbol = new MarketIdentifier("VFV", null, AssetType.ETF, "Vanguard S&P 500 ETF", "CAD",
                null);

        // Assets
        Asset shopifyStock = createAsset("asset-1", shopifySymbol, "50", cad);
        Asset royalBankStock = createAsset("asset-2", ryCSymbol, "100", cad);
        Asset vfvEtf = createAsset("asset-3", vfvSymbol, "200", cad);

        // Accounts
        Account tfsaAccount = Account.builder()
                .accountId(accountId1)
                .baseCurrency(cad)
                .name("TFSA")
                .accountType(AccountType.INVESTMENT)
                .systemCreationDate(Instant.now())
                .lastSystemInteraction(Instant.now())
                .cashBalance(new Money(new BigDecimal("1000"), cad))
                .assets(List.of(shopifyStock, royalBankStock))
                .build();

        Account rrspAccount = Account.builder()
                .accountId(accountId2)
                .baseCurrency(cad)
                .name("RRSP")
                .accountType(AccountType.INVESTMENT)
                .systemCreationDate(Instant.now())
                .lastSystemInteraction(Instant.now())
                .cashBalance(new Money(new BigDecimal("2000"), cad))
                .assets(List.of(vfvEtf))
                .build();

        Portfolio portfolio = Portfolio.builder()
                .portfolioId(portfolioId1)
                .accounts(List.of(tfsaAccount, rrspAccount))
                .userId(userId1)
                .systemCreationDate(Instant.now())
                .lastUpdatedAt(Instant.now())
                .portfolioCurrencyPreference(cad)
                .build();

        // FIX: Mock getCurrentQuote with Optional results
        when(marketDataService.getCurrentQuote(shopifySymbol))
                .thenReturn(Optional.of(createQuote(shopifySymbol, 80, cad)));
        when(marketDataService.getCurrentQuote(ryCSymbol))
                .thenReturn(Optional.of(createQuote(ryCSymbol, 120, cad)));
        when(marketDataService.getCurrentQuote(vfvSymbol))
                .thenReturn(Optional.of(createQuote(vfvSymbol, 100, cad)));

        // Handle currency conversion (CAD to CAD identity)
        lenient().when(exchangeRateService.convert(any(), eq(cad), any()))
                .thenAnswer(inv -> inv.getArgument(0));

        // --- Act ---
        Money totalValue = valuationService.calculateTotalValue(portfolio, null);

        // --- Assert ---
        // TFSA: (50 * $80) + (100 * $120) + $1000 = $17000
        // RRSP: (200 * $100) + $2000 = $22000
        // Total: $39000
        Money expectedValue = new Money(new BigDecimal("39000"), cad);
        assertEquals(expectedValue.amount().setScale(2), totalValue.amount().setScale(2));

        verify(marketDataService, times(1)).getCurrentQuote(shopifySymbol);
        verify(marketDataService, times(1)).getCurrentQuote(ryCSymbol);
        verify(marketDataService, times(1)).getCurrentQuote(vfvSymbol);
    }

    // Helper to minimize repetitive quote creation
    private MarketAssetQuote createQuote(MarketIdentifier id, double price, ValidatedCurrency currency) {
        return new MarketAssetQuote(
                id,
                Money.of(BigDecimal.valueOf(price), currency.getCode()),
                null, null, null, null, null, null, null, null,
                Instant.now(),
                "FMP");
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
        Money totalValue = valuationService.calculateTotalValue(portfolio, Instant.now());

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
        Money totalValue = valuationService.calculateTotalValue(portfolio, Instant.now());

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
        MarketAssetQuote currentTslaQuote = new MarketAssetQuote(
                teslaSymbol,
                Money.of(250.00, "USD"), // Changed to 100 to match your 500 total assetion
                null, null, null, null, null, null, null, null,
                Instant.now(),
                "FMP");
        when(marketDataService.getCurrentQuote(teslaSymbol))
                .thenReturn(Optional.of(currentTslaQuote));

        // Act
        Money accountValue = valuationService.calculateAccountValue(account, ValidatedCurrency.USD, Instant.now());

        // Assert
        // 25 shares * $250 + $3000 cash = $9250
        Money expectedValue = new Money(new BigDecimal("9250"), usd);
        assertEquals(expectedValue, accountValue);
    }

    @Test
    @DisplayName("Should calculate account value correctly in target currency")
    void calculateAccountValue_Success_TriggerExchangeService() {
        // Arrange
        ValidatedCurrency usd = ValidatedCurrency.USD;
        ValidatedCurrency eur = ValidatedCurrency.EUR;

        MarketIdentifier teslaSymbol = new MarketIdentifier("TSLA", null, AssetType.STOCK, "Tesla", "USD", null);

        Asset teslaStock = createAsset("asset-1", teslaSymbol, "25", usd);

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

        MarketAssetQuote currentTslaQuote = new MarketAssetQuote(
                teslaSymbol,
                Money.of(250.00, "USD"), // Changed to 100 to match your 500 total assetion
                null, null, null, null, null, null, null, null,
                Instant.now(),
                "FMP");
        // Mock the market price for the asset
        when(marketDataService.getCurrentQuote(teslaSymbol))
                .thenReturn(Optional.of(currentTslaQuote));

        // Mock the exchange conversion from USD to EUR
        when(exchangeRateService.convert(any(Money.class), eq(eur), any()))
                .thenAnswer(invocation -> {
                    Money money = invocation.getArgument(0);
                    // simple mock conversion: assume $1 = 0.85 EUR
                    BigDecimal rate = new BigDecimal("0.85");
                    return new Money(money.amount().multiply(rate), eur);
                });

        // Act
        Money accountValue = valuationService.calculateAccountValue(account, eur, Instant.now());

        // Assert
        // Calculation: 25 shares * $250 = 6250 + $3000 cash = 9250 USD
        // Converted to EUR: 9250 * 0.85 = 7862.5 EUR
        Money expectedValue = new Money(new BigDecimal("7862.5"), eur);

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

        MarketAssetQuote currentBTCQuote = new MarketAssetQuote(
                bitcoinSymbol,
                Money.of(50000.00, "USD"), // Changed to 100 to match your 500 total assetion
                null, null, null, null, null, null, null, null,
                Instant.now(),
                "FMP");

        when(marketDataService.getCurrentQuote(bitcoinSymbol))
                .thenReturn(Optional.of(currentBTCQuote));

        // Act
        Money assetValue = valuationService.calculateAssetValue(bitcoin, ValidatedCurrency.USD, Instant.now());

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
                .thenThrow(MarketDataException.symbolNotFound("INVALID"));

        // Act & Assert
        assertThrows(MarketDataException.class, () -> {
            valuationService.calculateAssetValue(asset, ValidatedCurrency.USD, Instant.now());
        });
    }

    @Test
    @DisplayName("Should call getHistoricalPrice when asOfDate is in the past")
    void testCalculateAssetValue_HistoricalPath() {
        // 1. Setup - Initialize Mocks FIRST
        MarketDataService mockMarketData = mock(MarketDataService.class);
        ExchangeRateService mockExchange = mock(ExchangeRateService.class);
        PortfolioValuationService service = new PortfolioValuationService(mockMarketData, mockExchange);

        Asset mockAsset = mock(Asset.class);
        MarketIdentifier assetId = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "CAD", null);

        Instant historicalInstant = Instant.now().minus(Duration.ofDays(10));
        // Truncate to avoid nanosecond mismatch during Mockito's argument comparison
        LocalDateTime expectedLdt = LocalDateTime.ofInstant(historicalInstant, ZoneOffset.UTC);

        when(mockAsset.getAssetIdentifier()).thenReturn(assetId);
        when(mockAsset.getQuantity()).thenReturn(new BigDecimal("10"));

        // Mock the historical price response
        Money historicalPrice = new Money(new BigDecimal("150.00"), ValidatedCurrency.CAD);
        MarketAssetQuote historicalQuote = new MarketAssetQuote(
                assetId, historicalPrice, historicalPrice, historicalPrice, historicalPrice, historicalPrice, null,
                null, null, null, historicalInstant, "FMP");

        when(mockMarketData.getHistoricalQuote(eq(assetId), eq(expectedLdt)))
                .thenReturn(Optional.of(historicalQuote));

        // Mock the exchange rate service (since CAD != USD)
        // If you don't mock this, 'result' will be null!
        Money expectedConvertedValue = new Money(new BigDecimal("1100.00"), ValidatedCurrency.USD);
        when(mockExchange.convert(any(Money.class), eq(ValidatedCurrency.USD), eq(historicalInstant)))
                .thenReturn(expectedConvertedValue);

        // 2. Execute
        Money result = service.calculateAssetValue(mockAsset, ValidatedCurrency.USD, historicalInstant);

        // 3. Verify
        assertNotNull(result);
        assertEquals(new BigDecimal("1100.00"), result.amount().setScale(2));

        verify(mockMarketData, times(1)).getHistoricalQuote(eq(assetId), any());
        verify(mockMarketData, never()).getCurrentPrice(any());
    }

    @Nested
    @DisplayName("Tests for current price path (the 'if' branch)")
    class CurrentPriceTests {

        @Test
        @DisplayName("Should call getCurrentPrice when asOfDate is null")
        void testCalculateAssetValue_NullDate() {
            // 1. Initialize Mocks
            // Use the class-level @Mock fields or local ones, but be consistent!
            // Here we use local ones to match your style
            MarketDataService mockMarketData = mock(MarketDataService.class);
            ExchangeRateService mockExchange = mock(ExchangeRateService.class);

            // 2. Inject the specific mocks used in step 1 into the service
            PortfolioValuationService service = new PortfolioValuationService(mockMarketData, mockExchange);

            // 3. Setup data
            MarketIdentifier assetId = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);
            Asset mockAsset = mock(Asset.class);

            MarketAssetQuote currentAaplQuote = new MarketAssetQuote(
                    assetId,
                    Money.of(100.00, "USD"), // Changed to 100 to match your 500 total assetion
                    null, null, null, null, null, null, null, null,
                    Instant.now(),
                    "FMP");

            when(mockAsset.getAssetIdentifier()).thenReturn(assetId);
            when(mockAsset.getQuantity()).thenReturn(new BigDecimal("5"));

            // FIX: Stub the local 'mockMarketData' object, NOT the class field
            // 'marketDataService'
            when(mockMarketData.getCurrentQuote(any(MarketIdentifier.class)))
                    .thenReturn(Optional.of(currentAaplQuote));

            // 4. Act
            Money result = service.calculateAssetValue(mockAsset, ValidatedCurrency.USD, null);

            // 5. Assert
            // 5 shares * 100 = 500
            assertEquals(new BigDecimal("500"), result.amount().setScale(0));
            verify(mockMarketData).getCurrentQuote(assetId);
        }

        @Test
        @DisplayName("Should call getCurrentPrice when asOfDate is very recent (e.g., 1 second ago)")
        void testCalculateAssetValue_RecentDate() {
            Asset mockAsset = mock(Asset.class);
            AssetIdentifier assetId = mock(AssetIdentifier.class);
            MarketAssetQuote currentAaplQuote = new MarketAssetQuote(
                    assetId,
                    Money.of(180.0, "USD"),
                    null, null, null, null, null, null, null, null,
                    Instant.now(),
                    "FMP");
            // Set date to 1 second ago (well within the 5-second threshold)
            Instant recentInstant = Instant.now().minusSeconds(1);

            when(mockAsset.getAssetIdentifier()).thenReturn(assetId);
            when(mockAsset.getQuantity()).thenReturn(new BigDecimal("2"));
            when(marketDataService.getCurrentQuote(assetId)).thenReturn(Optional.of(currentAaplQuote));

            // Act
            valuationService.calculateAssetValue(mockAsset, ValidatedCurrency.USD, recentInstant);

            // Assert
            verify(marketDataService).getCurrentQuote(assetId);
        }

        @Test
        @DisplayName("Should throw MarketDataException when historical quote is missing")
        void calculateAssetValue_HistoricalQuoteMissing_ThrowsException() {
            // --- Arrange ---
            MarketIdentifier appleSymbol = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);
            Asset appleStock = createAsset("asset-1", appleSymbol, "10", ValidatedCurrency.USD);

            // Ensure we are well outside the 5-second "current" window
            Instant historicalInstant = Instant.now().minus(Duration.ofDays(1));

            // Mock the historical service to return empty
            when(marketDataService.getHistoricalQuote(eq(appleSymbol), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            // --- Act ---
            MarketDataException exception = assertThrows(MarketDataException.class, () -> {
                valuationService.calculateAssetValue(appleStock, ValidatedCurrency.USD, historicalInstant);
            });

            // --- Assert ---
            // 1. Check that the message starts with "Historical"
            assertTrue(exception.getMessage().startsWith("Historical price unavailable"),
                    "Expected historical error message, but got: " + exception.getMessage());

            // 2. Check that it contains the ID (using the object's toString to be safe)
            assertTrue(exception.getMessage().contains(appleSymbol.toString()),
                    "Message missing asset identifier string representation");

            // 3. Verify the ErrorType
            assertEquals(ErrorType.DATA_UNAVAILABLE, exception.getErrorType());
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
