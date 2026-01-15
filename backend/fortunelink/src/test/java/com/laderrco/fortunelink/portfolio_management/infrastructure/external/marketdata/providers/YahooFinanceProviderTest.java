package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.providers;

// import java.time.Duration;
// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.Map;
// import java.util.Optional;

// import org.junit.jupiter.api.Assumptions;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Tag;
// import org.junit.jupiter.api.Test;
// import org.springframework.boot.web.client.RestTemplateBuilder;
// import org.springframework.web.client.RestTemplate;

// import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.mappers.YahooResponseMapper;
// import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.ProviderAssetInfo;
// import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.ProviderQuote;

// import static org.assertj.core.api.Assertions.*;

/**
 * NOTE: WE ARE RATE LIMITED EASILY
 * Integration tests for YahooFinanceProvider.
 * 
 * These tests make REAL API calls to Yahoo Finance.
 * Tagged as "integration" so they can be excluded from fast unit test runs.
 * 
 * Run with: mvn test -Dgroups="integration"
 * Skip with: mvn test -DexcludedGroups="integration"
 */
// @Tag("integration")
// @DisplayName("Yahoo Finance Provider Integration Tests")
// class YahooFinanceProviderIntegrationTest {
    
//     private YahooFinanceProvider provider;
//     private YahooResponseMapper mapper; 
    
//     @BeforeEach
//     void setUp() {
//         RestTemplate restTemplate = new RestTemplateBuilder()
//             .connectTimeout(Duration.ofSeconds(10))
//             .readTimeout(Duration.ofSeconds(10))
//             .build();

//         mapper = new YahooResponseMapper();
//         provider = new YahooFinanceProvider(restTemplate, mapper);

//         // Add this "Circuit Breaker"
//         try {
//             System.out.println("Checking Yahoo connectivity...");
//             provider.fetchCurrentQuote("AAPL");
//         } catch (Exception e) {
//             System.err.println("CRITICAL: Yahoo is blocking us (429) or network is down: " + e.getMessage());
//             // This will stop the test gracefully instead of crashing the VM
//             Assumptions.assumeTrue(false, "Yahoo Finance is unavailable - skipping integration tests.");
//     }
//     }

//     @Test
//     @DisplayName("Should fetch real current quote for AAPL")
//     void shouldFetchRealCurrentQuoteForAAPL() {
//         // When
//         // when(mapper)
//         Optional<ProviderQuote> quote = provider.fetchCurrentQuote("AAPL");
        
//         // Then
//         assertThat(quote).isPresent();
//         assertThat(quote.get().symbol()).isEqualTo("AAPL");
//         assertThat(quote.get().price()).isPositive();
//         assertThat(quote.get().currency()).isEqualTo("USD");
//         assertThat(quote.get().source()).isEqualTo("YAHOO");
        
//         System.out.println("AAPL Current Price: " + quote.get().price() + " " + quote.get().currency());
//     }
    
//     @Test
//     @DisplayName("Should fetch real current quote for Canadian stock")
//     void shouldFetchRealCurrentQuoteForCanadianStock() {
//         // When - VGRO is a popular Canadian ETF
//         // when(mapper);
//         Optional<ProviderQuote> quote = provider.fetchCurrentQuote("VGRO.TO");
        
//         // Then
//         assertThat(quote).isPresent();
//         assertThat(quote.get().symbol()).isEqualTo("VGRO.TO");
//         assertThat(quote.get().price()).isPositive();
//         assertThat(quote.get().currency()).isEqualTo("CAD");
        
//         System.out.println("VGRO.TO Current Price: " + quote.get().price() + " " + quote.get().currency());
//     }
    
//     @Test
//     @DisplayName("Should fetch real current quote for crypto")
//     void shouldFetchRealCurrentQuoteForCrypto() {
//         // When
//         Optional<ProviderQuote> quote = provider.fetchCurrentQuote("BTC-USD");
        
//         // Then
//         assertThat(quote).isPresent();
//         assertThat(quote.get().symbol()).isEqualTo("BTC-USD");
//         assertThat(quote.get().price()).isPositive();
//         assertThat(quote.get().currency()).isEqualTo("USD");
        
//         System.out.println("BTC-USD Current Price: " + quote.get().price() + " " + quote.get().currency());
//     }
    
//     @Test
//     @DisplayName("Should return empty for invalid symbol")
//     void shouldReturnEmptyForInvalidSymbol() {
//         // When
//         Optional<ProviderQuote> quote = provider.fetchCurrentQuote("THISSYMBOLWILLNEVEREXIST12345");
        
