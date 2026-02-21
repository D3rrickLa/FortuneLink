package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

public class TransactionDateTest {

    @Test
    void testTransactionDateConstructor_success() {
        TransactionDate date = new TransactionDate(Instant.now());
        assertNotNull(date);
    }
    
    @Test
    void testMethods() {
        Instant time = Instant.now();
        TransactionDate date = new TransactionDate(time);
        TransactionDate otherDate = new TransactionDate(Instant.MAX);
        TransactionDate otherDate2 = new TransactionDate(Instant.MIN);

        assertTrue(date.isBefore(otherDate));
        assertTrue(date.isAfter(otherDate2));

        assertEquals(time.toEpochMilli(), date.toEpochMilli());


    }
}
