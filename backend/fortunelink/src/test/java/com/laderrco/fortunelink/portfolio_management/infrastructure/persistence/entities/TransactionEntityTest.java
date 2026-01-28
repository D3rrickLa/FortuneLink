package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

public class TransactionEntityTest {
    @Test
    void testOnCreateMethod() {
        TransactionEntity entity = new TransactionEntity();
        entity.onCreate();
        assertTrue(entity.getCreatedAt().isBefore(Instant.now().plus(Duration.ofHours(2))));
    }
}
