package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

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
    void testConstructorGood() {
        UUID voidingTransactionId1 = UUID.randomUUID();
        VoidInfo voidInfo1 = new VoidInfo(voidingTransactionId1);
        assertTrue(!voidInfo1.equals(voidInfo));
        
    }
    @Test 
    void testConstructorBadNull() {
        assertThrows(NullPointerException.class, () -> new VoidInfo(null));
    }
}
