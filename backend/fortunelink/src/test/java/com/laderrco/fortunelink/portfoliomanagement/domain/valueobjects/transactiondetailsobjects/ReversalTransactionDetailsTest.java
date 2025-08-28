package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.TransactionId;

public class ReversalTransactionDetailsTest {
    
    @Test
    void constructor_isValid() {
        TransactionId id = TransactionId.createRandom();
        String reason = "some test reason";
        TransactionSource source = TransactionSource.EXTERNAL;
        String description = "testing";

        ReversalTransactionDetails details = new ReversalTransactionDetails(id, reason, source, description, null);
        assertEquals(id, details.getTransactionId());
        assertEquals(reason, details.getReason());
    }

    @Test
    void constructor_throwsExceptionWhenReasonIsEmpty() {
        TransactionId id = TransactionId.createRandom();
        String reason = "\r\r\r\r\r";
        TransactionSource source = TransactionSource.EXTERNAL;
        String description = "testing";

        assertThrows(IllegalArgumentException.class, () ->new ReversalTransactionDetails(id, reason, source, description, null));
    }

    @Test
    void constructor_throwsExceptionWhenReasonIsGreaterThanLength1000() {
        TransactionId id = TransactionId.createRandom();
        String reason = "a".repeat(10001);
        TransactionSource source = TransactionSource.EXTERNAL;
        String description = "testing";

        assertThrows(IllegalArgumentException.class, () ->new ReversalTransactionDetails(id, reason, source, description, null));
    }
    @Test
    void constructor_throwsNotExceptionWhenReasonIsLength1000() {
        TransactionId id = TransactionId.createRandom();
        String reason = "a".repeat(1000);
        TransactionSource source = TransactionSource.EXTERNAL;
        String description = "testing";

        assertDoesNotThrow(() ->new ReversalTransactionDetails(id, reason, source, description, null));
    }


}
