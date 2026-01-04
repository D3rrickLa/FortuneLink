package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.queries;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


public class TransactionQueryTest {
    @Test
    @DisplayName("Should throw exception when startDate is after endDate")
    void constructor_ThrowsException_WhenDatesAreInvalid() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 1, 12, 0);

        assertThatThrownBy(() -> new TransactionQuery(null, null, null, start, end, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Start date must be before end date");
    }

    @Test
    @DisplayName("Should initialize empty set when assetSymbols is null")
    void constructor_InitializesEmptySet_WhenSymbolsNull() {
        TransactionQuery query = new TransactionQuery(null, null, null, null, null, null);
        
        assertThat(query.assetSymbols())
                .isNotNull()
                .isEmpty();
    }

    @Test
    @DisplayName("Should convert LocalDateTime to Instant at UTC")
    void helperMethods_ReturnCorrectInstants() {
        LocalDateTime start = LocalDateTime.of(2024, 6, 1, 10, 0);
        TransactionQuery query = new TransactionQuery(null, null, null, start, null, null);

        assertThat(query.startInstant()).isEqualTo(start.toInstant(ZoneOffset.UTC));
        assertThat(query.endInstant()).isNull();
    }

    @Test
    @DisplayName("Should handle null dates in helper methods without crashing")
    void helperMethods_HandleNullDates() {
        TransactionQuery query = new TransactionQuery(null, null, null, null, null, null);

        assertThatNoException().isThrownBy(query::startInstant);
        assertThat(query.startInstant()).isNull();
        assertThat(query.endInstant()).isNull();
    }
}
