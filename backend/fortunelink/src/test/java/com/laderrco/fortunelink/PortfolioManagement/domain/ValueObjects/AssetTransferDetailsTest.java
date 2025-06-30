package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetTransferDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.AssetType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class AssetTransferDetailsTest {
    
    @Test
    void testConstructor() {
        UUID sourceId = UUID.randomUUID();
        UUID destinationID = UUID.randomUUID();
        AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
        AssetIdentifier assetIdentifier2 = new AssetIdentifier(AssetType.ETF, "XEQT", "Blackrock Pure Stocks", "TSX");
        BigDecimal quantity = new BigDecimal(20);
        Money costBasisPerUnit = new Money(new BigDecimal(10), new PortfolioCurrency(Currency.getInstance("USD")));
        

        AssetTransferDetails assetTransferDetails1 = new AssetTransferDetails(sourceId, destinationID, assetIdentifier, quantity, costBasisPerUnit);
        AssetTransferDetails assetTransferDetails2 = new AssetTransferDetails(sourceId, destinationID, assetIdentifier2, quantity, costBasisPerUnit);
        AssetTransferDetails assetTransferDetails3 = new AssetTransferDetails(sourceId, destinationID, assetIdentifier, quantity, costBasisPerUnit);
        
        
        
        assertThrows(NullPointerException.class, () -> new AssetTransferDetails(sourceId, destinationID, assetIdentifier, null, costBasisPerUnit));
        assertThrows(NullPointerException.class, () -> new AssetTransferDetails(sourceId, destinationID, null, quantity, costBasisPerUnit));
        assertThrows(IllegalArgumentException.class, () -> new AssetTransferDetails(sourceId, destinationID, assetIdentifier, new BigDecimal(-1), costBasisPerUnit));
        assertThrows(IllegalArgumentException.class, () -> new AssetTransferDetails(sourceId, destinationID, assetIdentifier, new BigDecimal(0), costBasisPerUnit));
        assertThrows(IllegalArgumentException.class, () -> new AssetTransferDetails(null, null, assetIdentifier, quantity, costBasisPerUnit));
        new AssetTransferDetails(null, UUID.randomUUID(), assetIdentifier, quantity, costBasisPerUnit);
        new AssetTransferDetails(UUID.randomUUID(), null, assetIdentifier, quantity, costBasisPerUnit);
        
        assertNotEquals(assetTransferDetails2, assetTransferDetails3);
        assertEquals(assetTransferDetails1.getSourceAccountId(), assetTransferDetails3.getSourceAccountId());
        assertEquals(assetTransferDetails1.getDestinationAccountId(), assetTransferDetails3.getDestinationAccountId());
        assertEquals(assetTransferDetails1.getAssetIdentifier(), assetTransferDetails3.getAssetIdentifier());
        assertEquals(assetTransferDetails1.getQuantity(), assetTransferDetails3.getQuantity());
        assertEquals(assetTransferDetails1.getCostBasisPerUnit(), assetTransferDetails3.getCostBasisPerUnit());
    }
    
    @Test
    void testEquals() {
        UUID sourceId = UUID.randomUUID();
        UUID destinationID = UUID.randomUUID();
        AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
        AssetIdentifier assetIdentifier2 = new AssetIdentifier(AssetType.ETF, "XEQT", "Blackrock Pure Stocks", "TSX");
        BigDecimal quantity = new BigDecimal(20);
        Money costBasisPerUnit = new Money(new BigDecimal(10), new PortfolioCurrency(Currency.getInstance("USD")));
        
    
        AssetTransferDetails assetTransferDetails1 = new AssetTransferDetails(sourceId, destinationID, assetIdentifier, quantity, costBasisPerUnit);
        AssetTransferDetails assetTransferDetails2 = new AssetTransferDetails(UUID.randomUUID(), destinationID, assetIdentifier2, quantity, costBasisPerUnit);
        AssetTransferDetails assetTransferDetails3 = new AssetTransferDetails(sourceId, destinationID, assetIdentifier, quantity, costBasisPerUnit);

        
        assertTrue(assetTransferDetails1.equals(assetTransferDetails1));
        assertTrue(assetTransferDetails1.equals(assetTransferDetails3));
        assertFalse(assetTransferDetails1.equals(assetTransferDetails2));
        assertFalse(assetTransferDetails1.equals(null));
        assertFalse(assetTransferDetails1.equals(new Object()));
        
        assertFalse(assetTransferDetails1.equals(new AssetTransferDetails(UUID.randomUUID(), destinationID, assetIdentifier, quantity, costBasisPerUnit)));
        assertFalse(assetTransferDetails1.equals(new AssetTransferDetails(sourceId, UUID.randomUUID(), assetIdentifier, quantity, costBasisPerUnit)));
        assertFalse(assetTransferDetails1.equals(new AssetTransferDetails(sourceId, destinationID, assetIdentifier2, quantity, costBasisPerUnit)));
        assertFalse(assetTransferDetails1.equals(new AssetTransferDetails(sourceId, destinationID, assetIdentifier, new BigDecimal(200), costBasisPerUnit)));
        assertFalse(assetTransferDetails1.equals(new AssetTransferDetails(sourceId, destinationID, assetIdentifier, quantity, new Money(new BigDecimal(10), new PortfolioCurrency(Currency.getInstance("CAD"))))));
        
    }
    
    @Test
    void testHashCode() {
        UUID sourceId = UUID.randomUUID();
        UUID destinationID = UUID.randomUUID();
        AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
        AssetIdentifier assetIdentifier2 = new AssetIdentifier(AssetType.ETF, "XEQT", "Blackrock Pure Stocks", "TSX");
        BigDecimal quantity = new BigDecimal(20);
        Money costBasisPerUnit = new Money(new BigDecimal(10), new PortfolioCurrency(Currency.getInstance("USD")));
        
    
        AssetTransferDetails assetTransferDetails1 = new AssetTransferDetails(sourceId, destinationID, assetIdentifier, quantity, costBasisPerUnit);
        AssetTransferDetails assetTransferDetails2 = new AssetTransferDetails(UUID.randomUUID(), destinationID, assetIdentifier2, quantity, costBasisPerUnit);
        AssetTransferDetails assetTransferDetails3 = new AssetTransferDetails(sourceId, destinationID, assetIdentifier, quantity, costBasisPerUnit);
    
        assertEquals(assetTransferDetails1.hashCode(), assetTransferDetails3.hashCode());        
        assertNotEquals(assetTransferDetails2.hashCode(), assetTransferDetails3.hashCode());
    }
}
