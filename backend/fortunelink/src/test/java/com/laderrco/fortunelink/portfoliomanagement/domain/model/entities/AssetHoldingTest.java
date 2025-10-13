package com.laderrco.fortunelink.portfoliomanagement.domain.model.entities;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.events.DividendReceivedEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.DividendReinvestedEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.DomainEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.HoldingDecreasedEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.HoldingIncreasedEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InsufficientHoldingException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidDividendAmountException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidHoldingCostBasisException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidHoldingOperationException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidHoldingQuantityException;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.Precision;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.MarketSymbol;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Price;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Quantity;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.shared.domain.valueobjects.Money;


public class AssetHoldingTest {

    private AssetHoldingId testHoldingId;
    private PortfolioId testPortfolioId;
    private AssetIdentifier testAssetIdentifier;
    private AssetType testAssetType;
    private Currency CAD;
    private Currency USD;

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private Quantity quantity(String value) {
        return new Quantity(decimal(value));
    }

    private Money cadMoney(String amount) {
        return new Money(decimal(amount), CAD);
    }
    private Money usdMoney(String amount) {
        return new Money(decimal(amount), USD);
    }

    private Price priceCAD(String value) {
        return new Price(cadMoney(value));
    }

    private Price priceUSD(String value) {
        return new Price(usdMoney(value));
    }

    @BeforeEach
    void init() {
        testHoldingId = AssetHoldingId.randomId();
        testPortfolioId = PortfolioId.randomId();
        testAssetIdentifier = new MarketSymbol("AAPL");
        testAssetType = AssetType.STOCK;
        CAD = Currency.getInstance("CAD");
        USD = Currency.getInstance("USD");
    }

    @Nested
    public class CreatingInitialHoldingTests {
        @Test
        @DisplayName("Should create holding with correct initial data")
        void givenCorrect_whenInitializing_returnAssetHolding() {
            Quantity quantity = quantity("100");
            Price pricePerUnit = new Price(cadMoney("50"));

            AssetHolding holding = AssetHolding.createInitialHolding(testPortfolioId, testHoldingId,
                    testAssetIdentifier, AssetType.STOCK, quantity, pricePerUnit, Instant.now());

            assertAll(
                    () -> assertEquals(decimal("100"), holding.getTotalQuantity().amount()),
                    () -> assertEquals(cadMoney("50"), holding.getAverageCostBasis().pricePerUnit()),
                    () -> assertEquals(cadMoney("5000"), holding.getTotalCostBasis()));
        }

        @Test
        @DisplayName("Should fail when creating with negative quantity")
        void givenNulls_whenInitializing_throwException() {
            assertAll(
                () -> assertThrows(NullPointerException.class, () -> AssetHolding.createInitialHolding(null, testHoldingId,
                        testAssetIdentifier, AssetType.STOCK, quantity("100"), new Price(cadMoney("50")), Instant.now())),
                () -> assertThrows(NullPointerException.class, () -> AssetHolding.createInitialHolding(testPortfolioId, null,
                        testAssetIdentifier, AssetType.STOCK, quantity("100"), new Price(cadMoney("50")), Instant.now())),
                () -> assertThrows(NullPointerException.class, () -> AssetHolding.createInitialHolding(testPortfolioId, testHoldingId,
                        null, AssetType.STOCK, quantity("100"), new Price(cadMoney("50")), Instant.now())),
                () -> assertThrows(NullPointerException.class, () -> AssetHolding.createInitialHolding(testPortfolioId, testHoldingId,
                        testAssetIdentifier, null, quantity("100"), new Price(cadMoney("50")), Instant.now())),
                () -> assertThrows(NullPointerException.class, () -> AssetHolding.createInitialHolding(testPortfolioId, testHoldingId,
                        testAssetIdentifier, AssetType.STOCK, null, new Price(cadMoney("50")), Instant.now())),
                () -> assertThrows(NullPointerException.class, () -> AssetHolding.createInitialHolding(testPortfolioId, testHoldingId,
                        testAssetIdentifier, AssetType.STOCK, quantity("100"), null, Instant.now())),
                () -> assertThrows(NullPointerException.class, () -> AssetHolding.createInitialHolding(testPortfolioId, testHoldingId,
                        testAssetIdentifier, AssetType.STOCK, quantity("100"), new Price(cadMoney("50")), null))
            );
        }

        @Test
        @DisplayName("Should fail when creating with zero quantity")
        void givenZeroQuantity_whenInitializing_throwException() {
            assertThrows(InvalidHoldingQuantityException.class, () ->{
                AssetHolding.createInitialHolding(testPortfolioId, testHoldingId, testAssetIdentifier, AssetType.STOCK, quantity("0"), new Price(cadMoney("100")), Instant.now());
            });
        }
        @Test
        @DisplayName("Should fail when creating with negative price per unit")
        void givenNegativeQuantity_whenInitializing_throwException() {
            assertThrows(IllegalArgumentException.class, () ->{
                AssetHolding.createInitialHolding(testPortfolioId, testHoldingId, testAssetIdentifier, AssetType.STOCK, quantity("-10"), new Price(cadMoney("100")), Instant.now());
            });
        }
        @Test
        @DisplayName("Should fail when creating with negative price per unit")
        void givenNegativePricePerUnit_whenInitializing_throwException() {
            assertThrows(InvalidHoldingCostBasisException.class, () -> {
                AssetHolding.createInitialHolding(testPortfolioId, testHoldingId, testAssetIdentifier, AssetType.STOCK, quantity("20"),  new Price(cadMoney("-10")), Instant.now());
            });
        }

        @Test
        @DisplayName("Return correct timestamps")
        void givenProperInput_whenInitializing_returnAssetHoldingWithCorrectTimestamps() {
            Instant expectedTime = Instant.now(); 
            Quantity quantity = quantity("100");
            Price pricePerUnit = new Price(cadMoney("50"));

            AssetHolding holding = AssetHolding.createInitialHolding(testPortfolioId, testHoldingId,
                    testAssetIdentifier, AssetType.STOCK, quantity, pricePerUnit, expectedTime);
            assertEquals(holding.getCreatedAt(), expectedTime);
        }

    }

    @Nested
    public class IncreasingPositionTests {   
        
        // Helper methods for test data
        private AssetHoldingId testHoldingId() {
            return AssetHoldingId.randomId();
        }

        private PortfolioId testPortfolioId() {
            return PortfolioId.randomId();
        }

        private Money cadMoney(String amount) {
            return new Money(new BigDecimal(amount), CAD);
        }

        private Money usdMoney(String amount) {
            return new Money(new BigDecimal(amount), USD);
        }

        private BigDecimal decimal(String value) {
            return new BigDecimal(value);
        }

        private Price price(String value) {
            return new Price(cadMoney(value));
        }

        private Instant NOW;

