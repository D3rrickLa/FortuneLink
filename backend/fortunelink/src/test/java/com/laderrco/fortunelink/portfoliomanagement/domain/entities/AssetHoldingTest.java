package com.laderrco.fortunelink.portfoliomanagement.domain.entities;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InsufficientHoldingException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidHoldingOperationException;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.PortfolioId;

public class AssetHoldingTest {
    private AssetHoldingId assetId;
    private PortfolioId portfolioId;
    private AssetIdentifier assetIdentifier;
    private Instant createdAt;

    private BigDecimal totalQuantity;
    private Money pricePerUnit;
    private Money averageCostBasis;
    private Money totalCostBasis;

    private Currency USD = Currency.getInstance("USD");

    private AssetHolding testHolding;

    @BeforeEach
    void init() {
        assetId = AssetHoldingId.createRandom();
        portfolioId = PortfolioId.createRandom();
        assetIdentifier = new AssetIdentifier(AssetType.STOCK, "US0378331005", Map.of("CUSIP", "037833100"), "Apple", "NASDAQ", USD.toString());
        createdAt = Instant.now();
        totalQuantity = BigDecimal.valueOf(100);
        pricePerUnit = Money.of(215.57, USD);
        totalCostBasis = pricePerUnit.multiply(totalQuantity);
        averageCostBasis = totalCostBasis.divide(totalQuantity);
          
        testHolding = AssetHolding.createInitialHolding(
            portfolioId, 
            assetId, 
            assetIdentifier, 
            totalQuantity, 
            pricePerUnit, 
            createdAt
        );
    }

    @Test
    void constructorBuilderIsValid() {
        AssetHolding holding = new AssetHolding.Builder()
        .portfolioId(portfolioId)
        .assetHoldingId(assetId)
        .assetIdentifier(assetIdentifier)
        .totalQuantity(totalQuantity)
        .averageCostBasis(averageCostBasis)
        .totalCostBasis(totalCostBasis)
        .createdAt(createdAt)
        .updatedAt(createdAt)
        .lastTransactionAt(createdAt)
        .version(1)
        .build();

        assertAll(
            () -> assertEquals(portfolioId, holding.getPortfolioId()),
            () -> assertEquals(assetId, holding.getAssetId()),
            () -> assertEquals(assetIdentifier, holding.getAssetIdentifier()),
            () -> assertEquals(totalQuantity, holding.getTotalQuantity()),
            () -> assertEquals(averageCostBasis, holding.getAverageCostBasis()),
            () -> assertEquals(totalCostBasis, holding.getTotalCostBasis()),
            () -> assertEquals(createdAt, holding.getCreatedAt()),
            () -> assertEquals(createdAt, holding.getUpdatedAt()),
            () -> assertEquals(createdAt, holding.getLastTransactionAt()),
            () -> assertEquals(1, holding.getVersion())
        );
    }

