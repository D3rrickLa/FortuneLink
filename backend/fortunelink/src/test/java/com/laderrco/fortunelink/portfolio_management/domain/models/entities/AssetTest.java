package com.laderrco.fortunelink.portfolio_management.domain.models.entities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class AssetTest {
    
    private AssetId assetId;
    private AssetIdentifier assetIdentifier;
    private ValidatedCurrency currency;
    private BigDecimal quantity;
    private Money costBasis;
    private Instant acquiredOn;

    @BeforeEach
    void setUp() {
        assetId = mock(AssetId.class);
        assetIdentifier = mock(AssetIdentifier.class);
        currency = ValidatedCurrency.USD;
        quantity = BigDecimal.valueOf(100);
        costBasis = Money.of(BigDecimal.valueOf(5000), ValidatedCurrency.USD);
        acquiredOn = Instant.MIN;
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create valid asset with all required fields")
        void shouldCreateValidAsset() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                quantity,
                costBasis,
                acquiredOn
            );

            assertNotNull(asset);
            assertEquals(assetId, asset.getAssetId());
            assertEquals(assetIdentifier, asset.getAssetIdentifier());
            assertEquals(currency, asset.getCurrency());
            assertEquals(quantity, asset.getQuantity());
            assertEquals(costBasis, asset.getCostBasis());
            assertEquals(acquiredOn, asset.getAcquiredOn());
            assertEquals(1, asset.getVersion());
        }

        @Test
        @DisplayName("Should derive currency from cost basis")
        void shouldDeriveCurrencyFromCostBasis() {
            Money cadCostBasis = Money.of(BigDecimal.valueOf(5000), ValidatedCurrency.CAD);
            
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                quantity,
                cadCostBasis,
                acquiredOn
            );

            assertEquals(ValidatedCurrency.CAD, asset.getCurrency());
        }

        @Test
        @DisplayName("Should set acquiredOn as lastSystemInteraction initially")
        void shouldSetAcquiredOnAsLastSystemInteraction() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                quantity,
                costBasis,
                acquiredOn
            );

            assertEquals(acquiredOn, asset.getLastSystemInteraction());
        }

        @Test
        @DisplayName("Should throw NullPointerException when assetId is null")
        void shouldThrowExceptionWhenAssetIdIsNull() {
            assertThrows(NullPointerException.class, () -> 
                new Asset(
                    null,
                    assetIdentifier,
                    quantity,
                    costBasis,
                    acquiredOn
                )
            );
        }

        @Test
        @DisplayName("Should throw NullPointerException when assetIdentifier is null")
        void shouldThrowExceptionWhenAssetIdentifierIsNull() {
            assertThrows(NullPointerException.class, () -> 
                new Asset(
                    assetId,
                    null,
                    quantity,
                    costBasis,
                    acquiredOn
                )
            );
        }

        @Test
        @DisplayName("Should throw NullPointerException when quantity is null")
        void shouldThrowExceptionWhenQuantityIsNull() {
            assertThrows(NullPointerException.class, () -> 
                new Asset(
                    assetId,
                    assetIdentifier,
                    null,
                    costBasis,
                    acquiredOn
                )
            );
        }

        @Test
        @DisplayName("Should throw NullPointerException when costBasis is null")
        void shouldThrowExceptionWhenCostBasisIsNull() {
            assertThrows(NullPointerException.class, () -> 
                new Asset(
                    assetId,
                    assetIdentifier,
                    quantity,
                    null,
                    acquiredOn
                )
            );
        }

        @Test
        @DisplayName("Should throw NullPointerException when acquiredOn is null")
        void shouldThrowExceptionWhenAcquiredOnIsNull() {
            assertThrows(NullPointerException.class, () -> 
                new Asset(
                    assetId,
                    assetIdentifier,
                    quantity,
                    costBasis,
                    null
                )
            );
        }
    }

    @Nested
    @DisplayName("Adjust Quantity Tests")
    class addQuantityTests {

        @Test
        @DisplayName("Should increase quantity when adding positive amount")
        void shouldIncreaseQuantity() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                BigDecimal.valueOf(100),
                costBasis,
                acquiredOn
            );

            int initialVersion = asset.getVersion();
            asset.addQuantity(BigDecimal.valueOf(50));

            assertEquals(BigDecimal.valueOf(150), asset.getQuantity());
            assertEquals(initialVersion + 1, asset.getVersion());
            assertTrue(asset.getLastSystemInteraction().isAfter(acquiredOn));
        }

        @Test
        @DisplayName("Should decrease quantity when adding negative amount")
        void shouldDecreaseQuantity() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                BigDecimal.valueOf(100),
                costBasis,
                acquiredOn
            );

            asset.addQuantity(BigDecimal.valueOf(-30));

            assertEquals(BigDecimal.valueOf(70), asset.getQuantity());
        }

        @Test
        @DisplayName("Should handle zero adjustment")
        void shouldHandleZeroAdjustment() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                BigDecimal.valueOf(100),
                costBasis,
                acquiredOn
            );

            asset.addQuantity(BigDecimal.ZERO);

            assertEquals(BigDecimal.valueOf(100), asset.getQuantity());
        }

        @Test
        @DisplayName("Should throw NullPointerException when adjustment is null")
        void shouldThrowExceptionWhenAdjustmentIsNull() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                quantity,
                costBasis,
                acquiredOn
            );

            assertThrows(NullPointerException.class, () -> 
                asset.addQuantity(null)
            );
        }

        @Test
        @DisplayName("Should update metadata after adjustment")
        void shouldUpdateMetadataAfterAdjustment() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                quantity,
                costBasis,
                acquiredOn
            );

            int initialVersion = asset.getVersion();
            Instant initialInteraction = asset.getLastSystemInteraction();

            asset.addQuantity(BigDecimal.TEN);

            assertEquals(initialVersion + 1, asset.getVersion());
            assertTrue(asset.getLastSystemInteraction().isAfter(initialInteraction) || 
                       asset.getLastSystemInteraction().equals(initialInteraction));
        }
    }

    @Nested
    @DisplayName("Reduce Quantity Tests")
    class ReduceQuantityTests {

        @Test
        @DisplayName("Should reduce quantity when amount is valid")
        void shouldReduceQuantity() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                BigDecimal.valueOf(100),
                costBasis,
                acquiredOn
            );

            asset.reduceQuantity(BigDecimal.valueOf(30));

            assertEquals(BigDecimal.valueOf(70), asset.getQuantity());
        }

        @Test
        @DisplayName("Should allow reducing to zero")
        void shouldAllowReducingToZero() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                BigDecimal.valueOf(100),
                costBasis,
                acquiredOn
            );

            asset.reduceQuantity(BigDecimal.valueOf(100));

            assertEquals(BigDecimal.ZERO, asset.getQuantity());
            assertTrue(asset.hasZeroQuantity());
        }

        @Test
        @DisplayName("Should throw exception when reducing below zero")
        void shouldThrowExceptionWhenReducingBelowZero() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                BigDecimal.valueOf(100),
                costBasis,
                acquiredOn
            );

            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> asset.reduceQuantity(BigDecimal.valueOf(150))
            );

            assertTrue(exception.getMessage().contains("Cannot remove"));
        }

        @Test
        @DisplayName("Should throw NullPointerException when reduction amount is null")
        void shouldThrowExceptionWhenReductionIsNull() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                quantity,
                costBasis,
                acquiredOn
            );

            assertThrows(NullPointerException.class, () -> 
                asset.reduceQuantity(null)
            );
        }

        @Test
        @DisplayName("Should update metadata after reduction")
        void shouldUpdateMetadataAfterReduction() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                BigDecimal.valueOf(100),
                costBasis,
                acquiredOn
            );

            int initialVersion = asset.getVersion();
            asset.reduceQuantity(BigDecimal.valueOf(20));

            assertEquals(initialVersion + 1, asset.getVersion());
            assertTrue(asset.getLastSystemInteraction().isAfter(acquiredOn));
        }
    }

    @Nested
    @DisplayName("Update Cost Basis Tests")
    class UpdateCostBasisTests {

        @Test
        @DisplayName("Should update cost basis when valid")
        void shouldUpdateCostBasis() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                quantity,
                costBasis,
                acquiredOn
            );

            Money newCostBasis = Money.of(BigDecimal.valueOf(6000), ValidatedCurrency.USD);
            asset.updateCostBasis(newCostBasis);

            assertEquals(newCostBasis, asset.getCostBasis());
        }

        @Test
        @DisplayName("Should throw exception when cost basis currency mismatches")
        void shouldThrowExceptionWhenCurrencyMismatches() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                quantity,
                costBasis,
                acquiredOn
            );

            Money cadCostBasis = Money.of(BigDecimal.valueOf(6000), ValidatedCurrency.CAD);

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> asset.updateCostBasis(cadCostBasis)
            );

            assertTrue(exception.getMessage().contains("currency must match"));
        }

        @Test
        @DisplayName("Should throw exception when cost basis is negative")
        void shouldThrowExceptionWhenCostBasisIsNegative() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                quantity,
                costBasis,
                acquiredOn
            );

            Money negativeCostBasis = Money.of(BigDecimal.valueOf(-1000), ValidatedCurrency.USD);

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> asset.updateCostBasis(negativeCostBasis)
            );

            assertTrue(exception.getMessage().contains("cannot be negative"));
        }

        @Test
        @DisplayName("Should throw NullPointerException when cost basis is null")
        void shouldThrowExceptionWhenCostBasisIsNull() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                quantity,
                costBasis,
                acquiredOn
            );

            assertThrows(NullPointerException.class, () -> 
                asset.updateCostBasis(null)
            );
        }

        @Test
        @DisplayName("Should allow zero cost basis")
        void shouldAllowZeroCostBasis() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                quantity,
                costBasis,
                acquiredOn
            );

            Money zeroCostBasis = Money.of(BigDecimal.ZERO, ValidatedCurrency.USD);
            
            assertDoesNotThrow(() -> asset.updateCostBasis(zeroCostBasis));
            assertEquals(zeroCostBasis, asset.getCostBasis());
        }

        @Test
        @DisplayName("Should update metadata after cost basis change")
        void shouldUpdateMetadataAfterCostBasisChange() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                quantity,
                costBasis,
                acquiredOn
            );

            int initialVersion = asset.getVersion();
            Money newCostBasis = Money.of(BigDecimal.valueOf(7000), ValidatedCurrency.USD);
            
            asset.updateCostBasis(newCostBasis);

            assertEquals(initialVersion + 1, asset.getVersion());
            assertTrue(asset.getLastSystemInteraction().isAfter(acquiredOn));
        }
    }

    @Nested
    @DisplayName("Get Cost Per Unit Tests")
    class GetCostPerUnitTests {

        @Test
        @DisplayName("Should calculate cost per unit correctly")
        void shouldCalculateCostPerUnit() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                BigDecimal.valueOf(100),
                Money.of(BigDecimal.valueOf(5000), ValidatedCurrency.USD),
                acquiredOn
            );

            Money costPerUnit = asset.getCostPerUnit();

            assertEquals(new BigDecimal("50.00").setScale(Precision.getMoneyPrecision()), costPerUnit.amount());
            assertEquals(ValidatedCurrency.USD, costPerUnit.currency());
        }

        @Test
        @DisplayName("Should handle fractional quantities")
        void shouldHandleFractionalQuantities() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                BigDecimal.valueOf(33.333),
                Money.of(BigDecimal.valueOf(1000), ValidatedCurrency.USD),
                acquiredOn
            );

            Money costPerUnit = asset.getCostPerUnit();

            assertNotNull(costPerUnit);
            assertTrue(costPerUnit.amount().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("Should return zero when quantity is zero")
        void shouldReturnZeroWhenQuantityIsZero() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                BigDecimal.ZERO,
                Money.of(BigDecimal.valueOf(5000), ValidatedCurrency.USD),
                acquiredOn
            );

            Money costPerUnit = asset.getCostPerUnit();

            assertEquals(BigDecimal.ZERO.setScale(Precision.getMoneyPrecision()), costPerUnit.amount());
        }
    }

    @Nested
    @DisplayName("Calculate Current Value Tests")
    class CalculateCurrentValueTests {

        @Test
        @DisplayName("Should calculate current value correctly")
        void shouldCalculateCurrentValue() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                BigDecimal.valueOf(100),
                costBasis,
                acquiredOn
            );

            Money currentPrice = Money.of(BigDecimal.valueOf(60), ValidatedCurrency.USD);
            Money currentValue = asset.calculateCurrentValue(currentPrice);

            assertEquals(new BigDecimal("6000").setScale(Precision.getMoneyPrecision()), currentValue.amount());
        }

        @Test
        @DisplayName("Should handle zero quantity")
        void shouldHandleZeroQuantity() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                BigDecimal.ZERO,
                costBasis,
                acquiredOn
            );

            Money currentPrice = Money.of(BigDecimal.valueOf(60), ValidatedCurrency.USD);
            Money currentValue = asset.calculateCurrentValue(currentPrice);

            assertEquals(BigDecimal.ZERO.setScale(Precision.getMoneyPrecision()), currentValue.amount());
        }

        @Test
        @DisplayName("Should throw exception when price currency mismatches")
        void shouldThrowExceptionWhenPriceCurrencyMismatches() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                quantity,
                costBasis,
                acquiredOn
            );

            Money cadPrice = Money.of(BigDecimal.valueOf(60), ValidatedCurrency.CAD);

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> asset.calculateCurrentValue(cadPrice)
            );

            assertTrue(exception.getMessage().contains("currency"));
            assertTrue(exception.getMessage().contains("does not match"));
        }

        @Test
        @DisplayName("Should throw NullPointerException when price is null")
        void shouldThrowExceptionWhenPriceIsNull() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                quantity,
                costBasis,
                acquiredOn
            );

            assertThrows(NullPointerException.class, () -> 
                asset.calculateCurrentValue(null)
            );
        }

        @Test
        @DisplayName("Should handle fractional prices")
        void shouldHandleFractionalPrices() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                BigDecimal.valueOf(100),
                costBasis,
                acquiredOn
            );

            Money fractionalPrice = Money.of(BigDecimal.valueOf(55.75), ValidatedCurrency.USD);
            Money currentValue = asset.calculateCurrentValue(fractionalPrice);

            assertEquals(new BigDecimal("5575.00").setScale(Precision.getMoneyPrecision()), currentValue.amount());
        }
    }

    @Nested
    @DisplayName("Calculate Unrealized Gain/Loss Tests")
    class CalculateUnrealizedGainLossTests {

        @Test
        @DisplayName("Should calculate unrealized gain")
        void shouldCalculateUnrealizedGain() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                BigDecimal.valueOf(100),
                Money.of(BigDecimal.valueOf(5000), ValidatedCurrency.USD),
                acquiredOn
            );

            Money currentPrice = Money.of(BigDecimal.valueOf(60), ValidatedCurrency.USD);
            Money unrealizedGain = asset.calculateUnrealizedGainLoss(currentPrice);

            // Current value: 100 * 60 = 6000
            // Cost basis: 5000
            // Gain: 1000
            assertEquals(new BigDecimal("1000").setScale(Precision.getMoneyPrecision()), unrealizedGain.amount());
        }

        @Test
        @DisplayName("Should calculate unrealized loss")
        void shouldCalculateUnrealizedLoss() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                BigDecimal.valueOf(100),
                Money.of(BigDecimal.valueOf(5000), ValidatedCurrency.USD),
                acquiredOn
            );

            Money currentPrice = Money.of(BigDecimal.valueOf(40), ValidatedCurrency.USD);
            Money unrealizedLoss = asset.calculateUnrealizedGainLoss(currentPrice);

            // Current value: 100 * 40 = 4000
            // Cost basis: 5000
            // Loss: -1000
            assertEquals(new BigDecimal("-1000").setScale(Precision.getMoneyPrecision()), unrealizedLoss.amount());
        }

        @Test
        @DisplayName("Should return zero when no gain or loss")
        void shouldReturnZeroWhenNoGainOrLoss() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                BigDecimal.valueOf(100),
                Money.of(BigDecimal.valueOf(5000), ValidatedCurrency.USD),
                acquiredOn
            );

            Money currentPrice = Money.of(BigDecimal.valueOf(50), ValidatedCurrency.USD);
            Money unrealizedGain = asset.calculateUnrealizedGainLoss(currentPrice);

            assertEquals(BigDecimal.ZERO.setScale(Precision.getMoneyPrecision()), unrealizedGain.amount());
        }

        @Test
        @DisplayName("Should throw NullPointerException when price is null")
        void shouldThrowExceptionWhenPriceIsNull() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                quantity,
                costBasis,
                acquiredOn
            );

            assertThrows(NullPointerException.class, () -> 
                asset.calculateUnrealizedGainLoss(null)
            );
        }
    }

    @Nested
    @DisplayName("Has Zero Quantity Tests")
    class HasZeroQuantityTests {

        @Test
        @DisplayName("Should return true when quantity is zero")
        void shouldReturnTrueWhenQuantityIsZero() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                BigDecimal.ZERO,
                costBasis,
                acquiredOn
            );

            assertTrue(asset.hasZeroQuantity());
        }

        @Test
        @DisplayName("Should return false when quantity is positive")
        void shouldReturnFalseWhenQuantityIsPositive() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                BigDecimal.valueOf(100),
                costBasis,
                acquiredOn
            );

            assertFalse(asset.hasZeroQuantity());
        }

        @Test
        @DisplayName("Should return false when quantity is negative")
        void shouldReturnFalseWhenQuantityIsNegative() {
            // Note: This tests the method behavior, though negative quantities 
            // shouldn't normally occur due to reduceQuantity validation
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                BigDecimal.valueOf(100),
                costBasis,
                acquiredOn
            );

            // Artificially create negative through addQuantity
            asset.addQuantity(BigDecimal.valueOf(-150));

            assertFalse(asset.hasZeroQuantity());
        }
    }

    @Nested
    @DisplayName("Version and Metadata Tests")
    class VersionAndMetadataTests {

        @Test
        @DisplayName("Should increment version on each mutation")
        void shouldIncrementVersionOnMutations() {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                quantity,
                costBasis,
                acquiredOn
            );

            assertEquals(1, asset.getVersion());

            asset.addQuantity(BigDecimal.TEN);
            assertEquals(2, asset.getVersion());

            asset.reduceQuantity(BigDecimal.ONE);
            assertEquals(3, asset.getVersion());

            Money newCostBasis = Money.of(BigDecimal.valueOf(6000), ValidatedCurrency.USD);
            asset.updateCostBasis(newCostBasis);
            assertEquals(4, asset.getVersion());
        }

        @Test
        @DisplayName("Should update lastSystemInteraction on each mutation")
        void shouldUpdateLastSystemInteractionOnMutations() throws InterruptedException {
            Asset asset = new Asset(
                assetId,
                assetIdentifier,
                
                quantity,
                costBasis,
                acquiredOn
            );

            Instant firstInteraction = asset.getLastSystemInteraction();
            
            // Small delay to ensure timestamp difference
            Thread.sleep(10);
            
            asset.addQuantity(BigDecimal.TEN);
            Instant secondInteraction = asset.getLastSystemInteraction();
            
            assertTrue(secondInteraction.isAfter(firstInteraction) || 
                       secondInteraction.equals(firstInteraction));
        }
    }
}
