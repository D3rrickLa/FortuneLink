package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.laderrco.fortunelink.portfoliomanagment.domain.services.CurrencyConversionService;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.MarketPrice;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;

public class AssetHoldingTest {
 private AssetHolding assetHolding;
    private final Currency usd = Currency.getInstance("USD");

    @BeforeEach
    void setUp() {
        // Initial state: 10 units at a total cost of $100,000 USD
        assetHolding = new AssetHolding(
            new AssetHoldingId(UUID.randomUUID()),
            new PortfolioId(UUID.randomUUID()),
            new AssetIdentifier(AssetType.CRYPTO, "BTC", "BTC-Spot", "Bitcoin", "CoinGeeko", usd),
            BigDecimal.valueOf(10),
            new Money(new BigDecimal("100000.00"), usd),
            Instant.now()
        );
    }

    // Test cases for the constructor and getters
    @Test
    void testConstructorAndGetters() {
        assertNotNull(assetHolding.getAssetHoldingId());
        assertNotNull(assetHolding.getPortfolioId());
        assertEquals("BTC", assetHolding.getAssetIdentifier().symbol()); // Assuming a getter for symbol exists
        assertEquals(new BigDecimal("10"), assetHolding.getQuantity());
        assertEquals(new Money(new BigDecimal("100000.00"), usd), assetHolding.getTotalAdjustedCostBasisNativeCurrency());
        assertNotNull(assetHolding.getCreatedAt());
        assertNotNull(assetHolding.getUpdatedAt());
    }

    // Test cases for addToPosition()
    @Test
    void testAddToPosition_addsToQuantityAndCostBasis() {
        BigDecimal quantityToAdd = new BigDecimal("5");
        Money costToAdd = new Money(new BigDecimal("60000.00"), usd);
        
        assetHolding.addToPosition(quantityToAdd, costToAdd);

        assertEquals(new BigDecimal("15"), assetHolding.getQuantity());
        assertEquals(new Money(new BigDecimal("160000.00"), usd), assetHolding.getTotalAdjustedCostBasisNativeCurrency());
    }

    @Test
    void testAddToPosition_throwsForNonPositiveQuantity() {
        assertThrows(IllegalArgumentException.class, () -> assetHolding.addToPosition(BigDecimal.ZERO, new Money(BigDecimal.ZERO, usd)));
        assertThrows(IllegalArgumentException.class, () -> assetHolding.addToPosition(new BigDecimal("-1"), new Money(BigDecimal.ZERO, usd)));
    }

    @Test
    void testAddToPosition_throwsForMismatchedCurrency() {
        Money mismatchedCurrency = new Money(new BigDecimal("10000"), Currency.getInstance("EUR"));
        assertThrows(IllegalArgumentException.class, () -> assetHolding.addToPosition(new BigDecimal("1"), mismatchedCurrency));
    }
    
    // Test cases for removeFromPosition()
    @Test
    void testRemoveFromPosition_partialSale() {
        BigDecimal quantityToRemove = new BigDecimal("2.5");
        
        assetHolding.removeFromPosition(quantityToRemove);
        
        assertEquals(new BigDecimal("7.5"), assetHolding.getQuantity());
        assertEquals(new Money(new BigDecimal("75000.00"), usd), assetHolding.getTotalAdjustedCostBasisNativeCurrency());
    }

    @Test
    void testRemoveFromPosition_fullSale() {
        BigDecimal quantityToRemove = new BigDecimal("10");
        
        assetHolding.removeFromPosition(quantityToRemove);
        
        assertEquals(BigDecimal.ZERO, assetHolding.getQuantity());
        assertEquals(Money.ZERO(usd), assetHolding.getTotalAdjustedCostBasisNativeCurrency());
    }
    
