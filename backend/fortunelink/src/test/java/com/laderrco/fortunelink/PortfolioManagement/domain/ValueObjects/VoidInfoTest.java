package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VoidInfoTest {
    private UUID voidingTransactionId;
    private VoidInfo voidInfo;

    @BeforeEach
    void init() {
        voidingTransactionId = UUID.randomUUID();
        voidInfo = new VoidInfo(voidingTransactionId);
    }

    @Test
    void testConstructor() {
        UUID voidingTransactionId1 = UUID.randomUUID();
        VoidInfo voidInfo1 = new VoidInfo(voidingTransactionId1);
        assertTrue(!voidInfo1.equals(voidInfo));
        
        assertThrows(NullPointerException.class, () -> new VoidInfo(null));
        
    }
    
    @Test
    void testEquals() {
        UUID voidingTransactionId1 = UUID.randomUUID();
        VoidInfo voidInfo1 = new VoidInfo(voidingTransactionId);
        
        assertTrue(voidInfo.equals(voidInfo1));
        assertTrue(voidInfo.equals(voidInfo));
        assertFalse(voidInfo.equals(null));
        assertFalse(voidInfo.equals(new Object()));
        assertFalse(voidInfo.equals(new VoidInfo(voidingTransactionId1)));
        assertTrue(voidInfo.equals(new VoidInfo(voidingTransactionId)));
        
    }
    
    @Test
    void testHashCode() {
        UUID voidingTransactionId1 = UUID.randomUUID();
        VoidInfo voidInfo1 = new VoidInfo(voidingTransactionId);
        assertTrue(voidInfo1.hashCode() == voidInfo.hashCode());
        assertTrue(new VoidInfo(voidingTransactionId1).hashCode() != voidInfo.hashCode());
        
    }

    @Test
    void testToString() {
        assertTrue(!voidInfo.toString().isEmpty());
    }

    @Test
    void testGetters() {

    }
}
