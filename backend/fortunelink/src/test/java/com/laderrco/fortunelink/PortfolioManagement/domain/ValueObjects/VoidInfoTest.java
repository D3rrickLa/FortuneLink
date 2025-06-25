package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VoidInfoTest {
    private UUID voidingTransactionId;
    private String voidReason;
    private VoidInfo voidInfo;

    @BeforeEach
    void init() {
        voidingTransactionId = UUID.randomUUID();
        voidReason = "SOME TEST REASON";
        voidInfo = new VoidInfo(voidingTransactionId, voidReason);
    }

    @Test
    void testConstructor() {
        UUID voidingTransactionId1 = UUID.randomUUID();
        String voidReasonq = "SOME TEST REASON";
        VoidInfo voidInfo1 = new VoidInfo(voidingTransactionId1, voidReasonq);
        assertTrue(!voidInfo1.equals(voidInfo));
        
        assertThrows(NullPointerException.class, () -> new VoidInfo(null, voidReasonq));
        assertThrows(NullPointerException.class, () -> new VoidInfo(voidingTransactionId1, null));
        assertThrows(IllegalArgumentException.class, () -> new VoidInfo(voidingTransactionId1, "    \r\r\n\n"));
        
    }
    
    @Test
    void testEquals() {
        UUID voidingTransactionId1 = UUID.randomUUID();
        String voidReasonq = "SOME TEST REASON 2";
        VoidInfo voidInfo1 = new VoidInfo(voidingTransactionId, voidReason);
        
        assertTrue(voidInfo.equals(voidInfo1));
        assertTrue(voidInfo.equals(voidInfo));
        assertFalse(voidInfo.equals(null));
        assertFalse(voidInfo.equals(new Object()));
        assertFalse(voidInfo.equals(new VoidInfo(voidingTransactionId1, voidReason)));
        assertFalse(voidInfo.equals(new VoidInfo(voidingTransactionId, voidReasonq)));
        
    }
    
    @Test
    void testHashCode() {
        UUID voidingTransactionId1 = UUID.randomUUID();
        String voidReasonq = "SOME TEST REASON";
        VoidInfo voidInfo1 = new VoidInfo(voidingTransactionId, voidReasonq);
        assertTrue(voidInfo1.hashCode() == voidInfo.hashCode());
        assertTrue(new VoidInfo(voidingTransactionId1, voidReasonq).hashCode() != voidInfo.hashCode());
        
    }

    @Test
    void testToString() {
        assertTrue(!voidInfo.toString().isEmpty());
    }

    @Test
    void testGetters() {

    }
}
