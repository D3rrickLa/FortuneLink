package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.AssetType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class AssetTransactionDetailsTest {
    private AssetTransactionDetails assetTransactionDetails;
    private AssetIdentifier assetIdentifier;
    private BigDecimal quantity;
    private Money pricePerUnit;

    @BeforeEach
    void init() {
        assetIdentifier = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
        quantity = new BigDecimal(100).setScale(6);
        pricePerUnit = new Money(new BigDecimal(10), new PortfolioCurrency(Currency.getInstance("USD")));
        assetTransactionDetails = new AssetTransactionDetails(assetIdentifier, quantity, pricePerUnit);
    }

    @Test
    void testConstructor() {
        AssetIdentifier ai1 = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
        BigDecimal bd1 = new BigDecimal(100);
        Money m1 = new Money(new BigDecimal(10), new PortfolioCurrency(Currency.getInstance("USD")));
        assertThrows(IllegalArgumentException.class, () -> new AssetTransactionDetails(ai1, bd1,
                new Money(new BigDecimal(-2), new PortfolioCurrency(Currency.getInstance("USD")))));
        assertThrows(IllegalArgumentException.class, () -> new AssetTransactionDetails(ai1, new BigDecimal(-2), m1));
        assertThrows(IllegalArgumentException.class, () -> new AssetTransactionDetails(ai1, new BigDecimal(0), m1));
    }

    @Test
    void testEquals() {
        AssetIdentifier ai1 = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
        BigDecimal bd1 = new BigDecimal(100);
        Money m1 = new Money(new BigDecimal(10), new PortfolioCurrency(Currency.getInstance("USD")));
        AssetTransactionDetails atd = new AssetTransactionDetails(ai1, bd1, m1);

        assertTrue(assetTransactionDetails.equals(atd));
        assertTrue(assetTransactionDetails.equals(assetTransactionDetails));
        assertFalse(assetTransactionDetails.equals(null));
        assertFalse(assetTransactionDetails.equals(new Object()));
        assertFalse(assetTransactionDetails
                .equals(new AssetTransactionDetails(new AssetIdentifier(AssetType.BOND, "APPL", "APPLE", "NASDAQ"), bd1, m1)));
        assertFalse(assetTransactionDetails.equals(new AssetTransactionDetails(ai1, new BigDecimal(1), m1)));
        assertFalse(assetTransactionDetails.equals(new AssetTransactionDetails(ai1, bd1,
                new Money(new BigDecimal(10), new PortfolioCurrency(Currency.getInstance("CAD"))))));
    }

    @Test
    void testGetters() {
        assertEquals(assetIdentifier, assetTransactionDetails.getAssetIdentifier());
        assertEquals(quantity, assetTransactionDetails.getQuantity());
        assertEquals(pricePerUnit, assetTransactionDetails.getPricePerUnit());
    }

    @Test
    void testHashCode() {
        AssetIdentifier ai1 = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
        BigDecimal bd1 = new BigDecimal(100);
        Money m1 = new Money(new BigDecimal(10), new PortfolioCurrency(Currency.getInstance("USD")));
        AssetTransactionDetails atd = new AssetTransactionDetails(ai1, bd1, m1);
        assertEquals(atd.hashCode(), assetTransactionDetails.hashCode());
    }
}
