package com.laderrco.fortunelink.portfolio_management.application.services;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.flyway.enabled=false",
    "spring.data.redis.repositories.enabled=false", // Disable Redis
    "spring.cache.type=none",                       // Disable Caching
    "fortunelink.cache.enabled=false"              // Disable your custom cache
})
public class Phase2CompletionTest {
    @Autowired
    private MarketDataService marketDataService;

    @Test
    @Disabled("Manual verification test")
    void phase2CompletionCheck() {
        // This should compile and run without errors
        AssetIdentifier apple = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);

        // Test 1: Can fetch price?
        Money price = marketDataService.getCurrentPrice(apple);
        assertThat(price).isNotNull();

        // Test 2: Can fetch asset info?
        MarketAssetInfo info = marketDataService.getAssetInfo(apple).orElseThrow();
        assertThat(info).isNotNull();

        // Test 3: Can fetch trading currency?
        ValidatedCurrency currency = marketDataService.getTradingCurrency(apple);
        assertThat(currency).isNotNull();

        // Test 4: Batch operations?
        List<AssetIdentifier> symbols = List.of(apple, new MarketIdentifier("GOOGL", null, AssetType.STOCK, "GOOGLE", "USD", null));
        Map<AssetIdentifier, MarketAssetQuote> prices = marketDataService.getBatchQuotes(symbols);
        assertThat(prices).hasSize(2);
    }
}