    @Test
    void createInitialHoldingFactoryIsValid() {
        AssetHolding holding = AssetHolding.createInitialHolding(
            portfolioId, 
            assetId, 
            assetIdentifier, 
            totalQuantity, 
            pricePerUnit, 
            createdAt
        );
        assertAll(
            () -> assertEquals(portfolioId, holding.getPortfolioId()),
            () -> assertEquals(assetId, holding.getAssetId()),
            () -> assertEquals(assetIdentifier, holding.getAssetIdentifier()),
            () -> assertEquals(totalQuantity, holding.getTotalQuantity()),
            () -> assertEquals(averageCostBasis, holding.getAverageCostBasis()),
            () -> assertEquals(totalCostBasis, holding.getTotalCostBasis()),
            () -> assertEquals(createdAt.truncatedTo(ChronoUnit.SECONDS), holding.getCreatedAt().truncatedTo(ChronoUnit.SECONDS)),
            () -> assertEquals(createdAt.truncatedTo(ChronoUnit.SECONDS), holding.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS)),
            () -> assertEquals(createdAt.truncatedTo(ChronoUnit.SECONDS), holding.getLastTransactionAt().truncatedTo(ChronoUnit.SECONDS)),
            () -> assertEquals(0, holding.getVersion())
        );
    }

    @Test
    void reconstructFactoryMethodIsValid() {
        AssetHolding holding = AssetHolding.reconstruct(
            portfolioId, 
            assetId, 
            assetIdentifier, 
            totalQuantity, 
            averageCostBasis, 
            totalCostBasis, 
            createdAt,
            0, 
            createdAt, 
            createdAt.plusSeconds(3600)
        );
        assertAll(
            () -> assertEquals(portfolioId, holding.getPortfolioId()),
            () -> assertEquals(assetId, holding.getAssetId()),
            () -> assertEquals(assetIdentifier, holding.getAssetIdentifier()),
            () -> assertEquals(totalQuantity, holding.getTotalQuantity()),
            () -> assertEquals(averageCostBasis, holding.getAverageCostBasis()),
            () -> assertEquals(totalCostBasis, holding.getTotalCostBasis()),
            () -> assertEquals(createdAt.truncatedTo(ChronoUnit.SECONDS), holding.getCreatedAt().truncatedTo(ChronoUnit.SECONDS)),
            () -> assertEquals(createdAt.truncatedTo(ChronoUnit.SECONDS).plusSeconds(3600), holding.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS)),
            () -> assertEquals(createdAt.truncatedTo(ChronoUnit.SECONDS), holding.getLastTransactionAt().truncatedTo(ChronoUnit.SECONDS)),
            () -> assertEquals(0, holding.getVersion())
        );

    }

    @Test
    void increasePositionIsValid() {
        BigDecimal quantityToAdd = BigDecimal.valueOf(35);
        Money pricePerUnit = Money.of(220.57, USD);
        Instant transactionDate = Instant.now();

        assertDoesNotThrow(() -> testHolding.increasePosition(quantityToAdd, pricePerUnit, transactionDate));
        assertEquals(1, testHolding.getUncommittedEvents().size());
        assertTrue(testHolding.getCreatedAt().isBefore(transactionDate));
    }

    @Test
    void increasePositionThrowsInvalidHoldingOperationExceptionWhenQuantityIsNegative() {
        BigDecimal quantityToAdd = BigDecimal.valueOf(-35);
        Money pricePerUnit = Money.of(220.57, USD);
        Instant transactionDate = Instant.now(); 

        assertThrows(InvalidHoldingOperationException.class, () -> testHolding.increasePosition(quantityToAdd, pricePerUnit, transactionDate));
    }

    @Test
    void increasePositionThrowsInvalidHoldingOperationExceptionWhenQuantityIsZero() {
        BigDecimal quantityToAdd = BigDecimal.ZERO;
        Money pricePerUnit = Money.of(220.57, USD);
        Instant transactionDate = Instant.now(); 

        assertThrows(InvalidHoldingOperationException.class, () -> testHolding.increasePosition(quantityToAdd, pricePerUnit, transactionDate));
    }

    @Test
    void increasePositionThrowsInvalidHoldingOperationExceptionWhenPriceIsNegative() {
        BigDecimal quantityToAdd = BigDecimal.valueOf(35);
        Money pricePerUnit = Money.of(-220.57, USD);
        Instant transactionDate = Instant.now(); 

        assertThrows(InvalidHoldingOperationException.class, () -> testHolding.increasePosition(quantityToAdd, pricePerUnit, transactionDate));
    }

    @Test
    void increasePositionThrowsInvalidHoldingOperationExceptionWhenPriceIsZero() {
        BigDecimal quantityToAdd = BigDecimal.valueOf(35);
        Money pricePerUnit = Money.of(0, USD);
        Instant transactionDate = Instant.now(); 

        assertThrows(InvalidHoldingOperationException.class, () -> testHolding.increasePosition(quantityToAdd, pricePerUnit, transactionDate));
    }

    @Test
    void increasePositionThrowsInvalidHoldingOperationExceptionWhenTransactionDateIsNull() {
        BigDecimal quantityToAdd = BigDecimal.valueOf(35);
        Money pricePerUnit = Money.of(220.57, USD);

        assertThrows(NullPointerException.class, () -> testHolding.increasePosition(quantityToAdd, pricePerUnit, null));
    }

    @Test
    void increasePositionUpdatesQuantityAndCostBasisCorrectly() {
        BigDecimal quantityToAdd = BigDecimal.valueOf(50);
        Money newPricePerUnit = Money.of(200.00, USD);
        Instant transactionDate = Instant.now();

        BigDecimal originalQuantity = testHolding.getTotalQuantity();
        Money originalTotalCost = testHolding.getTotalCostBasis();
        
        testHolding.increasePosition(quantityToAdd, newPricePerUnit, transactionDate);

        BigDecimal expectedTotalQuantity = originalQuantity.add(quantityToAdd);
        Money expectedNewTotalCost = originalTotalCost.add(newPricePerUnit.multiply(quantityToAdd));
        Money expectedNewAverageCost = expectedNewTotalCost.divide(expectedTotalQuantity);

        assertAll(
            () -> assertEquals(expectedTotalQuantity, testHolding.getTotalQuantity()),
            () -> assertEquals(expectedNewTotalCost, testHolding.getTotalCostBasis()),
            () -> assertEquals(expectedNewAverageCost, testHolding.getAverageCostBasis()),
            () -> assertEquals(transactionDate.truncatedTo(ChronoUnit.SECONDS), testHolding.getLastTransactionAt().truncatedTo(ChronoUnit.SECONDS)),
            () -> assertEquals(transactionDate.truncatedTo(ChronoUnit.SECONDS), testHolding.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS))
        );
    }

    @Test
    void decreasePositionIsValid() {
        BigDecimal quantityToSell = BigDecimal.valueOf(25);
        Money sellPrice = Money.of(230.00, USD);
        Instant transactionDate = Instant.now();

        assertDoesNotThrow(() -> testHolding.decreasePosition(quantityToSell, sellPrice, transactionDate));
        assertEquals(1, testHolding.getUncommittedEvents().size());
    }

    @Test
    void decreasePositionThrowsInvalidHoldingOperationExceptionWhenQuantityIsNegative() {
        BigDecimal quantityToSell = BigDecimal.valueOf(-25);
        Money sellPrice = Money.of(230.00, USD);
        Instant transactionDate = Instant.now();

        assertThrows(InvalidHoldingOperationException.class, () -> testHolding.decreasePosition(quantityToSell, sellPrice, transactionDate));
    }

    @Test
    void decreasePositionThrowsInvalidHoldingOperationExceptionWhenQuantityIsZero() {
        BigDecimal quantityToSell = BigDecimal.ZERO;
        Money sellPrice = Money.of(230.00, USD);
        Instant transactionDate = Instant.now();

        assertThrows(InvalidHoldingOperationException.class, () -> testHolding.decreasePosition(quantityToSell, sellPrice, transactionDate));
    }

    @Test
    void decreasePositionThrowsInsufficientHoldingException() {
        BigDecimal quantityToSell = BigDecimal.valueOf(150); // More than current holding (100)
        Money sellPrice = Money.of(230.00, USD);
        Instant transactionDate = Instant.now();

        assertThrows(InsufficientHoldingException.class, () -> testHolding.decreasePosition(quantityToSell, sellPrice, transactionDate));
    }

    @Test
    void decreasePositionThrowsInvalidHoldingOperationExceptionWhenPriceIsNegative() {
        BigDecimal quantityToSell = BigDecimal.valueOf(25);
        Money sellPrice = Money.of(-230.00, USD);
        Instant transactionDate = Instant.now();

        assertThrows(InvalidHoldingOperationException.class, () -> testHolding.decreasePosition(quantityToSell, sellPrice, transactionDate));
    }

    @Test
    void decreasePositionThrowsInvalidHoldingOperationExceptionWhenTransactionDateIsNull() {
        BigDecimal quantityToSell = BigDecimal.valueOf(25);
        Money sellPrice = Money.of(230.00, USD);

        assertThrows(NullPointerException.class, () -> testHolding.decreasePosition(quantityToSell, sellPrice, null));
    }

    @Test
    void decreasePositionUpdatesQuantityAndCostBasisCorrectly() {
        BigDecimal quantityToSell = BigDecimal.valueOf(25);
        Money sellPrice = Money.of(230.00, USD);
        Instant transactionDate = Instant.now();

        BigDecimal originalQuantity = testHolding.getTotalQuantity();
        Money originalAverageCost = testHolding.getAverageCostBasis();
        
        testHolding.decreasePosition(quantityToSell, sellPrice, transactionDate);

        BigDecimal expectedQuantity = originalQuantity.subtract(quantityToSell);
        Money expectedTotalCost = originalAverageCost.multiply(expectedQuantity);

        assertAll(
            () -> assertEquals(expectedQuantity, testHolding.getTotalQuantity()),
            () -> assertEquals(expectedTotalCost, testHolding.getTotalCostBasis()),
            () -> assertEquals(originalAverageCost, testHolding.getAverageCostBasis()), // Average cost basis should remain the same
            () -> assertEquals(transactionDate.truncatedTo(ChronoUnit.SECONDS), testHolding.getLastTransactionAt().truncatedTo(ChronoUnit.SECONDS)),
            () -> assertEquals(transactionDate.truncatedTo(ChronoUnit.SECONDS), testHolding.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS))
        );
    }

    @Test
    void decreasePositionToZeroMakesHoldingEmpty() {
        BigDecimal quantityToSell = testHolding.getTotalQuantity(); // Sell all
        Money sellPrice = Money.of(230.00, USD);
        Instant transactionDate = Instant.now();

        testHolding.decreasePosition(quantityToSell, sellPrice, transactionDate);

        assertAll(
            () -> assertEquals(BigDecimal.ZERO, testHolding.getTotalQuantity()),
            () -> assertEquals(Money.of(0, USD), testHolding.getTotalCostBasis()),
            () -> assertTrue(testHolding.isEmpty())
        );
    }

    @Test
    void recordDividendIsValid() {
        Money dividendAmount = Money.of(150.00, USD);
        Instant paymentDate = Instant.now();

        assertDoesNotThrow(() -> testHolding.recordDividend(dividendAmount, paymentDate));
        assertEquals(1, testHolding.getUncommittedEvents().size());
    }

    @Test
    void recordDividendThrowsInvalidHoldingOperationExceptionWhenTotalIsZero() {
         // Start with your existing testHolding and sell all shares
        testHolding.decreasePosition(testHolding.getTotalQuantity(), Money.of(230.00, USD), Instant.now());
        
        // Now try to record dividend on zero quantity holding
        Money dividendAmount = Money.of(150.00, USD);
        Instant paymentDate = Instant.now();
        
        assertThrows(InvalidHoldingOperationException.class, 
            () -> testHolding.recordDividend(dividendAmount, paymentDate));
    
    }

    @Test
    void recordDividendThrowsInvalidHoldingOperationExceptionWhenAmountIsNegative() {
        Money dividendAmount = Money.of(-150.00, USD);
        Instant paymentDate = Instant.now();

        assertThrows(InvalidHoldingOperationException.class, () -> testHolding.recordDividend(dividendAmount, paymentDate));
    }

    @Test
    void recordDividendThrowsInvalidHoldingOperationExceptionWhenAmountIsZero() {
        Money dividendAmount = Money.of(0, USD);
        Instant paymentDate = Instant.now();

        assertThrows(InvalidHoldingOperationException.class, () -> testHolding.recordDividend(dividendAmount, paymentDate));
    }

    @Test
    void recordDividendThrowsInvalidHoldingOperationExceptionWhenPaymentDateIsNull() {
        Money dividendAmount = Money.of(150.00, USD);

        assertThrows(NullPointerException.class, () -> testHolding.recordDividend(dividendAmount, null));
    }

    @Test
    void recordDividendUpdatesLastTransactionDate() {
        Money dividendAmount = Money.of(150.00, USD);
        Instant paymentDate = Instant.now();
        
        testHolding.recordDividend(dividendAmount, paymentDate);

        assertEquals(paymentDate.truncatedTo(ChronoUnit.SECONDS), testHolding.getLastTransactionAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void getCurrentMarketValueWithValidMarketPrice() {
        Money marketPrice = Money.of(250.00, USD);
        Money expectedValue = marketPrice.multiply(testHolding.getTotalQuantity());

        Money currentValue = testHolding.getCurrentMarketValue(marketPrice);

        assertEquals(expectedValue, currentValue);
    }

    @Test
    void getCurrentMarketValueThrowsExceptionWhenMarketPriceIsNegative() {
        Money marketPrice = Money.of(-250.00, USD);

        assertThrows(InvalidHoldingOperationException.class, () -> testHolding.getCurrentMarketValue(marketPrice));
    }

    @Test
    void getCurrentMarketValueThrowsExceptionWhenMarketPriceIsNull() {
        assertThrows(NullPointerException.class, () -> testHolding.getCurrentMarketValue(null));
    }

    @Test
    void getUnrealizedGainLossWithGain() {
        Money marketPrice = Money.of(250.00, USD); // Higher than average cost
        Money currentValue = testHolding.getCurrentMarketValue(marketPrice);
        Money expectedGainLoss = currentValue.subtract(testHolding.getTotalCostBasis());

        Money gainLoss = testHolding.getUnrealizedGainLoss(marketPrice);

        assertTrue(gainLoss.isPositive());
        assertEquals(expectedGainLoss, gainLoss);
    }

    @Test
    void getUnrealizedGainLossWithLoss() {
        Money marketPrice = Money.of(180.00, USD); // Lower than average cost
        Money currentValue = testHolding.getCurrentMarketValue(marketPrice);
        Money expectedGainLoss = currentValue.subtract(testHolding.getTotalCostBasis());

        Money gainLoss = testHolding.getUnrealizedGainLoss(marketPrice);

        assertTrue(gainLoss.isNegative());
        assertEquals(expectedGainLoss, gainLoss);
    }

    @Test
    void getUnrealizedGainLossThrowsExceptionWhenMarketPriceIsNull() {
        assertThrows(NullPointerException.class, () -> testHolding.getUnrealizedGainLoss(null));
    }

    @Test
    void isEmptyReturnsFalseForNonZeroHolding() {
        assertFalse(testHolding.isEmpty());
    }

    @Test
    void isEmptyReturnsTrueAfterSellingAllShares() {
        testHolding.decreasePosition(testHolding.getTotalQuantity(), Money.of(230.00, USD), Instant.now());
        
        assertTrue(testHolding.isEmpty());
    }



    @Test
    void multipleTransactionsUpdateVersionCorrectly() {
        int initialVersion = testHolding.getVersion();
        
        testHolding.increasePosition(BigDecimal.valueOf(25), Money.of(220.00, USD), Instant.now());
        testHolding.recordDividend(Money.of(100.00, USD), Instant.now());
        testHolding.decreasePosition(BigDecimal.valueOf(10), Money.of(230.00, USD), Instant.now());

        assertEquals(3, testHolding.getUncommittedEvents().size());
        // Version should be incremented with each transaction
        assertTrue(testHolding.getVersion() > initialVersion);
    }

    @Test
    void getUncommittedEventsReturnsCorrectCount() {
        assertEquals(0, testHolding.getUncommittedEvents().size());
        
        testHolding.increasePosition(BigDecimal.valueOf(25), Money.of(220.00, USD), Instant.now());
        assertEquals(1, testHolding.getUncommittedEvents().size());
        
        testHolding.recordDividend(Money.of(100.00, USD), Instant.now());
        assertEquals(2, testHolding.getUncommittedEvents().size());
    }

    @Test
    void markEventsAsCommittedClearsUncommittedEvents() {
        testHolding.increasePosition(BigDecimal.valueOf(25), Money.of(220.00, USD), Instant.now());
        testHolding.recordDividend(Money.of(100.00, USD), Instant.now());
        
        assertEquals(2, testHolding.getUncommittedEvents().size());
        
        testHolding.markEventsAsCommitted();
        
        assertEquals(0, testHolding.getUncommittedEvents().size());
    }

    @Test
    void builderThrowsExceptionWhenRequiredFieldsAreMissing() {
        assertThrows(NullPointerException.class, () -> new AssetHolding.Builder().build());
        
        assertThrows(NullPointerException.class, () -> 
            new AssetHolding.Builder()
                .portfolioId(portfolioId)
                .build()
        );
    }

    @Test
    void createInitialHoldingThrowsExceptionWithInvalidInputs() {
        assertThrows(NullPointerException.class, () -> 
            AssetHolding.createInitialHolding(null, assetId, assetIdentifier, totalQuantity, pricePerUnit, createdAt)
        );
        
        assertThrows(NullPointerException.class, () -> 
            AssetHolding.createInitialHolding(portfolioId, null, assetIdentifier, totalQuantity, pricePerUnit, createdAt)
        );
        
        assertThrows(NullPointerException.class, () -> 
            AssetHolding.createInitialHolding(portfolioId, assetId, null, totalQuantity, pricePerUnit, createdAt)
        );
        
        assertThrows(NullPointerException.class, () -> 
            AssetHolding.createInitialHolding(portfolioId, assetId, assetIdentifier, null, pricePerUnit, createdAt)
        );
        
        assertThrows(NullPointerException.class, () -> 
            AssetHolding.createInitialHolding(portfolioId, assetId, assetIdentifier, totalQuantity, null, createdAt)
        );
        
        assertThrows(NullPointerException.class, () -> 
            AssetHolding.createInitialHolding(portfolioId, assetId, assetIdentifier, totalQuantity, pricePerUnit, null)
        );
    }
}