        @BeforeEach
        void init() {
            NOW = Instant.now();
        }
        // Increase Position & ACB Logic
        @Test
        void shouldIncreasePositionAndRecalculateACBCorrectly_singlePurchase() {
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId, 
                testHoldingId, 
                testAssetIdentifier, 
                AssetType.STOCK, quantity("100"), new Price(cadMoney("50")), Instant.now());
            
            holding.increasePosition(quantity("50"), new Price(cadMoney("60")), Instant.now());
            assertEquals(quantity("150"), holding.getTotalQuantity());
            assertEquals(cadMoney("8000"), holding.getTotalCostBasis(), "TOtal cost basis should be 8000");
             // New average ACB: $8,000 / 150 = $53.33 (rounded to 2 decimals)
            BigDecimal expectedAvgACB = new BigDecimal("8000.00")
                .divide(new BigDecimal("150"), Precision.getMoneyDecimalPlaces(), java.math.RoundingMode.HALF_EVEN);
            
            assertEquals(
                0,
                expectedAvgACB.compareTo(holding.getAverageCostBasis().pricePerUnit().amount()),
                "Average cost basis should be approximately $53.33"
            );
 
        }   

        @Test
        void shouldHandleMultiplePurchasesWithACBAveraging() {
            AssetHolding holding = AssetHolding.createInitialHolding(testPortfolioId, testHoldingId, testAssetIdentifier, testAssetType, quantity("100"), price("50"), NOW);

            holding.increasePosition(quantity("50"), new Price(cadMoney("60")), Instant.now());
            holding.increasePosition(quantity("75"), new Price(cadMoney("55")), Instant.now()); 
            assertEquals(
                quantity("225"), 
                holding.getTotalQuantity(),
             "Total quantity should be 225 after three purchases"
            );
            assertEquals(cadMoney("12125.00"), holding.getTotalCostBasis(), "Total cost basis chouls be 12125");
            BigDecimal expectedAvgACB = new BigDecimal("12125.00")
                .divide(new BigDecimal("225"), Precision.getMoneyDecimalPlaces(), java.math.RoundingMode.HALF_EVEN);
        
            System.out.println(expectedAvgACB);
            System.out.println(holding.getAverageCostBasis().pricePerUnit().amount());
            assertEquals(
                0,
                expectedAvgACB.compareTo(holding.getAverageCostBasis().pricePerUnit().amount()),
                "Average cost basis should be correctly averaged across all purchases"
            );

        }

        // Domain Events
        @Test
        void shouldEmitHoldingIncreasedEventWhenPositionIncreases() {
            AssetHolding holding = AssetHolding.createInitialHolding(testPortfolioId, testHoldingId, testAssetIdentifier, testAssetType, quantity("100"), price("50"), NOW);
            holding.markEventsAsCommitted();

            assertFalse(holding.hasUncommittedEvents(), "Should have no uncommitted evetns after markign as committed");

            Quantity purchaseQuantity = quantity("50");
            Price purchasePrice = price("60");
            Instant transactionDate = Instant.now();

            holding.increasePosition(purchaseQuantity, purchasePrice, transactionDate);

            assertTrue(
                holding.hasUncommittedEvents(),
                "Should have uncommitted events after increasing position"
            );
            
            assertEquals(
                1, 
                holding.getUncommittedEvents().size(),
                "Should have exactly one event"
            );
            
            assertTrue(
                holding.getUncommittedEvents().get(0) instanceof HoldingIncreasedEvent,
                "Event should be of type HoldingIncreasedEvent"
            );

            // Verify event details
            HoldingIncreasedEvent event = (HoldingIncreasedEvent) holding.getUncommittedEvents().get(0);
            assertEquals(testPortfolioId, event.portfolioId(), "Event should have correct portfolioId");
            assertEquals(testHoldingId, event.assetHoldingId(), "Event should have correct holdingId");
            assertEquals(purchaseQuantity, event.quantity(), "Event should have correct quantity");
            assertEquals(purchasePrice, event.pricePerUnit(), "Event should have correct price");
            assertEquals(transactionDate, event.transactionDate(), "Event should have correct timestamp");
            assertEquals(transactionDate, event.occuredOn());
            assertEquals(String.format("{PortfolioId: %s, AssetHoldingId: %s}", testPortfolioId, testHoldingId), event.aggregateId());
            assertEquals("Increase position event.", event.eventType());

        }

        // Timestamps & Versioning
        @Test
        void shouldUpdateLastTransactionAtTimestampOnIncrease() {
            // Arrange
            Instant initialTime = Instant.parse("2024-01-01T10:00:00Z");
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId(),
                testHoldingId(),
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                price("50.00"),
                initialTime
            );
            
            assertEquals(
                initialTime, 
                holding.getLastTransactionAt(),
                "Initial lastTransactionAt should match creation time"
            );

            // Act
            Instant newTransactionTime = Instant.parse("2024-01-15T14:30:00Z");
            holding.increasePosition(
                quantity("50"),
                price("60.00"),
                newTransactionTime
            );

            // Assert
            assertEquals(
                newTransactionTime, 
                holding.getLastTransactionAt(),
                "lastTransactionAt should be updated to new transaction time"
            );
            
