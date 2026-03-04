package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Ratio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.shared.enums.Precision;

class AcbPositionTest {

    private final AssetSymbol SYMBOL = new AssetSymbol("AAPL");
    private final AssetType TYPE = AssetType.STOCK;
    private final Currency USD = Currency.USD;
    private final Instant NOW = Instant.now();

    @Nested
    @DisplayName("buy() Tests")
    class BuyTests {

        @Test
        @DisplayName("buy_success_initialPurchaseReturnsNewPosition")
        void buy_success_createsNewState() {
            AcbPosition initial = AcbPosition.empty(SYMBOL, TYPE, USD);
            Quantity buyQty = new Quantity(new BigDecimal("10"));
            Money buyCost = new Money(new BigDecimal("1000.00"), USD);

            var result = initial.buy(buyQty, buyCost, NOW);
            AcbPosition updated = (AcbPosition) result.getUpdatedPosition();

            // Note: Your current implementation replaces the state rather than adding to
            // it.
            // If the intention was to accumulate, the buy method in the provided code needs
            // a fix.
            assertEquals(new BigDecimal("10.00000000"), updated.totalQuantity().amount());
            assertEquals(new BigDecimal("1000.0000000000000000000000000000000000"), updated.totalCostBasis().amount());
        }
    }

    @Nested
    @DisplayName("sell() Tests")
    class SellTests {

        @Test
        @DisplayName("sell_success_reducesQuantityAndProportionalCostBasis")
        void sell_success_calculatesRealizedGain() {
            // Setup: 10 units at $100 total cost ($10/unit)
            AcbPosition position = new AcbPosition(SYMBOL, TYPE, USD,
                    new Quantity(new BigDecimal("10")),
                    new Money(new BigDecimal("100.00"), USD),
                    Instant.now());

            Quantity sellQty = new Quantity(new BigDecimal("5"));
            Money proceeds = new Money(new BigDecimal("80.00"), USD); // Sold for $16/unit

            ApplyResult<? extends Position> rawResult = position.sell(sellQty, proceeds, NOW);
            if (!(rawResult instanceof ApplyResult.Sale)) {
                throw new AssertionError("Expected a Sale result but got " + rawResult.getClass());
            }

            @SuppressWarnings("unchecked")
            var result = (ApplyResult.Sale<AcbPosition>) rawResult;

            AcbPosition updated = (AcbPosition) result.getUpdatedPosition();

            // Assert State
            assertEquals(new BigDecimal("5.00000000"), updated.totalQuantity().amount());
            assertEquals(new BigDecimal("50.0000000000000000000000000000000000"), updated.totalCostBasis().amount(),
                    "Cost basis should be halved");

            // Assert Gains
            assertEquals(new BigDecimal("50.0000000000000000000000000000000000"), result.costBasisSold().amount());
            assertEquals(new BigDecimal("30.0000000000000000000000000000000000"), result.realizedGainLoss().amount(),
                    "80 proceeds - 50 cost = 30 gain");
        }

        @Test
        @DisplayName("sell_failure_insufficientQuantityThrowsException")
        void sell_failure_insufficientQuantity() {
            AcbPosition position = new AcbPosition(SYMBOL, TYPE, USD,
                    new Quantity(new BigDecimal("10")),
                    new Money(new BigDecimal("100.00000000"), USD),
                    Instant.now());

            Quantity sellQty = new Quantity(new BigDecimal("11"));
            Money proceeds = new Money(new BigDecimal("150.00"), USD);

            assertThrows(IllegalStateException.class, () -> position.sell(sellQty, proceeds, NOW));
        }
    }

    @Nested
    @DisplayName("split() Tests")
    class SplitTests {

        @Test
        @DisplayName("split_success_adjustsQuantityKeepsBasisFixed")
        void split_success_2For1Split() {
            AcbPosition position = new AcbPosition(SYMBOL, TYPE, USD,
                    new Quantity(new BigDecimal("10")),
                    new Money(new BigDecimal("100.00"), USD),
                    Instant.now());

            Ratio ratio = new Ratio(2, 1);
            var result = position.split(ratio);
            AcbPosition updated = (AcbPosition) result.getUpdatedPosition();

            assertEquals(new BigDecimal("20.00000000"), updated.totalQuantity().amount());
            assertEquals(new BigDecimal("100.0000000000000000000000000000000000"), updated.totalCostBasis().amount(),
                    "Total cost basis never changes in a split");
            assertEquals(new BigDecimal("5.0000000000000000000000000000000000"), updated.costPerUnit().amount(),
                    "New cost per unit should be $5");
        }

        @Test
        @DisplayName("split_failure_negativeOrZeroRatioThrowsException")
        void split_failure_invalidRatio() {
            AcbPosition position = AcbPosition.empty(SYMBOL, TYPE, USD);
            assertThrows(IllegalArgumentException.class, () -> position.split(new Ratio(-1, 1)));
            assertThrows(IllegalArgumentException.class, () -> position.split(new Ratio(2, -1)));
        }
    }

    @Nested
    @DisplayName("Financial Calculation Tests")
    class CalculationTests {

        @Test
        @DisplayName("costPerUnit_success_handlesEmptyAndPopulatedPositions")
        void costPerUnit_logic() {
            AcbPosition empty = AcbPosition.empty(SYMBOL, TYPE, USD);
            assertEquals(BigDecimal.ZERO.setScale(Precision.getMoneyPrecision()), empty.costPerUnit().amount());

            AcbPosition populated = new AcbPosition(SYMBOL, TYPE, USD,
                    new Quantity(new BigDecimal("4")),
                    new Money(new BigDecimal("100.00"), USD),
                    Instant.now());
            assertEquals(new BigDecimal("25.0000000000000000000000000000000000"), populated.costPerUnit().amount());
        }

        @Test
        @DisplayName("calculateUnrealizedGain_success_correctDifference")
        void calculateUnrealizedGain_success() {
            AcbPosition position = new AcbPosition(SYMBOL, TYPE, USD,
                    new Quantity(new BigDecimal("10")),
                    new Money(new BigDecimal("100.00"), USD),
                    Instant.now());

            Money currentPrice = new Money(new BigDecimal("15.00"), USD); // Total value 150

            Money unrealized = position.calculateUnrealizedGain(currentPrice);
            assertEquals(new BigDecimal("50.0000000000000000000000000000000000"), unrealized.amount());
        }
    }
}