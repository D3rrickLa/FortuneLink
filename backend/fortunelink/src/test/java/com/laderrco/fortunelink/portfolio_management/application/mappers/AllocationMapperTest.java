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
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class AllocationMapperTest {

    private final ValidatedCurrency USD = ValidatedCurrency.USD;
    private final Instant TEST_TIME = Instant.now();
    private final int PRECISION = Precision.PERCENTAGE.getDecimalPlaces();
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
        assertEquals(new BigDecimal("60.00").setScale(PRECISION), response.getAllocations().get("STOCK").getPercentage().value());
        assertEquals(new BigDecimal("30.00").setScale(PRECISION), response.getAllocations().get("ETF").getPercentage().value());
        assertEquals(new BigDecimal("10.00").setScale(PRECISION), response.getAllocations().get("CRYPTO").getPercentage().value());
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
        assertEquals(new BigDecimal("100.00").setScale(PRECISION), stockDetail.getPercentage().value());
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
        assertEquals(new BigDecimal("60.00").setScale(PRECISION), response.getAllocations().get("STOCK").getPercentage().value());
        assertEquals(new BigDecimal("30.00").setScale(PRECISION), response.getAllocations().get("ETF").getPercentage().value());
        assertEquals(new BigDecimal("10.00").setScale(PRECISION), response.getAllocations().get("CRYPTO").getPercentage().value());
    }

    // @Test
    // @DisplayName("Should map different currencies to correct percentages of the base total")
    // void shouldMapCurrenciesToPercentages() {
    //     // In the real app, these values would have been normalized to a base currency 
    //     // for the 'totalValue' calculation to make sense.
    //     Map<String, Money> currencyAllocations = Map.of(
    //         "USD", Money.of(new BigDecimal("50000.00"), USD),
    //         "CAD", Money.of(new BigDecimal("30000.00"), USD), // Normalized to USD for math
    //         "EUR", Money.of(new BigDecimal("20000.00"), USD)  // Normalized to USD for math
    //     );
    //     Money totalValue = Money.of(new BigDecimal("100000.00"), USD);

    //     AllocationResponse response = AllocationMapper.toResponse(currencyAllocations, totalValue, TEST_TIME);

    //     assertEquals(new BigDecimal("50.00").setScale(PRECISION), response.getAllocations().get("USD").getPercentage().value());
    //     assertEquals(new BigDecimal("30.00").setScale(PRECISION), response.getAllocations().get("CAD").getPercentage().value());
    //     assertEquals(new BigDecimal("20.00").setScale(PRECISION), response.getAllocations().get("EUR").getPercentage().value());
    // }
            
}
