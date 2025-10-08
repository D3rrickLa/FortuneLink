package com.laderrco.fortunelink.portfoliomanagement.domain.model.entities;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.events.HoldingIncreasedEvent;
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
            assertEquals(String.format("{PortfolioId: %s, AssetHoldingId: $s}", testPortfolioId, testHoldingId), event.aggregateId());
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
            InvalidHoldingOperationException exception = assertThrows(
                InvalidHoldingOperationException.class,
                () -> holding.increasePosition(
                    quantity("0"),          // Zero quantity
                    price("60.00"),
                    Instant.now()
                ),
                "Should throw exception when quantity is zero"
            );
            
            assertTrue(
                exception.getMessage().contains("positive") || 
                exception.getMessage().contains("zero"),
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
            InvalidHoldingOperationException exception = assertThrows(
                InvalidHoldingOperationException.class,
                () -> holding.increasePosition(
                    quantity("-50"),        // Negative quantity
                    price("60.00"),
                    Instant.now()
                ),
                "Should throw exception when quantity is negative"
            );
            
            assertTrue(
                exception.getMessage().contains("positive"),
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
                    price("60.00"),     // USD currency - mismatch!
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
        }

        @Test
        void shouldCalculateRealizedLossWhenSellingAtALoss() {
        }

        @Test
        void shouldMaintainCorrectACBAfterPartialSale() {
        }

        @Test
        void shouldZeroOutACBWhenFullySellingPosition() {
        }

        // Domain Events
        @Test
        void shouldEmitHoldingDecreasedEventWithCorrectRealizedGainLoss() {
        }

        // Timestamps & Versioning
        @Test
        void shouldUpdateLastTransactionAtTimestampOnDecrease() {
        }

        @Test
        void shouldIncrementVersionNumberOnDecrease() {
        }

        // Validation Failures
        @Test
        void shouldFailWhenTryingToSellMoreThanHeld() {
        }

        @Test
        void shouldFailWhenSellingZeroQuantity() {
        }

        @Test
        void shouldFailWhenSellingNegativeQuantity() {
        }

        @Test
        void shouldFailWhenSalePriceIsNegative() {
        }

        @Test
        void shouldFailWhenCurrencyDoesNotMatchBaseCurrency() {
        }

        @Test
        void shouldFailWhenAnyParameterIsNull() {
        }

    }

    @Nested
    public class MultipleBuysAndSellSequencesACBTests {

        @Test
        void shouldCorrectlyAverageACBAcrossMultiplePurchases() {
        }

        @Test
        void shouldMaintainCorrectACBAfterBuySellBuySequence() {
        }

        @Test
        void shouldMaintainCorrectACBAfterBuyBuySellBuySellSequence() {
        }

        @Test
        void shouldHandleAlternatingBuysAndSellsCorrectly() {
        }

        @Test
        void shouldCalculateCorrectTotalACBAfterComplexSequence() {
        }

    }

    @Nested
    public class DividendOperationTests {
        @Test
        void shouldRecordDividendWithoutAffectingACB() {
        }

        @Test
        void shouldRecordDividendWithoutAffectingQuantity() {
        }

        @Test
        void shouldEmitDividendReceivedEvent() {
        }

        @Test
        void shouldUpdateLastTransactionAtTimestamp() {
        }

        @Test
        void shouldFailToRecordDividendOnEmptyPosition() {
        }

        @Test
        void shouldFailWhenDividendAmountIsNegative() {
        }

        @Test
        void shouldFailWhenCurrencyDoesNotMatch() {
        }

        @Test
        void shouldFailWhenAnyParameterIsNull() {
        }

    }

    @Nested
    public class DividendReinvestmentTests {
        @Test
        void shouldIncreaseACBWhenReinvestingDividends() {
        }

        @Test
        void shouldIncreaseQuantityBySharesReceived() {
        }

        @Test
        void shouldCalculateNewAverageACBCorrectly() {
        }

        @Test
        void shouldEmitDividendReinvestedEvent() {
        }

        @Test
        void shouldUpdateLastTransactionAtTimestamp() {
        }

        @Test
        void shouldFailWhenSharesReceivedIsZero() {
        }

        @Test
        void shouldFailWhenSharesReceivedIsNegative() {
        }

        @Test
        void shouldFailWhenDividendAmountIsNegative() {
        }

        @Test
        void shouldFailWhenPricePerShareIsNegative() {
        }

        @Test
        void shouldFailWhenCurrencyDoesNotMatch() {
        }

        @Test
        void shouldFailWhenAnyParameterIsNull() {
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
