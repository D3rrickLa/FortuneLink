package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;

public class AssetAllocationTest {
    Money totalValue;
    AssetAllocation allocation;
    Currency usd;
    AssetIdentifier appleIdentifier;
    @BeforeEach
    void init() {
        usd = Currency.getInstance("USD");
        totalValue = new Money(new BigDecimal("30000"), usd);
        allocation = new AssetAllocation(totalValue, usd);
        appleIdentifier = new AssetIdentifier(
            "APPL",
            AssetType.STOCK, 
            "US0378331005",
            "APPLE", 
            "NASDAQ", "SOME APPLE DESC", "TECH");
    }


    @Test
    void testConstructor() {
        assertEquals(totalValue, allocation.getTotalValue());
        assertEquals(usd, allocation.getBaseCurrency());
        assertNotNull(allocation.getCalculatedAt());
        assertTrue(allocation.getAllocationsBySymbol().isEmpty());
        assertTrue(allocation.getAllocationsByType().isEmpty());
    }
    
    @Test 
    void testAddSingleAllocation() {
        Money appleValue = new Money(10000, usd);
        Percentage applePercentage = new Percentage(BigDecimal.valueOf(33.33));
    
    
        allocation.addAllocation(appleIdentifier, appleValue, applePercentage);
    
        assertEquals(1, allocation.getAllocationsBySymbol().size());
        assertEquals(1, allocation.getAllocationsByType().size());
        
        AllocationItem appleItem = allocation.getAllocationsBySymbol().get("APPL");
        assertNotNull(appleItem);
        assertEquals("US0378331005", appleItem.assetIdentifier().isin());
        assertEquals(AssetType.STOCK, appleItem.assetIdentifier().assetType());
        assertEquals(appleValue, appleItem.value());
        assertEquals(applePercentage, appleItem.percentage());

    }

    @Test 
    void testAddMultipleAllocationsWithSameType() {
        // Given
        Money appleValue = new Money(new BigDecimal("10000"), usd);
        Money googleValue = new Money(new BigDecimal("5000"), usd);
        Percentage applePercentage  = new Percentage(new BigDecimal("33.33"));
        Percentage googlePercentage = new Percentage(new BigDecimal("16.67"));
        
        AssetIdentifier googleIdentifier = new AssetIdentifier(
            "GOOGL",
            AssetType.STOCK, 
            "US0378331007",
            "GOOGLE", 
            "NASDAQ", "SOME GOOGLE DESC", "TECH");
        // When
        allocation.addAllocation(appleIdentifier, appleValue, applePercentage);
        allocation.addAllocation(googleIdentifier, googleValue, googlePercentage);
        
        System.out.println(allocation.getAllocationsByType());
        System.out.println(allocation.getAllocationsBySymbol());

        // Then
        assertEquals(2, allocation.getAllocationsBySymbol().size());
        assertEquals(1, allocation.getAllocationsByType().size()); // Only STOCKS type
        
        // Check type aggregation
        AllocationItem stocksItem = allocation.getAllocationsByType().get(AssetType.STOCK);
        assertNotNull(stocksItem);
        assertEquals(new Money(new BigDecimal("15000"), usd), stocksItem.value());
        assertEquals(new BigDecimal("50.00").setScale(6), stocksItem.percentage().percentageValue());
    }

    @Test
    void testAddMultipleAllocationsWithDifferentTypes() {
        // Given
        Money appleValue = new Money(new BigDecimal("10000"), usd);
        Money bondValue = new Money(new BigDecimal("15000"), usd);
        Money bitcoinValue = new Money(new BigDecimal("5000"), usd);
        
        AssetIdentifier governmentBond = new AssetIdentifier(
            "GOOGL",
            AssetType.BOND, 
            "US0378331007",
            "10 Year Treasury", 
            "GOV", "SOME GOVERNMENT DESC", "TECH"
        );
        AssetIdentifier btc = new AssetIdentifier(
            "BTC",
            AssetType.CRYPTO, 
            "US0378331010",
            "Bitcoin", 
            "COINGEEKO", "SOME BTC DESC", "TECH"
        );


        // When
        allocation.addAllocation(appleIdentifier, appleValue, new Percentage(new BigDecimal("33.33")));
        allocation.addAllocation(governmentBond, bondValue, new Percentage(new BigDecimal("50.00")));
        allocation.addAllocation(btc, bitcoinValue, new Percentage(new BigDecimal("16.67")));
        
        // Then
        assertEquals(3, allocation.getAllocationsBySymbol().size());
        assertEquals(3, allocation.getAllocationsByType().size());
        
        // Verify each type exists
        assertTrue(allocation.getAllocationsByType().containsKey(AssetType.STOCK));
        assertTrue(allocation.getAllocationsByType().containsKey(AssetType.BOND));
        assertTrue(allocation.getAllocationsByType().containsKey(AssetType.CRYPTO));
    }

    @Test
    void testGetPercentageBySymbol() {
        // Given
        allocation.addAllocation(appleIdentifier,
                               new Money(new BigDecimal("10000"), usd), 
                               new Percentage(new BigDecimal("33.33")));
        
        // When & Then
        assertEquals(new Percentage(new BigDecimal("33.33")), allocation.getPercentageBySymbol("APPL"));
        assertEquals(new Percentage(BigDecimal.ZERO), allocation.getPercentageBySymbol("NONEXISTENT"));
    }

