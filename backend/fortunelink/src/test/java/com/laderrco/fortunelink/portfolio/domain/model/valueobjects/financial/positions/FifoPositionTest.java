package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.*;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FifoPositionTest {

    private final AssetSymbol SYMBOL = new AssetSymbol("MSFT");
    private final AssetType TYPE = AssetType.STOCK;
    private final Currency USD = Currency.USD;
    private final Instant EARLIER = Instant.parse("2023-01-01T10:00:00Z");
    private final Instant LATER = Instant.parse("2023-02-01T10:00:00Z");
    private final Instant T1 = Instant.parse("2023-01-01T10:00:00Z");
    private final Instant T2 = Instant.parse("2023-02-01T10:00:00Z");
    private final Instant T3 = Instant.parse("2023-03-01T10:00:00Z");

    @Test
    void testConstructor_sucess() {
        FifoPosition position = new FifoPosition(SYMBOL, TYPE, USD, null);
        FifoPosition copy = (FifoPosition) position.copy();
        assertNotNull(position);

        assertEquals(position, copy);
    }

    @Nested
    @DisplayName("buy() Tests")
    class BuyTests {
        @Test
        @DisplayName("buy_success_addsNewLotToTrailingList")
        void buy_success_appendsLot() {
            FifoPosition initial = FifoPosition.empty(SYMBOL, TYPE, USD);
            var result = initial.buy(new Quantity(BigDecimal.valueOf(10)), Money.of(100, "USD"), EARLIER);

            assertEquals(1, result.newPosition().lots().size());
            assertEquals(new BigDecimal("10.00000000"), result.newPosition().totalQuantity().amount());
            assertFalse(result.isNoChange());
        }
    }

    @Nested
    @DisplayName("sell() Tests")
    class SellTests {

        @Test
        @DisplayName("sell_success_consumesMultipleLotsInFifoOrder")
        void sell_success_fifoOrder() {
            // Setup:
            // Lot 1: 10 units @ $100 ($10/ea) - Acquired T1
            // Lot 2: 10 units @ $200 ($20/ea) - Acquired T2
            FifoPosition pos = FifoPosition.empty(SYMBOL, TYPE, USD)
                    .buy(new Quantity(BigDecimal.valueOf(10)), Money.of(100, "USD"), T1).newPosition()
                    .buy(new Quantity(BigDecimal.valueOf(10)), Money.of(200, "USD"), T2).newPosition();

            // Action: Sell 15 units for $450 total proceeds
            // Math:
            // - 10 units from Lot 1 (Cost: $100)
            // - 5 units from Lot 2 (Cost: 5 * $20 = $100)
            // Total Cost Basis Sold = $200. Realized Gain = $450 - $200 = $250.
            var result = (ApplyResult.Sale<FifoPosition>) pos.sell(new Quantity(BigDecimal.valueOf(15)),
                    Money.of(450, "USD"),
                    Instant.now());

            // Assertions on the Sale Record components
            assertEquals(new BigDecimal("200.0000000000000000000000000000000000"), result.costBasisSold().amount());
            assertEquals(new BigDecimal("250.0000000000000000000000000000000000"), result.realizedGainLoss().amount());

            // Assertions on the resulting State
            FifoPosition nextPos = result.newPosition();
            assertEquals(1, nextPos.lots().size(), "Only Lot 2 (partially) should remain");
            assertEquals(new BigDecimal("5.00000000"), nextPos.totalQuantity().amount());
            assertEquals(new BigDecimal("100.0000000000000000000000000000000000"), nextPos.totalCostBasis().amount(),
                    "Remaining basis should be 5 units * $20");
            assertEquals(T2, nextPos.lots().get(0).acquiredDate(), "Remaining lot must be the LATER one");
        }

        @Test
        @DisplayName("sell_failure_bugDetection_partialLotSubtraction")
        void sell_logic_partialConsumption() {
            // Lot: 10 units @ $100
            FifoPosition pos = new FifoPosition(SYMBOL, TYPE, USD,
                    List.of(new TaxLot(new Quantity(BigDecimal.valueOf(10)), Money.of(100, "USD"), EARLIER)));

            // Sell 4 units
            var result = pos.sell(new Quantity(BigDecimal.valueOf(4)), Money.of(100, "USD"), Instant.now());
            FifoPosition updated = result.newPosition();

            // This test will FAIL on your current code because of the .add() bug in the
            // 'else' block
            assertEquals(new BigDecimal("6.00000000"), updated.totalQuantity().amount(), "10 - 4 should be 6");
            assertEquals(new BigDecimal("60.0000000000000000000000000000000000"), updated.totalCostBasis().amount(),
                    "Remaining cost should be 60");
        }

        @Test
        @DisplayName("sell_failure_insufficientQuantity")
        void sell_failure_tooMany() {
            FifoPosition pos = FifoPosition.empty(SYMBOL, TYPE, USD)
                    .buy(new Quantity(BigDecimal.valueOf(5)), Money.of(50, "USD"), EARLIER).newPosition();

            assertThrows(IllegalStateException.class,
                    () -> pos.sell(new Quantity(BigDecimal.valueOf(6)), Money.of(100, "USD"), Instant.now()));
        }

        @Test
        @DisplayName("sell_success_preservesSubsequentLots_whenRemainingToSellIsZero")
        void sell_success_preservesTrailingLots() {
            // Setup: 3 Lots
            // Lot 1: 10 units (to be fully consumed)
            // Lot 2: 10 units (to be partially consumed)
            // Lot 3: 10 units (should be preserved via the isZero() branch)
            FifoPosition pos = FifoPosition.empty(SYMBOL, TYPE, USD)
                    .buy(new Quantity(BigDecimal.valueOf(10)), Money.of(100, "USD"), T1).newPosition()
                    .buy(new Quantity(BigDecimal.valueOf(10)), Money.of(200, "USD"), T2).newPosition()
                    .buy(new Quantity(BigDecimal.valueOf(10)), Money.of(300, "USD"), T3).newPosition();

            // Action: Sell 15 units.
            // This consumes Lot 1, 5 units of Lot 2, and should trigger isZero() for Lot 3.
            var result = (ApplyResult.Sale<FifoPosition>) pos.sell(
                    new Quantity(BigDecimal.valueOf(15)),
                    Money.of(500, "USD"),
                    Instant.now());

            FifoPosition updated = result.newPosition();

            // Verification
            assertEquals(2, updated.lots().size(), "Should have partial Lot 2 and untouched Lot 3");

            // Verify Lot 3 was preserved exactly (Date and Amount)
            TaxLot preservedLot = updated.lots().get(1);
            assertEquals(T3, preservedLot.acquiredDate());
            assertEquals(new BigDecimal("10.00000000"), preservedLot.quantity().amount());
            assertEquals(new BigDecimal("300.0000000000000000000000000000000000"), preservedLot.costBasis().amount());
        }
    }

    @Nested
    @DisplayName("split() Tests")
    class SplitTests {
        @Test
        @DisplayName("split_success_appliesToAllLots")
        void split_success_2for1() {
            FifoPosition pos = FifoPosition.empty(SYMBOL, TYPE, USD)
                    .buy(new Quantity(BigDecimal.valueOf(10)), Money.of(100, "USD"), EARLIER).newPosition()
                    .buy(new Quantity(BigDecimal.valueOf(20)), Money.of(400, "USD"), LATER).newPosition();

            Ratio ratio = new Ratio(2, 1);
            var result = pos.split(ratio);

            assertEquals(new BigDecimal("60.00000000"), result.newPosition().totalQuantity().amount());
            assertEquals(new BigDecimal("500.0000000000000000000000000000000000"),
                    result.newPosition().totalCostBasis().amount());
        }

        @Test
        @DisplayName("split_failure_invalidRatio")
        void split_failure_zero() {
            FifoPosition pos = FifoPosition.empty(SYMBOL, TYPE, USD);
            assertThrows(IllegalArgumentException.class, () -> pos.split(new Ratio(-1, 0)));
        }
    }

    @Nested
    @DisplayName("Aggregation Tests")
    class AggregationTests {
        @Test
        @DisplayName("totalCostBasis_success_sumsLots")
        void totalCostBasis_calculation() {
            FifoPosition pos = new FifoPosition(SYMBOL, TYPE, USD, List.of(
                    new TaxLot(new Quantity(BigDecimal.valueOf(10)), Money.of(100.0, "USD"), EARLIER),
                    new TaxLot(new Quantity(BigDecimal.valueOf(10)), Money.of(200.0, "USD"), LATER)));
            assertEquals(new BigDecimal("300.0000000000000000000000000000000000"), pos.totalCostBasis().amount());
            assertEquals(new BigDecimal("15.0000000000000000000000000000000000"), pos.costPerUnit().amount());
        }

        @Test
        @DisplayName("currentValue_success_multipliesTotalQuantity")
        void currentValue_calculation() {
            FifoPosition pos = new FifoPosition(SYMBOL, TYPE, USD, List.of(
                    new TaxLot(new Quantity(BigDecimal.valueOf(10)), Money.of(100, "USD"), EARLIER)));
            Money price = new Money(new BigDecimal("15.50"), USD);
            assertEquals(new BigDecimal("155.0000000000000000000000000000000000"), pos.currentValue(price).amount());
        }
    }

    @Nested
    @DisplayName("costPerUnit() Branch Tests")
    class CostPerUnitTests {

        @Test
        @DisplayName("costPerUnit_success_returnsZeroWhenEmpty")
        void costPerUnit_branch_empty() {
            FifoPosition emptyPos = FifoPosition.empty(SYMBOL, TYPE, USD);

            Money result = emptyPos.costPerUnit();

            assertTrue(result.isZero());
            assertEquals(USD, result.currency());
        }

        @Test
        @DisplayName("costPerUnit_success_calculatesWeightedAverageWhenNotEmpty")
        void costPerUnit_branch_notEmpty() {
            // 10 units @ $100 + 10 units @ $200 = 20 units @ $300
            FifoPosition pos = FifoPosition.empty(SYMBOL, TYPE, USD)
                    .buy(new Quantity(BigDecimal.valueOf(10)), Money.of(100, "USD"), T1).newPosition()
                    .buy(new Quantity(BigDecimal.valueOf(10)), Money.of(200, "USD"), T2).newPosition();

            Money result = pos.costPerUnit();

            // $300 / 20 units = $15.00
            assertEquals(new BigDecimal("15.0000000000000000000000000000000000"), result.amount());
        }
    }
}