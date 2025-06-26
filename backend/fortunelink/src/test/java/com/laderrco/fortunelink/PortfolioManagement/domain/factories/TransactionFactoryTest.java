package com.laderrco.fortunelink.PortfolioManagement.domain.factories;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.Transaction;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Fee;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionMetadata;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.AssetType;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.FeeType;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionSource;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionStatus;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class TransactionFactoryTest {
    @Test
    void testCreateBuyAssetTransaction() {
        UUID transactionID = UUID.randomUUID();
        UUID portfolioID = UUID.randomUUID();
        AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
        Instant transactionDate = Instant.now();
        BigDecimal quantity = new BigDecimal(100);
        Money pricePerUnit = new Money(new BigDecimal(35.32), new PortfolioCurrency(Currency.getInstance("USD")));
        TransactionMetadata transactionMetadata = new TransactionMetadata(TransactionStatus.ACTIVE,
                TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION");
        List<Fee> fees = new ArrayList<>();
        fees.add(new Fee(FeeType.COMMISSION,
                new Money(new BigDecimal(2), new PortfolioCurrency(Currency.getInstance("USD"))))); // money fee should
                                                                                                    // be == to
                                                                                                    // portoflio
                                                                                                    // currency pref
        fees.add(new Fee(FeeType.BROKERAGE,
                new Money(new BigDecimal(0.34), new PortfolioCurrency(Currency.getInstance("USD"))))); // money fee
                                                                                                       // should be ==
                                                                                                       // to portoflio
                                                                                                       // currency pref

        Transaction transaction = TransactionFactory.createBuyAssetTransaction(transactionID, portfolioID,
                assetIdentifier, transactionDate, quantity, pricePerUnit, transactionMetadata, fees);
        Transaction transaction2 = TransactionFactory.createBuyAssetTransaction(transactionID, portfolioID,
                assetIdentifier, transactionDate, quantity, pricePerUnit, transactionMetadata, null);
        // failing tests
        assertThrows(NullPointerException.class, () -> TransactionFactory.createBuyAssetTransaction(null,
                portfolioID, assetIdentifier, transactionDate, quantity, pricePerUnit, transactionMetadata, fees));
        assertThrows(NullPointerException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                null, assetIdentifier, transactionDate, quantity, pricePerUnit, transactionMetadata, fees));
        assertThrows(NullPointerException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                portfolioID, null, transactionDate, quantity, pricePerUnit, transactionMetadata, fees));
        assertThrows(NullPointerException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                portfolioID, assetIdentifier, null, quantity, pricePerUnit, transactionMetadata, fees));
        assertThrows(NullPointerException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                portfolioID, assetIdentifier, transactionDate, null, pricePerUnit, transactionMetadata, fees));
        assertThrows(NullPointerException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                portfolioID, assetIdentifier, transactionDate, quantity, null, transactionMetadata, fees));
        assertThrows(NullPointerException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                portfolioID, assetIdentifier, transactionDate, quantity, pricePerUnit, null, fees));

        // quantity check
        assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                portfolioID, assetIdentifier, transactionDate, new BigDecimal(-6), pricePerUnit, transactionMetadata,
                null));
        assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                portfolioID, assetIdentifier, transactionDate, new BigDecimal(0), pricePerUnit, transactionMetadata,
                null));

        // price per unit check
        Money pricePerUnitNegative = new Money(new BigDecimal(-13.14),
                new PortfolioCurrency(Currency.getInstance("USD")));
        Money pricePerUnitZero = new Money(new BigDecimal(0), new PortfolioCurrency(Currency.getInstance("USD")));

        assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                portfolioID, assetIdentifier, transactionDate, quantity, pricePerUnitNegative, transactionMetadata,
                null));
        assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createBuyAssetTransaction(transactionID,
                portfolioID, assetIdentifier, transactionDate, quantity, pricePerUnitZero, transactionMetadata,
                null));

        // final good test
        assertFalse(transaction.equals(null));
        assertFalse(transaction2.equals(null));
    }

    @Test
    void testCreateSellAssetTransaction() {
        UUID transactionID = UUID.randomUUID();
        UUID portfolioID = UUID.randomUUID();
        AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
        Instant transactionDate = Instant.now();
        BigDecimal quantity = new BigDecimal(100);
        Money pricePerUnit = new Money(new BigDecimal(35.32), new PortfolioCurrency(Currency.getInstance("USD")));
        TransactionMetadata transactionMetadata = new TransactionMetadata(TransactionStatus.ACTIVE,
                TransactionSource.MANUAL_INPUT, "SOME DESCRIPTION");
        List<Fee> fees = new ArrayList<>();
        fees.add(new Fee(FeeType.COMMISSION,
                new Money(new BigDecimal(2), new PortfolioCurrency(Currency.getInstance("USD"))))); // money fee should
                                                                                                    // be == to
                                                                                                    // portoflio
                                                                                                    // currency pref
        fees.add(new Fee(FeeType.BROKERAGE,
                new Money(new BigDecimal(0.34), new PortfolioCurrency(Currency.getInstance("USD"))))); // money fee
                                                                                                       // should be ==
                                                                                                       // to portoflio
                                                                                                       // currency pref

        Transaction transaction = TransactionFactory.createSellAssetTransaction(transactionID, portfolioID,
                assetIdentifier, transactionDate, quantity, pricePerUnit, transactionMetadata, fees);
        Transaction transaction2 = TransactionFactory.createSellAssetTransaction(transactionID, portfolioID,
                assetIdentifier, transactionDate, quantity, pricePerUnit, transactionMetadata, null);
        // failing tests
        assertThrows(NullPointerException.class, () -> TransactionFactory.createSellAssetTransaction(null,
                portfolioID, assetIdentifier, transactionDate, quantity, pricePerUnit, transactionMetadata, fees));
        assertThrows(NullPointerException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                null, assetIdentifier, transactionDate, quantity, pricePerUnit, transactionMetadata, fees));
        assertThrows(NullPointerException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                portfolioID, null, transactionDate, quantity, pricePerUnit, transactionMetadata, fees));
        assertThrows(NullPointerException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                portfolioID, assetIdentifier, null, quantity, pricePerUnit, transactionMetadata, fees));
        assertThrows(NullPointerException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                portfolioID, assetIdentifier, transactionDate, null, pricePerUnit, transactionMetadata, fees));
        assertThrows(NullPointerException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                portfolioID, assetIdentifier, transactionDate, quantity, null, transactionMetadata, fees));
        assertThrows(NullPointerException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                portfolioID, assetIdentifier, transactionDate, quantity, pricePerUnit, null, fees));

        // quantity check
        assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                portfolioID, assetIdentifier, transactionDate, new BigDecimal(-6), pricePerUnit, transactionMetadata,
                null));
        assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                portfolioID, assetIdentifier, transactionDate, new BigDecimal(0), pricePerUnit, transactionMetadata,
                null));

        // price per unit check
        Money pricePerUnitNegative = new Money(new BigDecimal(-13.14),
                new PortfolioCurrency(Currency.getInstance("USD")));
        Money pricePerUnitZero = new Money(new BigDecimal(0), new PortfolioCurrency(Currency.getInstance("USD")));

        assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                portfolioID, assetIdentifier, transactionDate, quantity, pricePerUnitNegative, transactionMetadata,
                null));
        assertThrows(IllegalArgumentException.class, () -> TransactionFactory.createSellAssetTransaction(transactionID,
                portfolioID, assetIdentifier, transactionDate, quantity, pricePerUnitZero, transactionMetadata,
                null));

        // final good test
        assertFalse(transaction.equals(null));
        assertFalse(transaction2.equals(null));

    }
}
