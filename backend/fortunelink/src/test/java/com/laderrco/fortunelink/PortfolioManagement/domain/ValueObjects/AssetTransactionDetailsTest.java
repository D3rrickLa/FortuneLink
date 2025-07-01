package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.AssetType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class AssetTransactionDetailsTest {
    private AssetTransactionDetails assetTransactionDetails;
    private AssetIdentifier assetIdentifier;
    private BigDecimal quantity;
    private Money pricePerUnit;
    private  Money grossAssetCostInAssetCurrency;
    private  Money grossAssestCostInPorfolioCurrency;
    private  Money totalFOREXConversionFeesInPortfolioCurrency;
    private  Money totalOtherFeesInPortfolioCurrency;
    

    @BeforeEach
    void init() {
        assetIdentifier = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
        quantity = new BigDecimal(100).setScale(6);
        pricePerUnit = new Money(new BigDecimal(10), new PortfolioCurrency(Currency.getInstance("USD")));

        grossAssetCostInAssetCurrency = new Money(new BigDecimal(144.32 * 29.1),
                        new PortfolioCurrency(Currency.getInstance("CAD")));
        grossAssestCostInPorfolioCurrency = new Money(new BigDecimal(144.32 * 29.1),
                        new PortfolioCurrency(Currency.getInstance("USD")));
        totalFOREXConversionFeesInPortfolioCurrency = new Money(new BigDecimal(29.1),
                        new PortfolioCurrency(Currency.getInstance("USD")));
        totalOtherFeesInPortfolioCurrency = new Money(new BigDecimal(1),
                        new PortfolioCurrency(Currency.getInstance("USD")));


        assetTransactionDetails = new AssetTransactionDetails(assetIdentifier, quantity, pricePerUnit, grossAssetCostInAssetCurrency, grossAssestCostInPorfolioCurrency, totalFOREXConversionFeesInPortfolioCurrency, totalOtherFeesInPortfolioCurrency);
    }

    @Test
    void testConstructor() {
        AssetIdentifier ai1 = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
        BigDecimal bd1 = new BigDecimal(100);
        Money m1 = new Money(new BigDecimal(10), new PortfolioCurrency(Currency.getInstance("USD")));
        assertThrows(IllegalArgumentException.class, () -> new AssetTransactionDetails(ai1, bd1,
                new Money(new BigDecimal(-2), new PortfolioCurrency(Currency.getInstance("USD"))), grossAssetCostInAssetCurrency, grossAssestCostInPorfolioCurrency, totalFOREXConversionFeesInPortfolioCurrency, totalOtherFeesInPortfolioCurrency));
        assertThrows(IllegalArgumentException.class, () -> new AssetTransactionDetails(ai1, new BigDecimal(-2), m1, grossAssetCostInAssetCurrency, grossAssestCostInPorfolioCurrency, totalFOREXConversionFeesInPortfolioCurrency, totalOtherFeesInPortfolioCurrency));
        assertThrows(IllegalArgumentException.class, () -> new AssetTransactionDetails(ai1, new BigDecimal(0), m1, grossAssetCostInAssetCurrency, grossAssestCostInPorfolioCurrency, totalFOREXConversionFeesInPortfolioCurrency, totalOtherFeesInPortfolioCurrency));
    }

    @Test
    void testEquals() {
        AssetIdentifier ai1 = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
        BigDecimal bd1 = new BigDecimal(100);
        Money m1 = new Money(new BigDecimal(10), new PortfolioCurrency(Currency.getInstance("USD")));
        AssetTransactionDetails atd = new AssetTransactionDetails(ai1, bd1, m1, grossAssetCostInAssetCurrency, grossAssestCostInPorfolioCurrency, totalFOREXConversionFeesInPortfolioCurrency, totalOtherFeesInPortfolioCurrency);

        assertTrue(assetTransactionDetails.equals(atd));
        assertTrue(assetTransactionDetails.equals(assetTransactionDetails));
        assertFalse(assetTransactionDetails.equals(null));
        assertFalse(assetTransactionDetails.equals(new Object()));
        assertFalse(assetTransactionDetails
                .equals(new AssetTransactionDetails(new AssetIdentifier(AssetType.BOND, "APPL", "APPLE", "NASDAQ"), bd1, m1, grossAssetCostInAssetCurrency, grossAssestCostInPorfolioCurrency, totalFOREXConversionFeesInPortfolioCurrency, totalOtherFeesInPortfolioCurrency)));
        assertFalse(assetTransactionDetails.equals(new AssetTransactionDetails(ai1, new BigDecimal(1), m1,  grossAssetCostInAssetCurrency, grossAssestCostInPorfolioCurrency, totalFOREXConversionFeesInPortfolioCurrency, totalOtherFeesInPortfolioCurrency)));
        assertFalse(assetTransactionDetails.equals(new AssetTransactionDetails(ai1, bd1,
                new Money(new BigDecimal(10), new PortfolioCurrency(Currency.getInstance("CAD"))), grossAssetCostInAssetCurrency, grossAssestCostInPorfolioCurrency, totalFOREXConversionFeesInPortfolioCurrency, totalOtherFeesInPortfolioCurrency)));
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
        AssetTransactionDetails atd = new AssetTransactionDetails(ai1, bd1, m1,  grossAssetCostInAssetCurrency, grossAssestCostInPorfolioCurrency, totalFOREXConversionFeesInPortfolioCurrency, totalOtherFeesInPortfolioCurrency);
        assertEquals(atd.hashCode(), assetTransactionDetails.hashCode());
    }
}
