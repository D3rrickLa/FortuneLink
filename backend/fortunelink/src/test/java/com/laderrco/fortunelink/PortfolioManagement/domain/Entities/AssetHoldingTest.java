package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.entities.AssetHolding;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.AssetType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class AssetHoldingTest {
    private UUID portfolioId;
    private UUID assetHoldingId;
    private AssetIdentifier assetIdentifier;
    private BigDecimal initQuantity;
    private Money initialSpentCost; // total spent on shares
    private AssetHolding assetHolding;
    private ZonedDateTime dateTime;

    @BeforeEach
    void init() {
        portfolioId = UUID.randomUUID();
        assetHoldingId = UUID.randomUUID();
        assetIdentifier = new AssetIdentifier(AssetType.STOCK, "APPL",  "APPLE", "NASDAQ");
        initQuantity = new BigDecimal(20);
        initialSpentCost = new Money(new BigDecimal(200), new PortfolioCurrency(Currency.getInstance("USD")));
        dateTime = ZonedDateTime.now();
        assetHolding = new AssetHolding(portfolioId, assetHoldingId, assetIdentifier, initQuantity, dateTime, initialSpentCost);
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
                        initialSpentCost));

        assertThrows(NullPointerException.class,
                () -> new AssetHolding(portfolioId, null, assetIdentifier, initQuantity, dateTime,
                        initialSpentCost));

        assertThrows(NullPointerException.class,
                () -> new AssetHolding(portfolioId, assetHoldingId, null, initQuantity, dateTime,
                        initialSpentCost));

        assertThrows(NullPointerException.class,
                () -> new AssetHolding(portfolioId, assetHoldingId, assetIdentifier, null, dateTime,
                        initialSpentCost));

        assertThrows(NullPointerException.class,
                () -> new AssetHolding(portfolioId, assetHoldingId, assetIdentifier, initQuantity, null,
                        initialSpentCost));

        assertThrows(NullPointerException.class,
                () -> new AssetHolding(portfolioId, assetHoldingId, assetIdentifier, initQuantity, dateTime,
                        null));

        BigDecimal negativeQuant = new BigDecimal(-30);
        BigDecimal zeroedQuant = new BigDecimal(0);

        Exception e1 = assertThrows(IllegalArgumentException.class,
                () -> new AssetHolding(portfolioId, assetHoldingId, assetIdentifier, negativeQuant, dateTime,
                        initialSpentCost));
        assertTrue(e1.getMessage().equals("Quantity of asset bought cannot be less than or equal to zero."));

        Exception e2 = assertThrows(IllegalArgumentException.class,
                () -> new AssetHolding(portfolioId, assetHoldingId, assetIdentifier, zeroedQuant, dateTime,
                        initialSpentCost));
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
        Money totalCost = new Money(new BigDecimal(214), new PortfolioCurrency(Currency.getInstance("USD")));
        Money totalCostNegative = new Money(new BigDecimal(-214), new PortfolioCurrency(Currency.getInstance("USD")));
        Money totalCostCAD = new Money(new BigDecimal(214), new PortfolioCurrency(Currency.getInstance("CAD")));

        assertThrows(NullPointerException.class, () -> assetHolding.recordAdditionPurchaseOfAssetHolding(null, totalCost));
        assertThrows(NullPointerException.class, () -> assetHolding.recordAdditionPurchaseOfAssetHolding(quantityOfAssets, null));
        
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> assetHolding.recordAdditionPurchaseOfAssetHolding(new BigDecimal(0), totalCost));
        assertTrue(e1.getMessage().equals("Quantity of asset bought cannot be less than or equal to zero."));
        IllegalArgumentException e1_2 = assertThrows(IllegalArgumentException.class, () -> assetHolding.recordAdditionPurchaseOfAssetHolding(quantityOfAssetsNegative, totalCost));
        assertTrue(e1_2.getMessage().equals("Quantity of asset bought cannot be less than or equal to zero."));
        
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> assetHolding.recordAdditionPurchaseOfAssetHolding(quantityOfAssets, totalCostNegative));
        assertTrue(e2.getMessage().equals("Cost total of asset cannot be less than or equal to zero."));
        IllegalArgumentException e2_2 = assertThrows(IllegalArgumentException.class, () -> assetHolding.recordAdditionPurchaseOfAssetHolding(quantityOfAssets, new Money(new BigDecimal(0), new PortfolioCurrency(Currency.getInstance("USD")))));
        assertTrue(e2_2.getMessage().equals("Cost total of asset cannot be less than or equal to zero."));
        
        IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> assetHolding.recordAdditionPurchaseOfAssetHolding(quantityOfAssets, totalCostCAD));
        assertTrue(e3.getMessage().equals("Cost total currency must be the same of the AssetHolding currency."));

        assetHolding.recordAdditionPurchaseOfAssetHolding(quantityOfAssets, totalCost);
        System.out.println(assetHolding.getQuantity().doubleValue() );
        System.out.println(assetHolding.getCostBasis().amount().doubleValue());
        assertTrue(assetHolding.getCostBasis().amount().doubleValue() == 10.35);
        assertTrue(assetHolding.getQuantity().doubleValue() == 40D);
        
    }

    @Test
    void testRecordSaleOfAssetHolding() {
        BigDecimal quantToSell = new BigDecimal(10);
        BigDecimal quantToSellOverLimit = new BigDecimal(20000);
        BigDecimal quantToSellsNegative = new BigDecimal(-20);
        Money totalCost = new Money(new BigDecimal(214), new PortfolioCurrency(Currency.getInstance("USD")));
        Money totalCostNegative = new Money(new BigDecimal(-214), new PortfolioCurrency(Currency.getInstance("USD")));
        Money totalCostCAD = new Money(new BigDecimal(214), new PortfolioCurrency(Currency.getInstance("CAD")));

        
        /*
        * Errors to test
        * - Nulls
        * - if both are negatives
        * - if sell currency doesn't match
        * - if you try to sell more than you have
        */
        
        assertThrows(NullPointerException.class, () -> assetHolding.recordSaleOfAssetHolding(null, totalCost));
        assertThrows(NullPointerException.class, () -> assetHolding.recordSaleOfAssetHolding(quantToSell, null));
        
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> assetHolding.recordSaleOfAssetHolding(quantToSellsNegative, totalCost));
        assertTrue(e1.getMessage().equals("Quantity to sell cannot be less than or equal to zero."));

        IllegalArgumentException e1_2 = assertThrows(IllegalArgumentException.class, () -> assetHolding.recordSaleOfAssetHolding(new BigDecimal(0), totalCost));
        assertTrue(e1_2.getMessage().equals("Quantity to sell cannot be less than or equal to zero."));
        
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> assetHolding.recordSaleOfAssetHolding(quantToSell, totalCostNegative));
        assertTrue(e2.getMessage().equals("Total Proceeds cannot be less than or equal to zero."));

        IllegalArgumentException e2_2 = assertThrows(IllegalArgumentException.class, () -> assetHolding.recordSaleOfAssetHolding(quantToSell, new Money(new BigDecimal(0), new PortfolioCurrency(Currency.getInstance("USD")))));
        assertTrue(e2_2.getMessage().equals("Total Proceeds cannot be less than or equal to zero."));
      
        IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> assetHolding.recordSaleOfAssetHolding(quantToSell, totalCostCAD));
        assertTrue(e3.getMessage().equals("Currency of Total Proceeds must be the same of the AssetHolding currency."));
        
        IllegalArgumentException e4 = assertThrows(IllegalArgumentException.class, () -> assetHolding.recordSaleOfAssetHolding(quantToSellOverLimit, totalCost));
        assertTrue(e4.getMessage().equals("Amount enter to sell is larger than what you have for this asset."));

        assetHolding.recordSaleOfAssetHolding(quantToSell, totalCost);
        System.out.println(assetHolding.getQuantity().doubleValue() );
        System.out.println(assetHolding.getCostBasis().amount().doubleValue());
        assertTrue(assetHolding.getQuantity().doubleValue() == 10D);
        assertTrue(assetHolding.getCostBasis().amount().doubleValue() == 10);



    }

    @Test 
    void testGetterMethods() {
        assertEquals(portfolioId, assetHolding.getPorfolioId());
        assertEquals(assetHoldingId, assetHolding.getAssetHoldingId());
        assertEquals(assetIdentifier, assetHolding.getAssetIdentifier());
        assertEquals(initQuantity, assetHolding.getQuantity());
        assertEquals(initialSpentCost.divide(initQuantity), assetHolding.getCostBasis());
        assertEquals(dateTime, assetHolding.getAcqusisitionDate());
        assertTrue(assetHolding.getCreatedAt().isBefore(Instant.MAX));
        assertTrue(assetHolding.getUpdatedAt().isBefore(Instant.MAX));
    }
}
