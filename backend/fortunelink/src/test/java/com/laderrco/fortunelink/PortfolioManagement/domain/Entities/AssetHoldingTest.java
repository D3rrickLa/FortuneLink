package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class AssetHoldingTest {
    private UUID portfolioId;
    private UUID assetHoldingId;
    private AssetIdentifier assetIdentifier;
    private BigDecimal initQuantity;
    private Money initCostBasis;
    private AssetHolding assetHolding;
    private ZonedDateTime dateTime;

    @BeforeEach
    void init() {
        portfolioId = UUID.randomUUID();
        assetHoldingId = UUID.randomUUID();
        assetIdentifier = new AssetIdentifier("APPL", "NASDAW", null, "APPLE");
        initQuantity = new BigDecimal(20);
        initCostBasis = new Money(new BigDecimal(200), new PortfolioCurrency(Currency.getInstance("USD")));
        dateTime = ZonedDateTime.now();
        assetHolding = new AssetHolding(portfolioId, assetHoldingId, assetIdentifier, initQuantity, dateTime, initCostBasis);
    }

    @Test
    void testConstructor() {
        /*
         * Error tests
         * - if ID are NULLs
         * - if everything is null
         * - if quantity is <= 0
         * - if current price is negative
         */

        assertThrows(NullPointerException.class,
                () -> new AssetHolding(null, assetHoldingId, assetIdentifier, initQuantity, dateTime,
                        initCostBasis));

        assertThrows(NullPointerException.class,
                () -> new AssetHolding(portfolioId, null, assetIdentifier, initQuantity, dateTime,
                        initCostBasis));

        assertThrows(NullPointerException.class,
                () -> new AssetHolding(portfolioId, assetHoldingId, null, initQuantity, dateTime,
                        initCostBasis));

        assertThrows(NullPointerException.class,
                () -> new AssetHolding(portfolioId, assetHoldingId, assetIdentifier, null, dateTime,
                        initCostBasis));

        assertThrows(NullPointerException.class,
                () -> new AssetHolding(portfolioId, assetHoldingId, assetIdentifier, initQuantity, null,
                        initCostBasis));

        assertThrows(NullPointerException.class,
                () -> new AssetHolding(portfolioId, assetHoldingId, assetIdentifier, initQuantity, dateTime,
                        null));

        BigDecimal negativeQuant = new BigDecimal(-30);
        BigDecimal zeroedQuant = new BigDecimal(0);

        Exception e1 = assertThrows(IllegalArgumentException.class,
                () -> new AssetHolding(portfolioId, assetHoldingId, assetIdentifier, negativeQuant, dateTime,
                        initCostBasis));
        assertTrue(e1.getMessage().equals("Quantity of asset bought cannot be less than or equal to zero."));

        Exception e2 = assertThrows(IllegalArgumentException.class,
                () -> new AssetHolding(portfolioId, assetHoldingId, assetIdentifier, zeroedQuant, dateTime,
                        initCostBasis));
        assertTrue(e2.getMessage().equals("Quantity of asset bought cannot be less than or equal to zero."));
    }

    @Test
    void testRecordAdditionPurchaseOfAssetHolding() {
        /*
         * Error tests
         * - if quantity is 0 or less
         * - if price per unit is 0 or less
         * - if both values are null 
         * - if the price per unit currency is the right one
        */

        BigDecimal quantityOfAssets = new BigDecimal(20);
        BigDecimal quantityOfAssetsNegative = new BigDecimal(-20);
        Money pricePUnit = new Money(new BigDecimal(214), new PortfolioCurrency(Currency.getInstance("USD")));
        Money pricePUnitCAD = new Money(new BigDecimal(214), new PortfolioCurrency(Currency.getInstance("CAD")));
        Money pricePUnitNegative = new Money(new BigDecimal(-214), new PortfolioCurrency(Currency.getInstance("USD")));

        assertThrows(NullPointerException.class, () -> assetHolding.recordAdditionPurchaseOfAssetHolding(null, pricePUnit));
        assertThrows(NullPointerException.class, () -> assetHolding.recordAdditionPurchaseOfAssetHolding(quantityOfAssets, null));
        
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> assetHolding.recordAdditionPurchaseOfAssetHolding(quantityOfAssetsNegative, pricePUnit));
        assertTrue(e1.getMessage().equals("Quantity of asset bought cannot be less than or equal to zero."));
        
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> assetHolding.recordAdditionPurchaseOfAssetHolding(quantityOfAssets, pricePUnitNegative));
        assertTrue(e2.getMessage().equals("Cost total of asset cannot be less than or equal to zero."));
        
        IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> assetHolding.recordAdditionPurchaseOfAssetHolding(quantityOfAssets, pricePUnitCAD));
        assertTrue(e3.getMessage().equals("Cost total currency must be the same of the AssetHolding currency."));
        
    }

    @Test
    void testRecordSaleOfAssetHolding() {

    }
}
