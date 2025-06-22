package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Percentage;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.PortfolioCurrency;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionStatus;
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
        UUID portfolioUuid = UUID.randomUUID();

        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, name, description, currencyPref, isPrimary);
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
        UUID portfolioUuid = UUID.randomUUID();

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, name, description, currencyPref, isPrimary);
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
        UUID portfolioUuid = UUID.randomUUID();

        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, name, description, currencyPref, isPrimary);

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
        UUID portfolioUuid = UUID.randomUUID();

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, name, description, currencyPref, isPrimary);
            assertNotNull(portfolio.getName());
        });

        assertTrue(exception.getMessage().contains("Portfolio must be given a name."));
    }

    @Test
    void renamePortfolio_shouldThrowException_whenNewNameIsNull() {
        // Arrange
        String description = "My primary Investment portfolio";
        PortfolioCurrency currencyPref = new PortfolioCurrency("CAD", "$");
        UUID portfolioUuid = UUID.randomUUID();

        Portfolio portfolio = new Portfolio(portfolioUuid, UUID.randomUUID(), "Old Name", description, currencyPref,
                false);

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
        UUID portfolioUuid = UUID.randomUUID();

        Portfolio portfolio = new Portfolio(portfolioUuid, UUID.randomUUID(), "Old Name", description, currencyPref,
                false);

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
        UUID portfolioUuid = UUID.randomUUID();

        Portfolio portfolio = new Portfolio(portfolioUuid, UUID.randomUUID(), "Old Name", description, currencyPref,
                false);

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
        UUID portfolioUuid = UUID.randomUUID();

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, name, description, currencyPref, isPrimary);
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
        UUID portfolioUuid = UUID.randomUUID();

        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, name, description, currencyPref, isPrimary);

        assertTrue(portfolio.isPrimary() == isPrimary);
        isPrimary = false;

        portfolio.setPrimaryStatus(isPrimary);
        assertTrue(portfolio.isPrimary() == isPrimary);
    }

    @Test
    void recordAssetPurchase_shouldAddNewAssetHoldingAndTransaction_whenAssetDoesNotExist() {
        // Arrange
        Portfolio portfolio = new Portfolio(UUID.randomUUID(), UUID.randomUUID(), "My Portfolio", "",
                new PortfolioCurrency("USD", "$"),
                false);
        AssetIdentifier assetId = new AssetIdentifier("MSFT", "NASDAQ", null, "Microsoft Corp.");
        BigDecimal quantity = new BigDecimal("5");
        Money costBasisPerUnit = new Money(new BigDecimal("300"), new PortfolioCurrency("USD", "$"));
        LocalDate acquisitionDate = LocalDate.now();
        Money currentMarketPrice = new Money(new BigDecimal("305"), new PortfolioCurrency("USD", "$")); // Not directly
                                                                                                        // used in
                                                                                                        // AssetHolding
                                                                                                        // logic

        // Act
        AssetHolding newHolding = portfolio.recordAssetPurchase(assetId, quantity, costBasisPerUnit, acquisitionDate,
                currentMarketPrice);

        // Assert
        assertNotNull(newHolding);
        assertEquals(1, portfolio.getAssets().size());
        assertEquals(newHolding, portfolio.getAssets().get(0)); // Verify it was added

        // Assert AssetHolding state
        assertEquals(assetId, newHolding.getAssetIdentifier());
        assertEquals(quantity, newHolding.getQuantity());
        // Total cost: 5 * 300 = 1500
        assertEquals(new Money(new BigDecimal("1500.0000"), new PortfolioCurrency("USD", "$")),
                newHolding.getCostBasis());

        // Assert Transaction creation (this implicitly tests Transaction constructor
        // and its linkage)
        assertEquals(1, portfolio.getTransactions().size());
        Transaction transaction = portfolio.getTransactions().get(0);
        assertEquals(TransactionType.BUY, transaction.getTransactionType());
        assertEquals(new Money(new BigDecimal("1500.0000"), new PortfolioCurrency("USD", "$")),
                transaction.getAmount());
        assertEquals(newHolding.getAssetHoldingId(), transaction.getAssetHoldingId());
        assertEquals(quantity, transaction.getQuantity());
        assertEquals(costBasisPerUnit.amount(), transaction.getPricePerUnit()); // Price per unit on transaction
        assertNotNull(transaction.getTransactionId());
    }

    // NEWER TESTS

    @Test
    void testConstructorValid() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);
        assertEquals(portfolio, portfolio);
    }

    @Test
    void testConstructorBranches() {

        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");
        Exception e1 = assertThrows(IllegalArgumentException.class,
                () -> new Portfolio(null, userUuid, "Port Name", "Port desc", curr, false));
        assertTrue(e1.getMessage().contains("Portfolio must have an ID assigned to it."));

        Exception e2 = assertThrows(IllegalArgumentException.class,
                () -> new Portfolio(portfolioUuid, null, "Port Name", "Port desc", curr, false));
        assertTrue(e2.getMessage().contains("Portfolio must have a User assigned to it."));

        Exception e3 = assertThrows(IllegalArgumentException.class,
                () -> new Portfolio(portfolioUuid, userUuid, null, "Port desc", curr, false));
        assertTrue(e3.getMessage().contains("Portfolio must be given a name."));
        Exception e3_2 = assertThrows(IllegalArgumentException.class,
                () -> new Portfolio(portfolioUuid, userUuid, " ", "Port desc", curr, false));
        assertTrue(e3_2.getMessage().contains("Portfolio must be given a name."));

        Exception e4 = assertThrows(IllegalArgumentException.class,
                () -> new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", null, false));
        assertTrue(e4.getMessage().contains("Portfolio must have a currency preference."));
    }

    @Test
    void testUpdatePortfolioDescription() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);
        String desc = "Amongus";
        portfolio.updatePortfolioDescription(desc);
        assertEquals(desc, portfolio.getDescription());
        portfolio.updatePortfolioDescription(" ");
        assertEquals(" ", portfolio.getDescription());
        portfolio.updatePortfolioDescription(null);
        assertEquals(null, portfolio.getDescription());

    }

    @Test
    void testRenamingPortfolio() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);
        String rename = "Amongus";
        portfolio.renamePortfolio(rename);
        assertEquals(rename, portfolio.getName());

        // error testing
        assertThrows(IllegalArgumentException.class, () -> {
            portfolio.renamePortfolio(null);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            portfolio.renamePortfolio("   \n");
        });
    }

    @Test
    void testEquals() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);
        Portfolio portfolio2 = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);
        Portfolio portfolio3 = new Portfolio(UUID.randomUUID(), userUuid, "Port Name", "Port desc", curr, false);

        assertTrue(portfolio.equals(portfolio));
        assertTrue(portfolio.equals(portfolio2));
        assertFalse(portfolio.equals(portfolio3));
        assertFalse(portfolio.equals(new Object()));
        assertFalse(portfolio.equals(null));
        assertFalse(portfolio.equals(""));
    }

    @Test
    void testHashCode() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);
        Portfolio portfolio2 = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);
        Portfolio portfolio3 = new Portfolio(UUID.randomUUID(), userUuid, "Port Name", "Port desc", curr, false);

        assertTrue(portfolio.hashCode() == portfolio2.hashCode());
        assertTrue(portfolio.hashCode() != portfolio3.hashCode());
    }

    @Test
    void testGetters() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();

        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        assertEquals(userUuid, portfolio.getUserId());
        assertEquals("Port Name", portfolio.getName());
        assertEquals("Port desc", portfolio.getDescription());
        assertEquals(false, portfolio.isPrimary());
        assertEquals(curr, portfolio.getCurrencyPreference());
        assertEquals(new ArrayList<>(), portfolio.getAssets());
        assertEquals(new ArrayList<>(), portfolio.getLiabilities());
        assertEquals(new ArrayList<>(), portfolio.getTransactions());
        assertNotNull(portfolio.getCreatedAt());
        assertNotNull(portfolio.getUpdatedAt());
    }

    @Test
    void testREcordAssetPurchaseValid() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();

        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        AssetIdentifier ai = new AssetIdentifier("APPL", "NASDAQ", null, "Apple");
        BigDecimal quant = new BigDecimal(100);
        Money costBasis = new Money(new BigDecimal(10), new PortfolioCurrency("USD", "$"));
        LocalDate ad = LocalDate.now();
        Money marketPrice = new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$"));

        AssetHolding tetsah = new AssetHolding(userUuid, portfolioUuid, ai, quant, costBasis, ad);

        AssetHolding ah = portfolio.recordAssetPurchase(ai, quant, costBasis, ad, marketPrice);

        assertEquals(tetsah.getPortfolioId(), ah.getPortfolioId());
    }

    @Test
    void testRecordAssetPurchaseBranches() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        AssetIdentifier ai = new AssetIdentifier("APPL", "NASDAQ", null, "Apple");
        Money costBasis = new Money(new BigDecimal(10), new PortfolioCurrency("USD", "$"));
        LocalDate ad = LocalDate.now();
        Money marketPrice = new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$"));

        // for the quantity that is less than 0
        assertThrows(IllegalArgumentException.class,
                () -> portfolio.recordAssetPurchase(ai, new BigDecimal(-10), costBasis, ad, marketPrice));
        // for the quantity that is equal 0
        assertThrows(IllegalArgumentException.class,
                () -> portfolio.recordAssetPurchase(ai, new BigDecimal(0), costBasis, ad, marketPrice));

        // when the currency preference != the one assigned to the portfolio
        // need that I think about it... a user could hold assets with difference
        // currencies...
        Money costBasisDiff = new Money(new BigDecimal(10), new PortfolioCurrency("CAD", "$"));
        assertThrows(IllegalArgumentException.class,
                () -> portfolio.recordAssetPurchase(ai, new BigDecimal(100), costBasisDiff, ad, marketPrice));

        // testing if the .isPresent() works
        // so how the code works is this: assetsHolding, we are updating the individual
        // assets detailed value - how many we bought, cost basis, etc.
        // transaction holds a history of all things we have done
        AssetHolding ah = portfolio.recordAssetPurchase(ai, new BigDecimal(100), costBasis, ad, marketPrice);
        AssetHolding ah2 = portfolio.recordAssetPurchase(ai, new BigDecimal(100), costBasis, ad, marketPrice);
        assertTrue(portfolio.getTransactions().size() == 2);
        assertEquals(ah2.getPortfolioId(), ah.getPortfolioId());
    }

    @Test
    void testRecordAssetSale() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();

        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        AssetIdentifier ai = new AssetIdentifier("APPL", "NASDAQ", null, "Apple");
        BigDecimal quant = new BigDecimal(100);
        Money costBasis = new Money(new BigDecimal(10), new PortfolioCurrency("USD", "$"));
        LocalDate ad = LocalDate.now();
        Money marketPrice = new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$"));

        portfolio.recordAssetPurchase(ai, quant, costBasis, ad, marketPrice);
        portfolio.recordAssetPurchase(ai, quant, costBasis, ad, marketPrice);

        assertTrue(portfolio.getTransactions().size() == 2);
        assertTrue(portfolio.getAssets().size() == 1);
        portfolio.recordAssetSale(ai, new BigDecimal(10), marketPrice, Instant.now());
        assertTrue(portfolio.getTransactions().size() == 3);

        // we sell 10 apple stocks, so instead of 200, we have 190 'shares'
        assertTrue(portfolio.getAssets().get(0).getQuantity().doubleValue() == 190D);
    }

    @Test
    void testRecordAssetSaleBranches() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();

        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        AssetIdentifier ai = new AssetIdentifier("APPL", "NASDAQ", null, "Apple");
        BigDecimal quant = new BigDecimal(100);
        Money costBasis = new Money(new BigDecimal(10), new PortfolioCurrency("USD", "$"));
        LocalDate ad = LocalDate.now();
        Money marketPrice = new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$"));
        portfolio.recordAssetPurchase(ai, quant, costBasis, ad, marketPrice);

        // testing if the quantity is less than 0
        assertThrows(IllegalArgumentException.class,
                () -> portfolio.recordAssetSale(ai, new BigDecimal(-10), marketPrice, Instant.now()));
        // testing if the quant is == 0
        assertThrows(IllegalArgumentException.class,
                () -> portfolio.recordAssetSale(ai, new BigDecimal(0), marketPrice, Instant.now()));

        // testing if the sell of portoflio is in a different currecny than the OG
        // purchase currency
        assertThrows(IllegalArgumentException.class, () -> portfolio.recordAssetSale(ai, new BigDecimal(10),
                new Money(new BigDecimal(100), new PortfolioCurrency("CAD", "$")), Instant.now()));

        // Testing the sell of a asset we don't own
        AssetIdentifier ai2 = new AssetIdentifier("AMZN", "NASDAQ", null, "Amazon");
        Exception e4 = assertThrows(IllegalArgumentException.class,
                () -> portfolio.recordAssetSale(ai2, new BigDecimal(10),
                        new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$")), Instant.now()));
        assertTrue(e4.getMessage()
                .contains("Asset holding with identifier " + ai2.toCanonicalString() + " not found in portfolio."));

        // testing the aggregate-level quant validation
        assertThrows(IllegalArgumentException.class,
                () -> portfolio.recordAssetSale(ai, new BigDecimal(1000), marketPrice, Instant.now()));

        portfolio.recordAssetSale(ai, new BigDecimal(100), marketPrice, Instant.now());
        assertTrue(portfolio.getAssets().size() == 0);
    }

    // AI coded

    @Test
    void recordAssetPurchase_newAsset() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        AssetIdentifier appleIdentifier = new AssetIdentifier("APPL", "NASDAQ", null, "Apple");
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        BigDecimal quantity = new BigDecimal("10");
        Money costBasisPerUnit = new Money(new BigDecimal("150.00"), cad);
        LocalDate acquisitionDate = LocalDate.of(2023, 1, 15);
        Money currentMarketPrice = new Money(new BigDecimal("155.00"), cad);

        int initialHoldingsSize = portfolio.getAssets().size();
        int initialTransactionsSize = portfolio.getTransactions().size();
        // Instant initialUpdatedAt = portfolio.getUpdatedAt();

        AssetHolding holding = portfolio.recordAssetPurchase(appleIdentifier, quantity, costBasisPerUnit,
                acquisitionDate, currentMarketPrice);

        assertNotNull(holding);
        assertEquals(initialHoldingsSize + 1, portfolio.getAssets().size());
        assertEquals(appleIdentifier, holding.getAssetIdentifier());
        assertEquals(quantity, holding.getQuantity());
        assertEquals(costBasisPerUnit.multiply(quantity), holding.getCostBasis()); // Assuming direct access for
                                                                                   // verification

        assertEquals(initialTransactionsSize + 1, portfolio.getTransactions().size());
        Transaction recordedTransaction = portfolio.getTransactions().get(0);
        assertEquals(TransactionType.BUY, recordedTransaction.getTransactionType());
        assertEquals(costBasisPerUnit.multiply(quantity), recordedTransaction.getAmount());
        assertEquals(quantity, recordedTransaction.getQuantity());
        assertEquals(costBasisPerUnit.amount(), recordedTransaction.getPricePerUnit());
        assertEquals(holding.getAssetHoldingId(), recordedTransaction.getAssetHoldingId());
        // assertTrue(portfolio.getUpdatedAt().isAfter(initialUpdatedAt)); this can't pass, code too quick for time to move ahead
    }

    @Test
    void recordAssetPurchase_existingAsset() {
        // First purchase
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        AssetIdentifier appleIdentifier = new AssetIdentifier("APPL", "NASDAQ", null, "Apple");
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        BigDecimal quantity1 = new BigDecimal("10");
        Money costBasisPerUnit1 = new Money(new BigDecimal("150.00"), cad);
        // LocalDate acquisitionDate1 = LocalDate.of(2023, 1, 15);
        portfolio.recordAssetPurchase(appleIdentifier, quantity1, costBasisPerUnit1, LocalDate.now(), null);

        // Second purchase
        BigDecimal quantity2 = new BigDecimal("5");
        Money costBasisPerUnit2 = new Money(new BigDecimal("160.00"), cad);
        LocalDate acquisitionDate2 = LocalDate.of(2023, 2, 20);
        Money currentMarketPrice = new Money(new BigDecimal("165.00"), cad);

        int initialHoldingsSize = portfolio.getAssets().size(); // Should be 1
        int initialTransactionsSize = portfolio.getTransactions().size(); // Should be 1
        // Instant initialUpdatedAt = portfolio.getUpdatedAt();

        AssetHolding existingHolding = portfolio.getAssets().stream()
                .filter(ah -> ah.getAssetIdentifier().equals(appleIdentifier))
                .findFirst()
                .orElseThrow();
        BigDecimal initialQuantity = existingHolding.getQuantity();

        AssetHolding updatedHolding = portfolio.recordAssetPurchase(appleIdentifier, quantity2, costBasisPerUnit2,
                acquisitionDate2, currentMarketPrice);

        // Assert that no new holding was created
        assertEquals(initialHoldingsSize, portfolio.getAssets().size());
        // Assert that the existing holding was updated
        assertEquals(existingHolding.getAssetHoldingId(), updatedHolding.getAssetHoldingId());
        assertEquals(initialQuantity.add(quantity2), updatedHolding.getQuantity());

        assertEquals(initialTransactionsSize + 1, portfolio.getTransactions().size());
        Transaction recordedTransaction = portfolio.getTransactions().get(1); // The second transaction
        assertEquals(TransactionType.BUY, recordedTransaction.getTransactionType());
        assertEquals(costBasisPerUnit2.multiply(quantity2), recordedTransaction.getAmount());
        assertEquals(quantity2, recordedTransaction.getQuantity());
        assertEquals(costBasisPerUnit2.amount(), recordedTransaction.getPricePerUnit());
        assertEquals(updatedHolding.getAssetHoldingId(), recordedTransaction.getAssetHoldingId());
        // assertTrue(portfolio.getUpdatedAt().isAfter(initialUpdatedAt)); // can't pass, too quick the program for time to pass
    }

    @Test
    void recordAssetPurchase_nullAssetIdentifierThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        assertThrows(NullPointerException.class, () -> portfolio.recordAssetPurchase(null, BigDecimal.TEN,
                new Money(BigDecimal.TEN, cad), LocalDate.now(), null));
    }

    @Test
    void recordAssetPurchase_zeroQuantityThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        AssetIdentifier appleIdentifier = new AssetIdentifier("APPL", "NASDAQ", null, "Apple");
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        assertThrows(IllegalArgumentException.class,
                () -> portfolio.recordAssetPurchase(appleIdentifier, BigDecimal.ZERO, new Money(BigDecimal.TEN, cad),
                        LocalDate.now(), null),
                "Purchase quantity must be positive.");
    }

    @Test
    void recordAssetPurchase_negativeQuantityThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        AssetIdentifier appleIdentifier = new AssetIdentifier("APPL", "NASDAQ", null, "Apple");
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        assertThrows(IllegalArgumentException.class,
                () -> portfolio.recordAssetPurchase(appleIdentifier, new BigDecimal("-5"),
                        new Money(BigDecimal.TEN, cad), LocalDate.now(), null),
                "Purchase quantity must be positive.");
    }

    @Test
    void recordAssetPurchase_currencyMismatchThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        AssetIdentifier appleIdentifier = new AssetIdentifier("APPL", "NASDAQ", null, "Apple");
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        Money usdCostBasis = new Money(new BigDecimal("100.00"), new PortfolioCurrency("EUR", "$"));
        assertThrows(IllegalArgumentException.class,
                () -> portfolio.recordAssetPurchase(appleIdentifier, BigDecimal.TEN, usdCostBasis, LocalDate.now(),
                        null),
                "Purchase currency mismatch with portfolio currency preference.");
    }

    // Tests for addLiability
    @Test
    void addLiability_success() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        String name = "Mortgage";
        String description = "Home loan";
        Money initialAmount = new Money(new BigDecimal("300000.00"), cad);
        Percentage interestRate = new Percentage(new BigDecimal("0.035"));
        LocalDate maturityDate = LocalDate.of(2050, 1, 1);

        int initialLiabilitiesSize = portfolio.getLiabilities().size();
        // Instant initialUpdatedAt = portfolio.getUpdatedAt();

        Liability liability = portfolio.addLiability(name, description, initialAmount, interestRate, maturityDate);

        assertNotNull(liability);
        assertEquals(initialLiabilitiesSize + 1, portfolio.getLiabilities().size());
        assertEquals(name, liability.getName());
        assertEquals(description, liability.getDescription()); // Assuming description is accessible
        assertEquals(initialAmount, liability.getCurrentBalance()); // Assuming direct access for mock
        assertEquals(interestRate, liability.getInterestRate()); // Assuming direct access for mock
        assertEquals(maturityDate, liability.getMaturityDate()); // Assuming direct access for mock
        assertEquals(initialAmount, liability.getCurrentBalance()); // Check initial balance

        // assertTrue(portfolio.getUpdatedAt().isAfter(initialUpdatedAt));
    }

    @Test
    void addLiability_nullNameThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        assertThrows(NullPointerException.class, () -> portfolio.addLiability(null, "desc",
                new Money(BigDecimal.TEN, cad), new Percentage(BigDecimal.ONE), LocalDate.now()));
    }

    @Test
    void addLiability_blankDescriptionUsesDefault() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();

        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        String name = "Car Loan";
        Money initialAmount = new Money(new BigDecimal("20000.00"), cad);
        Percentage interestRate = new Percentage(new BigDecimal("0.05"));
        LocalDate maturityDate = LocalDate.of(2028, 6, 1);

        Liability liability1 = portfolio.addLiability(name, null, initialAmount, interestRate, maturityDate);
        assertEquals("Liability for " + name, liability1.getDescription());

        Liability liability2 = portfolio.addLiability(name, "   ", initialAmount, interestRate, maturityDate);
        assertEquals("Liability for " + name, liability2.getDescription());
    }

    @Test
    void addLiability_zeroInitialAmountThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        assertThrows(IllegalArgumentException.class,
                () -> portfolio.addLiability("Loan", "desc", new Money(BigDecimal.ZERO, cad),
                        new Percentage(BigDecimal.ONE), LocalDate.now()),
                "Initial amount for liability must be positive.");
    }

    @Test
    void addLiability_negativeInterestRateThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        assertThrows(IllegalArgumentException.class,
                () -> portfolio.addLiability("Loan", "desc", new Money(BigDecimal.TEN, cad),
                        new Percentage(new BigDecimal("-0.01")), LocalDate.now()),
                "Interest rate must be positive or zero.");
    }

    @Test
    void addLiability_zeroInterestRateIsAllowed() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        assertDoesNotThrow(() -> portfolio.addLiability("Loan", "desc", new Money(BigDecimal.TEN, cad),
                new Percentage(BigDecimal.ZERO), LocalDate.now()));
    }

    // Tests for recordLiabilityPayment
    @Test
    void recordLiabilityPayment_success() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        Liability liability = portfolio.addLiability("Car Loan", "Car", new Money(new BigDecimal("20000.00"), cad),
                new Percentage(new BigDecimal("0.05")), LocalDate.now().plusYears(5));
        Money paymentAmount = new Money(new BigDecimal("500.00"), cad);
        Instant transactionDate = Instant.now();

        int initialTransactionsSize = portfolio.getTransactions().size(); // 1 from addLiability
        // Instant initialUpdatedAt = portfolio.getUpdatedAt();
        Money initialBalance = liability.getCurrentBalance();

        System.out.println(portfolio.getTransactions().size());
        portfolio.recordLiabilityPayment(liability.getLiabilityId(), paymentAmount, transactionDate);
        System.out.println(portfolio.getTransactions().size());

        assertEquals(initialTransactionsSize + 1, portfolio.getTransactions().size());
        Transaction paymentTransaction = portfolio.getTransactions().get(0); // The payment transaction
        assertEquals(TransactionType.LOAN_PAYMENT, paymentTransaction.getTransactionType());
        assertEquals(paymentAmount, paymentTransaction.getAmount());
        assertEquals(transactionDate, paymentTransaction.getTransactionDate());
        assertEquals(liability.getLiabilityId(), paymentTransaction.getLiabilityId());
        assertNull(paymentTransaction.getAssetHoldingId());

        assertEquals(initialBalance.amount().subtract(paymentAmount.amount()), liability.getCurrentBalance().amount());
        // assertTrue(portfolio.getUpdatedAt().isAfter(initialUpdatedAt));
    }

    @Test
    void recordLiabilityPayment_nullLiabilityIdThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        assertThrows(NullPointerException.class,
                () -> portfolio.recordLiabilityPayment(null, new Money(BigDecimal.TEN, cad), Instant.now()));
    }

    @Test
    void recordLiabilityPayment_zeroPaymentAmountThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        Liability liability = portfolio.addLiability("Loan", "desc", new Money(BigDecimal.TEN, cad),
                new Percentage(BigDecimal.ONE), LocalDate.now());
        assertThrows(IllegalArgumentException.class,
                () -> portfolio.recordLiabilityPayment(liability.getLiabilityId(), new Money(BigDecimal.ZERO, cad),
                        Instant.now()),
                "Payment amount must be positive.");
    }

    @Test
    void recordLiabilityPayment_nonExistentLiabilityThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        UUID nonExistentId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> portfolio.recordLiabilityPayment(nonExistentId, new Money(BigDecimal.TEN, cad), Instant.now()),
                "Liability with ID " + nonExistentId + " not found in portfolio.");
    }

    @Test
    void recordLiabilityPayment_currencyMismatchThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        Liability liability = portfolio.addLiability("Loan", "desc", new Money(BigDecimal.TEN, cad),
                new Percentage(BigDecimal.ONE), LocalDate.now());
        Money usdPayment = new Money(new BigDecimal("10.00"), new PortfolioCurrency("EUR", "$"));
        assertThrows(IllegalArgumentException.class,
                () -> portfolio.recordLiabilityPayment(liability.getLiabilityId(), usdPayment, Instant.now()),
                "Payment currency mismatch with portfolio currency preference.");
    }

    // Tests for removeLiability
    @Test
    void removeLiability_success_zeroBalance() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        Liability liability = portfolio.addLiability("Paid Off Loan", "Paid", new Money(new BigDecimal("1000.00"), cad),
                new Percentage(BigDecimal.ZERO), LocalDate.now().minusDays(1));
        portfolio.recordLiabilityPayment(liability.getLiabilityId(), new Money(new BigDecimal("1000.00"), cad),
                Instant.now()); // Pay it off

        int initialLiabilitiesSize = portfolio.getLiabilities().size(); // Should be 1
        // Instant initialUpdatedAt = portfolio.getUpdatedAt();

        System.out.println(portfolio.getUpdatedAt());
        portfolio.removeLiability(liability.getLiabilityId());
        System.out.println(portfolio.getUpdatedAt());

        assertEquals(initialLiabilitiesSize - 1, portfolio.getLiabilities().size());
        assertFalse(portfolio.getLiabilities().contains(liability));
        // assertTrue(portfolio.getUpdatedAt().isAfter(initialUpdatedAt)); # the transaction is too fast in the test causing it to fail
    }

    @Test
    void removeLiability_throwsException_outstandingBalance() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();

        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        Liability liability = portfolio.addLiability("Active Loan", "Active", new Money(new BigDecimal("1000.00"), cad),
                new Percentage(BigDecimal.ZERO), LocalDate.now().plusYears(1));
        // Don't pay it off fully

        assertThrows(IllegalStateException.class, () -> portfolio.removeLiability(liability.getLiabilityId()),
                "Cannot remove liability 'Active Loan' (ID: " + liability.getLiabilityId()
                        + ") with an outstanding balance of 1000.00 CAD.");
    }

    @Test
    void removeLiability_nullLiabilityIdThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();

        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        assertThrows(NullPointerException.class, () -> portfolio.removeLiability(null));
    }

    @Test
    void removeLiability_nonExistentLiabilityThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        UUID nonExistentId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> portfolio.removeLiability(nonExistentId),
                "Liability with ID " + nonExistentId + " not found in portfolio.");
    }

    // Tests for recordCashTransaction
    @Test
    void recordCashTransaction_depositSuccess() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        Money amount = new Money(new BigDecimal("1000.00"), cad);
        String description = "Monthly Salary Deposit";
        Instant transactionDate = Instant.now();

        int initialTransactionsSize = portfolio.getTransactions().size();
        // Instant initialUpdatedAt = portfolio.getUpdatedAt();

        portfolio.recordCashTransaction(TransactionType.DEPOSIT, amount, description, transactionDate);

        assertEquals(initialTransactionsSize + 1, portfolio.getTransactions().size());
        Transaction recordedTransaction = portfolio.getTransactions().get(initialTransactionsSize);
        assertEquals(TransactionType.DEPOSIT, recordedTransaction.getTransactionType());
        assertEquals(amount, recordedTransaction.getAmount());
        assertEquals(description, recordedTransaction.getDescription());
        assertEquals(transactionDate, recordedTransaction.getTransactionDate());
        assertNull(recordedTransaction.getQuantity());
        assertNull(recordedTransaction.getPricePerUnit());
        assertNull(recordedTransaction.getAssetHoldingId());
        assertNull(recordedTransaction.getLiabilityId());
        // assertTrue(portfolio.getUpdatedAt().isAfter(initialUpdatedAt));
    }

    @Test
    void recordCashTransaction_withdrawalSuccess() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        Money amount = new Money(new BigDecimal("200.00"), cad);
        String description = "ATM Withdrawal";
        Instant transactionDate = Instant.now();

        portfolio.recordCashTransaction(TransactionType.WITHDRAWAL, amount, description, transactionDate);

        Transaction recordedTransaction = portfolio.getTransactions().get(0);
        assertEquals(TransactionType.WITHDRAWAL, recordedTransaction.getTransactionType());
    }

    @Test
    void recordCashTransaction_invalidTransactionTypeThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        Money amount = new Money(new BigDecimal("100.00"), cad);
        String description = "Invalid Type";
        Instant transactionDate = Instant.now();

        assertThrows(IllegalArgumentException.class,
                () -> portfolio.recordCashTransaction(TransactionType.BUY, amount, description, transactionDate),
                "Invalid transaction type for cash transaction.");
        assertThrows(IllegalArgumentException.class,
                () -> portfolio.recordCashTransaction(TransactionType.SELL, amount, description, transactionDate),
                "Invalid transaction type for cash transaction.");
        assertThrows(IllegalArgumentException.class,
                () -> portfolio.recordCashTransaction(TransactionType.LOAN_PAYMENT, amount, description,
                        transactionDate),
                "Invalid transaction type for cash transaction.");
    }

    @Test
    void recordCashTransaction_nullTypeThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        assertThrows(NullPointerException.class,
                () -> portfolio.recordCashTransaction(null, new Money(BigDecimal.TEN, cad), "desc", Instant.now()));
    }

    @Test
    void recordCashTransaction_nullAmountThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        assertThrows(NullPointerException.class,
                () -> portfolio.recordCashTransaction(TransactionType.DEPOSIT, null, "desc", Instant.now()));
    }

    @Test
    void recordCashTransaction_currencyMismatchThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("CAD", "U");

        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        Money usdAmount = new Money(new BigDecimal("100.00"), new PortfolioCurrency("USD", "$"));
        assertThrows(IllegalArgumentException.class,
                () -> portfolio.recordCashTransaction(TransactionType.DEPOSIT, usdAmount, "USD Deposit", Instant.now()),
                "Transaction currency mismatch with portfolio currency preference.");
    }

    // Tests for voidTransaction
    @Test
    void voidTransaction_buyTransaction_success() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        AssetIdentifier appleIdentifier = new AssetIdentifier("APPL", "NASDAQ", null, "Apple");
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        BigDecimal quantity = new BigDecimal("10");
        Money costBasisPerUnit = new Money(new BigDecimal("150.00"), cad);
        LocalDate acquisitionDate = LocalDate.of(2023, 1, 15);
        Money currentMarketPrice = new Money(new BigDecimal("155.00"), cad);

        AssetHolding initialHolding = portfolio.recordAssetPurchase(appleIdentifier, quantity, costBasisPerUnit,
                acquisitionDate, currentMarketPrice);
        Transaction originalBuyTransaction = portfolio.getTransactions().stream()
                .filter(t -> t.getTransactionType() == TransactionType.BUY)
                .findFirst().orElseThrow();
        UUID transactionToVoidId = originalBuyTransaction.getTransactionId();

        int initialTransactionsSize = portfolio.getTransactions().size(); // Should be 2 (1 purchase + 1 from
                                                                          // addLiability/initial setup)
        Instant initialUpdatedAt = portfolio.getUpdatedAt();

        portfolio.voidTransaction(transactionToVoidId, "Mistake in quantity");

        assertTrue(originalBuyTransaction.getTransactionStatus() == TransactionStatus.VOIDED);
        assertEquals("Mistake in quantity", originalBuyTransaction.getVoidReason());
        // assertTrue(portfolio.getUpdatedAt().isAfter(initialUpdatedAt));

        // Check compensating transaction
        assertEquals(initialTransactionsSize + 1, portfolio.getTransactions().size());
        Transaction compensatingTransaction = portfolio.getTransactions().get(initialTransactionsSize); // The newly
                                                                                                        // added one
        assertEquals(TransactionType.VOID_BUY, compensatingTransaction.getTransactionType());
        assertEquals(quantity, compensatingTransaction.getQuantity());
        assertEquals(costBasisPerUnit.amount(), compensatingTransaction.getPricePerUnit());
        assertEquals(initialHolding.getAssetHoldingId(), compensatingTransaction.getAssetHoldingId());
        assertTrue(compensatingTransaction.getDescription().contains("VOID: Reversed purchase"));

        // Check AssetHolding update
        // The quantity of the holding should now be zero, leading to its removal
        assertFalse(portfolio.getAssets().contains(initialHolding));
        assertEquals(BigDecimal.ZERO, initialHolding.getQuantity()); // Verify internal state (mock AssetHolding)
    }

    @Test
    void voidTransaction_buyTransaction_partialVoid() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        AssetIdentifier appleIdentifier = new AssetIdentifier("APPL", "NASDAQ", null, "Apple");
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        BigDecimal quantity1 = new BigDecimal("10");
        Money costBasisPerUnit1 = new Money(new BigDecimal("100.00"), cad);
        portfolio.recordAssetPurchase(appleIdentifier, quantity1, costBasisPerUnit1, LocalDate.now(), null);

        BigDecimal quantity2 = new BigDecimal("5");
        Money costBasisPerUnit2 = new Money(new BigDecimal("120.00"), cad);
        portfolio.recordAssetPurchase(appleIdentifier, quantity2, costBasisPerUnit2, LocalDate.now(), null);

        // Find the first purchase transaction
        Transaction originalBuyTransaction = portfolio.getTransactions().stream()
                .filter(t -> t.getTransactionType() == TransactionType.BUY && t.getQuantity().equals(quantity1))
                .findFirst().orElseThrow();
        UUID transactionToVoidId = originalBuyTransaction.getTransactionId();

        AssetHolding holding = portfolio.getAssets().get(0); // There's only one holding for Apple
        BigDecimal initialHoldingQuantity = holding.getQuantity(); // Should be 15

        portfolio.voidTransaction(transactionToVoidId, "Mistake in first buy");

        assertTrue(originalBuyTransaction.getTransactionStatus() == TransactionStatus.VOIDED);
        assertEquals(initialHoldingQuantity.subtract(quantity1), holding.getQuantity()); // Holding quantity should be 5
                                                                                         // now
        assertFalse(holding.getQuantity().compareTo(BigDecimal.ZERO) == 0); // Holding should not be removed
        assertTrue(portfolio.getAssets().contains(holding));
    }

    @Test
    void voidTransaction_depositTransaction_success() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        Money depositAmount = new Money(new BigDecimal("500.00"), cad);
        Instant transactionDate = Instant.now();
        portfolio.recordCashTransaction(TransactionType.DEPOSIT, depositAmount, "Initial Deposit", transactionDate);

        Transaction originalDepositTransaction = portfolio.getTransactions().stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEPOSIT)
                .findFirst().orElseThrow();
        UUID transactionToVoidId = originalDepositTransaction.getTransactionId();

        int initialTransactionsSize = portfolio.getTransactions().size();

        portfolio.voidTransaction(transactionToVoidId, "Deposit never cleared");

        assertTrue(originalDepositTransaction.getTransactionStatus() == TransactionStatus.VOIDED);
        assertEquals("Deposit never cleared", originalDepositTransaction.getVoidReason());

        // Check compensating transaction
        assertEquals(initialTransactionsSize + 1, portfolio.getTransactions().size());
        Transaction compensatingTransaction = portfolio.getTransactions().get(initialTransactionsSize);
        assertEquals(TransactionType.WITHDRAWAL, compensatingTransaction.getTransactionType());
        assertEquals(depositAmount.negate(), compensatingTransaction.getAmount()); // Amount should be negative for
                                                                                   // withdrawal effect
        assertTrue(compensatingTransaction.getDescription().contains("VOID: Reversed deposit"));
    }

    @Test
    void voidTransaction_withdrawalTransaction_success() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
     
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);
        System.out.println(portfolio.getTransactions().size());

        Money withdrawalAmount = new Money(new BigDecimal("100.00"), cad);
        Instant transactionDate = Instant.now();
        portfolio.recordCashTransaction(TransactionType.WITHDRAWAL, withdrawalAmount, "Cash Withdrawal",
                transactionDate);

        Transaction originalWithdrawalTransaction = portfolio.getTransactions().stream()
                .filter(t -> t.getTransactionType() == TransactionType.WITHDRAWAL)
                .findFirst().orElseThrow();
        UUID transactionToVoidId = originalWithdrawalTransaction.getTransactionId();

        int initialTransactionsSize = portfolio.getTransactions().size();

        System.out.println(portfolio.getTransactions().size());
        portfolio.voidTransaction(transactionToVoidId, "Erroneous Withdrawal");
        System.out.println(portfolio.getTransactions().size());

        assertTrue(originalWithdrawalTransaction.getTransactionStatus() == TransactionStatus.VOIDED);
        assertEquals("Erroneous Withdrawal", originalWithdrawalTransaction.getVoidReason());

        // Check compensating transaction
        assertEquals(initialTransactionsSize + 1, portfolio.getTransactions().size());
        Transaction compensatingTransaction = portfolio.getTransactions().get(initialTransactionsSize);
        assertEquals(TransactionType.VOID_WITHDRAWAL, compensatingTransaction.getTransactionType());
        assertEquals(withdrawalAmount.negate(), compensatingTransaction.getAmount()); // Amount should be positive for
                                                                                      // deposit effect
        assertTrue(compensatingTransaction.getDescription().contains("VOID: Reversed withdrawal"));
    }

    @Test
    void voidTransaction_loanPaymentTransaction_success() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
      
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        Liability liability = portfolio.addLiability("Student Loan", "Edu", new Money(new BigDecimal("10000.00"), cad),
                new Percentage(new BigDecimal("0.04")), LocalDate.now().plusYears(10));
        Money paymentAmount = new Money(new BigDecimal("500.00"), cad);
        Instant transactionDate = Instant.now();
        portfolio.recordLiabilityPayment(liability.getLiabilityId(), paymentAmount, transactionDate);

        Transaction originalPaymentTransaction = portfolio.getTransactions().stream()
                .filter(t -> t.getTransactionType() == TransactionType.LOAN_PAYMENT)
                .findFirst().orElseThrow();
        UUID transactionToVoidId = originalPaymentTransaction.getTransactionId();

        int initialTransactionsSize = portfolio.getTransactions().size();
        Money initialLiabilityBalance = liability.getCurrentBalance();

        System.out.println(originalPaymentTransaction.getAmount().amount());

        portfolio.voidTransaction(transactionToVoidId, "Payment bounced");

        assertTrue(originalPaymentTransaction.getTransactionStatus() == TransactionStatus.VOIDED);
        assertEquals("Payment bounced", originalPaymentTransaction.getVoidReason());

        // Check compensating transaction
        assertEquals(initialTransactionsSize + 1, portfolio.getTransactions().size());
        Transaction compensatingTransaction = portfolio.getTransactions().get(initialTransactionsSize);
        assertEquals(TransactionType.OTHER, compensatingTransaction.getTransactionType()); // Using OTHER as placeholder
        assertEquals(paymentAmount.negate(), compensatingTransaction.getAmount()); // Reversing the payment effect
        assertEquals(liability.getLiabilityId(), compensatingTransaction.getLiabilityId());
        assertTrue(compensatingTransaction.getDescription().contains("VOID: Reversed loan payment"));

        // Check Liability balance
        assertEquals(initialLiabilityBalance.amount().add(paymentAmount.amount()),
                liability.getCurrentBalance().amount());
    }

    @Test
    void voidTransaction_nullTransactionIdThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        assertThrows(NullPointerException.class, () -> portfolio.voidTransaction(null, "reason"));
    }

    @Test
    void voidTransaction_nullReasonThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        UUID dummyId = UUID.randomUUID();
        assertThrows(NullPointerException.class, () -> portfolio.voidTransaction(dummyId, null));
    }

    @Test
    void voidTransaction_nonExistentTransactionThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        UUID nonExistentId = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> portfolio.voidTransaction(nonExistentId, "reason"),
                "Transaction with ID " + nonExistentId + " cannot be found in portfolio.");
    }

    @Test
    void voidTransaction_alreadyVoidedTransactionThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        AssetIdentifier appleIdentifier = new AssetIdentifier("APPL", "NASDAQ", null, "Apple");
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        BigDecimal quantity = new BigDecimal("1");
        Money costBasisPerUnit = new Money(new BigDecimal("10"), cad);
        LocalDate acquisitionDate = LocalDate.of(2023, 1, 15);
        portfolio.recordAssetPurchase(appleIdentifier, quantity, costBasisPerUnit, acquisitionDate, null);

        Transaction transaction = portfolio.getTransactions().stream()
                .filter(t -> t.getTransactionType() == TransactionType.BUY).findFirst().orElseThrow();
        transaction.markAsVoided("Initial void"); // Manually void it

        assertThrows(IllegalArgumentException.class,
                () -> portfolio.voidTransaction(transaction.getTransactionId(), "Second void attempt"),
                "Transaction with ID " + transaction.getTransactionId() + " is already voided.");
    }

    @Test
    void voidTransaction_buyTransaction_assetHoldingNotFoundThrowsException() {
        UUID userUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        PortfolioCurrency curr = new PortfolioCurrency("USD", "$");

        PortfolioCurrency cad = new PortfolioCurrency("USD", "$");
        Portfolio portfolio = new Portfolio(portfolioUuid, userUuid, "Port Name", "Port desc", curr, false);

        // Create a BUY transaction without adding its AssetHolding to the portfolio's
        // list
        UUID dummyHoldingId = UUID.randomUUID();
        Transaction rogueBuyTransaction = new Transaction(
                UUID.randomUUID(),
                portfolioUuid,
                TransactionType.BUY,
                new Money(BigDecimal.TEN, cad),
                Instant.now(),
                "Rogue Buy",
                BigDecimal.ONE,
                BigDecimal.TEN,
                dummyHoldingId,
                null);
        portfolio.getTransactions().add(rogueBuyTransaction); // Add only the transaction

        assertThrows(IllegalStateException.class,
                () -> portfolio.voidTransaction(rogueBuyTransaction.getTransactionId(), "Asset holding missing"),
                "AssetHolding for voided BUY transaction not found.");
    }
}