    @Test
    void testGetPercentageByType() {
        AssetIdentifier googleIdentifier = new AssetIdentifier(
            "GOOGL",
            AssetType.STOCK, 
            "US0378331007",
            "GOOGLE", 
            "NASDAQ", "SOME GOOGLE DESC", "TECH");

        // Given
        allocation.addAllocation(appleIdentifier,  
                               new Money(new BigDecimal("10000"), usd), 
                               new Percentage(new BigDecimal("33.33")));
        allocation.addAllocation(googleIdentifier, 
                               new Money(new BigDecimal("5000"), usd), 
                               new Percentage(new BigDecimal("16.67")));
        
        // When & Then
        assertEquals(new BigDecimal("50.00").setScale(6), allocation.getPercentageByType(AssetType.STOCK).percentageValue());
        assertEquals(BigDecimal.ZERO.setScale(6), allocation.getPercentageByType(AssetType.BOND).percentageValue());
    }
    
    @Test
    void testIsDiversified() {
        AssetIdentifier googleIdentifier = new AssetIdentifier(
            "GOOGL",
            AssetType.STOCK, 
            "US0378331007",
            "GOOGLE", 
            "NASDAQ", "SOME GOOGLE DESC", "TECH");
        AssetIdentifier btc = new AssetIdentifier(
            "BTC",
            AssetType.CRYPTO, 
            "US0378331010",
            "Bitcoin", 
            "COINGEEKO", "SOME BTC DESC", "TECH"
        );
        // Given - Well diversified portfolio
        allocation.addAllocation(appleIdentifier, 
                               new Money(new BigDecimal("6000"), usd), 
                               new Percentage(new BigDecimal("20.00")));
        allocation.addAllocation(googleIdentifier,
                               new Money(new BigDecimal("6000"), usd), 
                               new Percentage(new BigDecimal("20.00")));
        allocation.addAllocation(btc,
                               new Money(new BigDecimal("18000"), usd), 
                               new Percentage(new BigDecimal("60.00")));
        
        // When & Then
        assertTrue(allocation.isDiversified(new Percentage(new BigDecimal("70")))); // 70% threshold
        assertFalse(allocation.isDiversified(new Percentage(new BigDecimal("50")))); // 50% threshold (bonds exceed this)
    }
    
    @Test
    void testGetTopAllocations() {
       AssetIdentifier googleIdentifier = new AssetIdentifier(
            "GOOGL",
            AssetType.STOCK, 
            "US0378331007",
            "GOOGLE", 
            "NASDAQ", "SOME GOOGLE DESC", "TECH");
        AssetIdentifier btc = new AssetIdentifier(
            "BTC",
            AssetType.CRYPTO, 
            "US0378331010",
            "Bitcoin", 
            "COINGEEKO", "SOME BTC DESC", "TECH"
        );

        // Given
        allocation.addAllocation(appleIdentifier,
                               new Money(new BigDecimal("15000"), usd), 
                               new Percentage(new BigDecimal("50.00")));
        allocation.addAllocation(googleIdentifier,
                               new Money(new BigDecimal("9000"), usd), 
                               new Percentage(new BigDecimal("30.00")));
        allocation.addAllocation(btc, 
                               new Money(new BigDecimal("6000"), usd), 
                               new Percentage(new BigDecimal("20.00")));
        
        // When
        List<AllocationItem> top2 = allocation.getTopAllocations(2);
        
        // Then
        assertEquals(2, top2.size());
        assertEquals("APPL", top2.get(0).assetIdentifier().symbol()); // Highest percentage first
        assertEquals("GOOGL", top2.get(1).assetIdentifier().symbol());
    }
    
    @Test
    void testEmptyAllocation() {
        // Given - Empty allocation
        
        // When & Then
        assertTrue(allocation.getAllocationsBySymbol().isEmpty());
        assertTrue(allocation.getAllocationsByType().isEmpty());
        assertEquals(BigDecimal.ZERO.setScale(6), allocation.getPercentageBySymbol("APPL").toDecimal());
        assertEquals(BigDecimal.ZERO.setScale(6), allocation.getPercentageByType(AssetType.STOCK).toDecimal());
        assertTrue(allocation.isDiversified(new Percentage(new BigDecimal("50"))));
        assertTrue(allocation.getTopAllocations(5).isEmpty());
    }
    
    @Test
    void testImmutability() {
        // Given
        allocation.addAllocation(appleIdentifier, 
                               new Money(new BigDecimal("10000"), usd), 
                               new Percentage(new BigDecimal("33.33")));
        
        // When - Try to modify returned maps
        Map<String, AllocationItem> symbolMap = allocation.getAllocationsBySymbol();
        Map<AssetType, AllocationItem> typeMap = allocation.getAllocationsByType();
        
        // Then - Should throw exception when trying to modify
        assertThrows(UnsupportedOperationException.class, () -> {
            symbolMap.put("HACKER", null);
        });
        
        assertThrows(UnsupportedOperationException.class, () -> {
            typeMap.put(AssetType.BOND, null);
        });
    }
    
    @Test
    void testCalculatedAtTimestamp() {
        // Given
        LocalDateTime before = LocalDateTime.now();
        
        // When
        AssetAllocation newAllocation = new AssetAllocation(totalValue, usd);
        
        // Then
        LocalDateTime after = LocalDateTime.now();
        assertTrue(newAllocation.getCalculatedAt().isAfter(before) || 
                  newAllocation.getCalculatedAt().equals(before));
        assertTrue(newAllocation.getCalculatedAt().isBefore(after) || 
                  newAllocation.getCalculatedAt().equals(after));
    }
}
