package com.laderrco.fortunelink.portfolio_management.application.queries;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Pageable;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;

import static org.assertj.core.api.Assertions.*;

public class GetTransactionHistoryQueryTest {
    private final UserId validUserId = UserId.randomId();

    @Test
    @DisplayName("Should create query successfully with valid parameters")
    void shouldCreateValidQuery() {
        GetTransactionHistoryQuery query = new GetTransactionHistoryQuery(
            validUserId, null, null, null, null, 0, 50
        );

        assertThat(query.pageNumber()).isEqualTo(0);
        assertThat(query.pageSize()).isEqualTo(50);
        
        Pageable pageable = query.toPageable();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should use default values when using convenience constructor")
    void shouldHandleDefaultConstructor() {
        GetTransactionHistoryQuery query = new GetTransactionHistoryQuery(validUserId);

        assertThat(query.pageNumber()).isEqualTo(0);
        assertThat(query.pageSize()).isEqualTo(20);
        assertThat(query.accountId()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -5})
    @DisplayName("Should throw exception for negative page numbers")
    void shouldThrowForNegativePage(int invalidPage) {
        assertThatThrownBy(() -> 
            new GetTransactionHistoryQuery(validUserId, null, null, null, null, invalidPage, 20)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Page index must not be less than zero");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("Should throw exception for non-positive page sizes")
    void shouldThrowForNonPositivePageSize(int invalidSize) {
        assertThatThrownBy(() -> 
            new GetTransactionHistoryQuery(validUserId, null, null, null, null, 0, invalidSize)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Page size must not be less than one");
    }

    @Test
    @DisplayName("Should throw exception when page size exceeds custom limit of 100")
    void shouldThrowForTooLargePageSize() {
        assertThatThrownBy(() -> 
            new GetTransactionHistoryQuery(validUserId, null, null, null, null, 0, 101)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessage("Page size cannot exceed 100");
    }
}