    @Test
    void testRemoveFromPosition_throwsForNonPositiveQuantity() {
        assertThrows(IllegalArgumentException.class, () -> assetHolding.removeFromPosition(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> assetHolding.removeFromPosition(new BigDecimal("-1")));
    }
    
    @Test
    void testRemoveFromPosition_throwsForSellingMoreThanHeld() {
        BigDecimal quantityToRemove = new BigDecimal("11");
        assertThrows(IllegalArgumentException.class, () -> assetHolding.removeFromPosition(quantityToRemove));
    }
    
    // Test cases for calculateCapitalGain()
    @Test
    void testCalculateCapitalGain_positiveGain() {
        // Initial ACB: $100,000 / 10 = $10,000 per unit
        // Sold 2 units at $12,000 per unit
        BigDecimal soldQuantity = new BigDecimal("2");
        Money salePrice = new Money(new BigDecimal("12000"), usd);
        
        Money capitalGain = assetHolding.calculateCapitalGain(soldQuantity, salePrice);
        
        Money expectedCostBasis = new Money(new BigDecimal("20000.00"), usd); // 2 units * $10,000
        Money expectedProceeds = new Money(new BigDecimal("24000.00"), usd); // 2 units * $12,000
        assertEquals(expectedProceeds.subtract(expectedCostBasis), capitalGain);
    }
    
    @Test
    void testCalculateCapitalGain_negativeGain() {
        // Sold 3 units at $8,000 per unit (loss)
        BigDecimal soldQuantity = new BigDecimal("3");
        Money salePrice = new Money(new BigDecimal("8000"), usd);

        Money capitalGain = assetHolding.calculateCapitalGain(soldQuantity, salePrice);
        
        Money expectedCostBasis = new Money(new BigDecimal("30000.00"), usd); // 3 units * $10,000
        Money expectedProceeds = new Money(new BigDecimal("24000.00"), usd); // 3 units * $8,000
        assertEquals(expectedProceeds.subtract(expectedCostBasis), capitalGain);
    }

    @Test
    void testCalculateCapitalGain_throwsForInvalidQuantity() {
        assertThrows(IllegalArgumentException.class, () -> assetHolding.calculateCapitalGain(BigDecimal.ZERO, new Money(BigDecimal.ZERO, usd)));
        assertThrows(IllegalArgumentException.class, () -> assetHolding.calculateCapitalGain(new BigDecimal("11"), new Money(BigDecimal.ZERO, usd)));
    }
    
    // Test cases for getCurrentValue()
    @Test
    void testGetCurrentValue_calculatesCorrectly() {
        MarketPrice currentPrice = Mockito.mock(MarketPrice.class);
        when(currentPrice.price()).thenReturn(new Money(new BigDecimal("15000"), usd));
        
        Money currentValue = assetHolding.getCurrentValue(currentPrice);
        
        assertEquals(new Money(new BigDecimal("150000"), usd), currentValue);
    }
    
    @Test
    void testGetCurrentValue_throwsForMismatchedCurrency() {
        MarketPrice currentPrice = Mockito.mock(MarketPrice.class);
        when(currentPrice.price()).thenReturn(new Money(BigDecimal.ZERO, Currency.getInstance("EUR")));

        assertThrows(IllegalArgumentException.class, () -> assetHolding.getCurrentValue(currentPrice));
    }
    
    // Test cases for getAverageACBPerUnit()
    @Test
    void testGetAverageACBPerUnit() {
        // Initial state is 10 units at a total cost of $100,000
        Money averageCost = assetHolding.getAverageACBPerUnit();
        assertEquals(new Money(new BigDecimal("10000.00"), usd), averageCost);
    }
    
    @Test
    void testGetAverageACBPerUnit_whenQuantityIsZero() {
        // Create an AssetHolding with zero quantity
        AssetHolding emptyHolding = new AssetHolding(
            new AssetHoldingId(UUID.randomUUID()),
            new PortfolioId(UUID.randomUUID()),
            new AssetIdentifier(AssetType.CRYPTO, "ETH", "ETH-ERC20", "Ethereum", "CoinGeeko", usd),
            BigDecimal.ZERO,
            Money.ZERO(usd),
            Instant.now()
        );
        Money averageCost = emptyHolding.getAverageACBPerUnit();
        assertEquals(Money.ZERO(usd), averageCost);
    }

    // Test cases for getCostBasisInOtherCurrency()
    @Test
    void testGetCostBasisInOtherCurrency() {
        CurrencyConversionService conversionService = Mockito.mock(CurrencyConversionService.class);
        Currency eur = Currency.getInstance("EUR");
        Instant date = Instant.now();
        
        Money nativeCost = new Money(new BigDecimal("100000.00"), usd);
        Money convertedCost = new Money(new BigDecimal("92000.00"), eur);

        when(conversionService.convert(eq(nativeCost), eq(eur), eq(date))).thenReturn(convertedCost);
        
        Money result = assetHolding.getCostBasisInOtherCurrency(conversionService, eur, date);
        
        assertEquals(convertedCost, result);
    }
}