            assertNotEquals(
                initialTime,
                holding.getLastTransactionAt(),
                "lastTransactionAt should have changed from initial time"
            );
        }

        @Test
        void shouldIncrementVersionNumberOnIncrease() {
            // Arrange
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId(),
                testHoldingId(),
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                price("50.00"),
                Instant.now()
            );
            
            int initialVersion = holding.getVersion();

            // Act - First increase
            holding.increasePosition(quantity("50"), price("60.00"), Instant.now());
            int versionAfterFirstIncrease = holding.getVersion();
            
            // Second increase
            holding.increasePosition(quantity("25"), price("55.00"), Instant.now());
            int versionAfterSecondIncrease = holding.getVersion();

            // Assert
            assertEquals(
                initialVersion + 1, 
                versionAfterFirstIncrease,
                "Version should increment by 1 after first increase"
            );
            
            assertEquals(
                initialVersion + 2, 
                versionAfterSecondIncrease,
                "Version should increment by 1 after each increase"
            );
            
            assertTrue(
                versionAfterSecondIncrease > versionAfterFirstIncrease,
                "Version should keep incrementing"
            );
        }

        // Validation Failures
        
        @Test
        void shouldFailWhenIncreasingWithZeroQuantity() {
            // Arrange
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId(),
                testHoldingId(),
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                price("50.00"),
                Instant.now()
            );

            // Act & Assert
            InvalidHoldingQuantityException exception = assertThrows(
                InvalidHoldingQuantityException.class,
                () -> holding.increasePosition(
                    quantity("0"),          // Zero quantity
                    price("60.00"),
                    Instant.now()
                ),
                "Should throw exception when quantity is zero"
            );
            
            assertTrue(
                exception.getLocalizedMessage().equals("quantity cannot be less than 0"),
                "Exception message should mention quantity must be positive"
            );
        }

        @Test
        void shouldFailWhenIncreasingWithNegativeQuantity() {
            // Arrange
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId(),
                testHoldingId(),
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                price("50.00"),
                Instant.now()
            );

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, // this is illegal arugment, not invalidholdingoperation due to fundamental
                () -> holding.increasePosition(
                    quantity("-50"),        // Negative quantity
                    price("60.00"),
                    Instant.now()
                ),
                "Should throw exception when quantity is negative"
            );
            
            assertTrue(
                exception.getMessage().contains("negative"),
                "Exception message should mention quantity must be positive"
            );
        }

        @Test
        void shouldFailWhenPriceIsZeroOrNegative() {
            // Arrange
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId(),
                testHoldingId(),
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                price("50.00"),
                Instant.now()
            );

            // Act & Assert - Zero price
            assertThrows(
                InvalidHoldingOperationException.class,
                () -> holding.increasePosition(
                    quantity("50"),
                    price("0.00"),      // Zero price
                    Instant.now()
                ),
                "Should throw exception when price is zero"
            );
            
            // Act & Assert - Negative price
            assertThrows(
                InvalidHoldingOperationException.class,
                () -> holding.increasePosition(
                    quantity("50"),
                    price("-60.00"),    // Negative price
                    Instant.now()
                ),
                "Should throw exception when price is negative"
            );
        }

        @Test
        void shouldFailWhenCurrencyDoesNotMatchBaseCurrency() {
            // Arrange - Create holding in CAD
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId(),
                testHoldingId(),
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                price("50.00"),
                Instant.now()
            );
            
            assertEquals(
                CAD, 
                holding.getBaseCurrency(),
                "Base currency should be CAD"
            );

            // Act & Assert - Try to buy with USD
            InvalidHoldingOperationException exception = assertThrows(
                InvalidHoldingOperationException.class,
                () -> holding.increasePosition(
                    quantity("50"),
                    new Price(usdMoney("60.00")),     // USD currency - mismatch!
                    Instant.now()
                ),
                "Should throw exception when currency doesn't match"
            );
            
            assertTrue(
                exception.getMessage().toLowerCase().contains("currency"),
                "Exception message should mention currency mismatch"
            );
        }

        @Test
        void shouldFailWhenAnyParameterIsNull() {
            // Arrange
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId(),
                testHoldingId(),
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                price("50.00"),
                Instant.now()
            );

            // Act & Assert - Null quantity
            assertThrows(
                NullPointerException.class,
                () -> holding.increasePosition(
                    null,                  // Null quantity
                    price("60.00"),
                    Instant.now()
                ),
                "Should throw exception when quantity is null"
            );
            
            // Act & Assert - Null price
            assertThrows(
                NullPointerException.class,
                () -> holding.increasePosition(
                    quantity("50"),
                    null,                  // Null price
                    Instant.now()
                ),
                "Should throw exception when price is null"
            );
            
            // Act & Assert - Null transaction date
            assertThrows(
                NullPointerException.class,
                () -> holding.increasePosition(
                    quantity("50"),
                    price("60.00"),
                    null                   // Null transaction date
                ),
                "Should throw exception when transaction date is null"
            );
        }

    }

    @Nested
    public class DecreasingPositionTests {
        // Core Sale Logic
        @Test
        void shouldDecreasePositionAndCalculateRealizedGain() {
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId, 
                testHoldingId, 
                testAssetIdentifier, 
                testAssetType, 
                quantity("100"),
                priceCAD("50"),
                Instant.now());

            holding.decreasePosition(quantity("30"), priceCAD("70"), Instant.now());

            assertEquals(quantity("70"), holding.getTotalQuantity(), "'Should have 70 shares remaining after selling 30");
            assertEquals(Money.of(3500, "CAD"), holding.getTotalCostBasis(), "Total ACB should be reduced by sold shares' ACB");
            assertEquals(priceCAD("50"), holding.getAverageCostBasis(), "Average ACB should remina unchagned at $50");

            var events = holding.getUncommittedEvents();
            var decreaseEvent = (HoldingDecreasedEvent) events.get(events.size() - 1);
            assertEquals(Money.of(600, "CAD"), decreaseEvent.realizedGainLoss(), "Realised gain should be $600");
        }

        @Test
        void shouldCalculateRealizedLossWhenSellingAtALoss() {
            // Arrange - 100 shares @ $50 ACB
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),
                Instant.now()
            );

            // Act - Sell 50 shares @ $30 (below ACB!)
            holding.decreasePosition(
                quantity("50"),
                priceCAD("30.00"),
                Instant.now()
            );

            // Assert
            assertEquals(
                quantity("50"), 
                holding.getTotalQuantity(),
                "Should have 50 shares remaining"
            );
            
            // Remaining ACB: 50 * $50 = $2,500
            assertEquals(
                cadMoney("2500.00"), 
                holding.getTotalCostBasis(),
                "Remaining ACB should be $2,500"
            );
            
            // Check realized LOSS in event
            // Sale proceeds: 50 * $30 = $1,500
            // Sold ACB: 50 * $50 = $2,500
            // Realized loss: $1,500 - $2,500 = -$1,000
            var events = holding.getUncommittedEvents();
            var decreaseEvent = (HoldingDecreasedEvent) events.get(events.size() - 1);
            assertEquals(
                Money.of(-1000.00, "CAD"), 
                decreaseEvent.realizedGainLoss(),
                "Realized loss should be -$1,000"
            );
            
            assertTrue(
                decreaseEvent.realizedGainLoss().isNegative(),
                "Realized gain/loss should be negative (loss)"
            );
            
        }
        
        @Test
        void shouldMaintainCorrectACBAfterPartialSale() {
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId, 
                testHoldingId, 
                testAssetIdentifier, 
                testAssetType, 
                quantity("100"),
                priceCAD("40"), 
                Instant.now()
            );
    
            holding.increasePosition(quantity("100"), priceCAD("60"), Instant.now());
            holding.markEventsAsCommitted();
            holding.decreasePosition(quantity("75"), priceCAD("80"), Instant.now());
            
            // Assert
            // Remaining: 200 - 75 = 125 shares
            assertEquals(
                quantity("125"), 
                holding.getTotalQuantity(),
                "Should have 125 shares remaining"
            );
            
            // Sold ACB: 75 * $50 = $3,750
            // Remaining ACB: $10,000 - $3,750 = $6,250
            assertEquals(
                Money.of(6250.00, "CAD"), 
                holding.getTotalCostBasis(),
                "Remaining total ACB should be $6,250"
            );
            
            // Average ACB: $6,250 / 125 = $50 (unchanged)
            assertEquals(
                priceCAD("50.00"), 
                holding.getAverageCostBasis(),
                "Average ACB should still be $50"
            );
        }
        
        @Test
        void shouldZeroOutACBWhenFullySellingPosition() {
            // Arrange - 100 shares @ $50 ACB
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),
                Instant.now()
            );
    
            // Act - Sell ALL 100 shares
            holding.decreasePosition(
                quantity("100"),
                priceCAD("60.00"),
                Instant.now()
            );
    
            // Assert
            assertTrue(
                holding.isEmpty(),
                "Holding should be empty after selling everything"
            );
            
            assertEquals(
                quantity("0"), 
                holding.getTotalQuantity(),
                "Quantity should be zero"
            );
            
            assertEquals(
                Money.of(0.00, "CAD"), 
                holding.getTotalCostBasis(),
                "Total ACB should be zero"
            );
            
            assertEquals(
                priceCAD("0.00"), 
                holding.getAverageCostBasis(),
                "Average ACB should be zero"
            );
            
            assertTrue(
                holding.shouldBeRemoved(),
                "Holding should be marked for removal when fully liquidated"
            );
        }

        // Domain Events
        @Test
        void shouldEmitHoldingDecreasedEventWithCorrectRealizedGainLoss() {
            // Arrange
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),
                Instant.now()
            );
            holding.markEventsAsCommitted();

            // Act
            Quantity saleQuantity = quantity("25");
            Price salePrice = priceCAD("80.00");
            Instant saleDate = Instant.now();
            
            holding.decreasePosition(saleQuantity, salePrice, saleDate);

            // Assert
            assertTrue(
                holding.hasUncommittedEvents(),
                "Should have uncommitted events after sale"
            );
            
            assertEquals(
                1, 
                holding.getUncommittedEvents().size(),
                "Should have exactly one event"
            );
            
            assertTrue(
                holding.getUncommittedEvents().get(0) instanceof HoldingDecreasedEvent,
                "Event should be HoldingDecreasedEvent"
            );
            
            HoldingDecreasedEvent event = (HoldingDecreasedEvent) holding.getUncommittedEvents().get(0);
            
            assertEquals(testPortfolioId, event.portfolioId());
            assertEquals(testHoldingId, event.assetHoldingId());
            assertEquals(saleQuantity, event.quantity());
            assertEquals(salePrice, event.pricePerUnit());
            assertEquals(saleDate, event.transactionDate());
            
            // Realized gain: (25 * $80) - (25 * $50) = $2,000 - $1,250 = $750
            assertEquals(
                cadMoney("750.00"), 
                event.realizedGainLoss(),
                "Event should contain correct realized gain"
            );
        }

        // Timestamps & Versioning
        @Test
        void shouldUpdateLastTransactionAtTimestampOnDecrease() {
            Instant initialTime = Instant.parse("2024-01-01T10:00:00Z");
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),
                initialTime
            );

            // Act
            Instant saleTime = Instant.parse("2024-02-15T14:30:00Z");
            holding.decreasePosition(
                quantity("30"),
                priceCAD("70.00"),
                saleTime
            );

            // Assert
            assertEquals(
                saleTime, 
                holding.getLastTransactionAt(),
                "lastTransactionAt should be updated to sale time"
            );
            
            assertNotEquals(
                initialTime,
                holding.getLastTransactionAt(),
                "lastTransactionAt should have changed"
            );
        }

        @Test
        void shouldIncrementVersionNumberOnDecrease() {
            // Arrange
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),
                Instant.now()
            );
            
            int initialVersion = holding.getVersion();

            // Act - First sale
            holding.decreasePosition(quantity("20"), priceCAD("60.00"), Instant.now());
            int versionAfterFirstSale = holding.getVersion();
            
            // Second sale
            holding.decreasePosition(quantity("30"), priceCAD("65.00"), Instant.now());
            int versionAfterSecondSale = holding.getVersion();

            // Assert
            assertEquals(
                initialVersion + 1, 
                versionAfterFirstSale,
                "Version should increment after first sale"
            );
            
            assertEquals(
                initialVersion + 2, 
                versionAfterSecondSale,
                "Version should increment after each sale"
            );
        }

        // Validation Failures
        @Test
        void shouldFailWhenTryingToSellMoreThanHeld() {
            // Arrange - Only have 100 shares
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),
                Instant.now()
            );

            // Act & Assert - Try to sell 150 shares
            InsufficientHoldingException exception = assertThrows(
                InsufficientHoldingException.class,
                () -> holding.decreasePosition(
                    quantity("150"),  // More than available!
                    priceCAD("60.00"),
                    Instant.now()
                ),
                "Should throw exception when selling more than held"
            );
            
            assertTrue(
                exception.getMessage().contains("150") || 
                exception.getMessage().contains("100"),
                "Exception should mention quantity mismatch"
            );
        }

        @Test
        void shouldFailWhenSellingZeroQuantity() {
                // Arrange
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),
                Instant.now()
            );

            // Act & Assert
            assertThrows(
                InvalidHoldingQuantityException.class,
                () -> holding.decreasePosition(
                    quantity("0"),  // Zero!
                    priceCAD("60.00"),
                    Instant.now()
                ),
                "Should throw exception when selling zero quantity"
            );
        }

        @Test
        void shouldFailWhenSellingNegativeQuantity() {
                  // Arrange
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),
                Instant.now()
            );

            // Act & Assert
            assertThrows(
                IllegalArgumentException.class,
                () -> holding.decreasePosition(
                    quantity("-50"),  // Negative!
                    priceCAD("60.00"),
                    Instant.now()
                ),
                "Should throw exception when selling negative quantity"
            );
        }

        @Test
        void shouldFailWhenSalePriceIsNegative() {
            // Arrange
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),
                Instant.now()
            );

            // Act & Assert
            assertThrows(
                InvalidHoldingOperationException.class,
                () -> holding.decreasePosition(
                    quantity("50"),
                    priceCAD("-60.00"),  // Negative price!
                    Instant.now()
                ),
                "Should throw exception when sale price is negative"
            );
        }

        @Test
        void shouldFailWhenCurrencyDoesNotMatchBaseCurrency() {
                // Arrange - CAD holding
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),
                Instant.now()
            );

            // Act & Assert - Try to sell in USD
            assertThrows(
                InvalidHoldingOperationException.class,
                () -> holding.decreasePosition(
                    quantity("50"),
                    priceUSD("60.00"),  // Wrong currency!
                    Instant.now()
                ),
                "Should throw exception when currency doesn't match"
            );
        }

        @Test
        void shouldFailWhenAnyParameterIsNull() {
                // Arrange
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),
                Instant.now()
            );

            // Act & Assert - Null quantity
            assertThrows(
                NullPointerException.class,
                () -> holding.decreasePosition(
                    null,
                    priceCAD("60.00"),
                    Instant.now()
                )
            );
            
            // Null price
            assertThrows(
                NullPointerException.class,
                () -> holding.decreasePosition(
                    quantity("50"),
                    null,
                    Instant.now()
                )
            );
            
            // Null date
            assertThrows(
                NullPointerException.class,
                () -> holding.decreasePosition(
                    quantity("50"),
                    priceCAD("60.00"),
                    null
                )
            );
        }
        
    }

    @Nested
    public class MultipleBuysAndSellSequencesACBTests {

        @Test
        void shouldCorrectlyAverageACBAcrossMultiplePurchases() {
            // Arrange & Act - Make multiple purchases at different prices
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("40.00"),  // $4,000
                Instant.now()
            );
            
            holding.increasePosition(quantity("50"), priceCAD("60.00"), Instant.now());  // +$3,000
            holding.increasePosition(quantity("150"), priceCAD("50.00"), Instant.now()); // +$7,500

            // Assert
            // Total: 100 + 50 + 150 = 300 shares
            // Total cost: $4,000 + $3,000 + $7,500 = $14,500
            // Average: $14,500 / 300 = $48.33
            
            assertEquals(quantity("300"), holding.getTotalQuantity());
            assertEquals(cadMoney("14500.00"), holding.getTotalCostBasis());
            
            BigDecimal expectedAvg = new BigDecimal("14500.00")
                .divide(new BigDecimal("300"), Precision.getMoneyDecimalPlaces(), java.math.RoundingMode.HALF_EVEN);
            assertEquals(
                0, 
                expectedAvg.compareTo(holding.getAverageCostBasis().pricePerUnit().amount()),
                "Average ACB should be correctly calculated"
            );

        }

        @Test
        void shouldMaintainCorrectACBAfterBuySellBuySequence() {
            // Arrange
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),  // $5,000 total
                Instant.now()
            );

            // Act - Buy, Sell, Buy
            holding.increasePosition(quantity("100"), priceCAD("60.00"), Instant.now()); // +$6,000
            // Now: 200 shares, $11,000 total, $55 avg
            
            holding.decreasePosition(quantity("50"), priceCAD("70.00"), Instant.now());
            // Sold ACB: 50 * $55 = $2,750
            // Remaining: 150 shares, $8,250 total, $55 avg
            
            holding.increasePosition(quantity("50"), priceCAD("65.00"), Instant.now()); // +$3,250
            // Final: 200 shares, $11,500 total

            // Assert
            assertEquals(quantity("200"), holding.getTotalQuantity());
            assertEquals(Money.of(11500.00, "CAD"), holding.getTotalCostBasis());
            
            // Average: $11,500 / 200 = $57.50
            assertEquals(
                priceCAD("57.50"), 
                holding.getAverageCostBasis(),
                "Average ACB should be $57.50 after buy-sell-buy"
            );
        }

        @Test
        void shouldMaintainCorrectACBAfterBuyBuySellBuySellSequence() {
                  // Arrange
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("40.00"),  // $4,000
                Instant.now()
            );

            // Act - Complex sequence
            holding.increasePosition(quantity("50"), priceCAD("50.00"), Instant.now());   // +$2,500
            // 150 shares, $6,500, avg $43.33
            
            holding.increasePosition(quantity("50"), priceCAD("60.00"), Instant.now());   // +$3,000
            // 200 shares, $9,500, avg $47.50
            
            holding.decreasePosition(quantity("75"), priceCAD("80.00"), Instant.now());
            // Sold: 75 * $47.50 = $3,562.50
            // 125 shares, $5,937.50, avg $47.50
            
            holding.increasePosition(quantity("100"), priceCAD("55.00"), Instant.now());  // +$5,500
            // 225 shares, $11,437.50
            
            holding.decreasePosition(quantity("50"), priceCAD("90.00"), Instant.now());
            // Sold: 50 * $50.83 = $2,541.67
            // 175 shares, $8,895.83

            // Assert
            assertEquals(quantity("175"), holding.getTotalQuantity());
            
            // Verify ACB is maintained correctly
            assertTrue(
                holding.getTotalCostBasis().amount().compareTo(decimal("8800")) > 0 &&
                holding.getTotalCostBasis().amount().compareTo(decimal("9000")) < 0,
                "Total ACB should be approximately $8,900"
            );
        }

        @Test
        void shouldHandleAlternatingBuysAndSellsCorrectly() {
                   // Arrange
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),
                Instant.now()
            );

            // Act - Alternate buy/sell multiple times
            holding.increasePosition(quantity("50"), priceCAD("55.00"), Instant.now());
            holding.decreasePosition(quantity("40"), priceCAD("60.00"), Instant.now());
            holding.increasePosition(quantity("60"), priceCAD("52.00"), Instant.now());
            holding.decreasePosition(quantity("30"), priceCAD("58.00"), Instant.now());
            holding.increasePosition(quantity("40"), priceCAD("54.00"), Instant.now());

            // Assert - Verify quantity is correct
            // 100 + 50 - 40 + 60 - 30 + 40 = 180 shares
            assertEquals(
                quantity("180"), 
                holding.getTotalQuantity(),
                "Final quantity should be 180 shares"
            );
            
            // Verify ACB is positive and reasonable
            assertTrue(
                holding.getTotalCostBasis().amount().compareTo(BigDecimal.ZERO) > 0,
                "Total ACB should be positive"
            );
            
            assertTrue(
                holding.getAverageCostBasis().pricePerUnit().amount().compareTo(decimal("50")) > 0 &&
                holding.getAverageCostBasis().pricePerUnit().amount().compareTo(decimal("60")) < 0,
                "Average ACB should be between $50-$60"
            );
        }

        @Test
        void shouldCalculateCorrectTotalACBAfterComplexSequence() {
                // Arrange
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("200"),
                priceCAD("45.00"),  // $9,000
                Instant.now()
            );

            // Act - Simulate real trading pattern
            holding.increasePosition(quantity("100"), priceCAD("48.00"), Instant.now());  // +$4,800
            // 300 shares, $13,800, avg $46
            
            holding.decreasePosition(quantity("150"), priceCAD("52.00"), Instant.now());
            // Sold: 150 * $46 = $6,900
            // 150 shares, $6,900, avg $46
            
            holding.increasePosition(quantity("200"), priceCAD("50.00"), Instant.now());  // +$10,000
            // 350 shares, $16,900
            
            Money expectedTotalACB = cadMoney("16900.00");
            BigDecimal expectedAvgACB = new BigDecimal("16900.00")
                .divide(new BigDecimal("350"), Precision.getMoneyDecimalPlaces(), java.math.RoundingMode.HALF_EVEN);

            // Assert
            assertEquals(
                quantity("350"), 
                holding.getTotalQuantity(),
                "Should have 350 shares"
            );
            
            assertEquals(
                expectedTotalACB, 
                holding.getTotalCostBasis(),
                "Total ACB should be $16,900"
            );
            
            assertEquals(
                0,
                expectedAvgACB.compareTo(holding.getAverageCostBasis().pricePerUnit().amount()),
                "Average ACB should be correctly calculated"
            );
        }

    }

    @Nested
    public class DividendOperationTests {
        @Test
        void shouldRecordDividendWithoutAffectingACB() {
               // Arrange
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),
                Instant.now()
            );
            
            Money originalTotalACB = holding.getTotalCostBasis();
            Price originalAvgACB = holding.getAverageCostBasis();

            // Act - Record dividend
            holding.recordDividendReceived(Money.of(250.00, "CAD"), Instant.now());

            // Assert - ACB should be unchanged
            assertEquals(
                originalTotalACB, 
                holding.getTotalCostBasis(),
                "Total ACB should not change when recording dividend"
            );
            
            assertEquals(
                originalAvgACB, 
                holding.getAverageCostBasis(),
                "Average ACB should not change when recording dividend"
            );
        }

        @Test
        void shouldRecordDividendWithoutAffectingQuantity() {
              // Arrange
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),
                Instant.now()
            );
            
            Quantity originalQuantity = holding.getTotalQuantity();

            // Act - Record dividend
            holding.recordDividendReceived(cadMoney("250.00"), Instant.now());

            // Assert - Quantity should be unchanged
            assertEquals(
                originalQuantity, 
                holding.getTotalQuantity(),
                "Quantity should not change when recording dividend"
            );
        }

        @Test
        void shouldEmitDividendReceivedEvent() {
              // Arrange
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),
                Instant.now()
            );
            holding.markEventsAsCommitted();

            // Act
            Money dividendAmount = cadMoney("250.00");
            Instant receivedAt = Instant.now();
            holding.recordDividendReceived(dividendAmount, receivedAt);

            // Assert
            assertTrue(
                holding.hasUncommittedEvents(),
                "Should have uncommitted events after recording dividend"
            );
            
            assertEquals(
                1, 
                holding.getUncommittedEvents().size(),
                "Should have exactly one event"
            );
            
            assertTrue(
                holding.getUncommittedEvents().get(0) instanceof DividendReceivedEvent,
                "Event should be DividendReceivedEvent"
            );
            
            DividendReceivedEvent event = (DividendReceivedEvent) holding.getUncommittedEvents().get(0);
            assertEquals(testPortfolioId, event.portfolioId());
            assertEquals(testHoldingId, event.assetHoldingId());
            assertEquals(dividendAmount, event.dividendAmount());
            assertEquals(receivedAt, event.transactionDate());
        }

        @Test
        void shouldUpdateLastTransactionAtTimestamp() {
             // Arrange
            Instant initialTime = Instant.parse("2024-01-01T10:00:00Z");
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),
                initialTime
            );

            // Act
            Instant dividendTime = Instant.parse("2024-03-15T09:00:00Z");
            holding.recordDividendReceived(cadMoney("250.00"), dividendTime);
            
            // Assert
            assertEquals(
                dividendTime, 
                holding.getLastTransactionAt(),
                "lastTransactionAt should be updated to dividend received time"
                );
            }
            
            @Test
            void shouldFailToRecordDividendOnEmptyPosition() {
                Instant initialTime = Instant.parse("2024-01-01T10:00:00Z");
                AssetHolding holding = AssetHolding.createInitialHolding(
                    testPortfolioId,
                    testHoldingId,
                    testAssetIdentifier,
                    testAssetType,
                    quantity("100"),
                    priceCAD("50.00"),
                    initialTime
                );
                holding.decreasePosition(quantity("100"), priceCAD("50"), initialTime);
                
                Instant dividendTime = Instant.parse("2024-03-15T09:00:00Z");
                assertThrows(InvalidHoldingOperationException.class, () -> holding.recordDividendReceived(cadMoney("250.00"), dividendTime));
        }

        @Test
        void shouldFailWhenDividendAmountIsNegative() {
            Instant initialTime = Instant.parse("2024-01-01T10:00:00Z");
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),
                initialTime
            ); 
            Instant dividendTime = Instant.parse("2024-03-15T09:00:00Z");


            assertThrows(InvalidDividendAmountException.class, () -> holding.recordDividendReceived(cadMoney("-40"), dividendTime));
        }

        @Test
        void shouldFailWhenCurrencyDoesNotMatch() {
            Instant initialTime = Instant.parse("2024-01-01T10:00:00Z");
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),
                initialTime
            ); 
            Instant dividendTime = Instant.parse("2024-03-15T09:00:00Z");


            assertThrows(CurrencyMismatchException.class, () -> holding.recordDividendReceived(usdMoney("40"), dividendTime));

        }

        @Test
        void shouldFailWhenAnyParameterIsNull() {
            Instant initialTime = Instant.parse("2024-01-01T10:00:00Z");
            AssetHolding holding = AssetHolding.createInitialHolding(
                testPortfolioId,
                testHoldingId,
                testAssetIdentifier,
                testAssetType,
                quantity("100"),
                priceCAD("50.00"),
                initialTime
            ); 
            Instant dividendTime = Instant.parse("2024-03-15T09:00:00Z");


            assertThrows(NullPointerException.class, () -> holding.recordDividendReceived(null, dividendTime));
            assertThrows(NullPointerException.class, () -> holding.recordDividendReceived(cadMoney("40"), null));

        }

    }

    @Nested
    public class DividendReinvestmentTests {
        private AssetHolding holding;

        @BeforeEach
        void init() {
            holding = AssetHolding.createInitialHolding(
                testPortfolioId, 
                testHoldingId, 
                testAssetIdentifier, 
                testAssetType, 
                quantity("100"), 
                priceCAD("20"),
                Instant.now()
            );
        }
        @Test
        void shouldIncreaseACBWhenReinvestingDividends() {
            Money initialTotalACB = holding.getTotalACB();
            assertThat(initialTotalACB).isEqualTo(Money.of(2000, "CAD"));

            Money dividendAmount = Money.of(100, "CAD");
            BigDecimal sharesReceived = BigDecimal.valueOf(5);
            Money pricePerShare = Money.of(20, "CAD");

            holding.processDividendReinvestment(dividendAmount, quantity(sharesReceived.toString()), new Price(pricePerShare), Instant.now());

            Money expectedTotalACB = Money.of(2100, "CAD");
            assertThat(holding.getTotalACB()).isEqualTo(expectedTotalACB);
        }

        @Test
        void shouldIncreaseQuantityBySharesReceived() {
            // Given: Initial 100 shares
            Quantity initialQuantity = holding.getTotalQuantity();
            assertEquals(initialQuantity, quantity("100"));
            
            // When: Reinvesting and receiving 5 shares
            holding.processDividendReinvestment(
                Money.of(100, "CAD"),
                quantity("5"),
                priceCAD("20"),
                Instant.now()
            );
            
            // Then: Quantity should be 105 shares
            assertEquals(holding.getTotalQuantity(), quantity("105"));
        }

        @Test
        void shouldCalculateNewAverageACBCorrectly() {
            Quantity initialQuantity = holding.getTotalQuantity();
            assertEquals(initialQuantity, quantity("100"));

            Money initialCostBasis = holding.getTotalCostBasis();
            Money expectedInitialACB = Money.of(2000, "CAD");
            assertEquals(expectedInitialACB, initialCostBasis);
            
            holding.processDividendReinvestment(
                Money.of(100, "CAD"),
                quantity("5"),
                priceCAD("25"),
                Instant.now()
            );  
                // Assert - Verify new ACB calculation
            // Old cost basis: $2,000 (100 shares × $20)
            // New investment: $125 (5 shares × $25)
            // Total cost basis: $2,125
            // Total shares: 105
            // New average ACB per share: $2,125 ÷ 105 = $20.238095...
            
            Quantity expectedNewQuantity = quantity("105");
            Money expectedNewCostBasis = Money.of(2125, "CAD");
            Money expectedNewACBPerShare = Money.of(20.238095, "CAD");
            
            assertEquals(expectedNewQuantity, holding.getTotalQuantity());
            assertEquals(expectedNewCostBasis, holding.getTotalCostBasis());
            
            // Calculate ACB per share
            Money actualACBPerShare = holding.getACBPerShare();
            assertEquals(expectedNewACBPerShare, actualACBPerShare);       
        }

        @Test
        void shouldEmitDividendReinvestedEvent() {
            // Arrange
            Money dividendAmount = Money.of(100, "CAD");
            Quantity sharesReceived = quantity("5");
            Price pricePerShare = priceCAD("20");
            Instant timestamp = Instant.now();
            
            // Act
            holding.processDividendReinvestment(
                dividendAmount,
                sharesReceived,
                pricePerShare,
                timestamp
            );
            
            // Assert
            List<DomainEvent> events = holding.getDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(DividendReinvestedEvent.class);
            
            DividendReinvestedEvent event = (DividendReinvestedEvent) events.get(0);
            assertThat(event.assetHoldingId()).isEqualTo(holding.getAssetHoldingId());
            assertThat(event.dividendAmount()).isEqualTo(dividendAmount);
            assertThat(event.sharesReceived()).isEqualTo(sharesReceived);
            assertThat(event.pricePerShare()).isEqualTo(pricePerShare);
            assertThat(event.reinvestmentDate()).isEqualTo(timestamp);
        }

        @Test
        void shouldUpdateLastTransactionAtTimestamp() {
            // Arrange
            Instant initialTimestamp = holding.getLastTransactionAt();
            Instant newTimestamp = Instant.now().plus(Duration.ofDays(1));
            
            // Act
            holding.processDividendReinvestment(
                Money.of(100, "CAD"),
                quantity("5"),
                priceCAD("20"),
                newTimestamp
            );
            
            // Assert
            assertThat(holding.getLastTransactionAt())
                .isEqualTo(newTimestamp)
                .isAfter(initialTimestamp);
        }

        @Test
        void shouldFailWhenSharesReceivedIsZero() {
            // Arrange
            Quantity zeroShares = quantity("0");
            
            // Act & Assert
            assertThatThrownBy(() -> 
                holding.processDividendReinvestment(
                    Money.of(100, "CAD"),
                    zeroShares,
                    priceCAD("20"),
                    Instant.now()
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Shares received must be positive")
                .hasMessageContaining("zero");
        }

        @Test
        void shouldFailWhenSharesReceivedIsNegative() {
            // Arrange
            Quantity negativeShares = quantity("-5");
            
            // Act & Assert
            assertThatThrownBy(() -> 
                holding.processDividendReinvestment(
                    Money.of(100, "CAD"),
                    negativeShares,
                    priceCAD("20"),
                    Instant.now()
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Shares received must be positive")
                .hasMessageContaining("negative");
        }

        @Test
        void shouldFailWhenDividendAmountIsNegative() {
            // Arrange
            Money negativeDividend = Money.of(-100, "CAD");
            
            // Act & Assert
            assertThatThrownBy(() -> 
                holding.processDividendReinvestment(
                    negativeDividend,
                    quantity("5"),
                    priceCAD("20"),
                    Instant.now()
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dividend amount cannot be negative");
        }

        @Test
        void shouldFailWhenPricePerShareIsNegative() {
            // Arrange
            Price negativePrice = priceCAD("-20");
            
            // Act & Assert
            assertThatThrownBy(() -> 
                holding.processDividendReinvestment(
                    Money.of(100, "CAD"),
                    quantity("5"),
                    negativePrice,
                    Instant.now()
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Price per share cannot be negative");
        }

        @Test
        void shouldFailWhenCurrencyDoesNotMatch() {
            // Arrange - Holding is in CAD, but dividend is in USD
            Money dividendInUSD = Money.of(100, "USD");
            Price priceInCAD = priceCAD("20");
            
            // Act & Assert
            assertThatThrownBy(() -> 
                holding.processDividendReinvestment(
                    dividendInUSD,
                    quantity("5"),
                    priceInCAD,
                    Instant.now()
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch")
                .hasMessageContaining("USD")
                .hasMessageContaining("CAD");
        }

        @Test
        void shouldFailWhenAnyParameterIsNull() {
            // Test null dividend amount
            assertThatThrownBy(() -> 
                holding.processDividendReinvestment(
                    null,
                    quantity("5"),
                    priceCAD("20"),
                    Instant.now()
                ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Dividend amount cannot be null");
            
            // Test null shares received
            assertThatThrownBy(() -> 
                holding.processDividendReinvestment(
                    Money.of(100, "CAD"),
                    null,
                    priceCAD("20"),
                    Instant.now()
                ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Shares received cannot be null");
            
            // Test null price per share
            assertThatThrownBy(() -> 
                holding.processDividendReinvestment(
                    Money.of(100, "CAD"),
                    quantity("5"),
                    null,
                    Instant.now()
                ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Price per share cannot be null");
            
            // Test null timestamp
            assertThatThrownBy(() -> 
                holding.processDividendReinvestment(
                    Money.of(100, "CAD"),
                    quantity("5"),
                    priceCAD("20"),
                    null
                ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Timestamp cannot be null");
        }

    }

    @Nested
    public class ReturnOfCapitalTests {
        @Test
        void shouldReduceTotalACBByROCAmount() {
        }

        @Test
        void shouldReduceAverageACBPerShare() {
        }

        @Test
        void shouldNotChangeQuantity() {
        }

        @Test
        void shouldCreateCapitalGainWhenROCExceedsACB() {
        }

        @Test
        void shouldSetACBToZeroWhenROCExceedsTotalACB() {
        }

        @Test
        void shouldEmitReturnOfCapitalProcessedEvent() {
        }

        @Test
        void shouldEmitEventWithExcessROCValueWhenApplicable() {
        }

        @Test
        void shouldUpdateLastTransactionAtTimestamp() {
        }

        @Test
        void shouldFailWhenROCAmountIsNegative() {
        }

        @Test
        void shouldFailWhenProcessingROCOnEmptyPosition() {
        }

        @Test
        void shouldFailWhenCurrencyDoesNotMatch() {
        }

        @Test
        void shouldFailWhenAnyParameterIsNull() {
        }

    }

    @Nested
    public class StockSplitTests {
        @Test
        void shouldMultiplyQuantityBySplitRatio() {
        }

        @Test
        void shouldDivideAverageACBBySplitRatio() {
        }

        @Test
        void shouldKeepTotalACBUUnchanged() {
        }

        @Test
        void shouldHandleReverseSplitsCorrectly() {
        }

        @Test
        void shouldHandleFractionalSplitsCorrectly() {
        }

        @Test
        void shouldEmitStockSplitProcessedEvent() {
        }

        @Test
        void shouldUpdateLastTransactionAtTimestamp() {
        }

        @Test
        void shouldFailWhenSplitRatioIsZero() {
        }

        @Test
        void shouldFailWhenSplitRatioIsNegative() {
        }

        @Test
        void shouldFailWhenSplittingEmptyPosition() {
        }

        @Test
        void shouldFailWhenAnyParameterIsNull() {
        }

    }

    @Nested
    public class QueryMethods {

        @Nested
        public class QueryMethodsForMaketValueTests {
            @Test
            void shouldCalculateCurrentMarketValueCorrectly() {
            }

            @Test
            void shouldReturnZeroMarketValueForEmptyPosition() {
            }

            @Test
            void shouldFailWhenCurrentPriceCurrencyDoesNotMatch() {
            }

            @Test
            void shouldFailWhenCurrentPriceIsNull() {
            }
        }

        @Nested
        public class QueryMethodsForCapitalGainLossTests {
            @Test
            void shouldCalculateUnrealizedGainCorrectly() {
            }

            @Test
            void shouldCalculateUnrealizedLossCorrectly() {
            }

            @Test
            void shouldReturnZeroUnrealizedGainLossWhenPriceEqualsACB() {
            }

            @Test
            void shouldCalculateUnrealizedGainLossPercentageCorrectly() {
            }

            @Test
            void shouldReturnZeroPercentWhenTotalACBIsZero() {
            }

            @Test
            void shouldFailWhenCurrentPriceCurrencyDoesNotMatch() {
            }

        }

        @Nested
        public class QueryMethodsFOrHypotheticalCaptialGainLossTests {
            @Test
            void shouldCalculateHypotheticalGainWithoutChangingState() {
            }

            @Test
            void shouldCalculateHypotheticalLossWithoutChangingState() {
            }

            @Test
            void shouldFailWhenQuantityExceedsAvailableShares() {
            }

            @Test
            void shouldFailWhenSalePriceCurrencyDoesNotMatch() {
            }

            @Test
            void shouldNotEmitAnyEventsFromQueryMethod() {
            }
        }

        @Nested
        public class QueryMethodsForACBTests {
            @Test
            void shouldReturnCorrectAverageACBPerShare() {
            }

            @Test
            void shouldReturnCorrectTotalACB() {
            }

            @Test
            void shouldReturnCorrectACBForSpecificQuantity() {
            }

            @Test
            void shouldFailWhenRequestedQuantityExceedsHolding() {
            }
        }

        @Nested
        public class QueryMethodsForStatusChecksTests {
            @Test
            void shouldReturnTrueForIsEmptyWhenQuantityIsZero() {
            }

            @Test
            void shouldReturnFalseForIsEmptyWhenQuantityIsPositive() {
            }

            @Test
            void shouldReturnTrueForHasPositionWhenQuantityIsPositive() {
            }

            @Test
            void shouldReturnFalseForHasPositionWhenQuantityIsZero() {
            }

            @Test
            void shouldReturnTrueForCanSellWhenQuantityIsSufficient() {
            }

            @Test
            void shouldReturnFalseForCanSellWhenQuantityIsInsufficient() {
            }

            @Test
            void shouldReturnTrueForShouldBeRemovedWhenEmptyAndACBIsZero() {
            }

            @Test
            void shouldReturnFalseForShouldBeRemovedWhenPositionExists() {
            }

        }

    }

    @Nested
    public class DomainEventTests {
        @Test
        void shouldAccumulateMultipleUncommittedEvents() {
        }

        @Test
        void shouldReturnAllUncommittedEvents() {
        }

        @Test
        void shouldClearEventsWhenMarkedAsCommitted() {
        }

        @Test
        void shouldReturnTrueForHasUncommittedEventsWhenEventsExist() {
        }

        @Test
        void shouldReturnFalseForHasUncommittedEventsAfterMarkingCommitted() {
        }

        @Test
        void shouldNotAllowNullEventsToBeAdded() {
        }

    }

    @Nested
    public class VersioningAndTimestampTests {
        @Test
        void shouldIncrementVersionOnEveryStateChange() {
        }

        @Test
        void shouldNotIncrementVersionOnQueryMethods() {
        }

        @Test
        void shouldUpdateUpdatedAtTimestampOnStateChanges() {
        }

        @Test
        void shouldNotUpdateUpdatedAtOnQueryMethods() {
        }

        @Test
        void shouldUpdateLastTransactionAtOnTransactionOperations() {
        }

    }

    @Nested
    public class ReconstructionFromPersistenceTests {
        @Test
        void shouldReconstructHoldingWithAllFieldsCorrectly() {
        }

        @Test
        void shouldReconstructWithCorrectVersionNumber() {
        }

        @Test
        void shouldReconstructWithCorrectTimestamps() {
        }

        @Test
        void shouldNotEmitEventsDuringReconstruction() {
        }

    }

    @Nested
    public class CurrencyValidationTests {
        @Test
        void shouldEnforceSingleBaseCurrencyAcrossAllOperations() {
        }

        @Test
        void shouldRejectMismatchedCurrencyInIncreasePosition() {
        }

        @Test
        void shouldRejectMismatchedCurrencyInDecreasePosition() {
        }

        @Test
        void shouldRejectMismatchedCurrencyInDividends() {
        }

        @Test
        void shouldRejectMismatchedCurrencyInROC() {
        }

        @Test
        void shouldRejectMismatchedCurrencyInQueries() {
        }

    }

    @Nested
    public class EdgeCaseTests {
        @Test
        void shouldHandleVerySmallQuantities() {
        }

        @Test
        void shouldHandleVeryLargeQuantities() {
        }

        @Test
        void shouldHandleVerySmallPrices() {
        }

        @Test
        void shouldHandleVeryLargePrices() {
        }

        @Test
        void shouldHandleRoundingCorrectlyInACBCalculations() {
        }

        @Test
        void shouldHandleSellingEntirePositionAfterMultipleBuys() {
        }

        @Test
        void shouldHandleROCThatExactlyEqualsACB() {
        }

    }

}
