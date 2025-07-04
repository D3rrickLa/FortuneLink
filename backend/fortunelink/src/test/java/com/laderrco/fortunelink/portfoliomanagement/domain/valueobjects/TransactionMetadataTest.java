package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.TransactionStatus;

public class TransactionMetadataTest {
    private TransactionStatus status;
    private TransactionSource source;
    private String description;
    private Instant createdAt;
    private TransactionMetadata testTransactionMetadata01;

    @BeforeEach 
    void init() {
        status = TransactionStatus.COMPLETED;
        source = TransactionSource.MANUAL_INPUT;
        description = "Transaction Metadata.";
        createdAt = Instant.now();
        testTransactionMetadata01 = new TransactionMetadata(status, source, description, createdAt, createdAt);
    }
    
    @Test
    void testCreateMetadata() {
        TransactionMetadata transactionMetadata = TransactionMetadata.createMetadata(status, source, description);
        assertEquals(status, transactionMetadata.transactionStatus());
        assertEquals(source, transactionMetadata.transactionSource());
        assertEquals(description, transactionMetadata.transactionDescription());
        assertTrue(transactionMetadata.createdAt().isAfter(createdAt));
    }

    @Test 
    void testCreateMetadataBadNull() {
        Exception e01 = assertThrows(NullPointerException.class, () -> TransactionMetadata.createMetadata(null, source, description));
        assertEquals("Transaction status cannot be null.", e01.getMessage());
        Exception e02 = assertThrows(NullPointerException.class, () -> TransactionMetadata.createMetadata(status, null, description));
        assertEquals("Transaction source cannot be null.", e02.getMessage());
    }

    @Test
    void testUpdateStatus() {
        TransactionStatus newStatus = TransactionStatus.FAILED;
        Instant newUpdatedAt = Instant.MAX;
        TransactionMetadata newMetadata = testTransactionMetadata01.updateStatus(newStatus, newUpdatedAt);

        assertTrue(newMetadata.updatedAt().isAfter(newMetadata.createdAt()));
        assertEquals(newStatus, newMetadata.transactionStatus());
    }
}
