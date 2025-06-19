package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.PortfolioCurrency;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionType;

public class PortfolioTest {

    // test the constructor method. it should generate a Portfolio class with the
    // info I provided

    @Test
    void newPortfolio_shouldBeCreatedWithValidDataAndGeneratedId() {
        UUID userUuid = UUID.randomUUID();
        String name = "My First Portfolio";
        String description = "My primary Investment portfolio";
        boolean isPrimary = true;
        PortfolioCurrency currencyPref = new PortfolioCurrency("CAD", "$");

        Portfolio portfolio = new Portfolio(userUuid, name, description, currencyPref, isPrimary);
        // Assert
        assertNotNull(portfolio.getPortfolioId(), "Portfolio ID should be generated.");
        assertNotNull(portfolio.getCreatedAt(), "Creation timestamp should be set.");
        assertNotNull(portfolio.getUpdatedAt(), "Update timestamp should be set.");
        assertEquals(userUuid, portfolio.getUserId());
        assertEquals(name, portfolio.getName());
        assertEquals(currencyPref, portfolio.getCurrencyPreference());
        assertEquals(description, portfolio.getDescription());
        assertEquals(isPrimary, portfolio.isPrimary());
        assertTrue(portfolio.getAssets().isEmpty(), "New portfolio should have no asset holdings.");
        assertTrue(portfolio.getLiabilities().isEmpty(), "New portfolio should have no liabilities.");
        assertTrue(portfolio.getTransactions().isEmpty(), "New portfolio should have no transactions.");
    }

