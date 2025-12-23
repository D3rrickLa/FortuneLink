package com.laderrco.fortunelink.portfolio_management.application.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.application.responses.AllocationDetail;
import com.laderrco.fortunelink.portfolio_management.application.responses.AllocationResponse;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class AllocationMapperTest {
/*
    ANYTHING WITH PERCENTAGE RELATED IN ASSETALLOCATION WILL BE TESTED HERE INSTEAD OF THAT UNIT TESTS
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
            assertEquals(new BigDecimal("60.00").setScale(Precision.PERCENTAGE.getDecimalPlaces()), result.get(AssetType.STOCK).toPercentage());
            assertEquals(new BigDecimal("30.00").setScale(Precision.PERCENTAGE.getDecimalPlaces()), result.get(AssetType.ETF).toPercentage());
            assertEquals(new BigDecimal("10.00").setScale(Precision.PERCENTAGE.getDecimalPlaces()), result.get(AssetType.CRYPTO).toPercentage());


            example
     
        } */
    private final ValidatedCurrency USD = ValidatedCurrency.USD;
    private final Instant TEST_TIME = Instant.now();
    @Test
    @DisplayName("Should calculate 60/30/10 percentages correctly")
    void shouldMapValuesToCorrectPercentages() {
        // Arrange
        Map<String, Money> values = Map.of(
            "STOCK", Money.of(6000, "USD"),
            "ETF", Money.of(3000, "USD"),
            "CRYPTO", Money.of(1000, "USD")
        );
        Money totalValue = Money.of(10000, "USD");

        // Act
        AllocationResponse response = AllocationMapper.toResponse(values, totalValue, TEST_TIME);

        // Assert - This is where your percentage logic is verified
        assertEquals(new BigDecimal("60.00"), response.getAllocations().get("STOCK").getPercentage().value());
        assertEquals(new BigDecimal("30.00"), response.getAllocations().get("ETF").getPercentage().value());
        assertEquals(new BigDecimal("10.00"), response.getAllocations().get("CRYPTO").getPercentage().value());
    }

    @Test
    @DisplayName("Should calculate 100% allocation correctly for aggregated values")
    void shouldCalculateFullAllocation() {
        // Arrange: Replicating your 4 assets * 2500 = 10000 scenario
        Map<String, Money> allocationMap = Map.of(
            "STOCK", Money.of(new BigDecimal("10000.00"), USD)
        );
        Money totalValue = Money.of(new BigDecimal("10000.00"), USD);

        // Act
        AllocationResponse response = AllocationMapper.toResponse(allocationMap, totalValue, TEST_TIME);

        // Assert
        AllocationDetail stockDetail = response.getAllocations().get("STOCK");
        
        // This is where your percentage check now lives
        assertEquals(new BigDecimal("100.00"), stockDetail.getPercentage().value());
        assertEquals(AssetType.STOCK, stockDetail.getCategory());
        assertEquals(TEST_TIME, response.getAsOfDate());
    }

    @Test
    @DisplayName("Should handle complex splits like 60/30/10 accurately")
    void shouldHandleMultipleCategories() {
        // Arrange
        Map<String, Money> allocationMap = Map.of(
            "STOCK", Money.of(new BigDecimal("6000.00"), USD),
            "ETF", Money.of(new BigDecimal("3000.00"), USD),
            "CRYPTO", Money.of(new BigDecimal("1000.00"), USD)
        );
        Money totalValue = Money.of(new BigDecimal("10000.00"), USD);

        // Act
        AllocationResponse response = AllocationMapper.toResponse(allocationMap, totalValue, TEST_TIME);

        // Assert
        assertEquals(new BigDecimal("60.00"), response.getAllocations().get("STOCK").getPercentage().value());
        assertEquals(new BigDecimal("30.00"), response.getAllocations().get("ETF").getPercentage().value());
        assertEquals(new BigDecimal("10.00"), response.getAllocations().get("CRYPTO").getPercentage().value());
    }
            
}
