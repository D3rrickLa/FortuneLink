package com.laderrco.fortunelink.portfolio_management.application.services;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.laderrco.fortunelink.portfolio_management.application.exceptions.InvalidDateRangeException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.InvalidSearchCriteriaException;
import com.laderrco.fortunelink.portfolio_management.application.models.TransactionSearchCriteria;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.queries.TransactionQuery;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories.TransactionQueryRepository;


@ExtendWith(MockitoExtension.class)
class TransactionQueryServiceTest {

    @Mock
    private TransactionQueryRepository repository;

    @InjectMocks
    private TransactionQueryService queryService;

    private PortfolioId portfolioId;
    private AccountId accountId;
    private TransactionSearchCriteria criteria;

    @BeforeEach
    void setUp() {
        portfolioId = PortfolioId.randomId();
        accountId = AccountId.randomId();
        // Default valid criteria
        criteria = TransactionSearchCriteria.builder()
                .portfolioId(portfolioId)
                .build();
    }

    @Nested
    @DisplayName("Paginated Queries")
    class PaginatedQueries {

        @Test
        @DisplayName("Should query transactions with default sorting")
        void queryWithDefaultSorting() {
            // Arrange
            List<Transaction> transactions = List.of(mock(Transaction.class));
            if (transactions.isEmpty()) {
                fail();
            }
            Page<Transaction> expectedPage = new PageImpl<>(transactions);
            
            when(repository.find(any(TransactionQuery.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            // Act
            Page<Transaction> result = queryService.queryTransactions(criteria, 0, 10);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(repository).find(any(), pageableCaptor.capture());
            
            Pageable capturedPageable = pageableCaptor.getValue();
            assertEquals(0, capturedPageable.getPageNumber());
            assertEquals(10, capturedPageable.getPageSize());
            String transactionDate = Objects.requireNonNull("transactionDate");
            var order = capturedPageable.getSort().getOrderFor(transactionDate);
            assertNotNull(order);
            assertTrue(order.isDescending());
        }

        @Test
        @DisplayName("Should query transactions with custom sorting")
        void queryWithCustomSorting() {
            Sort customSort = Sort.by("amount").ascending();
            when(repository.find(any(), any())).thenReturn(Page.empty());

            queryService.queryTransactions(criteria, 1, 20, customSort);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(repository).find(any(), captor.capture());
            assertEquals(customSort, captor.getValue().getSort());
        }
    }

    @Nested
    @DisplayName("Unpaginated (All) Queries")
    class UnpaginatedQueries {

        @Test
        @DisplayName("Should return all transactions as a list")
        void getAllTransactions() {
            List<Transaction> transactions = List.of(mock(Transaction.class), mock(Transaction.class));
            if (transactions.isEmpty()) {
                fail();
            }
            Page<Transaction> page = new PageImpl<>(transactions);
            
            when(repository.find(any(), any())).thenReturn(page);

            List<Transaction> result = queryService.getAllTransactions(criteria);

            assertEquals(2, result.size());
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(repository).find(any(), pageableCaptor.capture());
            
            // Verify it uses Integer.MAX_VALUE to simulate "all"
            assertEquals(Integer.MAX_VALUE, pageableCaptor.getValue().getPageSize());
        }
    }

    @Nested
    @DisplayName("Validation Logic")
    class ValidationLogic {

        @Test
        @DisplayName("Should throw exception if both portfolioId and accountId are missing")
        void throwWhenNoScopeProvided() {
            TransactionSearchCriteria invalidCriteria = TransactionSearchCriteria.builder().build();

            assertThrows(InvalidSearchCriteriaException.class, () -> 
                queryService.queryTransactions(invalidCriteria, 0, 10));
        }

        @Test
        @DisplayName("Should throw exception if startDate is after endDate")
        void throwWhenDateRangeInvalid() {
            TransactionSearchCriteria invalidDates = TransactionSearchCriteria.builder()
                    .portfolioId(portfolioId)
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().minusDays(1))
                    .build();

            assertThrows(InvalidDateRangeException.class, () -> 
                queryService.queryTransactions(invalidDates, 0, 10));
        }

        @ParameterizedTest
        @CsvSource({
            "-1, 10, Page number cannot be negative",
            "0, 0, Page size must be positive",
            "0, 1001, Page size cannot exceed 1000"
        })
        @DisplayName("Should validate pagination parameters")
        void validatePaginationParams(int page, int size, String expectedMessage) {
            Exception exception = assertThrows(IllegalArgumentException.class, () -> 
                queryService.queryTransactions(criteria, page, size));
            
            assertTrue(exception.getMessage().contains(expectedMessage));
        }
    }

    @Nested
    @DisplayName("Query Mapping")
    class MappingLogic {

        @Test
        @DisplayName("Should correctly map Criteria to Infrastructure Query")
        void mapCriteriaToQuery() {
            LocalDateTime start = LocalDateTime.now().minusDays(7);
            LocalDateTime end = LocalDateTime.now();
            
            TransactionSearchCriteria fullCriteria = TransactionSearchCriteria.builder()
                    .portfolioId(portfolioId)
                    .accountId(accountId)
                    .transactionType(TransactionType.BUY)
                    .startDate(start)
                    .endDate(end)
                    .assetSymbols(Set.of("AAPL"))
                    .build();

            when(repository.find(any(), any())).thenReturn(Page.empty());

            queryService.queryTransactions(fullCriteria, 0, 10);

            ArgumentCaptor<TransactionQuery> queryCaptor = ArgumentCaptor.forClass(TransactionQuery.class);
            verify(repository).find(queryCaptor.capture(), any());

            TransactionQuery captured = queryCaptor.getValue();
            assertEquals(portfolioId.portfolioId(), captured.portfolioId());
            assertEquals(accountId.accountId(), captured.accountId());
            assertEquals(TransactionType.BUY, captured.transactionType());
            assertEquals(start, captured.startDate());
            assertEquals(end, captured.endDate());
            assertEquals(Set.of("AAPL"), captured.assetSymbols());
        }
    }
}