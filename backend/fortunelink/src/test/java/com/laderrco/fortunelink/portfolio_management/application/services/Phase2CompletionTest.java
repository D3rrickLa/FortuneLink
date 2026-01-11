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
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
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
        System.out.println("✅ Price fetching works: " + price);

        // Test 2: Can fetch asset info?
        MarketAssetInfo info = marketDataService.getAssetInfo(apple).orElseThrow();
        assertThat(info).isNotNull();
        System.out.println("✅ Asset info works: " + info.getName());

        // Test 3: Can fetch trading currency?
        ValidatedCurrency currency = marketDataService.getTradingCurrency(apple);
        assertThat(currency).isNotNull();
        System.out.println("✅ Currency works: " + currency);

        // Test 4: Batch operations?
        List<AssetIdentifier> symbols = List.of(apple, new MarketIdentifier("GOOGL", null, AssetType.STOCK, "GOOGLE", "USD", null));
        Map<AssetIdentifier, Money> prices = marketDataService.getBatchPrices(symbols);
        assertThat(prices).hasSize(2);
        System.out.println("✅ Batch fetching works");

        System.out.println("\n🎉 PHASE 2 COMPLETE! Ready for Phase 3!");
    }
}