//         // Then
//         assertThat(quote).isEmpty();
//     }
    
//     @Test
//     @DisplayName("Should fetch real historical quote")
//     void shouldFetchRealHistoricalQuote() {
//         // When - Get AAPL price from 30 days ago
//         LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
//         Optional<ProviderQuote> quote = provider.fetchHistoricalQuote("AAPL", thirtyDaysAgo);
        
//         // Then
//         assertThat(quote).isPresent();
//         assertThat(quote.get().symbol()).isEqualTo("AAPL");
//         assertThat(quote.get().price()).isPositive();
        
//         System.out.println("AAPL Price 30 days ago: " + quote.get().price() + " on " + quote.get().timestamp());
//     }
    
//     @Test
//     @DisplayName("Should fetch batch quotes for multiple stocks")
//     void shouldFetchBatchQuotes() {
//         // When
//         List<String> symbols = List.of("AAPL", "GOOGL", "MSFT", "TSLA");
//         Map<String, ProviderQuote> quotes = provider.fetchBatchQuotes(symbols);
        
//         // Then
//         assertThat(quotes).isNotEmpty();
//         assertThat(quotes.size()).isGreaterThanOrEqualTo(3); // At least most should succeed
        
//         quotes.forEach((symbol, quote) -> {
//             assertThat(quote.symbol()).isEqualTo(symbol);
//             assertThat(quote.price()).isPositive();
//             System.out.println(symbol + ": " + quote.price() + " " + quote.currency());
//         });
//     }
    
//     @Test
//     @DisplayName("Should fetch real asset info for AAPL")
//     void shouldFetchRealAssetInfo() {
//         // When
//         Optional<ProviderAssetInfo> info = provider.fetchAssetInfo("AAPL");
        
//         // Then
//         assertThat(info).isPresent();
//         assertThat(info.get().symbol()).isEqualTo("AAPL");
//         assertThat(info.get().name()).containsIgnoringCase("Apple");
//         assertThat(info.get().assetType()).isIn("STOCK", "EQUITY");
//         assertThat(info.get().exchange()).isNotBlank();
//         assertThat(info.get().currency()).isEqualTo("USD");
        
//         System.out.println("AAPL Info: " + info.get().name() + " (" + info.get().assetType() + ") on " + info.get().exchange());
//     }
    
//     @Test
//     @DisplayName("Should fetch real asset info for ETF")
//     void shouldFetchRealAssetInfoForETF() {
//         // When - SPY is S&P 500 ETF
//         Optional<ProviderAssetInfo> info = provider.fetchAssetInfo("SPY");
        
//         // Then
//         assertThat(info).isPresent();
//         assertThat(info.get().symbol()).isEqualTo("SPY");
//         assertThat(info.get().assetType()).isEqualTo("ETF");
        
//         System.out.println("SPY Info: " + info.get().name() + " (" + info.get().assetType() + ")");
//     }
    
//     @Test
//     @DisplayName("Should fetch real asset info for crypto")
//     void shouldFetchRealAssetInfoForCrypto() {
//         // When
//         Optional<ProviderAssetInfo> info = provider.fetchAssetInfo("BTC-USD");
        
//         // Then
//         assertThat(info).isPresent();
//         assertThat(info.get().symbol()).isEqualTo("BTC-USD");
//         assertThat(info.get().assetType()).isEqualTo("CRYPTO");
        
//         System.out.println("BTC-USD Info: " + info.get().name() + " (" + info.get().assetType() + ")");
//     }
    
//     @Test
//     @DisplayName("Should validate symbol support")
//     void shouldValidateSymbolSupport() {
//         // Valid symbols
//         assertThat(provider.supportsSymbol("AAPL")).isTrue();
//         assertThat(provider.supportsSymbol("VGRO.TO")).isTrue();
//         assertThat(provider.supportsSymbol("BTC-USD")).isTrue();
//         assertThat(provider.supportsSymbol("^GSPC")).isTrue(); // S&P 500 index
        
//         // Invalid symbols
//         assertThat(provider.supportsSymbol("")).isFalse();
//         assertThat(provider.supportsSymbol(null)).isFalse();
//         assertThat(provider.supportsSymbol("invalid symbol with spaces")).isFalse();
//     }
    
//     @Test
//     @DisplayName("Should get provider name")
//     void shouldGetProviderName() {
//         assertThat(provider.getProviderName()).isEqualTo("YAHOO_FINANCE");
//     }
// }