    @Test
    void newPortfolio_invalidUserUUID() {
        UUID userUuid = null;
        String name = "My First Portfolio";
        String description = "My primary Investment portfolio";
        boolean isPrimary = false;
        PortfolioCurrency currencyPref = new PortfolioCurrency("CAD", "$");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Portfolio portfolio = new Portfolio(userUuid, name, description, currencyPref, isPrimary);
            assertNotNull(portfolio.getUserId());
        });

        assertTrue(exception.getMessage().contains("Portfolio must have a User assigned to it."));
    }

    @Test
    void testPortfolio_correctPortfolioRenaming() {
        UUID userUuid = UUID.randomUUID();
        String name = "My First Portfolio";
        String description = "My primary Investment portfolio";
        boolean isPrimary = true;
        PortfolioCurrency currencyPref = new PortfolioCurrency("CAD", "$");

        Portfolio portfolio = new Portfolio(userUuid, name, description, currencyPref, isPrimary);

        assertNotNull(portfolio.getName());
        portfolio.renamePortfolio("New Name");
        assertEquals("New Name", portfolio.getName());
    }

    @Test
    void newPortfolio_invalidPortfolioTitle() {
        UUID userUuid = UUID.randomUUID();
        String name = null;
        String description = "My primary Investment portfolio";
        boolean isPrimary = false;
        PortfolioCurrency currencyPref = new PortfolioCurrency("CAD", "$");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Portfolio portfolio = new Portfolio(userUuid, name, description, currencyPref, isPrimary);
            assertNotNull(portfolio.getName());
        });

        assertTrue(exception.getMessage().contains("Portfolio must be given a name."));
    }

    @Test
    void renamePortfolio_shouldThrowException_whenNewNameIsNull() {
        // Arrange
        String description = "My primary Investment portfolio";
        PortfolioCurrency currencyPref = new PortfolioCurrency("CAD", "$");

        Portfolio portfolio = new Portfolio(UUID.randomUUID(), "Old Name", description, currencyPref, false);

        // Act & Assert
        // Expecting an IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            portfolio.renamePortfolio(null);
        }, "Renaming to null should throw IllegalArgumentException.");
    }

    @Test
    void renamePortfolio_shouldThrowException_whenNewNameIsEmpty() {
        // Arrange
        String description = "My primary Investment portfolio";
        PortfolioCurrency currencyPref = new PortfolioCurrency("CAD", "$");

        Portfolio portfolio = new Portfolio(UUID.randomUUID(), "Old Name", description, currencyPref, false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            portfolio.renamePortfolio("");
        }, "Renaming to empty string should throw IllegalArgumentException.");
    }

    @Test
    void renamePortfolio_shouldThrowException_whenNewNameIsWhitespace() {
        // Arrange
        String description = "My primary Investment portfolio";
        PortfolioCurrency currencyPref = new PortfolioCurrency("CAD", "$");

        Portfolio portfolio = new Portfolio(UUID.randomUUID(), "Old Name", description, currencyPref, false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            portfolio.renamePortfolio("   ");
        }, "Renaming to whitespace should throw IllegalArgumentException.");
    }

    @Test
    void newPortfolio_invalidCurrency() {
        UUID userUuid = UUID.randomUUID();
        String name = "Some title";
        String description = "My primary Investment portfolio";
        boolean isPrimary = false;
        PortfolioCurrency currencyPref = null;

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Portfolio portfolio = new Portfolio(userUuid, name, description, currencyPref, isPrimary);
            assertNotNull(portfolio.getName());
        });

        assertTrue(exception.getMessage().contains("Portfolio must have a currency preference."));
    }

    @Test
    void test_IsPrimarySet() {
        UUID userUuid = UUID.randomUUID();
        String name = "My First Portfolio";
        String description = "My primary Investment portfolio";
        boolean isPrimary = true;
        PortfolioCurrency currencyPref = new PortfolioCurrency("CAD", "$");

        Portfolio portfolio = new Portfolio(userUuid, name, description, currencyPref, isPrimary);

        assertTrue(portfolio.isPrimary() == isPrimary);
        isPrimary = false;

        portfolio.setPrimaryStatus(isPrimary);
        assertTrue(portfolio.isPrimary() == isPrimary);
    }

    @Test
    void recordAssetPurchase_shouldAddNewAssetHoldingAndTransaction_whenAssetDoesNotExist() {
        // Arrange
        Portfolio portfolio = new Portfolio(UUID.randomUUID(), "My Portfolio", "", new PortfolioCurrency("USD", "$"), false);
        AssetIdentifier assetId = new AssetIdentifier("MSFT", "NASDAQ", null, "Microsoft Corp.");
        BigDecimal quantity = new BigDecimal("5");
        Money costBasisPerUnit = new Money(new BigDecimal("300"), new PortfolioCurrency("USD", "$"));
        LocalDate acquisitionDate = LocalDate.now();
        Money currentMarketPrice = new Money(new BigDecimal("305"), new PortfolioCurrency("USD", "$")); // Not directly used in AssetHolding logic

        // Act
        AssetHolding newHolding = portfolio.recordAssetPurchase(assetId, quantity, costBasisPerUnit, acquisitionDate, currentMarketPrice);

        // Assert
        assertNotNull(newHolding);
        assertEquals(1, portfolio.getAssets().size());
        assertEquals(newHolding, portfolio.getAssets().get(0)); // Verify it was added

        // Assert AssetHolding state
        assertEquals(assetId, newHolding.getAssetIdentifier());
        assertEquals(quantity, newHolding.getQuantity());
        // Total cost: 5 * 300 = 1500
        assertEquals(new Money(new BigDecimal("1500.0000"), new PortfolioCurrency("USD", "$")), newHolding.getCostBasis());

        // Assert Transaction creation (this implicitly tests Transaction constructor
        // and its linkage)
        assertEquals(1, portfolio.getTransactions().size());
        Transaction transaction = portfolio.getTransactions().get(0);
        assertEquals(TransactionType.BUY, transaction.getTransactionType());
        assertEquals(new Money(new BigDecimal("1500.0000"), new PortfolioCurrency("USD", "$")), transaction.getAmount());
        assertEquals(newHolding.getAssetHoldingId(), transaction.getAssetHoldingId());
        assertEquals(quantity, transaction.getQuantity());
        assertEquals(costBasisPerUnit.amount(), transaction.getPricePerUnit()); // Price per unit on transaction
        assertNotNull(transaction.getTransactionId());
    }
}
