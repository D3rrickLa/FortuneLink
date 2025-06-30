package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.VoidTransactionDetails;

public class VoidTransactionDetailsTest {
    @Test
    void testConstructor() {
        VoidTransactionDetails voidTransactionDetails1 = new VoidTransactionDetails(UUID.randomUUID(), "SOME REASON");
        VoidTransactionDetails voidTransactionDetails2 = new VoidTransactionDetails(UUID.randomUUID(), "SOME REASON");

        assertThrows(NullPointerException.class, () -> new VoidTransactionDetails(null, "REASON"));
        assertThrows(NullPointerException.class, () -> new VoidTransactionDetails(UUID.randomUUID(), null));
        assertThrows(IllegalArgumentException.class, () -> new VoidTransactionDetails(UUID.randomUUID(), " \n \n"));

        assertNotEquals(voidTransactionDetails1, voidTransactionDetails2);

        VoidTransactionDetails voidTransactionDetails3 = voidTransactionDetails1;
        assertEquals(voidTransactionDetails1.getOriginalTransactionId(),
                voidTransactionDetails3.getOriginalTransactionId());
        assertEquals(voidTransactionDetails1.getReason(), voidTransactionDetails3.getReason());
    }

    @Test
    void testEquals() {
        UUID id = UUID.randomUUID();
        VoidTransactionDetails voidTransactionDetails1 = new VoidTransactionDetails(id, "SOME REASON");
        VoidTransactionDetails voidTransactionDetails2 = new VoidTransactionDetails(UUID.randomUUID(), "SOME REASON2");
        VoidTransactionDetails voidTransactionDetails3 =  new VoidTransactionDetails(id, "SOME REASON");

        assertTrue(voidTransactionDetails1.equals(voidTransactionDetails3));
        assertTrue(voidTransactionDetails1.equals(voidTransactionDetails1));
        assertFalse(voidTransactionDetails1.equals(null));
        assertFalse(voidTransactionDetails1.equals(new Object()));
        assertFalse(voidTransactionDetails1.equals(voidTransactionDetails2));
        assertFalse(voidTransactionDetails1.equals(new VoidTransactionDetails(UUID.randomUUID(), "SOME REASON")));
        assertFalse(voidTransactionDetails1.equals(new VoidTransactionDetails(id, "my really good reason")));

    }

    @Test
    void testHashCode() {
        UUID id = UUID.randomUUID();
        VoidTransactionDetails voidTransactionDetails1 = new VoidTransactionDetails(id, "SOME REASON");
        VoidTransactionDetails voidTransactionDetails2 = new VoidTransactionDetails(UUID.randomUUID(), "SOME REASON2");

        assertFalse(voidTransactionDetails1.hashCode() == voidTransactionDetails2.hashCode());
    }
}
