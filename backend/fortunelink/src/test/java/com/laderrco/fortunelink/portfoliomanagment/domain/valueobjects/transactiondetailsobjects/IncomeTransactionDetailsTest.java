package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.AssetHoldingId;

public class IncomeTransactionDetailsTest {
    @Test
    public void testValidConstructionAndGetters() {
        // Arrange
        AssetHoldingId holdingId = new AssetHoldingId(UUID.randomUUID());
        AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "AAPL", "US0378331005", "Apple", "NASDAQ");
        Money amount = new Money(25.00, "CAD");
        TransactionSource source = TransactionSource.SYSTEM;
        String description = "Dividend payout";
        // List<Fee> fees = List.of(new Fee("Tax", new Money("CAD", 2.00)));

        // Act
        IncomeTransactionDetails details = new IncomeTransactionDetails(
            holdingId,
            assetIdentifier,
            amount,
            source,
            description,
            null
        );

        // Assert
        assertEquals(holdingId, details.getAssetHoldingId());
        assertEquals(assetIdentifier, details.getAssetIdentifier());
        assertEquals(amount, details.getAmount());
        assertEquals(source, details.getSource());
        assertEquals(description, details.getDescription());
        assertEquals(Collections.emptyList(), details.getFees());
    }

    @Test
    public void testNullAssetHoldingIdThrowsException() {
        AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "AAPL", "US0378331005", "Apple", "NASDAQ");
        Money amount = new Money(30.00, "CAD");

        Exception exception = assertThrows(NullPointerException.class, () -> {
            new IncomeTransactionDetails(
                null,
                assetIdentifier,
                amount,
                TransactionSource.MANUAL,
                "Interest income",
                List.of()
            );
        });

        assertEquals("Asset holding id cannot be null.", exception.getMessage());
    }

    @Test
    public void testNullAssetIdentifierThrowsException() {
        AssetHoldingId holdingId = new AssetHoldingId(UUID.randomUUID());
        Money amount = new Money(15.00, "CAD");

        Exception exception = assertThrows(NullPointerException.class, () -> {
            new IncomeTransactionDetails(
                holdingId,
                null,
                amount,
                TransactionSource.MANUAL,
                "Interest income",
                List.of()
            );
        });

        assertEquals("Asset identifier cannot be null.", exception.getMessage());
    }

    @Test
    public void testNullAmountThrowsException() {
        AssetHoldingId holdingId = new AssetHoldingId(UUID.randomUUID());
        AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "GOOG", "US02079K3059", "Google", "NASDAQ");

        Exception exception = assertThrows(NullPointerException.class, () -> {
            new IncomeTransactionDetails(
                holdingId,
                assetIdentifier,
                null,
                TransactionSource.SYSTEM,
                "Dividend income",
                List.of()
            );
        });

        assertEquals("Amount cannot be null.", exception.getMessage());
    }

    @Test
    public void testEmptyFeesListIsAllowed() {
        AssetHoldingId holdingId = new AssetHoldingId(UUID.randomUUID());
        AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "TSLA", "US88160R1014 ", "Tesla", "NASDAQ");
        Money amount = new Money(40.00, "CAD");

        IncomeTransactionDetails details = new IncomeTransactionDetails(
            holdingId,
            assetIdentifier,
            amount,
            TransactionSource.SYSTEM,
            "Income without fees",
            List.of()
        );

        assertTrue(details.getFees().isEmpty());
    }
}
