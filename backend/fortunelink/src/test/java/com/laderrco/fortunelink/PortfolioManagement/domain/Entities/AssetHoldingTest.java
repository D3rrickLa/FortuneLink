package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.PortfolioCurrency;

public class AssetHoldingTest {
    @Test
    void newAssetHolding_shouldBeCreatedWithValidDataAndGenerateId() {
        // Arrange
        UUID portfolioId = UUID.randomUUID();
        AssetIdentifier assetId = new AssetIdentifier("AAPL", "NASDAQ", null, "Apple Inc.");
        BigDecimal initialQuantity = new BigDecimal("10");
        Money initialCost = new Money(new BigDecimal("1500"), new PortfolioCurrency("USD", "$")); // 10 shares *
                                                                                                  // $150/share
        LocalDate acquisitionDate = LocalDate.now();

        // Act
        // This will likely be a compile error first, then runtime error
        AssetHolding holding = new AssetHolding(UUID.randomUUID(), portfolioId, assetId, initialQuantity, initialCost, acquisitionDate);

        // Assert
        assertNotNull(holding.getAssetHoldingId());
        assertEquals(portfolioId, holding.getPortfolioId());
        assertEquals(assetId, holding.getAssetIdentifier());
        assertEquals(initialQuantity, holding.getQuantity());
        assertEquals(initialCost, holding.getCostBasis());
        assertEquals(acquisitionDate, holding.getAcquisitionDate());
        assertNotNull(holding.getCreatedAt());
        assertNotNull(holding.getUpdatedAt());
    }

    // (Add RED tests for constructor validation: null portfolioId, zero/negative
    // quantity, etc.)
    @Test
    void newAssetHolding_shouldThrowExceptionForZeroOrNegativeInitialQuantity() {
        UUID portfolioId = UUID.randomUUID();
        AssetIdentifier assetId = new AssetIdentifier("AAPL", "NASDAQ", null, "Apple Inc.");
        Money initialCost = new Money(new BigDecimal("1500"), new PortfolioCurrency("USD", "$"));
        LocalDate acquisitionDate = LocalDate.now();

        assertThrows(IllegalArgumentException.class,
                () -> new AssetHolding(UUID.randomUUID(), portfolioId, assetId, BigDecimal.ZERO, initialCost, acquisitionDate));
        assertThrows(IllegalArgumentException.class,
                () -> new AssetHolding(UUID.randomUUID(), portfolioId, assetId, new BigDecimal("-5"), initialCost, acquisitionDate));
    }

    // Add to AssetHoldingTest.java
    @Test
    void recordAdditionalPurchase_shouldUpdateQuantityAndCostBasis() {
        // Arrange
        AssetHolding holding = new AssetHolding(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new AssetIdentifier("GOOGL", "NASDAQ", null, "Google Inc."),
                new BigDecimal("10"), new Money(new BigDecimal("10000"), new PortfolioCurrency("USD", "$")), // 10
                                                                                                             // shares @
                                                                                                             // $1000
                                                                                                             // each
                LocalDate.now());
        BigDecimal additionalQuantity = new BigDecimal("5");
        Money additionalCostTotal = new Money(new BigDecimal("7000"), new PortfolioCurrency("USD", "$")); // 5 shares @
                                                                                                          // $1400 each

        // Act
        holding.recordAdditionalPurchase(additionalQuantity, additionalCostTotal);

        // Assert
        assertEquals(new BigDecimal("15"), holding.getQuantity()); // 10 + 5
        // Total cost: 10000 + 7000 = 17000
        assertEquals(new Money(new BigDecimal("17000.0000"), new PortfolioCurrency("USD", "$")),
                holding.getCostBasis()); // Assuming Money normalizes scale
        assertNotEquals(holding.getCreatedAt(), holding.getUpdatedAt()); // Updated timestamp should change
    }

    @Test
    void recordAdditionalPurchase_shouldThrowExceptionForNegativeQuantity() {
        AssetHolding holding = new AssetHolding(
                UUID.randomUUID(), UUID.randomUUID(), new AssetIdentifier("XYZ", "NYSE", null, "XYZ Corp"),
                new BigDecimal("10"), new Money(new BigDecimal("1000"), new PortfolioCurrency("USD", "$")),
                LocalDate.now());
        assertThrows(IllegalArgumentException.class, () -> holding.recordAdditionalPurchase(new BigDecimal("-1"),
                new Money(new BigDecimal("100"), new PortfolioCurrency("USD", "$"))));
    }

    @Test
    void recordAdditionalPurchase_shouldThrowExceptionForCurrencyMismatch() {
        AssetHolding holding = new AssetHolding(
                UUID.randomUUID(), UUID.randomUUID(), new AssetIdentifier("ABC", "NYSE", null, "ABC Corp"),
                new BigDecimal("10"), new Money(new BigDecimal("1000"), new PortfolioCurrency("USD", "$")),
                LocalDate.now());
        assertThrows(IllegalArgumentException.class, () -> holding.recordAdditionalPurchase(new BigDecimal("1"),
                new Money(new BigDecimal("100"), new PortfolioCurrency("EUR", "$"))));
    }

    @Test
    void recordSale_shouldReduceQuantityAndAdjustCostBasisProportionally() {
        // Arrange
        AssetHolding holding = new AssetHolding(
                UUID.randomUUID(), UUID.randomUUID(),
                new AssetIdentifier("GOOGL", "NASDAQ", null, "Google Inc."),
                new BigDecimal("10"), new Money(new BigDecimal("10000"), new PortfolioCurrency("USD", "$")), // 10 shares @ $1000 each
                LocalDate.now());
        BigDecimal quantitySold = new BigDecimal("2");
        Money salePricePerUnit = new Money(new BigDecimal("1100"), new PortfolioCurrency("USD", "$")); // Sale price (not directly used for
                                                                           // costBasis here)

        // Act
        holding.recordSale(quantitySold, salePricePerUnit);

        // Assert
        assertEquals(new BigDecimal("8"), holding.getQuantity()); // 10 - 2
        // Initial average cost: 10000 / 10 = 1000
        // Cost of sold: 2 * 1000 = 2000
        // Remaining cost: 10000 - 2000 = 8000
        assertEquals(new Money(new BigDecimal("8000.0000"), new PortfolioCurrency("USD", "$")), holding.getCostBasis());
    }

    @Test
    void recordSale_shouldThrowExceptionForSellingMoreThanAvailable() {
        AssetHolding holding = new AssetHolding(
                UUID.randomUUID(), UUID.randomUUID(), new AssetIdentifier("XYZ", "NYSE", null, "XYZ Corp"),
                new BigDecimal("10"), new Money(new BigDecimal("1000"), new PortfolioCurrency("USD", "$")), LocalDate.now());
        assertThrows(IllegalArgumentException.class,
                () -> holding.recordSale(new BigDecimal("11"), new Money(new BigDecimal("100"), new PortfolioCurrency("USD", "$"))));
    }

}
