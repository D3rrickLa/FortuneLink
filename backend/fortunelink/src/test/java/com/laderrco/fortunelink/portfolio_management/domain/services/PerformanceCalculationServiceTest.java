package com.laderrco.fortunelink.portfolio_management.domain.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.FeeType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CashIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CryptoIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.exceptions.MarketDataUnavailableException;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PerformanceCalculationServiceTest {

    private PerformanceCalculationService performanceService;
    private PortfolioId portfolioId1;
    private UserId userId1;
    private AssetId assetId1;
    private AssetId assetId2;
    
    @Mock
    private MarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        performanceService = new PerformanceCalculationService();
        portfolioId1 = PortfolioId.randomId();
        userId1 = UserId.randomId();
        assetId1 = AssetId.randomId();
        assetId2 = AssetId.randomId();
    }

    @Nested
    @DisplayName("Total Return Calculation Tests")
    class TotalReturnTests {

        @Test
        @DisplayName("Should calculate positive total return correctly")
        void calculateTotalReturn_PositiveReturn() {
            // Arrange
            ValidatedCurrency usd = ValidatedCurrency.USD;
            Portfolio portfolio = createPortfolioWithTransactions(usd);
            
            // Mock current prices to give us a gain
            // Asset has 10 shares of AAPL, let's say current price is $180
            when(marketDataService.getCurrentPrice(new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null)))
                .thenReturn(new Money(new BigDecimal("180"), usd));

            // Act
            Percentage totalReturn = performanceService.calculateTotalReturn(portfolio, marketDataService);

            // Assert
            // Invested: $1500 (10 shares * $150) + $500 deposit = $2000
            // Current: (10 * $180) + $500 cash = $2300
            // Return: ($2300 - $2000) / $2000 = 15%
            assertEquals(new BigDecimal("15.00"), totalReturn.value().multiply(BigDecimal.valueOf(100)).setScale(2));
        }

        @Test
        @DisplayName("Should calculate negative total return correctly")
        void calculateTotalReturn_NegativeReturn() {
            // Arrange
            ValidatedCurrency usd = ValidatedCurrency.USD;
            Portfolio portfolio = createPortfolioWithTransactions(usd);
            
            // Mock current prices to give us a loss
            when(marketDataService.getCurrentPrice(new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null)))
                .thenReturn(new Money(new BigDecimal("120"), usd));

            // Act
            Percentage totalReturn = performanceService.calculateTotalReturn(portfolio, marketDataService);

            // Assert
            // Invested: $2000
            // Current: (10 * $120) + $500 cash = $1700
            // Return: ($1700 - $2000) / $2000 = -15%
            assertEquals(new BigDecimal("-15.00"), totalReturn.value().multiply(BigDecimal.valueOf(100)).setScale(2));
        }

        @Test
        @DisplayName("Should return 0% when no money invested")
        void calculateTotalReturn_NoInvestment() {
            // Arrange
            ValidatedCurrency usd = ValidatedCurrency.USD;
            Portfolio portfolio = Portfolio.builder()
                .portfolioId(portfolioId1)
                .userId(userId1)
                .accounts(List.of())
                .portfolioCurrencyPreference(usd)
                .systemCreationDate(Instant.now())
                .lastUpdatedAt(Instant.now())
                .build();

            // Act
            Percentage totalReturn = performanceService.calculateTotalReturn(portfolio, marketDataService);

            // Assert
            assertEquals(Percentage.of(0), totalReturn);
        }

        @Test
        @DisplayName("Should calculate return after withdrawal")
        void calculateTotalReturn_WithWithdrawal() {
            // Arrange
            ValidatedCurrency cad = ValidatedCurrency.CAD;
            MarketIdentifier shopifySymbol = new MarketIdentifier("SHOP", null, AssetType.STOCK, "Shopify", "CAD", null);
            
            Asset shopStock = createAsset("asset-1", shopifySymbol, "20", "2000", cad);
            
            List<Transaction> transactions = new ArrayList<>();
            transactions.add(createBuyTransaction("tx-1", shopifySymbol, "20", "100", cad));
            transactions.add(createDepositTransaction("tx-2", "1000", cad));
            transactions.add(createWithdrawalTransaction("tx-3", "500", cad));
            
            Account account = createAccount("account-1", cad, "500", 
                List.of(shopStock), transactions);
            
            Portfolio portfolio = Portfolio.builder()
                .portfolioId(portfolioId1)
                .userId(userId1)
                .accounts(List.of(account))
                .portfolioCurrencyPreference(cad)
                .systemCreationDate(Instant.now())
                .lastUpdatedAt(Instant.now())
                .build();

            when(marketDataService.getCurrentPrice(shopifySymbol))
                .thenReturn(new Money(new BigDecimal("120"), cad));

            // Act
            Percentage totalReturn = performanceService.calculateTotalReturn(portfolio, marketDataService);

            // Assert
            // Invested: $2000 (buy) + $1000 (deposit) - $500 (withdrawal) = $2500
            // Current: (20 * $120) + $500 cash = $2900
            // Return: ($2900 - $2500) / $2500 = 16%
            assertEquals(new BigDecimal("16.00"), totalReturn.value().multiply(BigDecimal.valueOf(100)).setScale(2));
        }
    }

    @Nested
    @DisplayName("Realized Gains Calculation Tests")
    class RealizedGainsTests {

        @Test
        @DisplayName("Should calculate realized gain from single sell transaction")
        void calculateRealizedGains_SingleSell_Profit() throws AccountNotFoundException {
            // Arrange
            ValidatedCurrency usd = ValidatedCurrency.USD;
            MarketIdentifier teslaSymbol = new MarketIdentifier("TSLA", null, AssetType.STOCK, "Tesla", "USD", null);
            
            // Bought 100 shares at $200, sold 50 at $250
            Asset teslaStock = createAsset("asset-1", teslaSymbol, "50", "10000", usd);
            
            List<Transaction> transactions = new ArrayList<>();
            transactions.add(createBuyTransaction("tx-1", teslaSymbol, "100", "200", usd));
            transactions.add(createSellTransaction("tx-2", teslaSymbol, "50", "250", usd));
            
            Account account = createAccount("account-1", usd, "12500", 
                List.of(teslaStock), transactions);
            
            Portfolio portfolio = Portfolio.builder()
                .portfolioId(portfolioId1)
                .userId(userId1)
                .accounts(List.of(account))
                .portfolioCurrencyPreference(usd)
                .systemCreationDate(Instant.now())
                .lastUpdatedAt(Instant.now())
                .build();

            // Act
            Money realizedGains = performanceService.calculateRealizedGains(portfolio, transactions);

            // Assert
            // Average cost per unit: $20,000 / 100 = $200
            // Cost basis for 50 shares: 50 * $200 = $10,000
            // Sale proceeds: 50 * $250 = $12,500
            // Realized gain: $12,500 - $10,000 = $2,500
            assertEquals(new Money(new BigDecimal("2500"), usd), realizedGains);
        }

        @Test
        @DisplayName("Should calculate realized loss from sell transaction")
        void calculateRealizedGains_SingleSell_Loss() throws AccountNotFoundException {
            // Arrange
            ValidatedCurrency usd = ValidatedCurrency.USD;
            MarketIdentifier gamestopSymbol = new MarketIdentifier("GME", null, AssetType.STOCK, "GameStop", "USD", null);
            
            // Bought 100 shares at $300, sold 100 at $150
            Asset gamestopStock = createAsset("asset-1", gamestopSymbol, "0", "0", usd);
            
            List<Transaction> transactions = new ArrayList<>();
            transactions.add(createBuyTransaction("tx-1", gamestopSymbol, "100", "300", usd));
            transactions.add(createSellTransaction("tx-2", gamestopSymbol, "100", "150", usd));
            
            Account account = createAccount("account-1", usd, "15000", 
                List.of(gamestopStock), Collections.emptyList());

              
            Portfolio portfolio = Portfolio.builder()
            .portfolioId(portfolioId1)
            .userId(userId1)
            .accounts(List.of(account))
            .portfolioCurrencyPreference(usd)
            .systemCreationDate(Instant.now())
            .lastUpdatedAt(Instant.now())
            .build();            
            portfolio.recordTransaction(account.getAccountId(), transactions.get(0));
            portfolio.recordTransaction(account.getAccountId(), transactions.get(1));

            // Act
            Money realizedGains = performanceService.calculateRealizedGains(portfolio, transactions);

            // // Assert
            // // Cost basis: 100 * $300 = $30,000
            // // Sale proceeds: 100 * $150 = $15,000
            // // Realized loss: $15,000 - $30,000 = -$15,000
            assertEquals(new Money(new BigDecimal("-15000"), usd), realizedGains);
        }

        @Test
        @DisplayName("Should calculate realized gains from multiple sell transactions")
        void calculateRealizedGains_MultipleSells() throws AccountNotFoundException {
            // Arrange
            ValidatedCurrency cad = ValidatedCurrency.CAD;
            MarketIdentifier appleSymbol = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);
            
            // Multiple buys and sells
            Asset appleStock = createAsset("asset-1", appleSymbol, "20", "3000", cad);
            
            List<Transaction> transactions = new ArrayList<>();
            transactions.add(createBuyTransaction("tx-1", appleSymbol, "100", "150", cad));
            transactions.add(createSellTransaction("tx-2", appleSymbol, "30", "180", cad));
            transactions.add(createSellTransaction("tx-3", appleSymbol, "50", "200", cad));
            
            Account account = createAccount("account-1", cad, "15400", 
                List.of(appleStock), transactions);
            
            Portfolio portfolio = Portfolio.builder()
                .portfolioId(portfolioId1)
                .userId(userId1)
                .accounts(List.of(account))
                .portfolioCurrencyPreference(cad)
                .systemCreationDate(Instant.now())
                .lastUpdatedAt(Instant.now())
                .build();

            // Act
            Money realizedGains = performanceService.calculateRealizedGains(portfolio, transactions);

            // Assert
            // Average cost: $15,000 / 100 = $150
            // First sell: (30 * $180) - (30 * $150) = $5,400 - $4,500 = $900
            // Second sell: (50 * $200) - (50 * $150) = $10,000 - $7,500 = $2,500
            // Total: $900 + $2,500 = $3,400
            assertEquals(new Money(new BigDecimal("3400"), cad), realizedGains);
        }

        @Test
        @DisplayName("Should return zero when no sell transactions")
        void calculateRealizedGains_NoSells() throws AccountNotFoundException {
            // Arrange
            ValidatedCurrency usd = ValidatedCurrency.USD;
            MarketIdentifier symbol = new MarketIdentifier("MSFT", null, AssetType.STOCK, "Microsoft", "USD", null);
            
            Asset msftStock = createAsset("asset-1", symbol, "100", "30000", usd);
            
            List<Transaction> transactions = new ArrayList<>();
            transactions.add(createBuyTransaction("tx-1", symbol, "100", "300", usd));
            transactions.add(createDepositTransaction("tx-2", "5000", usd));
            
            Account account = createAccount("account-1", usd, "5000", 
                List.of(msftStock), transactions);
            
            Portfolio portfolio = Portfolio.builder()
                .portfolioId(portfolioId1)
                .userId(userId1)
                .accounts(List.of(account))
                .portfolioCurrencyPreference(usd)
                .systemCreationDate(Instant.now())
                .lastUpdatedAt(Instant.now())
                .build();

            // Act
            Money realizedGains = performanceService.calculateRealizedGains(portfolio, transactions);

            // Assert
            assertEquals(Money.ZERO(usd), realizedGains);
        }

        // This test is NOT needed anymore, as we bypass the account lookup, we use the transactions
        // directly
        @Deprecated
        @Disabled
        @Test
        @DisplayName("Should throw exception when account not found")
        void calculateRealizedGains_AccountNotFound() {
            // Arrange
            ValidatedCurrency usd = ValidatedCurrency.USD;
            MarketIdentifier symbol = new MarketIdentifier("INVALID", null, AssetType.OTHER, "INVALID", "INVALID", null);
            
            List<Transaction> transactions = new ArrayList<>();
            transactions.add(createSellTransaction("tx-1", symbol, "10", "100", usd));
            
            Portfolio portfolio = Portfolio.builder()
                .portfolioId(portfolioId1)
                .userId(userId1)
                .accounts(List.of())
                .portfolioCurrencyPreference(usd)
                .systemCreationDate(Instant.now())
                .lastUpdatedAt(Instant.now())
                .build();

            // Act & Assert
            assertThrows(AccountNotFoundException.class, () -> {
                performanceService.calculateRealizedGains(portfolio, transactions);
            });
        }
    }

    @Nested
    @DisplayName("Unrealized Gains Calculation Tests")
    class UnrealizedGainsTests {

        @Test
        @DisplayName("Should calculate unrealized gains for single asset")
        void calculateUnrealizedGains_SingleAsset_Gain() {
            // Arrange
            ValidatedCurrency usd = ValidatedCurrency.USD;
            MarketIdentifier nvidiaSymbol = new MarketIdentifier("NVDA", null, AssetType.STOCK, "Nvidia", "USD", null);
            
            // Bought at $400 per share, now worth $500
            Asset nvidiaStock = Asset.builder()
                .assetId(assetId1)
                .assetIdentifier(nvidiaSymbol)
                .quantity(new BigDecimal("50"))
                .currency(usd)
                .costBasis(new Money(new BigDecimal("20000"), usd))
                .acquiredOn(LocalDateTime.now().minusMonths(3).toInstant(ZoneOffset.UTC))
                .lastSystemInteraction(Instant.now())
                .build();
            
            Account account = createAccount("account-1", usd, "0", 
                List.of(nvidiaStock), new ArrayList<>());
            
            Portfolio portfolio = Portfolio.builder()
                .portfolioId(portfolioId1)
                .userId(userId1)
                .accounts(List.of(account))
                .portfolioCurrencyPreference(usd)
                .systemCreationDate(Instant.now())
                .lastUpdatedAt(Instant.now())
                .build();

            when(marketDataService.getCurrentPrice(nvidiaSymbol))
                .thenReturn(new Money(new BigDecimal("500"), usd));

            // Act
            Money unrealizedGains = performanceService.calculateUnrealizedGains(
                portfolio, marketDataService);

            // Assert
            // Current value: 50 * $500 = $25,000
            // Cost basis: $20,000
            // Unrealized gain: $25,000 - $20,000 = $5,000
            assertEquals(new Money(new BigDecimal("5000"), usd), unrealizedGains);
        }

        @Test
        @DisplayName("Should calculate unrealized losses for asset")
        void calculateUnrealizedGains_SingleAsset_Loss() {
            // Arrange
            ValidatedCurrency cad = ValidatedCurrency.CAD;
            MarketIdentifier symbol = new MarketIdentifier("META", null, AssetType.STOCK, "Nvidia", "USD", null);
            
            // Bought at $350, now worth $250
            Asset metaStock = Asset.builder()
                .assetId(assetId1)
                .assetIdentifier(symbol)
                .quantity(new BigDecimal("100"))
                .currency(cad)
                .costBasis(new Money(new BigDecimal("35000"), cad))
                .acquiredOn(LocalDateTime.now().minusYears(1).toInstant(ZoneOffset.UTC))
                .lastSystemInteraction(Instant.now())
                .build();
            
            Account account = createAccount("account-1", cad, "0", 
                List.of(metaStock), new ArrayList<>());
            
            Portfolio portfolio = Portfolio.builder()
                .portfolioId(portfolioId1)
                .userId(userId1)
                .accounts(List.of(account))
                .portfolioCurrencyPreference(cad)
                .systemCreationDate(Instant.now())
                .lastUpdatedAt(Instant.now())
                .build();

            when(marketDataService.getCurrentPrice(symbol))
                .thenReturn(new Money(new BigDecimal("250"), cad));

            // Act
            Money unrealizedGains = performanceService.calculateUnrealizedGains(
                portfolio, marketDataService);

            // Assert
            // Current value: 100 * $250 = $25,000
            // Cost basis: $35,000
            // Unrealized loss: $25,000 - $35,000 = -$10,000
            assertEquals(new Money(new BigDecimal("-10000"), cad), unrealizedGains);
        }

        @Test
        @DisplayName("Should calculate unrealized gains across multiple assets")
        void calculateUnrealizedGains_MultipleAssets() {
            // Arrange
            ValidatedCurrency usd = ValidatedCurrency.USD;
            
            MarketIdentifier googleSymbol = new MarketIdentifier("GOOGL", null, AssetType.STOCK, "Google", "USD", null);
            MarketIdentifier amazonSymbol = new MarketIdentifier("AMZN", null, AssetType.STOCK, "Amazon", "USD", null);
            
            // Google: Cost $10,000, now worth $12,000 (gain $2,000)
            Asset googleStock = Asset.builder()
                .assetId(assetId1)
                .assetIdentifier(googleSymbol)
                .quantity(new BigDecimal("100"))
                .currency(usd)
                .costBasis(new Money(new BigDecimal("10000"), usd))
                .acquiredOn(LocalDateTime.now().minusMonths(6).toInstant(ZoneOffset.UTC))
                .lastSystemInteraction(Instant.now())
                .build();
            
            // Amazon: Cost $15,000, now worth $14,000 (loss $1,000)
            Asset amazonStock = Asset.builder()
                .assetId(assetId2)
                .assetIdentifier(amazonSymbol)
                .quantity(new BigDecimal("100"))
                .currency(usd)
                .costBasis(new Money(new BigDecimal("15000"), usd))
                .acquiredOn(LocalDateTime.now().minusMonths(4).toInstant(ZoneOffset.UTC))
                .lastSystemInteraction(Instant.now())
                .build();
            
            Account account = createAccount("account-1", usd, "0", 
                List.of(googleStock, amazonStock), new ArrayList<>());
            
            Portfolio portfolio = Portfolio.builder()
                .portfolioId(portfolioId1)
                .userId(userId1)
                .accounts(List.of(account))
                .portfolioCurrencyPreference(usd)
                .systemCreationDate(Instant.now())
                .lastUpdatedAt(Instant.now())
                .build();

            when(marketDataService.getCurrentPrice(googleSymbol))
                .thenReturn(new Money(new BigDecimal("120"), usd));
            when(marketDataService.getCurrentPrice(amazonSymbol))
                .thenReturn(new Money(new BigDecimal("140"), usd));

            // Act
            Money unrealizedGains = performanceService.calculateUnrealizedGains(
                portfolio, marketDataService);

            // Assert
            // Google: (100 * $120) - $10,000 = $2,000
            // Amazon: (100 * $140) - $15,000 = -$1,000
            // Total: $2,000 - $1,000 = $1,000
            assertEquals(new Money(new BigDecimal("1000"), usd), unrealizedGains);
        }

        @Test
        @DisplayName("Should calculate unrealized gains across multiple accounts")
        void calculateUnrealizedGains_MultipleAccounts() {
            // Arrange
            ValidatedCurrency cad = ValidatedCurrency.CAD;
            
            MarketIdentifier tfsaSymbol = new MarketIdentifier("VFV", null, AssetType.STOCK, "Vanguard S&P 500 ETF", "USD", null);
            MarketIdentifier rrspSymbol = new MarketIdentifier("XEQT", null, AssetType.STOCK, "iShares Core Equity ETF Portfolio", "USD", null);
            
            // TFSA Account
            Asset tfsaAsset = Asset.builder()
                .assetId(assetId1)
                .assetIdentifier(tfsaSymbol)
                .quantity(new BigDecimal("100"))
                .currency(cad)
                .costBasis(new Money(new BigDecimal("10000"), cad))
                .acquiredOn(LocalDateTime.now().minusYears(2).toInstant(ZoneOffset.UTC))
                .lastSystemInteraction(Instant.now())
                .build();
            
            Account tfsaAccount = createAccount("account-1", cad, "0", 
                List.of(tfsaAsset), new ArrayList<>());
            
            // RRSP Account
            Asset rrspAsset = Asset.builder()
                .assetId(assetId2)
                .assetIdentifier(rrspSymbol)
                .quantity(new BigDecimal("200"))
                .currency(cad)
                .costBasis(new Money(new BigDecimal("6000"), cad))
                .acquiredOn(LocalDateTime.now().minusYears(1).toInstant(ZoneOffset.UTC))
                .lastSystemInteraction(Instant.now())
                .build();
            
            Account rrspAccount = createAccount("account-2", cad, "0", 
                List.of(rrspAsset), new ArrayList<>());
            
            Portfolio portfolio = Portfolio.builder()
                .portfolioId(portfolioId1)
                .userId(userId1)
                .accounts(List.of(tfsaAccount, rrspAccount))
                .portfolioCurrencyPreference(cad)
                .systemCreationDate(Instant.now())
                .lastUpdatedAt(Instant.now())
                .build();

            when(marketDataService.getCurrentPrice(tfsaSymbol))
                .thenReturn(new Money(new BigDecimal("110"), cad));
            when(marketDataService.getCurrentPrice(rrspSymbol))
                .thenReturn(new Money(new BigDecimal("32"), cad));

            // Act
            Money unrealizedGains = performanceService.calculateUnrealizedGains(
                portfolio, marketDataService);

            // Assert
            // TFSA: (100 * $110) - $10,000 = $1,000
            // RRSP: (200 * $32) - $6,000 = $400
            // Total: $1,400
            assertEquals(new Money(new BigDecimal("1400"), cad), unrealizedGains);
        }

        @Test
        @DisplayName("Should return zero for portfolio with no assets")
        void calculateUnrealizedGains_NoAssets() {
            // Arrange
            ValidatedCurrency usd = ValidatedCurrency.USD;
            
            Account account = createAccount("account-1", usd, "10000", 
                List.of(), new ArrayList<>());
            
            Portfolio portfolio = Portfolio.builder()
                .portfolioId(portfolioId1)
                .userId(userId1)
                .accounts(List.of(account))
                .portfolioCurrencyPreference(usd)
                .systemCreationDate(Instant.now())
                .lastUpdatedAt(Instant.now())
                .build();

            // Act
            Money unrealizedGains = performanceService.calculateUnrealizedGains(
                portfolio, marketDataService);

            // Assert
            assertEquals(Money.ZERO(usd), unrealizedGains);
            verifyNoInteractions(marketDataService);
        }

        @Test
        @DisplayName("Should handle crypto assets correctly")
        void calculateUnrealizedGains_CryptoAsset() {
            // Arrange
            ValidatedCurrency usd = ValidatedCurrency.USD;
            CryptoIdentifier btcSymbol = new CryptoIdentifier("BTC", "Bitcoin", AssetType.CRYPTO, "USD", null);
            
            // Bought 2 BTC at $30,000 each, now worth $45,000 each
            Asset bitcoin = Asset.builder()
                .assetId(assetId1)
                .assetIdentifier(btcSymbol)
                .quantity(new BigDecimal("2"))
                .currency(usd)
                .costBasis(new Money(new BigDecimal("60000"), usd))
                .acquiredOn(LocalDateTime.now().minusMonths(8).toInstant(ZoneOffset.UTC))
                .lastSystemInteraction(Instant.now())
                .build();
            
            Account account = createAccount("account-1", usd, "0", 
                List.of(bitcoin), new ArrayList<>());
            
            Portfolio portfolio = Portfolio.builder()
                .portfolioId(portfolioId1)
                .userId(userId1)
                .accounts(List.of(account))
                .portfolioCurrencyPreference(usd)
                .systemCreationDate(Instant.now())
                .lastUpdatedAt(Instant.now())
                .build();

            when(marketDataService.getCurrentPrice(btcSymbol))
                .thenReturn(new Money(new BigDecimal("45000"), usd));

            // Act
            Money unrealizedGains = performanceService.calculateUnrealizedGains(
                portfolio, marketDataService);

            // Assert
            // Current value: 2 * $45,000 = $90,000
            // Cost basis: $60,000
            // Unrealized gain: $30,000
            assertEquals(new Money(new BigDecimal("30000"), usd), unrealizedGains);
        }
    }

    @Nested
    @DisplayName("Time-Weighted Return Tests")
    class TimeWeightedReturnTests {

        @Test
        @DisplayName("Should throw UnsupportedOperationException for TWR calculation")
        void calculateTimeWeightedReturn_NotImplemented() {
            // Arrange
            Portfolio portfolio = Portfolio.builder()
                .portfolioId(portfolioId1)
                .userId(userId1)
                .accounts(List.of())
                .portfolioCurrencyPreference(ValidatedCurrency.USD)
                .systemCreationDate(Instant.now())
                .lastUpdatedAt(Instant.now())
                .build();

            // Act & Assert
            assertThrows(UnsupportedOperationException.class, () -> {
                performanceService.calculateTimeWeightedReturn(portfolio);
            });
        }
    }

    /* OTHER TESTS FOR BUSINESS CASE */
    @Nested
    @DisplayName("Fractional and Fees Tests")
    class FractionalAndFeesTests {
        private TransactionId transactionId1;
        private TransactionId transactionId2;

        @BeforeEach
        void setup() {
            transactionId1 = TransactionId.randomId();
            transactionId2 = TransactionId.randomId();
        }

        @Test
        @DisplayName("Should handle fractional shares in realized gains calculation")
        void calculateRealizedGains_FractionalShares() {
            // Arrange
            ValidatedCurrency usd = ValidatedCurrency.USD;
            MarketIdentifier btcSymbol = new MarketIdentifier(
                "BTC", 
                null, 
                AssetType.CRYPTO, 
                "Bitcoin", 
                "USD", 
                null
            );
            
            // Buy 0.5 BTC at $50,000 per BTC = $25,000 total
            // Sell 0.3 BTC at $60,000 per BTC = $18,000 proceeds
            // Cost basis for 0.3 BTC: (0.3 / 0.5) * $25,000 = $15,000
            // Realized gain: $18,000 - $15,000 = $3,000
            
            Asset btcAsset = createAsset("asset-1", btcSymbol, "0.2", "10000", usd); // 0.2 remaining
            
            List<Transaction> transactions = new ArrayList<>();
            transactions.add(createBuyTransaction("tx-1", btcSymbol, "0.5", "50000", usd));
            transactions.add(createSellTransaction("tx-2", btcSymbol, "0.3", "60000", usd));
            
            Account account = createAccount(
                "account-1", 
                usd, 
                "18000", 
                List.of(btcAsset), 
                Collections.emptyList()
            );
            
            Portfolio portfolio = Portfolio.builder()
                .portfolioId(portfolioId1)
                .userId(userId1)
                .accounts(List.of(account))
                .portfolioCurrencyPreference(usd)
                .systemCreationDate(Instant.now())
                .lastUpdatedAt(Instant.now())
                .build();
            
            portfolio.recordTransaction(account.getAccountId(), transactions.get(0));
            portfolio.recordTransaction(account.getAccountId(), transactions.get(1));
            
            // Act
            Money realizedGains = performanceService.calculateRealizedGains(portfolio, transactions);
            
            // Assert
            assertEquals(new Money(new BigDecimal("3000.00"), usd), realizedGains);
        }
    
        @Test
        @DisplayName("Should include transaction fees in realized gain calculation")
        void calculateRealizedGains_WithFees() {
            // Arrange
            ValidatedCurrency usd = ValidatedCurrency.USD;
            MarketIdentifier appleSymbol = new MarketIdentifier(
                "AAPL", 
                null, 
                AssetType.STOCK, 
                "Apple Inc.", 
                "USD", 
                null
            );
            
            // Buy 100 shares at $150 with $10 fee
            // Total cost basis: (100 * $150) + $10 = $15,010
            
            // Sell 100 shares at $180 with $15 fee
            // Gross proceeds: 100 * $180 = $18,000
            // Net proceeds: $18,000 - $15 = $17,985
            
            // Realized gain: $17,985 - $15,010 = $2,975
            
            Asset appleStock = createAsset("asset-1", appleSymbol, "0", "0", usd);
            
            List<Transaction> transactions = new ArrayList<>();

            Fee fee1 = new Fee(FeeType.BROKERAGE, new Money(new BigDecimal("10"), usd), ExchangeRate.createSingle(usd, null), null, Instant.now());
            Fee fee2 = new Fee(FeeType.TRANSACTION_FEE, new Money(new BigDecimal("15"), usd), ExchangeRate.createSingle(usd, null), null, Instant.now());
            
            // Create buy transaction with fee
            Transaction buyTx = Transaction.builder()
                .transactionId(transactionId1)
                .transactionType(TransactionType.BUY)
                .assetIdentifier(appleSymbol)
                .quantity(new BigDecimal("100"))
                .pricePerUnit(new Money(new BigDecimal("150"), usd))
                .fees(List.of(fee1))
                .transactionDate(Instant.now().minus(Duration.ofDays(30)))
                .notes("Buy with fee")
                .build();
            
            // Create sell transaction with fee
            Transaction sellTx = Transaction.builder()
                .transactionId(transactionId2)
                .transactionType(TransactionType.SELL)
                .assetIdentifier(appleSymbol)
                .quantity(new BigDecimal("100"))
                .pricePerUnit(new Money(new BigDecimal("180"), usd))
                .fees(List.of(fee2))
                .transactionDate(Instant.now())
                .notes("Sell with fee")
                .build();
            
            transactions.add(buyTx);
            transactions.add(sellTx);
            
            Account account = createAccount(
                "account-1", 
                usd, 
                "17985", 
                List.of(appleStock), 
                Collections.emptyList()
            );
            
            Portfolio portfolio = Portfolio.builder()
                .portfolioId(portfolioId1)
                .userId(userId1)
                .accounts(List.of(account))
                .portfolioCurrencyPreference(usd)
                .systemCreationDate(Instant.now())
                .lastUpdatedAt(Instant.now())
                .build();
            
            portfolio.recordTransaction(account.getAccountId(), transactions.get(0));
            portfolio.recordTransaction(account.getAccountId(), transactions.get(1));
            
            // Act
            Money realizedGains = performanceService.calculateRealizedGains(portfolio, transactions);
            
            // Assert
            // Net gain after fees: $2,975
            assertEquals(new Money(new BigDecimal("2975.00"), usd), realizedGains);
        }
    
        @Test
        @DisplayName("Should handle multiple fractional sells from single fractional buy")
        void calculateRealizedGains_MultipleFractionalSells() {
            // Arrange
            ValidatedCurrency usd = ValidatedCurrency.USD;
            MarketIdentifier ethSymbol = new MarketIdentifier(
                "ETH", 
                null, 
                AssetType.CRYPTO, 
                "Ethereum", 
                "USD", 
                null
            );
            
            // Buy 2.5 ETH at $2,000 per ETH = $5,000 total cost
            // Sell 1.0 ETH at $2,500 = $2,500 proceeds, cost basis = $2,000, gain = $500
            // Sell 1.5 ETH at $3,000 = $4,500 proceeds, cost basis = $3,000, gain = $1,500
            // Total realized gain: $500 + $1,500 = $2,000
            
            Asset ethAsset = createAsset("asset-1", ethSymbol, "0", "0", usd);
            
            List<Transaction> transactions = new ArrayList<>();
            transactions.add(createBuyTransaction("tx-1", ethSymbol, "2.5", "2000", usd));
            transactions.add(createSellTransaction("tx-2", ethSymbol, "1.0", "2500", usd));
            transactions.add(createSellTransaction("tx-3", ethSymbol, "1.5", "3000", usd));
            
            Account account = createAccount(
                "account-1", 
                usd, 
                "7000", 
                List.of(ethAsset), 
                Collections.emptyList()
            );
            
            Portfolio portfolio = Portfolio.builder()
                .portfolioId(portfolioId1)
                .userId(userId1)
                .accounts(List.of(account))
                .portfolioCurrencyPreference(usd)
                .systemCreationDate(Instant.now())
                .lastUpdatedAt(Instant.now())
                .build();
            
            portfolio.recordTransaction(account.getAccountId(), transactions.get(0));
            portfolio.recordTransaction(account.getAccountId(), transactions.get(1));
            portfolio.recordTransaction(account.getAccountId(), transactions.get(2));
            
            // Act
            Money realizedGains = performanceService.calculateRealizedGains(portfolio, transactions);
            
            // Assert
            assertEquals(new Money(new BigDecimal("2000.00"), usd), realizedGains);
        }
    
        @Test
        @DisplayName("Should correctly apply fees to fractional crypto trades")
        void calculateRealizedGains_FractionalSharesWithFees() {
            // Arrange
            ValidatedCurrency usd = ValidatedCurrency.USD;
            MarketIdentifier btcSymbol = new MarketIdentifier(
                "BTC", 
                null, 
                AssetType.CRYPTO, 
                "Bitcoin", 
                "USD", 
                null
            );
            
            // Buy 0.5 BTC at $50,000 with $25 fee
            // Total cost: (0.5 * $50,000) + $25 = $25,025
            
            // Sell 0.5 BTC at $60,000 with $30 fee
            // Net proceeds: (0.5 * $60,000) - $30 = $29,970
            
            // Realized gain: $29,970 - $25,025 = $4,945
            
            Asset btcAsset = createAsset("asset-1", btcSymbol, "0", "0", usd);
            Fee fee1 = new Fee(FeeType.GAS, new Money(new BigDecimal("25"), usd), ExchangeRate.createSingle(usd, null), null, Instant.now());
            Fee fee2 = new Fee(FeeType.TRANSACTION_FEE, new Money(new BigDecimal("30"), usd), ExchangeRate.createSingle(usd, null), null, Instant.now());

            
            List<Transaction> transactions = new ArrayList<>();
            
            Transaction buyTx = Transaction.builder()
                .transactionId(transactionId1)
                .transactionType(TransactionType.BUY)
                .assetIdentifier(btcSymbol)
                .quantity(new BigDecimal("0.5"))
                .pricePerUnit(new Money(new BigDecimal("50000"), usd))
                .fees(List.of(fee1))
                .transactionDate(Instant.now().minus(Duration.ofDays(10)))
                .notes("Buy fractional BTC with fee")
                .build();
            
            Transaction sellTx = Transaction.builder()
                .transactionId(transactionId2)
                .transactionType(TransactionType.SELL)
                .assetIdentifier(btcSymbol)
                .quantity(new BigDecimal("0.5"))
                .pricePerUnit(new Money(new BigDecimal("60000"), usd))
                .fees(List.of(fee2))
                .transactionDate(Instant.now())
                .notes("Sell fractional BTC with fee")
                .build();
            
            transactions.add(buyTx);
            transactions.add(sellTx);
            
            Account account = createAccount(
                "account-1", 
                usd, 
                "29970", 
                List.of(btcAsset), 
                Collections.emptyList()
            );
            
            Portfolio portfolio = Portfolio.builder()
                .portfolioId(portfolioId1)
                .userId(userId1)
                .accounts(List.of(account))
                .portfolioCurrencyPreference(usd)
                .systemCreationDate(Instant.now())
                .lastUpdatedAt(Instant.now())
                .build();
            
            portfolio.recordTransaction(account.getAccountId(), transactions.get(0));
            portfolio.recordTransaction(account.getAccountId(), transactions.get(1));
            
            // Act
            Money realizedGains = performanceService.calculateRealizedGains(portfolio, transactions);
            
            // Assert
            assertEquals(new Money(new BigDecimal("4945.00"), usd), realizedGains);
        }
    }

    @Nested
    @DisplayName("Not implmented but need to test")
    public class TestsCADACBMethod {
        @Test
        void calcualteSellGainWithACB_should_throw_not_implemented() {
            assertDoesNotThrow(() -> {
                PerformanceCalculationService service = new PerformanceCalculationService();
     
                Transaction teTransaction = mock(Transaction.class);
                Portfolio tePortfolio = mock(Portfolio.class);

                service.calculateRealizedGainsCAD_ACB(tePortfolio, List.of(teTransaction));
            });
            assertThrows(UnsupportedOperationException.class, () -> {
                PerformanceCalculationService service = new PerformanceCalculationService();

                service.calculateRealizedGainsCAD_ACB(null, null);
            });
        }
        
    }

    // FOR LATER
    @Disabled
    @Test
    @DisplayName("Should handle multiple currencies correctly")
    void calculateTotalReturn_MultipleCurrencies() {
        // When portfolio has accounts in CAD and USD
        // Ensure proper currency conversion
    }

    // Integration poitns to mock
    // test integration between performance calculation service and portfolio valuation service
    // FOR LATER
    @Test
    @Disabled
    @DisplayName("Should correctly integrate with PortfolioValuationService")
    void calculateTotalReturn_IntegrationWithValuation() {
        // This tests the private method calculateCurrentValue()
        // which instantiates PortfolioValuationService
    }

    // for larger portfolios
    // FOR LATER
    @Test
    @Disabled
    @DisplayName("Should handle portfolio with many transactions efficiently")
    void calculateRealizedGains_LargeTransactionHistory() {
        // Create portfolio with 1000+ transactions
        // Ensure calculation completes in reasonable time
    }

    // FOR LATER / COVERRED BY OTHERS
    @ParameterizedTest
    @Disabled
    @CsvSource({
        "100, 150, 10, 180, 20.00",  // quantity, buy price, quantity, sell price, expected return %
        "50, 200, 25, 180, -5.00",
        "200, 50, 100, 75, 25.00"
    })
    @DisplayName("Should calculate total return for various scenarios")
    void calculateTotalReturn_ParameterizedScenarios(
        BigDecimal buyQty, BigDecimal buyPrice,
        BigDecimal currentQty, BigDecimal currentPrice,
        BigDecimal expectedReturn
    ) {
        // Test implementation
    }

    // FOR LATER
    @Test
    @Disabled
    @DisplayName("Should handle MarketDataService failures gracefully")
    void calculateUnrealizedGains_MarketDataFailure() {
        when(marketDataService.getCurrentPrice(any()))
            .thenThrow(new MarketDataUnavailableException(""));
        
        // How should the service handle this?
        // Should it throw? Return partial data? Return zero?
    }

    // Helper Methods for Test Data Creation

    private Portfolio createPortfolioWithTransactions(ValidatedCurrency currency) {
        MarketIdentifier appleSymbol = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);
        
        Asset appleStock = createAsset("asset-1", appleSymbol, "10", "1500", currency);
        
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(createBuyTransaction("tx-1", appleSymbol, "10", "150", currency));
        transactions.add(createDepositTransaction("tx-2", "500", currency));
        
        Account account = createAccount("account-1", currency, "500", 
            List.of(appleStock), transactions);
        
        return Portfolio.builder()
            .portfolioId(portfolioId1)
            .userId(userId1)
            .accounts(List.of(account))
            .portfolioCurrencyPreference(currency)
            .systemCreationDate(Instant.now())
            .lastUpdatedAt(Instant.now())
            .build();
    }

    private Asset createAsset(String id, AssetIdentifier symbol, String quantity, 
                             String costBasis, ValidatedCurrency currency) {
        return Asset.builder()
            .assetId(AssetId.randomId())
            .assetIdentifier(symbol)
            
            .quantity(new BigDecimal(quantity))
            .currency(currency)
            .costBasis(new Money(new BigDecimal(costBasis), currency))
            .acquiredOn(LocalDateTime.now().minusMonths(6).toInstant(ZoneOffset.UTC))
            .lastSystemInteraction(Instant.now())
            .build();
    }

    private Account createAccount(String id, ValidatedCurrency currency, String cashBalance,
                                 List<Asset> assets, List<Transaction> transactions) {
        return Account.builder()
            .accountId(AccountId.randomId())
            .name("Test Account")
            .accountType(AccountType.NON_REGISTERED)
            .baseCurrency(currency)
            .cashBalance(new Money(new BigDecimal(cashBalance), currency))
            .assets(assets)
            .transactions(transactions)
            .systemCreationDate(Instant.now())
            .lastSystemInteraction(Instant.now())
            .build();
    }

    private Transaction createBuyTransaction(String id, AssetIdentifier symbol, 
                                            String quantity, String price, 
                                            ValidatedCurrency currency) {
        return Transaction.builder()
            .transactionId(TransactionId.randomId())
            .transactionType(TransactionType.BUY)
            .assetIdentifier(symbol)
            .quantity(new BigDecimal(quantity))
            .pricePerUnit(new Money(new BigDecimal(price), currency))
            .fees(Collections.emptyList())
            .transactionDate(LocalDateTime.now().minusMonths(6).toInstant(ZoneOffset.UTC))
            .notes("Test buy")
            .build();
    }

    private Transaction createSellTransaction(String id, AssetIdentifier symbol, 
                                             String quantity, String price, 
                                             ValidatedCurrency currency) {
        return Transaction.builder()
            .transactionId(TransactionId.randomId())
            .transactionType(TransactionType.SELL)
            .assetIdentifier(symbol)
            .quantity(new BigDecimal(quantity))
            .pricePerUnit(new Money(new BigDecimal(price), currency))
            .fees(Collections.emptyList())
            .transactionDate(LocalDateTime.now().minusMonths(1).toInstant(ZoneOffset.UTC))
            .notes("Test sell")
            .build();
    }

    private Transaction createDepositTransaction(String id, String amount, 
                                                 ValidatedCurrency currency) {
        return Transaction.builder()
            .transactionId(TransactionId.randomId())
            .transactionType(TransactionType.DEPOSIT)
            .assetIdentifier(new CashIdentifier(currency.getCode()))
            .quantity(BigDecimal.ONE)       
            .pricePerUnit(new Money(new BigDecimal(amount), currency))
            .fees(Collections.emptyList())
            .transactionDate(LocalDateTime.now().minusMonths(5).toInstant(ZoneOffset.UTC))
            .notes("Test deposit")
            .build();
    }

    private Transaction createWithdrawalTransaction(String id, String amount, 
                                                   ValidatedCurrency currency) {
        return Transaction.builder()
            .transactionId(TransactionId.randomId())
            .transactionType(TransactionType.WITHDRAWAL)
            .assetIdentifier(new CashIdentifier(currency.getCode()))
            .quantity(BigDecimal.ONE)
            .pricePerUnit(new Money(new BigDecimal(amount), currency))
            .fees(Collections.emptyList())
            .transactionDate(LocalDateTime.now().minusMonths(2).toInstant(ZoneOffset.UTC))
            .notes("Test withdrawal")
            .build();
    }
}