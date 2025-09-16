package com.laderrco.fortunelink.portfoliomanagement.domain.entities;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.DecimalPrecision;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.DomainEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InsufficientHoldingException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidHoldingCostBasisException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidHoldingOperationException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidHoldingQuantityException;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Percentage;
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
    private Currency CAD = Currency.getInstance("CAD");

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

    // CONSTRUCTOR AND FACTORY METHOD TESTS //

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
    void builderThrowsExceptionWhenRequiredFieldsAreMissing() {
        assertThrows(NullPointerException.class, () -> new AssetHolding.Builder().build());
        
        assertThrows(NullPointerException.class, () -> 
            new AssetHolding.Builder()
                .portfolioId(portfolioId)
                .build()
        );
    }

    @Test
    void createInitialHoldingThrowsExceptionWithNullInputs() {
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

    // INCREASE POSITION TESTS //

    @Test
    void increasePositionIsValid() {
        BigDecimal quantityToAdd = BigDecimal.valueOf(35);
        Money pricePerUnit = Money.of(220.57, USD);
        Instant transactionDate = Instant.now().plusSeconds(3600);

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
    void increasePositionThrowsInvalidHoldingOperationExceptionWhenCurrencyMismatch() {
        BigDecimal quantityToAdd = BigDecimal.valueOf(35);
        Money pricePerUnit = Money.of(220.57, CAD); // Different currency
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
            () -> assertEquals(transactionDate.truncatedTo(ChronoUnit.SECONDS), testHolding.getLastTransactionAt().truncatedTo(ChronoUnit.SECONDS))
        );
    }

    // DECREASE POSITION TESTS //

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
            () -> assertEquals(transactionDate.truncatedTo(ChronoUnit.SECONDS), testHolding.getLastTransactionAt().truncatedTo(ChronoUnit.SECONDS))
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

    // DIVIDEND TESTS //

    @Test
    void recordDividendIsValid() {
        Money dividendAmount = Money.of(150.00, USD);
        Instant paymentDate = Instant.now();

        assertDoesNotThrow(() -> testHolding.recordDividend(dividendAmount, paymentDate));
        assertEquals(1, testHolding.getUncommittedEvents().size());
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
    void recordDividendThrowsInvalidHoldingOperationExceptionWhenTotalQuantityIsZero() {
        // Create holding and sell all shares to make quantity zero
        testHolding.decreasePosition(testHolding.getTotalQuantity(), Money.of(230.00, USD), Instant.now());
        
        Money dividendAmount = Money.of(150.00, USD);
        Instant paymentDate = Instant.now();
        
        assertThrows(InvalidHoldingOperationException.class, 
            () -> testHolding.recordDividend(dividendAmount, paymentDate));
    }

    @Test
    void recordDividendUpdatesLastTransactionDate() {
        Money dividendAmount = Money.of(150.00, USD);
        Instant paymentDate = Instant.now();
        
        testHolding.recordDividend(dividendAmount, paymentDate);

        assertEquals(paymentDate.truncatedTo(ChronoUnit.SECONDS), testHolding.getLastTransactionAt().truncatedTo(ChronoUnit.SECONDS));
    }

    // DIVIDEND REINVESTMENT TESTS //

    @Test
    void processDividendReinvestmentIsValid() {
        Money dividendAmount = Money.of(100.00, USD);
        BigDecimal sharesReceived = BigDecimal.valueOf(0.5);
        Money pricePerShare = Money.of(200.00, USD);
        Instant reinvestmentDate = Instant.now();

        assertDoesNotThrow(() -> testHolding.processDividendReinvestment(dividendAmount, sharesReceived, pricePerShare, reinvestmentDate));
        assertEquals(1, testHolding.getUncommittedEvents().size());
    }

    @Test
    void processDividendReinvestmentUpdatesHoldingCorrectly() {
        Money dividendAmount = Money.of(100.00, USD);
        BigDecimal sharesReceived = BigDecimal.valueOf(0.5);
        Money pricePerShare = Money.of(200.00, USD);
        Instant reinvestmentDate = Instant.now();

        BigDecimal originalQuantity = testHolding.getTotalQuantity();
        Money originalTotalCost = testHolding.getTotalCostBasis();

        testHolding.processDividendReinvestment(dividendAmount, sharesReceived, pricePerShare, reinvestmentDate);

        BigDecimal expectedQuantity = originalQuantity.add(sharesReceived);
        Money reinvestmentCost = pricePerShare.multiply(sharesReceived);
        Money expectedTotalCost = originalTotalCost.add(reinvestmentCost);
        Money expectedAverageCost = expectedTotalCost.divide(expectedQuantity);

        assertAll(
            () -> assertEquals(expectedQuantity, testHolding.getTotalQuantity()),
            () -> assertEquals(expectedTotalCost, testHolding.getTotalCostBasis()),
            () -> assertEquals(expectedAverageCost, testHolding.getAverageCostBasis())
        );
    }

    @Test
    void processDividendReinvestmentThrowsExceptionWhenSharesReceivedIsNegative() {
        Money dividendAmount = Money.of(100.00, USD);
        BigDecimal sharesReceived = BigDecimal.valueOf(-0.5);
        Money pricePerShare = Money.of(200.00, USD);
        Instant reinvestmentDate = Instant.now();

        assertThrows(InvalidHoldingOperationException.class, 
            () -> testHolding.processDividendReinvestment(dividendAmount, sharesReceived, pricePerShare, reinvestmentDate));
    }

    // RETURN OF CAPITAL TESTS //

    @Test
    void processReturnOfCapitalIsValid() {
        Money rocAmount = Money.of(500.00, USD);
        Instant effectiveDate = Instant.now();

        assertDoesNotThrow(() -> testHolding.processReturnOfCaptial(rocAmount, effectiveDate));
        assertEquals(1, testHolding.getUncommittedEvents().size());
    }

    @Test
    void processReturnOfCapitalReducesACBCorrectly() {
        Money rocAmount = Money.of(500.00, USD);
        Instant effectiveDate = Instant.now();

        Money originalTotalCost = testHolding.getTotalCostBasis();
        BigDecimal originalQuantity = testHolding.getTotalQuantity();

        testHolding.processReturnOfCaptial(rocAmount, effectiveDate);

        Money expectedTotalCost = originalTotalCost.subtract(rocAmount);
        Money expectedAverageCost = expectedTotalCost.divide(originalQuantity);

        assertAll(
            () -> assertEquals(expectedTotalCost, testHolding.getTotalCostBasis()),
            () -> assertEquals(expectedAverageCost, testHolding.getAverageCostBasis()),
            () -> assertEquals(originalQuantity, testHolding.getTotalQuantity()) // Quantity unchanged
        );
    }

    @Test
    void processReturnOfCapitalWithExcessROCHandledCorrectly() {
        Money rocAmount = Money.of(25000.00, USD); // More than total cost basis
        Instant effectiveDate = Instant.now();

        testHolding.processReturnOfCaptial(rocAmount, effectiveDate);

        assertAll(
            () -> assertEquals(Money.of(0, USD), testHolding.getTotalCostBasis()),
            () -> assertEquals(Money.of(0, USD), testHolding.getAverageCostBasis())
        );
    }

    @Test
    void processReturnOfCapitalThrowsExceptionWhenAmountIsNegative() {
        Money rocAmount = Money.of(-500.00, USD);
        Instant effectiveDate = Instant.now();

        assertThrows(InvalidHoldingOperationException.class, 
            () -> testHolding.processReturnOfCaptial(rocAmount, effectiveDate));
    }

    @Test
    void processReturnOfCapitalThrowsExceptionWhenHoldingIsEmpty() {
        // Create empty holding
        testHolding.decreasePosition(testHolding.getTotalQuantity(), Money.of(230.00, USD), Instant.now());
        
        Money rocAmount = Money.of(500.00, USD);
        Instant effectiveDate = Instant.now();

        assertThrows(InvalidHoldingOperationException.class, 
            () -> testHolding.processReturnOfCaptial(rocAmount, effectiveDate));
    }

    // STOCK SPLIT TESTS //

    @Test
    void processStockSplitACBIsValid() {
        BigDecimal splitRatio = BigDecimal.valueOf(2.0); // 2:1 split
        Instant effectiveDate = Instant.now();

        assertDoesNotThrow(() -> testHolding.processStockSplitACB(splitRatio, effectiveDate));
        assertEquals(1, testHolding.getUncommittedEvents().size());
    }

    @Test
    void processStockSplitACBUpdatesCorrectly() {
        BigDecimal splitRatio = BigDecimal.valueOf(2.0); // 2:1 split
        Instant effectiveDate = Instant.now();

        BigDecimal originalQuantity = testHolding.getTotalQuantity();
        Money originalTotalCost = testHolding.getTotalCostBasis();

        testHolding.processStockSplitACB(splitRatio, effectiveDate);

        BigDecimal expectedQuantity = originalQuantity.multiply(splitRatio);
        Money expectedAverageCost = originalTotalCost.divide(expectedQuantity);

        assertAll(
            () -> assertEquals(expectedQuantity, testHolding.getTotalQuantity()),
            () -> assertEquals(originalTotalCost, testHolding.getTotalCostBasis()), // Total cost unchanged
            () -> assertEquals(expectedAverageCost, testHolding.getAverageCostBasis())
        );
    }

    @Test
    void processStockSplitACBThrowsExceptionWhenSplitRatioIsNegative() {
        BigDecimal splitRatio = BigDecimal.valueOf(-2.0);
        Instant effectiveDate = Instant.now();

        assertThrows(InvalidHoldingOperationException.class, 
            () -> testHolding.processStockSplitACB(splitRatio, effectiveDate));
    }

    @Test
    void processStockSplitACBThrowsExceptionWhenSplitRatioIsZero() {
        BigDecimal splitRatio = BigDecimal.ZERO;
        Instant effectiveDate = Instant.now();

        assertThrows(InvalidHoldingOperationException.class, 
            () -> testHolding.processStockSplitACB(splitRatio, effectiveDate));
    }

    // ELIGIBLE DIVIDEND TESTS //

    @Test
    void recordEligibleDividendIsValid() {
        Money dividendAmount = Money.of(150.00, USD);
        Money grossUpAmount = Money.of(37.50, USD);
        Instant receivedAt = Instant.now();

        assertDoesNotThrow(() -> testHolding.recordEligibleDividend(dividendAmount, grossUpAmount, receivedAt));
        assertEquals(1, testHolding.getUncommittedEvents().size());
    }

    @Test
    void recordEligibleDividendThrowsExceptionWhenHoldingIsEmpty() {
        // Create empty holding
        testHolding.decreasePosition(testHolding.getTotalQuantity(), Money.of(230.00, USD), Instant.now());
        
        Money dividendAmount = Money.of(150.00, USD);
        Money grossUpAmount = Money.of(37.50, USD);
        Instant receivedAt = Instant.now();

        assertThrows(InvalidHoldingOperationException.class, 
            () -> testHolding.recordEligibleDividend(dividendAmount, grossUpAmount, receivedAt));
    }

    // QUERY METHOD TESTS //

    @Test
    void getCurrentMarketValueWithValidMarketPrice() {
        Money marketPrice = Money.of(250.00, USD);
        Money expectedValue = marketPrice.multiply(testHolding.getTotalQuantity());

        Money currentValue = testHolding.getCurrentMarketValue(marketPrice);

        assertEquals(expectedValue, currentValue);
    }

    @Test
    void getCurrentMarketValueThrowsExceptionWhenMarketPriceIsNull() {
        assertThrows(NullPointerException.class, () -> testHolding.getCurrentMarketValue(null));
    }

    @Test
    void getCurrentMarketValueThrowsExceptionWhenPriceIsNegative() {
        Money marketPrice = Money.of(-250.00, USD);

        assertThrows(InvalidHoldingOperationException.class, () -> testHolding.getCurrentMarketValue(marketPrice));
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
    void getUnrealizedGainLossPercentageWithGain() {
        Money marketPrice = Money.of(250.00, USD);
        
        Percentage gainLossPercentage = testHolding.getUnrealizedGainLossPercentage(marketPrice);
        
        assertTrue(gainLossPercentage.value().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void getUnrealizedGainLossPercentageWithLoss() {
        Money marketPrice = Money.of(180.00, USD);
        
        Percentage gainLossPercentage = testHolding.getUnrealizedGainLossPercentage(marketPrice);
        
        assertTrue(gainLossPercentage.value().compareTo(BigDecimal.ZERO) < 0);
    }

    @Test
    void getUnrealizedGainLossPercentageReturnsZeroWhenTotalCostBasisIsZero() {
        // Create holding with zero cost basis
        AssetHolding zeroHolding = new AssetHolding.Builder()
            .portfolioId(portfolioId)
            .assetHoldingId(assetId)
            .assetIdentifier(assetIdentifier)
            .totalQuantity(BigDecimal.valueOf(100))
            .averageCostBasis(Money.of(0, USD))
            .totalCostBasis(Money.of(0, USD))
            .createdAt(createdAt)
            .updatedAt(createdAt)
            .lastTransactionAt(createdAt)
            .version(0)
            .build();

        Money marketPrice = Money.of(250.00, USD);
        Percentage percentage = zeroHolding.getUnrealizedGainLossPercentage(marketPrice);
        
        assertEquals(Percentage.of(0), percentage);
    }

    @Test
    void calculateCapitalGainLossIsValid() {
        BigDecimal quantitySold = BigDecimal.valueOf(50);
        Money salePrice = Money.of(250.00, USD);

        Money capitalGainLoss = testHolding.calculateCapitalGainLoss(quantitySold, salePrice);

        Money saleProceeds = salePrice.multiply(quantitySold);
        Money acbOfSoldShares = testHolding.getAverageCostBasis().multiply(quantitySold);
        Money expectedGainLoss = saleProceeds.subtract(acbOfSoldShares);

        assertEquals(expectedGainLoss, capitalGainLoss);
    }

    @Test
    void calculateCapitalGainLossThrowsExceptionWhenQuantityExceedsHolding() {
        BigDecimal quantitySold = BigDecimal.valueOf(150); // More than current holding
        Money salePrice = Money.of(250.00, USD);

        assertThrows(InvalidHoldingOperationException.class, 
            () -> testHolding.calculateCapitalGainLoss(quantitySold, salePrice));
    }

    @Test
    void calculateCapitalGainLossThrowsExceptionWhenSalePriceIsNegative() {
        BigDecimal quantitySold = BigDecimal.valueOf(50);
        Money salePrice = Money.of(-250.00, USD);

        assertThrows(InvalidHoldingOperationException.class, 
            () -> testHolding.calculateCapitalGainLoss(quantitySold, salePrice));
    }

    @Test
    void getACBPerShareReturnsCorrectValue() {
        Money acbPerShare = testHolding.getACBPerShare();
        assertEquals(testHolding.getAverageCostBasis(), acbPerShare);
    }

    @Test
    void getTotalACBReturnsCorrectValue() {
        Money totalACB = testHolding.getTotalACB();
        assertEquals(testHolding.getTotalCostBasis(), totalACB);
    }

    @Test
    void shouldBeRemovedReturnsTrueWhenEmptyAndZeroCostBasis() {
        // Create empty holding with zero cost basis
        AssetHolding emptyHolding = new AssetHolding.Builder()
            .portfolioId(portfolioId)
            .assetHoldingId(assetId)
            .assetIdentifier(assetIdentifier)
            .totalQuantity(BigDecimal.ZERO)
            .averageCostBasis(Money.of(0, USD))
            .totalCostBasis(Money.of(0, USD))
            .createdAt(createdAt)
            .updatedAt(createdAt)
            .lastTransactionAt(createdAt)
            .version(0)
            .build();

        assertTrue(emptyHolding.shouldBeRemoved());
    }

    @Test
    void shouldBeRemovedReturnsFalseWhenHasPosition() {
        assertFalse(testHolding.shouldBeRemoved());
    }

    @Test
    void isACBBelowThresholdReturnsTrueWhenBelowThreshold() {
        Money threshold = Money.of(30000.00, USD); // Higher than current total cost basis
        
        assertTrue(testHolding.isACBBelowThreshold(threshold));
    }

    @Test
    void isACBBelowThresholdReturnsFalseWhenAboveThreshold() {
        Money threshold = Money.of(1000.00, USD); // Lower than current total cost basis
        
        assertFalse(testHolding.isACBBelowThreshold(threshold));
    }

    @Test
    void isEligibleForSuperficialLossRuleThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> testHolding.isEligibleForSuperficialLossRule());
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
    void hasPositionReturnsTrueForNonZeroHolding() {
        assertTrue(testHolding.hasPosition());
    }

    @Test
    void hasPositionReturnsFalseForZeroHolding() {
        testHolding.decreasePosition(testHolding.getTotalQuantity(), Money.of(230.00, USD), Instant.now());
        
        assertFalse(testHolding.hasPosition());
    }

    @Test
    void canSellReturnsTrueWhenQuantityIsAvailable() {
        BigDecimal requestedQuantity = BigDecimal.valueOf(50);
        
        assertTrue(testHolding.canSell(requestedQuantity));
    }

    @Test
    void canSellReturnsFalseWhenQuantityExceedsHolding() {
        BigDecimal requestedQuantity = BigDecimal.valueOf(150);
        
        assertFalse(testHolding.canSell(requestedQuantity));
    }

    @Test
    void canSellReturnsFalseWhenHoldingIsEmpty() {
        testHolding.decreasePosition(testHolding.getTotalQuantity(), Money.of(230.00, USD), Instant.now());
        BigDecimal requestedQuantity = BigDecimal.valueOf(10);
        
        assertFalse(testHolding.canSell(requestedQuantity));
    }

    @Test
    void canSellReturnsTrueWhenQuantityEqualsHolding() {
        BigDecimal requestedQuantity = testHolding.getTotalQuantity();
        
        assertTrue(testHolding.canSell(requestedQuantity));
    }

    @Test
    void getCostBasisForQuantityReturnsCorrectValue() {
        BigDecimal quantity = BigDecimal.valueOf(50);
        Money expectedCostBasis = testHolding.getAverageCostBasis().multiply(quantity);
        
        Money costBasis = testHolding.getCostBasisForQuantity(quantity);
        
        assertEquals(expectedCostBasis, costBasis);
    }

    @Test
    void getCostBasisForQuantityThrowsExceptionWhenQuantityExceedsHolding() {
        BigDecimal quantity = BigDecimal.valueOf(150);
        
        assertThrows(InvalidHoldingOperationException.class, 
            () -> testHolding.getCostBasisForQuantity(quantity));
    }

    // ASSET TYPE TESTS //

    @Test
    void isOfTypeReturnsTrueForCorrectType() {
        assertTrue(testHolding.isOfType(AssetType.STOCK));
        assertFalse(testHolding.isOfType(AssetType.ETF));
    }

    @Test
    void isStockReturnsTrueForStockAsset() {
        assertTrue(testHolding.isStock());
    }

    @Test
    void isETFReturnsFalseForStockAsset() {
        assertFalse(testHolding.isETF());
    }

    @Test
    void isCryptoReturnsFalseForStockAsset() {
        assertFalse(testHolding.isCrypto());
    }

    @Test
    void isBondReturnsFalseForStockAsset() {
        assertFalse(testHolding.isBond());
    }

    @Test
    void assetTypeTestsForETFAsset() {
        AssetIdentifier etfIdentifier = new AssetIdentifier(AssetType.ETF, "US0378331006", Map.of("TICKER", "VTI"), "Vanguard Total Stock Market ETF", "NYSE", USD.toString());
        AssetHolding etfHolding = AssetHolding.createInitialHolding(
            portfolioId, 
            AssetHoldingId.createRandom(), 
            etfIdentifier, 
            BigDecimal.valueOf(50), 
            Money.of(220.00, USD), 
            Instant.now()
        );

        assertAll(
            () -> assertTrue(etfHolding.isETF()),
            () -> assertFalse(etfHolding.isStock()),
            () -> assertFalse(etfHolding.isCrypto()),
            () -> assertFalse(etfHolding.isBond()),
            () -> assertTrue(etfHolding.isOfType(AssetType.ETF))
        );
    }

    // DOMAIN EVENTS TESTS //

    @Test
    void addDomainEventIsValid() {
        DomainEvent mockEvent = mock(DomainEvent.class);
        
        testHolding.addDomainEvent(mockEvent);
        
        assertEquals(1, testHolding.getUncommittedEvents().size());
        assertTrue(testHolding.getUncommittedEvents().contains(mockEvent));
    }

    @Test
    void addDomainEventThrowsExceptionWhenEventIsNull() {
        assertThrows(NullPointerException.class, () -> testHolding.addDomainEvent(null));
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
    void hasUncommittedEventsReturnsTrueWhenEventsExist() {
        testHolding.increasePosition(BigDecimal.valueOf(25), Money.of(220.00, USD), Instant.now());
        
        assertTrue(testHolding.hasUncommittedEvents());
    }

    @Test
    void hasUncommittedEventsReturnsFalseWhenNoEvents() {
        assertFalse(testHolding.hasUncommittedEvents());
    }

    // VERSION AND TIMESTAMP TESTS //

    @Test
    void multipleTransactionsUpdateVersionCorrectly() {
        int initialVersion = testHolding.getVersion();
        
        testHolding.increasePosition(BigDecimal.valueOf(25), Money.of(220.00, USD), Instant.now());
        testHolding.recordDividend(Money.of(100.00, USD), Instant.now());
        testHolding.decreasePosition(BigDecimal.valueOf(10), Money.of(230.00, USD), Instant.now());

        assertEquals(3, testHolding.getUncommittedEvents().size());
        assertEquals(initialVersion + 3, testHolding.getVersion());
    }

    @Test
    void transactionsUpdateUpdatedAtTimestamp() {
        Instant initialUpdatedAt = testHolding.getUpdatedAt();
        
        // Wait a bit to ensure time difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        testHolding.increasePosition(BigDecimal.valueOf(25), Money.of(220.00, USD), Instant.now());
        
        assertTrue(testHolding.getUpdatedAt().isAfter(initialUpdatedAt));
    }

    // EQUALS AND HASHCODE TESTS //

    @Test
    void equalsReturnsTrueForSameAssetHoldingIdAndPortfolioId() {
        AssetHolding otherHolding = AssetHolding.createInitialHolding(
            portfolioId, // Same portfolio ID
            assetId, // Same asset ID
            assetIdentifier,
            BigDecimal.valueOf(200), // Different quantity
            Money.of(300.00, USD), // Different price
            Instant.now()
        );

        assertEquals(testHolding, otherHolding);
    }

    @Test
    void equalsReturnsFalseForDifferentAssetHoldingId() {
        AssetHolding otherHolding = AssetHolding.createInitialHolding(
            portfolioId,
            AssetHoldingId.createRandom(), // Different asset ID
            assetIdentifier,
            totalQuantity,
            pricePerUnit,
            createdAt
        );

        assertNotEquals(testHolding, otherHolding);
    }

    @Test
    void equalsReturnsFalseForDifferentPortfolioId() {
        AssetHolding otherHolding = AssetHolding.createInitialHolding(
            PortfolioId.createRandom(), // Different portfolio ID
            assetId,
            assetIdentifier,
            totalQuantity,
            pricePerUnit,
            createdAt
        );

        assertNotEquals(testHolding, otherHolding);
    }

    @Test
    void equalsReturnsFalseForNull() {
        assertNotEquals(testHolding, null);
    }

    @Test
    void equalsReturnsFalseForDifferentClass() {
        assertNotEquals(testHolding, "not an AssetHolding");
    }

    @Test
    void equalsReturnsTrueAndFalseForObjects() {
        assertFalse(testHolding.equals(new Object()));
        assertTrue(testHolding.equals(testHolding));
    }

    @Test
    void hashCodeIsConsistentWithEquals() {
        AssetHolding otherHolding = AssetHolding.createInitialHolding(
            portfolioId, // Same portfolio ID
            assetId, // Same asset ID
            assetIdentifier,
            BigDecimal.valueOf(200), // Different quantity
            Money.of(300.00, USD), // Different price
            Instant.now()
        );

        assertEquals(testHolding.hashCode(), otherHolding.hashCode());
    }

    // VALIDATION TESTS //

    @Test
    void builderValidationThrowsExceptionForNegativeQuantity() {
        assertThrows(InvalidHoldingQuantityException.class, () -> 
            new AssetHolding.Builder()
                .portfolioId(portfolioId)
                .assetHoldingId(assetId)
                .assetIdentifier(assetIdentifier)
                .totalQuantity(BigDecimal.valueOf(-10))
                .averageCostBasis(averageCostBasis)
                .totalCostBasis(totalCostBasis)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .lastTransactionAt(createdAt)
                .version(0)
                .build()
        );
    }

    @Test
    void builderValidationThrowsExceptionForNegativeAverageCostBasis() {
        assertThrows(InvalidHoldingCostBasisException.class, () -> 
            new AssetHolding.Builder()
                .portfolioId(portfolioId)
                .assetHoldingId(assetId)
                .assetIdentifier(assetIdentifier)
                .totalQuantity(totalQuantity)
                .averageCostBasis(Money.of(-100, USD))
                .totalCostBasis(totalCostBasis)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .lastTransactionAt(createdAt)
                .version(0)
                .build()
        );
    }

    @Test
    void builderValidationThrowsExceptionForNegativeTotalCostBasis() {
        assertThrows(InvalidHoldingCostBasisException.class, () -> 
            new AssetHolding.Builder()
                .portfolioId(portfolioId)
                .assetHoldingId(assetId)
                .assetIdentifier(assetIdentifier)
                .totalQuantity(totalQuantity)
                .averageCostBasis(averageCostBasis)
                .totalCostBasis(Money.of(-1000, USD))
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .lastTransactionAt(createdAt)
                .version(0)
                .build()
        );
    }

    @Test
    void builderValidationThrowsExceptionForCurrencyMismatch() {
        assertThrows(InvalidHoldingOperationException.class, () -> 
            new AssetHolding.Builder()
                .portfolioId(portfolioId)
                .assetHoldingId(assetId)
                .assetIdentifier(assetIdentifier)
                .totalQuantity(totalQuantity)
                .averageCostBasis(Money.of(215.57, USD))
                .totalCostBasis(Money.of(21557, CAD)) // Different currency
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .lastTransactionAt(createdAt)
                .version(0)
                .build()
        );
    }

    // TOSTRING TEST //

    @Test
    void toStringReturnsCorrectFormat() {
        String toStringResult = testHolding.toString();
        
        assertAll(
            () -> assertTrue(toStringResult.contains("AssetHolding{")),
            () -> assertTrue(toStringResult.contains("assetId=" + assetId)),
            () -> assertTrue(toStringResult.contains("symbol='" + assetIdentifier.primaryId() + "'")),
            () -> assertTrue(toStringResult.contains("quantity=" + totalQuantity)),
            () -> assertTrue(toStringResult.contains("avgCost=" + averageCostBasis)),
            () -> assertTrue(toStringResult.contains("totalCost=" + totalCostBasis))
        );
    }

    // testing the isSTOCK/isETF/etc.
    @Test
    void isAssetType() {
        assertTrue(testHolding.isStock());
        assertFalse(testHolding.isETF());
        assertFalse(testHolding.isCrypto());
        assertFalse(testHolding.isBond());
    }

    @Test
    void isETF() {
        assetId = AssetHoldingId.createRandom();
        portfolioId = PortfolioId.createRandom();
        assetIdentifier = new AssetIdentifier(AssetType.ETF, "US0378331005", Map.of("CUSIP", "037833100"), "Apple", "NASDAQ", USD.toString());
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
              
        assertFalse(testHolding.isStock());
        assertTrue(testHolding.isETF());
        assertFalse(testHolding.isCrypto());
        assertFalse(testHolding.isBond());
    }

    @Test
    void isCrypto() {
        assetId = AssetHoldingId.createRandom();
        portfolioId = PortfolioId.createRandom();
        assetIdentifier = new AssetIdentifier(AssetType.CRYPTO, "US0378331005", Map.of("CUSIP", "037833100"), "Apple", "NASDAQ", USD.toString());
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
              
        assertFalse(testHolding.isStock());
        assertFalse(testHolding.isETF());
        assertTrue(testHolding.isCrypto());
        assertFalse(testHolding.isBond());
    }
    @Test
    void isBond() {
        assetId = AssetHoldingId.createRandom();
        portfolioId = PortfolioId.createRandom();
        assetIdentifier = new AssetIdentifier(AssetType.BOND, "US0378331005", Map.of("CUSIP", "037833100"), "Apple", "NASDAQ", USD.toString());
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
              
        assertFalse(testHolding.isStock());
        assertFalse(testHolding.isETF());
        assertFalse(testHolding.isCrypto());
        assertTrue(testHolding.isBond());
    }

    @Test
    void shouldBeRemovedIsValid() {
        // Initially: holding has quantity and cost basis
        assertFalse(testHolding.isEmpty(), "Holding should not be empty at start");
        assertFalse(testHolding.shouldBeRemoved(), "Non-empty holding should not be removed");

        // Act: sell entire position
        BigDecimal quantityToSell = BigDecimal.valueOf(100);
        Money sellPrice = Money.of(215.57, USD);
        Instant transactionDate = Instant.now();

        testHolding.decreasePosition(quantityToSell, sellPrice, transactionDate);

        // Assert: event emitted, holding emptied, cost basis zeroed
        assertEquals(1, testHolding.getUncommittedEvents().size(), "One event should be emitted");
        assertTrue(testHolding.isEmpty(), "Holding should now be empty");
        assertEquals(BigDecimal.ZERO.setScale(DecimalPrecision.getMoneyDecimalPlaces()), testHolding.getTotalCostBasis().amount());
        assertTrue(testHolding.shouldBeRemoved(), "Empty holding with zero cost basis should be removed");


        // isEmpty is not empty, should be false

    }

    @Test
    void shouldBeRemovedIsFalseWhenIsEmptyIsFalse() {       
        BigDecimal quantityToSell = BigDecimal.valueOf(50);
        Money sellPrice = Money.of(215.57, USD);
        Instant transactionDate = Instant.now();

        testHolding.decreasePosition(quantityToSell, sellPrice, transactionDate);
        assertFalse(testHolding.shouldBeRemoved());
    }
}