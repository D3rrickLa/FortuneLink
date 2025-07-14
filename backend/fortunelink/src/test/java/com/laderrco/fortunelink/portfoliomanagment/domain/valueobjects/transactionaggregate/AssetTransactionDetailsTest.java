package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class AssetTransactionDetailsTest {
    private AssetIdentifier assetIdentifier;
    private BigDecimal quantity;
    private Money pricePerUnit;
    private Money assetValueInAssetCurrency;
    private Money assetValueInPortfolioCurrency;
    private Money costBasisInPortfolioCurrency;
    private Money totalFeesInPortfolioCurrency;

    private Currency usd;
    private Currency cad;

    @BeforeEach
    void init() {
        usd = Currency.getInstance("USD");
        cad = Currency.getInstance("CAD");

        assetIdentifier = new AssetIdentifier (
            "APPL",
            AssetType.STOCK,
            "US0378331005",
            "Apple", 
            "NASDAQ",
            "DESCRIPTION"
        );

        quantity = new BigDecimal(204);
        pricePerUnit = new Money(215.23, usd);
        assetValueInAssetCurrency = pricePerUnit.multiply(quantity);
        assetValueInPortfolioCurrency = pricePerUnit.multiply(quantity).multiply(BigDecimal.valueOf(1.37));

        totalFeesInPortfolioCurrency = new Money(0.47, cad);

        costBasisInPortfolioCurrency = Money.of(
            quantity.multiply(pricePerUnit.amount()).add(totalFeesInPortfolioCurrency.amount()), 
            cad
        );
    }

    @Test
    void testConstructor() {
        AssetTransactionDetails transactionDetails = new AssetTransactionDetails(assetIdentifier, quantity, pricePerUnit, assetValueInAssetCurrency, assetValueInPortfolioCurrency, costBasisInPortfolioCurrency, totalFeesInPortfolioCurrency);
        assertNotNull(transactionDetails);
        assertEquals(assetIdentifier, transactionDetails.getAssetIdentifier());
        assertEquals(quantity, transactionDetails.getQuantity());
        assertEquals(pricePerUnit, transactionDetails.getPricePerUnit());
        assertEquals(assetValueInAssetCurrency, transactionDetails.getAssetValueInAssetCurrency());
        assertEquals(assetValueInPortfolioCurrency, transactionDetails.getAssetValueInPortfolioCurrency());
        assertEquals(costBasisInPortfolioCurrency, transactionDetails.getCostBasisInPortfolioCurrency());
        assertEquals(totalFeesInPortfolioCurrency, transactionDetails.getTotalFeesInPortfolioCurrency());
    }
}
