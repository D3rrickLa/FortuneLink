package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.MarketPrice;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class AssetHoldingTest {

    UUID assetId;
    UUID portfolioId;
    AssetIdentifier assetIdentifier;
    BigDecimal initalQuantity;
    Money initialCostPerUnit;
    Instant createdAt;

    Currency usd;

    @BeforeEach
    void init() {
        usd = Currency.getInstance("USD");
        assetId = UUID.randomUUID();
        portfolioId = UUID.randomUUID();
        assetIdentifier = new AssetIdentifier(
            AssetType.STOCK, 
            "US0378331005", 
            "APPLE", 
            "NASDAQ", 
            "SOME DESCRIPTOIN"
        );

        initalQuantity = new BigDecimal("207");
        initialCostPerUnit = new Money(215.55, usd);
        createdAt = Instant.now();

    }

    @Test
    void testConstructorValid() {
        AssetHolding testAssetHolding = new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            initalQuantity, 
            initialCostPerUnit, 
            createdAt
        );
        
        assertNotNull(testAssetHolding);
    }

    @Test
    void testConstructorInValidNulls() {
        Exception e1 = assertThrows(NullPointerException.class, () -> new AssetHolding(
            null, 
            portfolioId, 
            assetIdentifier, 
            initalQuantity, 
            initialCostPerUnit, 
            createdAt
        ));
        assertEquals("Asset id cannot be null.", e1.getMessage());
        Exception e2 = assertThrows(NullPointerException.class, () -> new AssetHolding(
            assetId, 
            null, 
            assetIdentifier, 
            initalQuantity, 
            initialCostPerUnit, 
            createdAt
        ));
        assertEquals("Portfolio id cannot be null.", e2.getMessage());
        Exception e3 = assertThrows(NullPointerException.class, () -> new AssetHolding(
            assetId, 
            portfolioId, 
            null, 
            initalQuantity, 
            initialCostPerUnit, 
            createdAt
        ));
        assertEquals("Asset identifier cannot be null.", e3.getMessage());
        Exception e4 = assertThrows(NullPointerException.class, () -> new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            null, 
            initialCostPerUnit, 
            createdAt
        ));
        assertEquals("Total quantity cannot be null.", e4.getMessage());
        Exception e5 = assertThrows(NullPointerException.class, () -> new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            initalQuantity, 
            null, 
            createdAt
        ));
        assertEquals("Total adjusted cost basis cannot be null.", e5.getMessage());
        
        Exception e6 = assertThrows(NullPointerException.class, () -> new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            initalQuantity, 
            initialCostPerUnit, 
            null
        ));
        assertEquals("Created at cannot be null.", e6.getMessage());
    }

    @Test
    void testConstructorInValidTotalQuantity() {
        initalQuantity = BigDecimal.valueOf(-21);
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            initalQuantity, 
            initialCostPerUnit, 
            createdAt
        ));
        assertEquals("Quantity of asset cannot be less than zero.", e1.getMessage());
    }
    
    @Test
    void testConstructorInValidCostPerUnit() {
        initialCostPerUnit = new Money(-12, usd);
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            initalQuantity, 
            initialCostPerUnit, 
            createdAt
            ));
            
        assertEquals("Total adjusted cost basis must be a positive number.", e1.getMessage());
        
        initialCostPerUnit = new Money(0, usd);
        e1 = assertThrows(IllegalArgumentException.class, () -> new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            initalQuantity, 
            initialCostPerUnit, 
            createdAt
            ));
        assertEquals("Total adjusted cost basis must be a positive number.", e1.getMessage());
           
    }

    @Test
    void testAddToPosition() {
        // Setup initial values
        BigDecimal initialQuantity = BigDecimal.valueOf(207); // 207 + 50 = 257
        Money initialCostBasis = new Money(new BigDecimal("476.02"), usd); // This + 245.32 = 721.34
        
        AssetHolding testAssetHolding = new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            initialQuantity, 
            initialCostBasis, 
            createdAt
        );

        BigDecimal quantity = new BigDecimal(50);
        Money costBasis = new Money(new BigDecimal("245.32"), usd);
        
        testAssetHolding.addToPosition(quantity, costBasis);
        
        // Correct assertions
        assertEquals(BigDecimal.valueOf(257), testAssetHolding.getTotalQuantity());
        assertEquals(new Money(new BigDecimal("721.34"), usd), testAssetHolding.getTotalAdjustedCostBasis());
    }   

    @Test
    void testCalculateCapitalGain() {
        AssetHolding testAssetHolding = new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            initalQuantity, 
            initialCostPerUnit, 
            createdAt
        );

        BigDecimal soldeQuantity = BigDecimal.valueOf(10);
        Money salePrice = new Money(15000, usd);
        Money captialGains = testAssetHolding.calculateCapitalGain(soldeQuantity, salePrice);
        assertEquals(BigDecimal.valueOf(14989.60).setScale(2), captialGains.amount());        
    }

    @Test
    void testCalculateCapitalGainInValidNull() {
        AssetHolding testAssetHolding = new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            initalQuantity, 
            initialCostPerUnit, 
            createdAt
        );

        BigDecimal soldeQuantity = BigDecimal.valueOf(10000);
        Money salePrice = new Money(15000, usd);
        assertThrows(IllegalArgumentException.class, () ->testAssetHolding.calculateCapitalGain(soldeQuantity, salePrice));
        BigDecimal soldeQuantity2 = BigDecimal.valueOf(-1000);
        assertThrows(IllegalArgumentException.class, () ->testAssetHolding.calculateCapitalGain(soldeQuantity2, salePrice));
    }

    @Test
    void testCalculateCapitalGainInValidNegativeValues() {
        AssetHolding testAssetHolding = new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            initalQuantity, 
            initialCostPerUnit, 
            createdAt
        );

        BigDecimal soldeQuantity = BigDecimal.valueOf(-10);
        Money salePrice = new Money(15000, usd);
        assertThrows(IllegalArgumentException.class, () ->testAssetHolding.calculateCapitalGain(soldeQuantity, salePrice));
        // assertThrows(IllegalArgumentException.class, () ->testAssetHolding.calculateCapitalGain(soldeQuantity, salePrice));
    }

    @Test
    void testGetAverageACBPerUnit() {
        AssetHolding testAssetHolding = new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            initalQuantity, 
            initialCostPerUnit, 
            createdAt
        );

        Money avgCostBasisPerUnit = testAssetHolding.getAverageACBPerUnit();
        Money expected = new Money(1.04, usd);
        assertEquals(expected, avgCostBasisPerUnit);
    }

    @Test
    void testGetCurrentValue() {
        AssetHolding testAssetHolding = new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            initalQuantity, 
            initialCostPerUnit, 
            createdAt
        );
        
        MarketPrice price = new MarketPrice(assetIdentifier, new Money(230.45, usd), createdAt, null);
        
        Money currentPrice = testAssetHolding.getCurrentValue(price);
        assertEquals(price.price().amount().multiply(initalQuantity), currentPrice.amount());
    }
    
    @Test
    void testGetCurrentValueInValidNull() {
        AssetHolding testAssetHolding = new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            initalQuantity, 
            initialCostPerUnit, 
            createdAt
        );
        Exception e1 = assertThrows(NullPointerException.class, () -> testAssetHolding.getCurrentValue(null));
        assertEquals("Current price cannot be null.", e1.getMessage());
    }
    
    @Test
    void testGetCurrentValueInValidNotSameCurrency() {
        AssetHolding testAssetHolding = new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            initalQuantity, 
            initialCostPerUnit, 
            createdAt
            );
            
        Currency cad = Currency.getInstance("CAD");
        MarketPrice price = new MarketPrice(assetIdentifier, new Money(230.45, cad), createdAt, null);
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> testAssetHolding.getCurrentValue(price));
        assertEquals("Market price curreny and liability currency must match.", e1.getMessage());


    }

    @Test
    void testRemoveFromPosition() {
        AssetHolding testAssetHolding = new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            initalQuantity, 
            initialCostPerUnit, 
            createdAt
        );
    
        BigDecimal quantityToSell = BigDecimal.valueOf(20);
        testAssetHolding.removeFromPosition(quantityToSell);
        assertEquals(BigDecimal.valueOf(187), testAssetHolding.getTotalQuantity());
    }
    
    @Test
    void testRemoveFromPositionInValidNegativeNumber() {
        AssetHolding testAssetHolding = new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            initalQuantity, 
            initialCostPerUnit, 
            createdAt
        );
    
        BigDecimal quantityToSell = BigDecimal.valueOf(-20);
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> testAssetHolding.removeFromPosition(quantityToSell));
        assertEquals("Cannot sell aseet with a negative quantity.", e1.getMessage());
        
    }
    
    @Test 
    void testRemoveFromPositionInValidRemoveMoreThanWeHave() {
        AssetHolding testAssetHolding = new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            initalQuantity, 
            initialCostPerUnit, 
            createdAt
        );
    
        BigDecimal quantityToSell = BigDecimal.valueOf(20000);
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> testAssetHolding.removeFromPosition(quantityToSell));
        assertEquals("Cannot sell more units than you have.", e1.getMessage());
        

    }
}
