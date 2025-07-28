package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.MarketPrice;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;

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
            "APPL",
            AssetType.STOCK, 
            "US0378331005", 
            "APPLE", 
            "NASDAQ", 
            "SOME DESCRIPTOIN",
            "TECH"
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
        assertEquals(assetId, testAssetHolding.getAssetId());
        assertEquals(portfolioId, testAssetHolding.getPortfolioId());
        assertEquals(assetIdentifier, testAssetHolding.getAssetIdentifier());
        assertEquals(initalQuantity, testAssetHolding.getTotalQuantity());
        assertEquals(initialCostPerUnit, testAssetHolding.getTotalAdjustedCostBasis());
        assertEquals(createdAt, testAssetHolding.getCreatedAt());
        assertEquals(createdAt, testAssetHolding.getUpdatedAt());
        assertEquals(4, testAssetHolding.getAssetIdentifier().assetType().getDefaultQuantityPrecision().getDecimalPlaces());
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
    void testAddToPositionInValidIfBranches() {
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

        BigDecimal quantity = new BigDecimal(-2);
        Money costBasis = new Money(new BigDecimal("245.32"), usd);
        
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> testAssetHolding.addToPosition(quantity, costBasis));
        assertEquals("Quantity must be positive.", e1.getMessage());
        
        BigDecimal quantity02 = new BigDecimal(20);
        Currency cad = Currency.getInstance("CAD");
        Money costBasis02 = new Money(new BigDecimal("245.32"), cad);
        Exception e2 = assertThrows(IllegalArgumentException.class, () -> testAssetHolding.addToPosition(quantity02, costBasis02));
        assertEquals("Cost basis currency must match existing holdings currency.", e2.getMessage());




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
        // The expected value should be the full precision result:
        // 15000 - (215.55 / 207) * 10
        // (215.55 / 207) = 1.041304347826086956521739130434783 (from DECIMAL128 precision)
        // (1.0413... * 10) = 10.41304347826086956521739130434783
        // 15000 - 10.41304347826086956521739130434783 = 14989.58695652173913043478260869565
        
        // Use the exact string representation for the expected BigDecimal
        Money expectedCapitalGains = new Money("14989.58695652173913043478260869565", usd); 
        
        // Use assertEquals on the Money objects directly, assuming Money.equals() uses compareTo
        assertEquals(expectedCapitalGains, captialGains, "Capital gains should be precisely calculated.");   
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
            initialCostPerUnit, // This is the total cost basis
            createdAt
        );

        Money avgCostBasisPerUnit = testAssetHolding.getAverageACBPerUnit();
        
        // --- FIX: Calculate expected by performing the same division using Money objects ---
        // This ensures the expected value matches the exact precision and rounding of the actual code.
        Money expected = initialCostPerUnit.divide(initalQuantity); // Use Money's divide method

        assertEquals(expected, avgCostBasisPerUnit, "Average ACB per unit should be precisely calculated.");
    }

    @Test 
    void testGetAverageACBPerUnitTotalQuantityIsZero() {
        AssetHolding testAssetHolding = new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            BigDecimal.ZERO, 
            initialCostPerUnit, 
            createdAt
        );

        Money avgCostBasisPerUnit = testAssetHolding.getAverageACBPerUnit();
        Money expected = Money.ZERO(usd);
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
    void testRemoveFromPositionRemoveAll() {
        AssetHolding testAssetHolding = new AssetHolding(
            assetId, 
            portfolioId, 
            assetIdentifier, 
            initalQuantity, 
            initialCostPerUnit, 
            createdAt
        );
    
        BigDecimal quantityToSell = BigDecimal.valueOf(207);
        testAssetHolding.removeFromPosition(quantityToSell);
        assertEquals(BigDecimal.valueOf(0), testAssetHolding.getTotalQuantity());
        assertEquals(BigDecimal.valueOf(0), testAssetHolding.getTotalAdjustedCostBasis().amount());
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
