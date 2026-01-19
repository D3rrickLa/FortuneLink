package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.queries;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.application.exceptions.InvalidDateRangeException;
import com.laderrco.fortunelink.portfolio_management.application.models.TransactionSearchCriteria;
import com.laderrco.fortunelink.portfolio_management.application.services.TransactionQueryService;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories.TransactionQueryRepository;


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

    @Nested
    @DisplayName("Testing the private methods")
    public class PrivateMethodTests {
        
        private TransactionQueryService transactionQueryService;
        private TransactionQueryRepository transactionQueryRepository;

        @BeforeEach
        void init() {
            transactionQueryRepository = mock(TransactionQueryRepository.class);
            transactionQueryService = new TransactionQueryService(transactionQueryRepository);
        }

        @Test
        void testBuildQueryReturnsNullWhenCriteriaPortfolioIdIsNull() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
            Method buildQuery = transactionQueryService.getClass().getDeclaredMethod("buildQuery", TransactionSearchCriteria.class);
            buildQuery.setAccessible(true);

            TransactionSearchCriteria criteria = TransactionSearchCriteria.builder()
                .portfolioId(null)
                .accountId(AccountId.randomId())
                .transactionType(TransactionType.BUY)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now())
                .assetSymbols(null)
                .build();
            
            TransactionQuery actual = (TransactionQuery) buildQuery.invoke(transactionQueryService, criteria);
            assertNull(actual.portfolioId());

        }

        @Test
        void testValidateCriteriaThrowsInvlaidDateRangeExceptionWhenStartDateIsAfterEndDate() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
            Method validateCriteria = transactionQueryService.getClass().getDeclaredMethod("validateCriteria", TransactionSearchCriteria.class);
            validateCriteria.setAccessible(true);

            TransactionSearchCriteria criteria = TransactionSearchCriteria.builder()
                .portfolioId(PortfolioId.randomId())
                .accountId(AccountId.randomId())
                .transactionType(TransactionType.BUY)
                .startDate(LocalDateTime.MAX)
                .endDate(LocalDateTime.now())
                .assetSymbols(null)
                .build();
            
            // 1. Assert that the reflection wrapper is thrown
            InvocationTargetException wrapper = assertThrows(InvocationTargetException.class, () ->
                validateCriteria.invoke(transactionQueryService, criteria));

            // 2. Assert that the ACTUAL cause inside the wrapper is your custom exception
            assertEquals(InvalidDateRangeException.class, wrapper.getCause().getClass());
            
            // 3. Optional: Verify the error message
            assertTrue(wrapper.getCause().getMessage().contains("Start date cannot be after end date"));

        }

        @Test
        void testValidateCriteriaDoesNotThrowsWhenStartDateOEndDaterIsNull() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
            Method validateCriteria = transactionQueryService.getClass().getDeclaredMethod("validateCriteria", TransactionSearchCriteria.class);
            validateCriteria.setAccessible(true);

            TransactionSearchCriteria criteria = TransactionSearchCriteria.builder()
                .portfolioId(PortfolioId.randomId())
                .accountId(AccountId.randomId())
                .transactionType(TransactionType.BUY)
                .startDate(null)
                .endDate(LocalDateTime.now())
                .assetSymbols(null)
                .build();
            
            assertDoesNotThrow(() -> validateCriteria.invoke(transactionQueryService, criteria));

            TransactionSearchCriteria criteria2 = TransactionSearchCriteria.builder()
                .portfolioId(PortfolioId.randomId())
                .accountId(AccountId.randomId())
                .transactionType(TransactionType.BUY)
                .startDate(LocalDateTime.now())
                .endDate(null)
                .assetSymbols(null)
                .build();
            
            assertDoesNotThrow(() -> validateCriteria.invoke(transactionQueryService, criteria2));

        }

        @Test
        void testValidateCriteriaDoesNotThrowsWhenPortfiolioOrAccountIdIsNull() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
            Method validateCriteria = transactionQueryService.getClass().getDeclaredMethod("validateCriteria", TransactionSearchCriteria.class);
            validateCriteria.setAccessible(true);

            TransactionSearchCriteria criteria = TransactionSearchCriteria.builder()
                .portfolioId(null)
                .accountId(AccountId.randomId())
                .transactionType(TransactionType.BUY)
                .startDate(LocalDateTime.MIN)
                .endDate(LocalDateTime.now())
                .assetSymbols(null)
                .build();
            
                assertDoesNotThrow(() -> validateCriteria.invoke(transactionQueryService, criteria));

            TransactionSearchCriteria criteria2 = TransactionSearchCriteria.builder()
                .portfolioId(PortfolioId.randomId())
                .accountId(null)
                .transactionType(TransactionType.BUY)
                .startDate(LocalDateTime.MIN)
                .endDate(LocalDateTime.now())
                .assetSymbols(null)
                .build();
            
                assertDoesNotThrow(() -> validateCriteria.invoke(transactionQueryService, criteria2));

        }
        
    }
}
