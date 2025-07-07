package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionStatus;

public class TransactionMetadataTest {

    @Test
    void testCreateMetadata() {
        TransactionStatus transactionStatus = TransactionStatus.COMPLETED;
        TransactionSource transactionSource = TransactionSource.MANUAL_INPUT;
        String desc = "something here";
        Instant createdAt = Instant.now();

        TransactionMetadata metadata = TransactionMetadata.createMetadata(transactionStatus, transactionSource, desc, createdAt);
        assertNotNull(metadata);
    }

    @Test 
    void testCreateMetadataInValidNull() {
        TransactionStatus transactionStatus = TransactionStatus.COMPLETED;
        TransactionSource transactionSource = TransactionSource.MANUAL_INPUT;
        String desc = "something here";
        Instant createdAt = Instant.now();

        Exception e1 = assertThrows(NullPointerException.class, () ->TransactionMetadata.createMetadata(null, transactionSource, desc, createdAt));
        assertEquals("Transaction status cannot be null.", e1.getMessage());
        e1 = assertThrows(NullPointerException.class, () ->TransactionMetadata.createMetadata(transactionStatus, null, desc, createdAt));
        assertEquals("Transaction source cannot be null.", e1.getMessage());
        e1 = assertThrows(NullPointerException.class, () ->TransactionMetadata.createMetadata(transactionStatus, transactionSource, desc, null));
        assertEquals("Creation timestamp cannot be null.", e1.getMessage());
    }

    @Test
    void testUpdateStatus() {
        TransactionStatus transactionStatus = TransactionStatus.COMPLETED;
        TransactionSource transactionSource = TransactionSource.MANUAL_INPUT;
        String desc = "something here";
        Instant createdAt = Instant.now();

        TransactionMetadata metadata = TransactionMetadata.createMetadata(transactionStatus, transactionSource, desc, createdAt);
        
        TransactionMetadata newMetadata = metadata.updateStatus(TransactionStatus.CANCELLED, Instant.MAX);
        assertTrue(metadata.createdAt().isBefore(newMetadata.updatedAt()));
    }
    
    @Test
    void testUpdateStatusInValidNull() {
        TransactionStatus transactionStatus = TransactionStatus.COMPLETED;
        TransactionSource transactionSource = TransactionSource.MANUAL_INPUT;
        String desc = "something here";
        Instant createdAt = Instant.now();
    
        TransactionMetadata metadata = TransactionMetadata.createMetadata(transactionStatus, transactionSource, desc, createdAt);
        Exception e1 = assertThrows(NullPointerException.class, () -> metadata.updateStatus(null, Instant.MAX));
        assertEquals("New transaction status cannot be null.", e1.getMessage());
        e1 = assertThrows(NullPointerException.class, () -> metadata.updateStatus(TransactionStatus.FAILED, null));
        assertEquals("New updated timestamp cannot be null.", e1.getMessage());
    }
}
