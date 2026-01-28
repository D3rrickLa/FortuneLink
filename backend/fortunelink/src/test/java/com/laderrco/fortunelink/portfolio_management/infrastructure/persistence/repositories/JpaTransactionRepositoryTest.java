package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.domain.Specification;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.TransactionEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.queries.TransactionQuery;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories.JpaTransactionRepository.TransactionSpecifications;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class JpaTransactionRepositoryTest {
    @Nested
    public class TransactionSpecificationsTests {
        private Root<TransactionEntity> root;
        private CriteriaQuery<?> query;
        private CriteriaBuilder cb;
        private Path<Object> path;

        @BeforeEach
        @SuppressWarnings("unchecked")
        void setUp() {
            root = mock(Root.class);
            query = mock(CriteriaQuery.class);
            cb = mock(CriteriaBuilder.class);
            path = mock(Path.class);

            // Mock common behavior: root.get("fieldName") returns a path
            when(root.get(anyString())).thenReturn(path);
            when(path.get(anyString())).thenReturn(path);
        }

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should build predicates for all provided filters")
        void shouldBuildFullPredicateList() {
            // Given
            UUID portfolioId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();
            TransactionQuery searchQuery = new TransactionQuery(
                    portfolioId,
                    accountId,
                    TransactionType.BUY, // Assuming Enum
                    now,
                    now.plusSeconds(3600),
                    Set.of("AAPL", "MSFT"));

            Specification<TransactionEntity> spec = JpaTransactionRepository.TransactionSpecifications
                    .withFilters(searchQuery);

            // When
            spec.toPredicate(root, query, cb);

            // Then
            // Verify Portfolio ID equality check
            verify(cb).equal(root.get("portfolioId"), portfolioId);

            // Verify Account ID check (nested root.get("account").get("id"))
            verify(root).get("account");
            verify(path).get("id");
            verify(cb).equal(path, accountId);

            // Verify Transaction Type check
            verify(cb).equal(root.get("transactionType"), "BUY");

            // Verify Date Range checks
            verify(cb).greaterThanOrEqualTo(root.get("transactionDate"), searchQuery.startInstant());
            verify(cb).lessThanOrEqualTo(root.get("transactionDate"), searchQuery.endInstant());

            // Verify IN clause for symbols
            verify(root).get("primaryId");
            verify(path).in(searchQuery.assetSymbols());
            TransactionSpecifications s = new TransactionSpecifications();
            assertNotNull(s);
        }

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should return empty predicate when query is empty")
        void shouldHandleEmptyQuery() {
            // Given
            TransactionQuery emptyQuery = new TransactionQuery(null, null, null, null, null, null);
            Specification<TransactionEntity> spec = JpaTransactionRepository.TransactionSpecifications
                    .withFilters(emptyQuery);

            // When
            spec.toPredicate(root, query, cb);

            // Then
            verify(cb).and(any(Predicate[].class));
            // Ensure no specific filters were called
            verify(cb, never()).equal(any(), any());
        }

        @SuppressWarnings({ "null", "unchecked" })
        @Test
        @DisplayName("Should add IN clause when asset symbols are provided")
        void shouldAddInClauseForSymbols() {
            // Given
            Set<String> symbols = Set.of("AAPL", "TSLA");
            TransactionQuery queryWithSymbols = new TransactionQuery(
                    null, null, null, null, null, symbols);

            // We need to mock the Path returned by root.get() because .in() is called on it
            Path<Object> primaryIdPath = mock(Path.class);
            when(root.get("primaryId")).thenReturn(primaryIdPath);

            Specification<TransactionEntity> spec = JpaTransactionRepository.TransactionSpecifications
                    .withFilters(queryWithSymbols);

            // When
            spec.toPredicate(root, query, cb);

            // Then
            verify(root).get("primaryId");
            verify(primaryIdPath).in(symbols); // Verifies the 'IN' branch was executed
        }

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should skip IN clause when asset symbols list is empty")
        void shouldSkipInClauseWhenSymbolsEmpty() {
            // Given - Test both null and empty scenarios
            TransactionQuery queryEmptySymbols = new TransactionQuery(
                    null, null, null, null, null, Collections.emptySet());

            Specification<TransactionEntity> spec = JpaTransactionRepository.TransactionSpecifications
                    .withFilters(queryEmptySymbols);

            // When
            spec.toPredicate(root, query, cb);

            // Then
            verify(root, never()).get("primaryId");
            verify(cb, times(1)).and(any(Predicate[].class)); // Only the empty 'and' should be called
        }

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should skip IN clause when asset symbols list is empty")
        void shouldSkipInClauseWhenSymbolsEmpty2() {
            // Given - Test both null and empty scenarios
            TransactionQuery queryEmptySymbols = new TransactionQuery(
                    null, null, null, null, null, null);

            Specification<TransactionEntity> spec = JpaTransactionRepository.TransactionSpecifications
                    .withFilters(queryEmptySymbols);

            // When
            spec.toPredicate(root, query, cb);

            // Then
            verify(root, never()).get("primaryId");
            verify(cb, times(1)).and(any(Predicate[].class)); // Only the empty 'and' should be called
        }

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should add IN clause for asset symbols")
        void shouldAddInClauseForSymbols2() {
            // Given
            Set<String> symbols = Set.of("AAPL", "GOOGL");
            TransactionQuery queryWithSymbols = mock(TransactionQuery.class);
            when(queryWithSymbols.assetSymbols()).thenReturn(symbols);

            // Mock the chain: root.get() -> returns a Path -> which calls .in()
            @SuppressWarnings("unchecked")
            Path<Object> primaryIdPath = mock(Path.class);
            when(root.get("primaryId")).thenReturn(primaryIdPath);

            // We must mock the return of .in() because it's added to the list
            Predicate inPredicate = mock(Predicate.class);
            when(primaryIdPath.in(any(Collection.class))).thenReturn(inPredicate);

            Specification<TransactionEntity> spec = JpaTransactionRepository.TransactionSpecifications
                    .withFilters(queryWithSymbols);

            // When
            spec.toPredicate(root, this.query, cb);

            // Then
            verify(root).get("primaryId");
            // Explicitly verify the Collection version of .in()
            verify(primaryIdPath).in(symbols);
        }

        @SuppressWarnings("null")
        @Test
        void testAssetSymbolsBranchCoverage() {
            // 1. Create query with actual data
            Set<String> symbols = Set.of("MSFT");
            TransactionQuery query = new TransactionQuery(null, null, null, null, null, symbols);

            // 2. Mock the Path and the Predicate it returns
            @SuppressWarnings("unchecked")
            Path<Object> pathMock = mock(Path.class);
            Predicate inPredicateMock = mock(Predicate.class);

            when(root.get("primaryId")).thenReturn(pathMock);
            when(pathMock.in(any(Collection.class))).thenReturn(inPredicateMock);

            // 3. Execute
            Specification<TransactionEntity> spec = JpaTransactionRepository.TransactionSpecifications
                    .withFilters(query);
            spec.toPredicate(root, this.query, cb);

            // 4. Verification
            verify(pathMock, times(1)).in(symbols);
        }

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should combine all predicates using AND")
        void shouldCombinePredicates() {
            // Given a query that triggers two predicates
            TransactionQuery dualQuery = new TransactionQuery(
                    UUID.randomUUID(), null, null, null, null, Set.of("MSFT"));

            // When
            Specification<TransactionEntity> spec = JpaTransactionRepository.TransactionSpecifications
                    .withFilters(dualQuery);
            spec.toPredicate(root, query, cb);

            // Then
            ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
            verify(cb).and(captor.capture());

            // Check that the array passed to cb.and() contains exactly 2 predicates
            assertThat(captor.getValue()).hasSize(2);
        }
    }
}
