package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.SimpleTransactionDetails;

public class SimpleTransactionDetailsTest {

    @Test
    void testConstructor() {
        String desc = "some desc";
        SimpleTransactionDetails simpleTransactionDetails = new SimpleTransactionDetails(desc);
        assertEquals(desc, simpleTransactionDetails.getDescription());
    }
}
