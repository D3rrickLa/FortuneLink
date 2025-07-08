package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.TransactionDetails;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class TransactionTest {
    UUID transactionId;
    UUID portfolioId;
    Currency usd;
    List<Fee> fees;
    
    @BeforeEach
    void init() {
        transactionId = UUID.randomUUID();
        portfolioId = UUID.randomUUID();
        usd = Currency.getInstance("USD");
        fees = new ArrayList<>();
    }
    
    @Test
    void testIsReversedValid() {
        UUID correlationId = UUID.randomUUID();
        UUID parentTransactionId = null;

        TransactionType transactionType = TransactionType.REVERSAL_BUY;
        Money totalTransactionAmount = new Money(25.45d, usd);
        Instant transactionDate = Instant.now();
        TransactionDetails transactionDetails = new TransactionDetails() {};
        
        TransactionMetadata transactionMetadata = TransactionMetadata.createMetadata(
            TransactionStatus.ACTIVE,
            TransactionSource.MANUAL_INPUT,
            "Some description about reversing my order.",
            transactionDate
        );

        fees.add(
            new Fee(FeeType.COMMISSION, new Money(0.05d, usd))
        );
        fees.add(
            new Fee(FeeType.OTHER, new Money(1.25d, usd))
        );
        boolean hidden = false;
        int version = 1;

        Transaction newTransaction = new Transaction(parentTransactionId, parentTransactionId, correlationId, parentTransactionId, transactionType, totalTransactionAmount, transactionDate, transactionDetails, transactionMetadata, fees, hidden, version);
        assertNotNull(newTransaction);

        assertTrue(newTransaction.isReversed());
    }

}
