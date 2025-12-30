package com.laderrco.fortunelink.portfolio_management.application.mappers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.application.responses.AllocationDetail;
import com.laderrco.fortunelink.portfolio_management.application.responses.AllocationResponse;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AllocationMapperTest {

    private static final ValidatedCurrency USD = ValidatedCurrency.USD;
    private static final ValidatedCurrency CAD = ValidatedCurrency.CAD;
    private static final ValidatedCurrency EUR = ValidatedCurrency.EUR;
    private static final Instant TEST_TIME = Instant.parse("2024-01-15T10:00:00Z");
    private static final int PRECISION = Precision.PERCENTAGE.getDecimalPlaces();

    @Nested
    @DisplayName("AssetType Allocation Tests")
    class AssetTypeAllocationTests {

        @Test
        @DisplayName("Should calculate 60/30/10 percentages correctly for asset types")
        void shouldMapAssetTypeValuesToCorrectPercentages() {
            // Arrange
            Map<AssetType, Money> allocation = Map.of(
                AssetType.STOCK, Money.of(6000, "USD"),
                AssetType.ETF, Money.of(3000, "USD"),
                AssetType.CRYPTO, Money.of(1000, "USD")
            );
            Money totalValue = Money.of(10000, "USD");

            // Act
            AllocationResponse response = AllocationMapper.toResponseFromAssetType(allocation, totalValue, TEST_TIME);

            // Assert
            assertEquals(new BigDecimal("60.00").setScale(PRECISION), 
                response.getAllocations().get("STOCK").getPercentage().value());
            assertEquals(new BigDecimal("30.00").setScale(PRECISION), 
                response.getAllocations().get("ETF").getPercentage().value());
            assertEquals(new BigDecimal("10.00").setScale(PRECISION), 
                response.getAllocations().get("CRYPTO").getPercentage().value());
            
            assertEquals(totalValue, response.getTotalValue());
            assertEquals(TEST_TIME, response.getAsOfDate());
        }

        @Test
        @DisplayName("Should calculate 100% allocation for single asset type")
        void shouldCalculateFullAllocationForAssetType() {
            // Arrange
            Map<AssetType, Money> allocation = Map.of(
                AssetType.STOCK, Money.of(new BigDecimal("10000.00"), USD)
            );
            Money totalValue = Money.of(new BigDecimal("10000.00"), USD);

            // Act
            AllocationResponse response = AllocationMapper.toResponseFromAssetType(allocation, totalValue, TEST_TIME);

            // Assert
            AllocationDetail stockDetail = response.getAllocations().get("STOCK");
            
            assertEquals(new BigDecimal("100.00").setScale(PRECISION), stockDetail.getPercentage().value());
            assertEquals("STOCK", stockDetail.getCategory());
            assertEquals("Asset Type", stockDetail.getCategoryType());
            assertEquals(Money.of(new BigDecimal("10000.00"), USD), stockDetail.getValue());
            assertEquals(TEST_TIME, response.getAsOfDate());
        }

        @Test
        @DisplayName("Should handle multiple asset types with complex splits")
        void shouldHandleMultipleAssetTypes() {
            // Arrange
            Map<AssetType, Money> allocation = Map.of(
                AssetType.STOCK, Money.of(new BigDecimal("5000.00"), USD),
                AssetType.ETF, Money.of(new BigDecimal("2500.00"), USD),
                AssetType.CRYPTO, Money.of(new BigDecimal("1500.00"), USD),
                AssetType.BOND, Money.of(new BigDecimal("1000.00"), USD)
            );
            Money totalValue = Money.of(new BigDecimal("10000.00"), USD);

            // Act
            AllocationResponse response = AllocationMapper.toResponseFromAssetType(allocation, totalValue, TEST_TIME);

            // Assert
            assertEquals(new BigDecimal("50.00").setScale(PRECISION), 
                response.getAllocations().get("STOCK").getPercentage().value());
            assertEquals(new BigDecimal("25.00").setScale(PRECISION), 
                response.getAllocations().get("ETF").getPercentage().value());
            assertEquals(new BigDecimal("15.00").setScale(PRECISION), 
                response.getAllocations().get("CRYPTO").getPercentage().value());
            assertEquals(new BigDecimal("10.00").setScale(PRECISION), 
                response.getAllocations().get("BOND").getPercentage().value());
        }

        @Test
        @DisplayName("Should handle empty asset type allocation")
        void shouldHandleEmptyAssetTypeAllocation() {
            // Arrange
            Map<AssetType, Money> allocation = new HashMap<>();
            Money totalValue = Money.of(10000, "USD");

            // Act
            AllocationResponse response = AllocationMapper.toResponseFromAssetType(allocation, totalValue, TEST_TIME);

            // Assert
            assertTrue(response.getAllocations().isEmpty());
            assertEquals(totalValue, response.getTotalValue());
            assertEquals(TEST_TIME, response.getAsOfDate());
        }

        @Test
        @DisplayName("Should handle null asset type allocation")
        void shouldHandleNullAssetTypeAllocation() {
            // Arrange
            Money totalValue = Money.of(10000, "USD");

            // Act
            AllocationResponse response = AllocationMapper.toResponseFromAssetType(null, totalValue, TEST_TIME);

            // Assert
            assertTrue(response.getAllocations().isEmpty());
            assertEquals(totalValue, response.getTotalValue());
        }

        @Test
        @DisplayName("Should throw exception for null asset type")
        void shouldThrowExceptionForNullAssetType() {
            // This tests the private method indirectly through invalid input
            // In real scenario, the map shouldn't contain null keys
            Map<AssetType, Money> allocation = new HashMap<>();
            allocation.put(null, Money.of(1000, "USD"));
            Money totalValue = Money.of(1000, "USD");

            // Act & Assert
            assertThrows(NullPointerException.class, () -> 
                AllocationMapper.toResponseFromAssetType(allocation, totalValue, TEST_TIME));
        }
    }

    @Nested
    @DisplayName("AccountType Allocation Tests")
    class AccountTypeAllocationTests {

        @Test
        @DisplayName("Should calculate percentages correctly for account types")
        void shouldMapAccountTypeValuesToCorrectPercentages() {
            // Arrange
            Map<AccountType, Money> allocation = Map.of(
                AccountType.TFSA, Money.of(5000, "USD"),
                AccountType.RRSP, Money.of(3000, "USD"),
                AccountType.NON_REGISTERED, Money.of(2000, "USD")
            );
            Money totalValue = Money.of(10000, "USD");

            // Act
            AllocationResponse response = AllocationMapper.toResponseFromAccountType(allocation, totalValue, TEST_TIME);

            // Assert
            assertEquals(new BigDecimal("50.00").setScale(PRECISION), 
                response.getAllocations().get("TFSA").getPercentage().value());
            assertEquals(new BigDecimal("30.00").setScale(PRECISION), 
                response.getAllocations().get("RRSP").getPercentage().value());
            assertEquals(new BigDecimal("20.00").setScale(PRECISION), 
                response.getAllocations().get("NON_REGISTERED").getPercentage().value());
        }

        @Test
        @DisplayName("Should verify category type is 'Account Type'")
        void shouldSetCorrectCategoryTypeForAccountType() {
            // Arrange
            Map<AccountType, Money> allocation = Map.of(
                AccountType.TFSA, Money.of(10000, "USD")
            );
            Money totalValue = Money.of(10000, "USD");

            // Act
            AllocationResponse response = AllocationMapper.toResponseFromAccountType(allocation, totalValue, TEST_TIME);

            // Assert
            AllocationDetail detail = response.getAllocations().get("TFSA");
            assertEquals("Account Type", detail.getCategoryType());
            assertEquals("TFSA", detail.getCategory());
        }

        @Test
        @DisplayName("Should handle empty account type allocation")
        void shouldHandleEmptyAccountTypeAllocation() {
            // Arrange
            Map<AccountType, Money> allocation = new HashMap<>();
            Money totalValue = Money.of(10000, "USD");

            // Act
            AllocationResponse response = AllocationMapper.toResponseFromAccountType(allocation, totalValue, TEST_TIME);

            // Assert
            assertTrue(response.getAllocations().isEmpty());
            assertEquals(totalValue, response.getTotalValue());
        }
    }

    @Nested
    @DisplayName("Currency Allocation Tests")
    class CurrencyAllocationTests {

        @Test
        @DisplayName("Should map currencies to correct percentages")
        void shouldMapCurrenciesToPercentages() {
            // Arrange - Values already normalized to USD for percentage calculation
            Map<ValidatedCurrency, Money> allocation = Map.of(
                USD, Money.of(new BigDecimal("50000.00"), USD),
                CAD, Money.of(new BigDecimal("30000.00"), USD), // Normalized to USD
                EUR, Money.of(new BigDecimal("20000.00"), USD)  // Normalized to USD
            );
            Money totalValue = Money.of(new BigDecimal("100000.00"), USD);

            // Act
            AllocationResponse response = AllocationMapper.toResponseFromCurrency(allocation, totalValue, TEST_TIME);

            // Assert
            assertEquals(new BigDecimal("50.00").setScale(PRECISION), 
                response.getAllocations().get("USD").getPercentage().value());
            assertEquals(new BigDecimal("30.00").setScale(PRECISION), 
                response.getAllocations().get("CAD").getPercentage().value());
            assertEquals(new BigDecimal("20.00").setScale(PRECISION), 
                response.getAllocations().get("EUR").getPercentage().value());
        }

        @Test
        @DisplayName("Should verify category type is 'Currency'")
        void shouldSetCorrectCategoryTypeForCurrency() {
            // Arrange
            Map<ValidatedCurrency, Money> allocation = Map.of(
                USD, Money.of(10000, "USD")
            );
            Money totalValue = Money.of(10000, "USD");

            // Act
            AllocationResponse response = AllocationMapper.toResponseFromCurrency(allocation, totalValue, TEST_TIME);

            // Assert
            AllocationDetail detail = response.getAllocations().get("USD");
            assertEquals("Currency", detail.getCategoryType());
            assertEquals("USD", detail.getCategory());
        }

        @Test
        @DisplayName("Should handle 70/20/10 currency split")
        void shouldHandleComplexCurrencySplit() {
            // Arrange
            Map<ValidatedCurrency, Money> allocation = Map.of(
                USD, Money.of(new BigDecimal("7000.00"), USD),
                CAD, Money.of(new BigDecimal("2000.00"), USD),
                EUR, Money.of(new BigDecimal("1000.00"), USD)
            );
            Money totalValue = Money.of(new BigDecimal("10000.00"), USD);

            // Act
            AllocationResponse response = AllocationMapper.toResponseFromCurrency(allocation, totalValue, TEST_TIME);

            // Assert
            assertEquals(new BigDecimal("70.00").setScale(PRECISION), 
                response.getAllocations().get("USD").getPercentage().value());
            assertEquals(new BigDecimal("20.00").setScale(PRECISION), 
                response.getAllocations().get("CAD").getPercentage().value());
            assertEquals(new BigDecimal("10.00").setScale(PRECISION), 
                response.getAllocations().get("EUR").getPercentage().value());
        }

        @Test
        @DisplayName("Should handle empty currency allocation")
        void shouldHandleEmptyCurrencyAllocation() {
            // Arrange
            Map<ValidatedCurrency, Money> allocation = new HashMap<>();
            Money totalValue = Money.of(10000, "USD");

            // Act
            AllocationResponse response = AllocationMapper.toResponseFromCurrency(allocation, totalValue, TEST_TIME);

            // Assert
            assertTrue(response.getAllocations().isEmpty());
            assertEquals(totalValue, response.getTotalValue());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle zero total value by returning zero percentages")
        void shouldHandleZeroTotalValue() {
            // Arrange
            Map<AssetType, Money> allocation = Map.of(
                AssetType.STOCK, Money.of(1000, "USD")
            );
            Money totalValue = Money.ZERO("USD");

            // Act
            AllocationResponse response = AllocationMapper.toResponseFromAssetType(allocation, totalValue, TEST_TIME);

            // Assert
            assertEquals(new BigDecimal("0.00").setScale(PRECISION), 
                response.getAllocations().get("STOCK").getPercentage().value());
        }

        @Test
        @DisplayName("Should handle null total value")
        void shouldHandleNullTotalValue() {
            // Arrange
            Map<AssetType, Money> allocation = Map.of(
                AssetType.STOCK, Money.of(1000, "USD")
            );

            // Act
            AllocationResponse response = AllocationMapper.toResponseFromAssetType(allocation, null, TEST_TIME);

            // Assert
            assertEquals(Money.ZERO("USD"), response.getTotalValue());
        }

        @Test
        @DisplayName("Should use current time when asOfDate is null")
        void shouldUseCurrentTimeWhenAsOfDateIsNull() {
            // Arrange
            Map<AssetType, Money> allocation = Map.of(
                AssetType.STOCK, Money.of(1000, "USD")
            );
            Money totalValue = Money.of(1000, "USD");
            Instant before = Instant.now();

            // Act
            AllocationResponse response = AllocationMapper.toResponseFromAssetType(allocation, totalValue, null);

            // Assert
            Instant after = Instant.now();
            assertNotNull(response.getAsOfDate());
            assertTrue(response.getAsOfDate().isAfter(before.minusSeconds(1)));
            assertTrue(response.getAsOfDate().isBefore(after.plusSeconds(1)));
        }

        @Test
        @DisplayName("Should handle very small percentages with correct precision")
        void shouldHandleSmallPercentages() {
            // Arrange
            Map<AssetType, Money> allocation = Map.of(
                AssetType.STOCK, Money.of(new BigDecimal("9999.00"), USD),
                AssetType.CRYPTO, Money.of(new BigDecimal("1.00"), USD)
            );
            Money totalValue = Money.of(new BigDecimal("10000.00"), USD);

            // Act
            AllocationResponse response = AllocationMapper.toResponseFromAssetType(allocation, totalValue, TEST_TIME);

            // Assert
            assertEquals(new BigDecimal("99.99").setScale(PRECISION), 
                response.getAllocations().get("STOCK").getPercentage().value());
            assertEquals(new BigDecimal("0.01").setScale(PRECISION), 
                response.getAllocations().get("CRYPTO").getPercentage().value());
        }

        @Test
        @DisplayName("Should handle allocation values summing to total")
        void shouldVerifyPercentagesSumTo100() {
            // Arrange
            Map<AssetType, Money> allocation = Map.of(
                AssetType.STOCK, Money.of(new BigDecimal("3333.33"), USD),
                AssetType.ETF, Money.of(new BigDecimal("3333.33"), USD),
                AssetType.CRYPTO, Money.of(new BigDecimal("3333.34"), USD)
            );
            Money totalValue = Money.of(new BigDecimal("10000.00"), USD);

            // Act
            AllocationResponse response = AllocationMapper.toResponseFromAssetType(allocation, totalValue, TEST_TIME);

            // Assert
            BigDecimal totalPercentage = response.getAllocations().values().stream()
                .map(detail -> detail.getPercentage().value())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Should be very close to 100.00 (allowing for rounding)
            assertTrue(totalPercentage.compareTo(new BigDecimal("99.99")) >= 0);
            assertTrue(totalPercentage.compareTo(new BigDecimal("100.01")) <= 0);
        }
    }

    @Nested
    @DisplayName("Deprecated String-Based Method Tests")
    class DeprecatedMethodTests {

        @Test
        @DisplayName("Deprecated method should still work for backward compatibility")
        void deprecatedMethodShouldWork() {
            // Arrange
            Map<String, Money> allocation = Map.of(
                "STOCK", Money.of(6000, "USD"),
                "ETF", Money.of(3000, "USD"),
                "CRYPTO", Money.of(1000, "USD")
            );
            Money totalValue = Money.of(10000, "USD");

            // Act
            @SuppressWarnings("deprecation")
            AllocationResponse response = AllocationMapper.toResponse(allocation, totalValue, TEST_TIME);

            // Assert
            assertEquals(new BigDecimal("60.00").setScale(PRECISION), 
                response.getAllocations().get("STOCK").getPercentage().value());
            assertEquals("Asset Type", response.getAllocations().get("STOCK").getCategoryType());
        }

        @Test
        @DisplayName("Deprecated method should handle empty allocation")
        void deprecatedMethodShouldHandleEmpty() {
            // Arrange
            Map<String, Money> allocation = new HashMap<>();
            Money totalValue = Money.of(10000, "USD");

            // Act
            @SuppressWarnings("deprecation")
            AllocationResponse response = AllocationMapper.toResponse(allocation, totalValue, TEST_TIME);

            // Assert
            assertTrue(response.getAllocations().isEmpty());
        }
    }
}