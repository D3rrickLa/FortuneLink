package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Instant;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.ReversalReason;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.ReversalType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.TradeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.TransactionId;

public class ReversalTransactionDetailsTest {
    Currency currency = Currency.getInstance("CAD");

    @Test
    void constructor_isValid() {
        TransactionId id = TransactionId.createRandom();
        ReversalReason reason = ReversalReason.ERROR_ENTRY;
        ReversalType tReversalType = ReversalType.FULL;
        String init = "User";
        Instant reverseInstant = Instant.now();

        TransactionSource source = TransactionSource.EXTERNAL;
        String desc = "something";

        ReversalTransactionDetails details = new ReversalTransactionDetails(id, reason, tReversalType, currency, init, reverseInstant, desc, source, desc, null);
        assertEquals(id, details.getOriginalTransactionId());
        assertEquals(reason, details.getReason());
        assertEquals(tReversalType, details.getReversalType());
        assertEquals(currency, details.getCurrency());
        assertEquals(init, details.getInitiatedBy());
        assertEquals(reverseInstant, details.getReversedAt());
        assertEquals(desc, details.getNote());
    }

    @Test
    void calcualateNetImpact_isValid() {
        TransactionId id = TransactionId.createRandom();
        ReversalReason reason = ReversalReason.ERROR_ENTRY;
        ReversalType tReversalType = ReversalType.FULL;
        String init = "User";
        Instant reverseInstant = Instant.now();

        TransactionSource source = TransactionSource.EXTERNAL;
        String desc = "something";

        ReversalTransactionDetails details = new ReversalTransactionDetails(id, reason, tReversalType, currency, init, reverseInstant, desc, source, desc, null);
        assertEquals(Money.ZERO(currency), details.calculateNetImpact(TradeType.BUY_REVERSAL));
    }
}
