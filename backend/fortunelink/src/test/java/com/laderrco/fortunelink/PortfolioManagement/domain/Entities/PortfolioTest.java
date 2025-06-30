package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.hamcrest.number.IsNaN;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.CashTransactionDetails;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Fee;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionMetadata;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.AssetType;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.FeeType;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionSource;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionStatus;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionType;
import com.laderrco.fortunelink.PortfolioManagement.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.PortfolioManagement.domain.services.SimpleExchangeRateService;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class PortfolioTest {

    private Portfolio portfolio;
    private UUID portfolioId;
    private UUID userId;
    private String name;
    private String desc;
    private PortfolioCurrency portfolioCurrency;
    private Money portfolioCashBalance;
    private Instant createdAt;
    private Instant updatedAt;
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void init() {
        portfolioId = UUID.randomUUID();
        userId = UUID.randomUUID();
        name = "Portfolio Name";
        desc = "Portfolio desc";
        portfolioCurrency = new PortfolioCurrency(Currency.getInstance("USD"));
        portfolioCashBalance = new Money(new BigDecimal(1000), portfolioCurrency);
        createdAt = Instant.now();
        exchangeRateService = new SimpleExchangeRateService();
        portfolio = new Portfolio(portfolioId, userId, name, desc, portfolioCurrency, portfolioCashBalance, exchangeRateService, createdAt);
        new Portfolio(portfolioId, userId, name, null, portfolioCurrency, portfolioCashBalance, exchangeRateService, createdAt);
        updatedAt = createdAt;
    }

    @Test
    void testConstructor() {
        PortfolioCurrency portfolioCurrency1 = new PortfolioCurrency(Currency.getInstance("CAD"));
        Money portfolioCashBalanace1 = new Money(new BigDecimal(-1), portfolioCurrency);
        Money portfolioCashBalanace2 = new Money(new BigDecimal(1000), portfolioCurrency1);

        // null pointer check for empty id
        assertThrows(NullPointerException.class,
                () -> new Portfolio(null, userId, name, desc, portfolioCurrency, portfolioCashBalance,exchangeRateService, createdAt));

        // when name is blank (i.e. \n\n\n)
        assertThrows(IllegalArgumentException.class, () -> new Portfolio(portfolioId, userId, "   \r \n   ", desc,
                portfolioCurrency1, portfolioCashBalanace1, exchangeRateService, createdAt));
        // when portfolio cash is negative, on creation
        assertThrows(IllegalArgumentException.class, () -> new Portfolio(portfolioId, userId, name, desc,
                portfolioCurrency1, portfolioCashBalanace1, exchangeRateService, createdAt));

        // when you put in cash that isn't your native currency
        assertThrows(IllegalArgumentException.class, () -> new Portfolio(portfolioId, userId, name, desc,
                portfolioCurrency, portfolioCashBalanace2, exchangeRateService, createdAt));

        assertNotNull(portfolio);
        assertEquals(portfolioId, portfolio.getPortfolioId());
        assertEquals(userId, portfolio.getUserId());
        assertEquals(name, portfolio.getPortfolioName());
        assertEquals(desc, portfolio.getPortfolioDescription());
        assertEquals(portfolioCurrency, portfolio.getPortfolioCurrencyPreference());
        assertEquals(portfolioCashBalance, portfolio.getPortfolioCashBalance());
        assertEquals(createdAt, portfolio.getCreatedAt());
        assertNotNull(portfolio.getUpdatedAt());
        // createdAt and updatedAt will be slightly different because updatedAt uses
        // Instant.now()
        // assert that updatedAt is at or after createdAt
        assertTrue(portfolio.getUpdatedAt().equals(createdAt) || portfolio.getUpdatedAt().isAfter(createdAt));

        assertTrue(portfolio.getTransactions().isEmpty());
        assertTrue(portfolio.getAssetHoldings().isEmpty());
        assertTrue(portfolio.getLiabilities().isEmpty());

    }

    @Test
    void testConstructor_zeroCashBalanceAllowed() {
        Money zeroCash = new Money(BigDecimal.ZERO, portfolioCurrency);
        assertDoesNotThrow(
                () -> new Portfolio(portfolioId, userId, name, desc, portfolioCurrency, zeroCash, exchangeRateService, createdAt));
    }

    @Test
    void testRecordNewLiability() {

    }

    @Test
    void testRecordAssetHoldingPurchase() {
        AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "AAPL", "APPLE", "NASDQA");
        Instant date = Instant.now();
        BigDecimal quantity = new BigDecimal(20);
        Money pricePerUnit = new Money(new BigDecimal(200), portfolioCurrency);
        TransactionMetadata transactionMetadata = new TransactionMetadata(TransactionStatus.COMPLETED,TransactionSource.MANUAL_INPUT, desc, createdAt, updatedAt);
                
        assertThrows(NullPointerException.class, () -> portfolio.recordAssetHoldingPurchase(null, quantity, date, pricePerUnit, transactionMetadata, null));
        assertThrows(NullPointerException.class, () -> portfolio.recordAssetHoldingPurchase(assetIdentifier, null, date, pricePerUnit, transactionMetadata, null));
        assertThrows(NullPointerException.class, () -> portfolio.recordAssetHoldingPurchase(assetIdentifier, quantity, null, pricePerUnit, transactionMetadata, null));
        assertThrows(NullPointerException.class, () -> portfolio.recordAssetHoldingPurchase(assetIdentifier, quantity, date, null, transactionMetadata, null));
        assertThrows(NullPointerException.class, () -> portfolio.recordAssetHoldingPurchase(assetIdentifier, quantity, date, pricePerUnit, null, null));
        
        assertThrows(IllegalArgumentException.class, () -> portfolio.recordAssetHoldingPurchase(assetIdentifier, new BigDecimal(-1), date, pricePerUnit, transactionMetadata, null));
        assertThrows(IllegalArgumentException.class, () -> portfolio.recordAssetHoldingPurchase(assetIdentifier, new BigDecimal(0), date, pricePerUnit, transactionMetadata, null));
        assertThrows(IllegalArgumentException.class, () -> portfolio.recordAssetHoldingPurchase(assetIdentifier, quantity, date, new Money(new BigDecimal(-1), portfolioCurrency), transactionMetadata, null));
        assertThrows(IllegalArgumentException.class, () -> portfolio.recordAssetHoldingPurchase(assetIdentifier, quantity, date, new Money(new BigDecimal(0), portfolioCurrency), transactionMetadata, null));


        TransactionMetadata transactionMetadataWrongStatus = new TransactionMetadata(TransactionStatus.FAILED, TransactionSource.MANUAL_INPUT, desc, createdAt, updatedAt);
        assertThrows(IllegalArgumentException.class, () -> portfolio.recordAssetHoldingPurchase(assetIdentifier, quantity, date, pricePerUnit, transactionMetadataWrongStatus, null));
        
        List<Fee> fees1 = new ArrayList<>();
        fees1.add(new Fee(FeeType.COMMISSION, new Money(new BigDecimal(2), new PortfolioCurrency(Currency.getInstance("CAD"))))); // money fee should be == to portoflio currency pref
        fees1.add(new Fee(FeeType.BROKERAGE, new Money(new BigDecimal(0.34), new PortfolioCurrency(Currency.getInstance("CAD"))))); // money fee should be == to portoflio currency pref
        

        assertThrows(IllegalArgumentException.class, () -> portfolio.recordAssetHoldingPurchase(assetIdentifier, quantity, date, pricePerUnit, transactionMetadata, fees1));
        
    }

    @Test
    void testRecordAssetHoldingSale() {

    }

    @Test
    void testRecordCashflow() {
        Money cashflow = new Money(new BigDecimal(200), portfolioCurrency);
        TransactionMetadata transactionMetadata = new TransactionMetadata(TransactionStatus.COMPLETED,
                TransactionSource.MANUAL_INPUT, desc, createdAt, updatedAt);
        assertThrows(NullPointerException.class,
                () -> portfolio.recordCashflow(null, cashflow, Instant.now(), transactionMetadata, null));
        assertThrows(NullPointerException.class, () -> portfolio.recordCashflow(TransactionType.DEPOSIT, null,
                Instant.now(), transactionMetadata, null));
        assertThrows(NullPointerException.class,
                () -> portfolio.recordCashflow(TransactionType.DEPOSIT, cashflow, null, transactionMetadata, null));
        assertThrows(NullPointerException.class,
                () -> portfolio.recordCashflow(TransactionType.DEPOSIT, null, Instant.now(), null, null));

        // testing if the transaction type is not 'cash' related
        assertThrows(IllegalArgumentException.class, () -> portfolio.recordCashflow(TransactionType.CORPORATE_ACTION,
                cashflow, Instant.now(), transactionMetadata, null));

        // testing to see if the money aded is the same as the currency pref
        Money cashflowWrongPref = new Money(new BigDecimal(200), new PortfolioCurrency(Currency.getInstance("CAD")));
        assertThrows(IllegalArgumentException.class, () -> portfolio.recordCashflow(TransactionType.DEPOSIT,
                cashflowWrongPref, Instant.now(), transactionMetadata, null));

        // testing if we remove more than we have in the balance
        Money cashflowTooLarge = new Money(new BigDecimal(200000), new PortfolioCurrency(Currency.getInstance("USD")));
        assertThrows(IllegalArgumentException.class, () -> portfolio.recordCashflow(TransactionType.WITHDRAWAL,
                cashflowTooLarge, Instant.now(), transactionMetadata, null));

        TransactionMetadata transactionMetadataNotCompleted = new TransactionMetadata(TransactionStatus.ACTIVE,
                TransactionSource.MANUAL_INPUT, desc, createdAt, updatedAt);
        assertThrows(IllegalArgumentException.class, () -> portfolio.recordCashflow(TransactionType.WITHDRAWAL,
                cashflowTooLarge, Instant.now(), transactionMetadataNotCompleted, null));

        // testing if money is negative
        Money cashflowWrongNegative = new Money(new BigDecimal(-200),
                new PortfolioCurrency(Currency.getInstance("CAD")));
        assertThrows(IllegalArgumentException.class, () -> portfolio.recordCashflow(TransactionType.WITHDRAWAL,
                cashflowWrongNegative, Instant.now(), transactionMetadata, null));

        portfolio.recordCashflow(TransactionType.DEPOSIT, cashflow, createdAt, transactionMetadata, null);
        portfolio.recordCashflow(TransactionType.WITHDRAWAL, cashflow, createdAt, transactionMetadata,
                new ArrayList<>());

    }

    @Test
    void testRecordCashflow_depositIncreasesBalanceAndRecordsTransaction() {
        Money depositAmount = new Money(new BigDecimal("500.00"), portfolioCurrency);
        Instant eventDate = Instant.now().minusSeconds(60); // Use a distinct date
        TransactionMetadata depositMetadata = new TransactionMetadata(
                TransactionStatus.COMPLETED,
                TransactionSource.MANUAL_INPUT,
                "Test deposit for positive scenario",
                eventDate, // Using eventDate for metadata's creation date for simplicity in test
                Instant.now() // updated date for metadata
        );
        List<Fee> noFees = Collections.emptyList(); // Pass an empty list, not null

        // Capture initial state
        Money initialBalance = portfolio.getPortfolioCashBalance();
        int initialTransactionCount = portfolio.getTransactions().size();
        Instant initialUpdatedAt = portfolio.getUpdatedAt();

        // Perform the action
        portfolio.recordCashflow(TransactionType.DEPOSIT, depositAmount, eventDate, depositMetadata, noFees);

        // Assertions
        // 1. Balance updated correctly
        Money expectedBalance = initialBalance.add(depositAmount);
        assertEquals(expectedBalance, portfolio.getPortfolioCashBalance(),
                "Portfolio cash balance should increase by deposit amount.");

        // 2. Transaction recorded
        assertEquals(initialTransactionCount + 1, portfolio.getTransactions().size(),
                "One transaction should be added.");
        Transaction recordedTx = portfolio.getTransactions().get(initialTransactionCount); // Get the newly added
                                                                                           // transaction

        // 3. Transaction details are correct
        assertNotNull(recordedTx.getTransactionId(), "Transaction ID should be generated.");
        assertEquals(portfolio.getPortfolioId(), recordedTx.getPortfolioId(), "Portfolio ID should match.");
        assertEquals(TransactionType.DEPOSIT, recordedTx.getTransactionType(), "Transaction type should be DEPOSIT.");
        assertEquals(depositAmount, recordedTx.getTotalTransactionAmount(),
                "Transaction amount should match deposit amount.");
        assertEquals(eventDate, recordedTx.getTransactionDate(), "Transaction date should match event date.");
        assertTrue(recordedTx.getTransactionDetails() instanceof CashTransactionDetails,
                "Transaction details should be CashTransactionDetails.");
        assertEquals(depositAmount, ((CashTransactionDetails) recordedTx.getTransactionDetails()).getNormalizedAmount(),
                "Cash transaction details amount should match.");

        // 4. Metadata is correctly assigned
        assertEquals(depositMetadata, recordedTx.getTransactionMetadata(),
                "Transaction metadata should be correctly assigned.");
        assertTrue(recordedTx.getFees().isEmpty(), "No fees should be associated.");

        // 5. UpdatedAt timestamp is updated
        assertTrue(
                portfolio.getUpdatedAt().isAfter(initialUpdatedAt) || portfolio.getUpdatedAt().equals(initialUpdatedAt),
                "Portfolio updatedAt timestamp should be updated.");
    }

    @Test
    void testRecordLiabilityPayment() {

    }

    @Test
    void testVoidTransaction() {

    }
}
