package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.PortfolioCurrency;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionSource;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionStatus;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionType;

public class TransactionTest {

	@Test
	void testEquals() {
        UUID transicationUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        TransactionType type = TransactionType.BUY;
    
        Money money = new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$"));
        Instant transDate = Instant.now();
        String desc = "some desc";
        BigDecimal quant = new BigDecimal(10000);
        BigDecimal cpu = new BigDecimal(100);
        UUID assetUuid = UUID.randomUUID();
        UUID lUuid = UUID.randomUUID();
    
        Transaction transaction = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant, cpu, assetUuid, lUuid);
        Transaction transaction2 = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant, cpu, assetUuid, lUuid);
        Transaction transaction3 = new Transaction(UUID.randomUUID(), portfolioUuid, type, money, transDate, desc, quant, cpu, assetUuid, lUuid);
        
        assertTrue(transaction.equals(transaction));
        assertTrue(transaction.equals(transaction2));
        assertFalse(transaction.equals(transaction3));
        assertFalse(transaction.equals(null));
        assertFalse(transaction.equals(""));
	}

    // technically transaction shouldn't have the same hash code
	@Test
	void testHashCode() {
        UUID transicationUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        TransactionType type = TransactionType.BUY;
    
        Money money = new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$"));
        Instant transDate = Instant.now();
        String desc = "some desc";
        BigDecimal quant = new BigDecimal(10000);
        BigDecimal cpu = new BigDecimal(100);
        UUID assetUuid = UUID.randomUUID();
        UUID lUuid = UUID.randomUUID();
    
        Transaction transaction = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant, cpu, assetUuid, lUuid);
        Transaction transaction2 = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant, cpu, assetUuid, lUuid);
        Transaction transaction3 = new Transaction(UUID.randomUUID(), portfolioUuid, type, money, transDate, desc, quant, cpu, assetUuid, lUuid);
        
        assertTrue(transaction.hashCode() == transaction2.hashCode());
        assertTrue(transaction.hashCode() != transaction3.hashCode());
		
	}

	@Test
	void testMarkAsVoidedBranches() {
        UUID transicationUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        TransactionType type = TransactionType.BUY;

        Money money = new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$"));
        Instant transDate = Instant.now();
        String desc = "some desc";
        BigDecimal quant = new BigDecimal(10000);
        BigDecimal cpu = new BigDecimal(100);
        UUID assetUuid = UUID.randomUUID();
        UUID lUuid = UUID.randomUUID();

        Transaction transaction = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant, cpu, assetUuid, lUuid);
        
        assertThrows(NullPointerException.class, () -> transaction.markAsVoided(null));
        assertThrows(IllegalArgumentException.class, () -> transaction.markAsVoided(""));
        
        
        Transaction transaction2 = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant, cpu, TransactionStatus.ACTIVE, null, null, assetUuid, lUuid, TransactionSource.PLATFORM_SYNC);
        assertThrows(IllegalStateException.class, () -> transaction2.markAsVoided("Some Reason"));

        Transaction transaction3 = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant, cpu, TransactionStatus.FAILED, null, null, assetUuid, lUuid, TransactionSource.MANUAL_INPUT);
        assertThrows(IllegalArgumentException.class, () -> transaction3.markAsVoided("Some Reason"));
        
        Transaction transaction4 = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant, cpu, TransactionStatus.ACTIVE, null, null, assetUuid, lUuid, TransactionSource.MANUAL_INPUT);
        transaction4.markAsVoided("some reason");
    }


    @Test 
    void testMarkAsVoidedGood() {
                UUID transicationUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        TransactionType type = TransactionType.BUY;

        Money money = new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$"));
        Instant transDate = Instant.now();
        String desc = "some desc";
        BigDecimal quant = new BigDecimal(10000);
        BigDecimal cpu = new BigDecimal(100);
        UUID assetUuid = UUID.randomUUID();
        UUID lUuid = UUID.randomUUID();

        Transaction transaction = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant, cpu, assetUuid, lUuid);
        transaction.markAsVoided("some good reason");
    }

    @Test 
    void testAllGetters() {
        UUID transicationUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        TransactionType type = TransactionType.BUY;

        Money money = new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$"));
        Instant transDate = Instant.now();
        String desc = "some desc";
        BigDecimal quant = new BigDecimal(10000);
        BigDecimal cpu = new BigDecimal(100);
        UUID assetUuid = UUID.randomUUID();
        UUID lUuid = UUID.randomUUID();

        Transaction transaction = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant, cpu, assetUuid, lUuid);
        
        assertEquals(transicationUuid, transaction.getTransactionId());
        assertEquals(portfolioUuid, transaction.getPortfolioId());
        assertEquals(type, transaction.getTransactionType());
        assertEquals(money, transaction.getAmount());
        assertEquals(transDate, transaction.getTransactionDate());
        assertEquals(desc, transaction.getDescription());
        assertEquals(quant, transaction.getQuantity());
        assertEquals(cpu, transaction.getPricePerUnit());
        assertEquals(assetUuid, transaction.getAssetHoldingId());
        assertEquals(lUuid, transaction.getLiabilityId());
        assertEquals(com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionStatus.ACTIVE, transaction.getTransactionStatus());
        assertEquals(TransactionSource.MANUAL_INPUT, transaction.getTransactionSource());
        assertTrue(transaction.getVoidReason() == null);
        assertTrue(transaction.getVoidedAt() == null);
    }
}
