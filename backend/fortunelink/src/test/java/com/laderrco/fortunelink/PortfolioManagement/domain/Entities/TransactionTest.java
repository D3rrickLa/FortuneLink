package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetTransactionDetails;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Fee;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionDetails;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionMetadata;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.VoidInfo;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.AssetType;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.FeeType;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionSource;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionStatus;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class TransactionTest {
    
    private UUID transactionId;
    private UUID portfolioId;
    private TransactionType transactionType;
    private Money totalTransactionAmount;
    private Instant transactionDate;
    private AssetIdentifier assetIdentifier;
    private Money pricerPerUnit;
    private BigDecimal quantity;
    private TransactionDetails transactionDetails;
    private TransactionMetadata transactionMetadata;
    private Optional<VoidInfo> voidInfo;
    private List<Fee> fees;
    private Transaction buyAssetTransaction;

    

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        portfolioId = UUID.randomUUID();        
        totalTransactionAmount = new Money(new BigDecimal(144.32*30), new PortfolioCurrency(Currency.getInstance("USD")));
        transactionDate = Instant.now();
        transactionType = TransactionType.BUY;
        assetIdentifier = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
        pricerPerUnit = new Money(new BigDecimal(144.32), new PortfolioCurrency(Currency.getInstance("USD")));
        quantity = new BigDecimal(30);

        transactionDetails = new AssetTransactionDetails(assetIdentifier, quantity, pricerPerUnit);
        
        transactionMetadata = new TransactionMetadata(TransactionStatus.ACTIVE, TransactionSource.MANUAL_INPUT, "SOME DESC.", transactionDate, transactionDate);
        voidInfo = null;
        fees = null;

        buyAssetTransaction = new Transaction.Builder()
            .transactionId(transactionId)
            .portfolioId(portfolioId)
            .transactionType(transactionType)
            .totalTransactionAmount(totalTransactionAmount)
            .transactionDate(transactionDate)
            .transactionDetails(transactionDetails)
            .transactionMetadata(transactionMetadata)
            .voidInfo(Optional.empty())
            .build();
    }
    
        
        
        
    @Test
    void testConstructor() {
        List<Fee> fees1 = new ArrayList<>();
        fees1.add(new Fee(FeeType.COMMISSION, new Money(new BigDecimal(2), new PortfolioCurrency(Currency.getInstance("USD"))))); // money fee should be == to portoflio currency pref
        fees1.add(new Fee(FeeType.BROKERAGE, new Money(new BigDecimal(0.34), new PortfolioCurrency(Currency.getInstance("USD"))))); // money fee should be == to portoflio currency pref
        
        Transaction buyAssetTransaction2 = new Transaction.Builder()
            .transactionId(transactionId)
            .portfolioId(portfolioId)
            .transactionType(transactionType)
            .totalTransactionAmount(totalTransactionAmount)
            .transactionDate(transactionDate)
            .transactionDetails(transactionDetails)
            .transactionMetadata(transactionMetadata)
            .fees(fees1)
            .build();

        assertEquals(buyAssetTransaction.getTransactionId(), buyAssetTransaction2.getTransactionId());
        assertEquals(buyAssetTransaction.getPortfolioId(), buyAssetTransaction2.getPortfolioId());
        assertEquals(buyAssetTransaction.getTransactionType(), buyAssetTransaction2.getTransactionType());
        assertEquals(buyAssetTransaction.getTotalTransactionAmount(), buyAssetTransaction2.getTotalTransactionAmount());
        assertEquals(buyAssetTransaction.getTransactionDate(), buyAssetTransaction2.getTransactionDate());
        assertEquals(buyAssetTransaction.getTransactionDetails(), buyAssetTransaction2.getTransactionDetails());
        assertEquals(buyAssetTransaction.getTransactionMetadata(), buyAssetTransaction2.getTransactionMetadata());
        assertNotEquals(buyAssetTransaction.getVoidInfo(), buyAssetTransaction2.getVoidInfo());
        assertNotEquals(buyAssetTransaction.getFees(), buyAssetTransaction2.getFees());
 

    }

    @Test 
    void testMarkAsVoided() {
        // 1. make a transaction, already done in @BeforeEach
        // 2. make another transaction for voiding
        // 3. using transaction from step 1. call its markAsVoided function
        UUID voidingTransaction = UUID.randomUUID();
        String reason = "SOME VOID REASON";
        Transaction updated = buyAssetTransaction.markAsVoided(voidingTransaction, reason);
        
        
        UUID transactionId1 = UUID.randomUUID();
        UUID portfolioId1 = UUID.randomUUID();        
        Money totalTransactionAmount1 = new Money(new BigDecimal(144.32*30), new PortfolioCurrency(Currency.getInstance("USD")));
        Instant transactionDate1 = Instant.now();
        
        // AssetIdentifier assetIdentifier1 = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
        // Money pricerPerUnit1 = new Money(new BigDecimal(144.32), new PortfolioCurrency(Currency.getInstance("USD")));
        // BigDecimal quantity1 = new BigDecimal(30);
        
        AssetTransactionDetails transactionDetails1 = new AssetTransactionDetails(assetIdentifier, quantity, pricerPerUnit);
        
        TransactionMetadata transactionMetadata2 = new TransactionMetadata(TransactionStatus.CANCELLED, TransactionSource.MANUAL_INPUT, "SOME DESC.", transactionDate, transactionDate);
        TransactionMetadata transactionMetadata3 = new TransactionMetadata(TransactionStatus.VOIDED, TransactionSource.MANUAL_INPUT, "SOME DESC.", transactionDate, transactionDate);
        
        Transaction buyAssetTransaction1 = new Transaction.Builder()
        .transactionId(transactionId1)
        .portfolioId(portfolioId1)
        .transactionType(transactionType)
        .totalTransactionAmount(totalTransactionAmount1)
        .transactionDate(transactionDate1)
        .transactionDetails(transactionDetails1)
        .transactionMetadata(transactionMetadata2)
        .voidInfo(voidInfo)
        .build();
        
        Transaction buyAssetTransaction2 = new Transaction.Builder()
        .transactionId(transactionId1)
        .portfolioId(portfolioId1)
            .transactionType(transactionType)
            .totalTransactionAmount(totalTransactionAmount1)
            .transactionDate(transactionDate1)
            .transactionDetails(transactionDetails1)
            .transactionMetadata(transactionMetadata3)
            .voidInfo(Optional.empty())
            .build();
            
            
            assertThrows(IllegalArgumentException.class, () -> buyAssetTransaction.markAsVoided(voidingTransaction, " \n \n \r"));
            assertThrows(IllegalStateException.class, () -> buyAssetTransaction1.markAsVoided(voidingTransaction, reason));
            assertThrows(IllegalStateException.class, () -> buyAssetTransaction2.markAsVoided(voidingTransaction, reason));
            assertTrue(updated.getTransactionMetadata().transactionStatus() == TransactionStatus.VOIDED);
            assertNotNull(updated.getVoidInfo());        
        }


    @Test
    void testMarkAsVoided_whenStatusIsCompleted_shouldThrowException() {
        UUID voidingTransactionId = UUID.randomUUID();
        String reason = "Duplicate transaction";

        TransactionMetadata metadata = new TransactionMetadata(
            TransactionStatus.COMPLETED, // <-- This triggers the guard clause
            TransactionSource.MANUAL_INPUT,
            "SOME DESC.", transactionDate, transactionDate
        );

        Transaction transaction = new Transaction.Builder()
            .transactionId(UUID.randomUUID())
            .portfolioId(UUID.randomUUID())
            .transactionType(TransactionType.BUY)
            .totalTransactionAmount(totalTransactionAmount)
            .transactionDate(Instant.now())
            .transactionDetails(transactionDetails)
            .transactionMetadata(metadata)
            .fees(fees)
            .build();

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            transaction.markAsVoided(voidingTransactionId, reason);
        });

        assertTrue(exception.getMessage().contains("Only ACTIVE or PENDING transactions can be voided"));
    }

}
