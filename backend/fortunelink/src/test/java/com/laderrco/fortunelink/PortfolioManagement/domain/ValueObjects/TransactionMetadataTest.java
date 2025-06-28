package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionSource;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionStatus;

public class TransactionMetadataTest {

    @Test
    void testConstructor() {
        TransactionStatus transactionStatus = TransactionStatus.ACTIVE;
        TransactionSource transactionSource = TransactionSource.MANUAL_INPUT;
        String desc = "Hello World!";

        assertThrows(NullPointerException.class, () -> new TransactionMetadata(null, transactionSource, desc, Instant.now(), Instant.now()));
        assertThrows(NullPointerException.class, () -> new TransactionMetadata(transactionStatus, null, desc, Instant.now(), Instant.now()));

        new TransactionMetadata(transactionStatus, transactionSource, desc, Instant.now(), Instant.now());
    }

    @Test
    void testEquals() {
        TransactionStatus transactionStatus = TransactionStatus.ACTIVE;
        TransactionSource transactionSource = TransactionSource.MANUAL_INPUT;
        String desc = "Hello World!";
        TransactionMetadata transactionMetadata = new TransactionMetadata(transactionStatus, transactionSource, desc, Instant.now(), Instant.now());
        TransactionMetadata transactionMetadata2 = new TransactionMetadata(transactionStatus, transactionSource, desc, Instant.now(), Instant.now());

        assertTrue(transactionMetadata.equals(transactionMetadata2));
        assertTrue(transactionMetadata.equals(transactionMetadata));
        assertFalse(transactionMetadata.equals(null));
        assertFalse(transactionMetadata.equals(new Object()));
        assertFalse(transactionMetadata
                .equals(new TransactionMetadata(TransactionStatus.CANCELLED, transactionSource, desc, Instant.now(), Instant.now())));
        assertFalse(transactionMetadata
                .equals(new TransactionMetadata(transactionStatus, TransactionSource.PLATFORM_SYNC, desc, Instant.now(), Instant.now())));
        assertFalse(transactionMetadata
                .equals(new TransactionMetadata(transactionStatus, transactionSource, "WRONG STRING DESC", Instant.now(), Instant.now())));
    }

    @Test
    void testHashCode() {
        TransactionStatus transactionStatus = TransactionStatus.ACTIVE;
        TransactionSource transactionSource = TransactionSource.MANUAL_INPUT;
        String desc = "Hello World!";
        TransactionMetadata transactionMetadata = new TransactionMetadata(transactionStatus, transactionSource, desc, Instant.now(), Instant.now());
        TransactionMetadata transactionMetadata2 = new TransactionMetadata(transactionStatus, transactionSource, desc, Instant.now(), Instant.now());
        assertEquals(transactionMetadata.hashCode(), transactionMetadata2.hashCode());
        assertFalse(transactionMetadata.hashCode() == new TransactionMetadata(transactionStatus, transactionSource,
                "WRONG STRING DESC", Instant.now(), Instant.now()).hashCode());
    }

    @Test
    void testToString() {
        TransactionStatus transactionStatus = TransactionStatus.ACTIVE;
        TransactionSource transactionSource = TransactionSource.MANUAL_INPUT;
        String desc = "Hello World!";
        TransactionMetadata transactionMetadata = new TransactionMetadata(transactionStatus, transactionSource, desc, Instant.now(), Instant.now());
        assertNotNull(transactionMetadata.toString());
    }

    @Test
    void testGetters() {
        TransactionStatus transactionStatus = TransactionStatus.ACTIVE;
        TransactionSource transactionSource = TransactionSource.MANUAL_INPUT;
        String desc = "Hello World!";
        TransactionMetadata transactionMetadata = new TransactionMetadata(transactionStatus, transactionSource, desc, Instant.now(), Instant.now());
        assertEquals(transactionStatus, transactionMetadata.transactionStatus());
        assertEquals(transactionSource, transactionMetadata.transactionSource());
        assertEquals(desc, transactionMetadata.transactionDescription());
    }
}
