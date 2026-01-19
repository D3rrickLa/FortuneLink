package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.AccountEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.PortfolioEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.TransactionEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.AssetEntityMapperImpl;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.PortfolioEntityMapperImpl;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.TransactionEntityMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.TransactionEntityMapperImpl;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.queries.TransactionQuery;

import jakarta.transaction.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DataJpaTest
@Testcontainers
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({
        TransactionQueryRepositoryImpl.class,
        TransactionEntityMapperImpl.class,
        PortfolioEntityMapperImpl.class,
        AssetEntityMapperImpl.class,
        PortfolioEntity.class,
        AccountEntity.class
})
class TransactionQueryRepositoryImplTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.7");

    @Autowired
    TransactionQueryRepository repository;

    @Autowired
    TestEntityManager entityManager;

    Instant testTime = Instant.now();
    UUID testUserId = UUID.fromString("482592a2-cb1d-4571-ad44-97047fa0b63f");

    @BeforeAll
    static void setupDb() throws Exception {
        // Force start the container if it hasn't started yet
        postgres.start();
        // We create the missing "auth" piece before Flyway triggers
        try (var conn = postgres.createConnection("")) {
            var st = conn.createStatement();
            st.execute("CREATE SCHEMA IF NOT EXISTS auth;");
            st.execute("CREATE TABLE IF NOT EXISTS auth.users (id UUID PRIMARY KEY, email VARCHAR(255));");
        }
    }

    @BeforeEach
    void init() {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO auth.users (id, email) VALUES (:id, :email)")
                .setParameter("id", testUserId)
                .setParameter("email", "test@example.com")
                .executeUpdate();
    }

    @Test
    void contextLoads() {
        // Just to see if it starts
    }

    @Test
    public void findByPortfolio_returnsTransactions() {
        PortfolioEntity portfolioEntity = creaPortfolioEntity();
        AccountEntity accountEntity = createAccountEntity(portfolioEntity);
        UUID portfolioId = portfolioEntity.getId();

        LocalDateTime testDate = LocalDateTime.of(2024, 6, 1, 12, 0);

        persistTransaction(portfolioId, accountEntity, "AAPL", TransactionType.BUY, testDate);
        persistTransaction(portfolioId, accountEntity, "MSFT", TransactionType.SELL, testDate);

        TransactionQuery query = new TransactionQuery(
                portfolioId,
                null,
                null,
                LocalDateTime.of(2024, 01, 1, 0, 0),
                LocalDateTime.of(2025, 01, 1, 0, 0),
                Set.of("AAPL", "MSFT"));

        Page<Transaction> result = repository.find(query, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(t -> t.getAssetIdentifier().getPrimaryId())
                .containsExactlyInAnyOrder("AAPL", "MSFT");
    }

    @Test
    public void findByAccountAndType_filtersCorrectly() {
        PortfolioEntity portfolioEntity = creaPortfolioEntity();
        AccountEntity accountId = createAccountEntity(portfolioEntity);
        portfolioEntity.setAccounts(List.of(accountId));

        persistTransaction(portfolioEntity.getId(), accountId, "AAPL", TransactionType.BUY, LocalDateTime.now());
        persistTransaction(portfolioEntity.getId(), accountId, "MSFT", TransactionType.SELL, LocalDateTime.now());

        TransactionQuery query = new TransactionQuery(
                null,
                accountId.getId(),
                TransactionType.BUY,
                null,
                null,
                null);

        Page<Transaction> result = repository.find(query, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)
                .getAssetIdentifier().getPrimaryId()).isEqualTo("AAPL");
    }

    @Test
    public void find_filtersByDateRange() {
        PortfolioEntity portfolioEntity = creaPortfolioEntity();
        AccountEntity accountEntity = createAccountEntity(portfolioEntity);
        UUID portfolioId = portfolioEntity.getId();

        LocalDateTime now = LocalDateTime.now();

        persistTransaction(portfolioId, accountEntity, "AAPL", TransactionType.BUY, now.minusDays(5));
        persistTransaction(portfolioId, accountEntity, "MSFT", TransactionType.SELL, now);

        TransactionQuery query = new TransactionQuery(
                portfolioId,
                null,
                null,
                now.minusDays(1),
                now.plusDays(1),
                null);

        Page<Transaction> result = repository.find(query, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAssetIdentifier().getPrimaryId()).isEqualTo("MSFT");
    }

    @Test
    public void find_paginatesCorrectly() {
        PortfolioEntity portfolioEntity = creaPortfolioEntity();
        AccountEntity accountEntity = createAccountEntity(portfolioEntity);
        UUID portfolioId = portfolioEntity.getId();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 12; i++) {
            persistTransaction(portfolioId, accountEntity, "AAPL", TransactionType.BUY, now.plusMinutes(i));
        }

        TransactionQuery query = new TransactionQuery(
                portfolioId,
                null,
                null,
                null,
                null,
                null);

        Page<Transaction> page = repository.find(query, PageRequest.of(0, 5));

        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isEqualTo(12);
    }

    @Test
    public void find_filtersByAssetSymbol() {
        PortfolioEntity portfolioEntity = creaPortfolioEntity();
        AccountEntity accountEntity = createAccountEntity(portfolioEntity);
        UUID portfolioId = portfolioEntity.getId();
        LocalDateTime now = LocalDateTime.now();

        persistTransaction(portfolioId, accountEntity, "AAPL", TransactionType.BUY, now);
        persistTransaction(portfolioId, accountEntity, "MSFT", TransactionType.SELL, now);

        TransactionQuery query = new TransactionQuery(
                portfolioId,
                null,
                null,
                LocalDateTime.of(2024, 01, 1, 0, 0),
                LocalDateTime.of(2027, 01, 1, 0, 0),
                Set.of("AAPL"));

        Page<Transaction> result = repository.find(query, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAssetIdentifier().getPrimaryId()).isEqualTo("AAPL");
    }

    @Test
    void countByPortfolioId_ReturnsCorrectTotal() {
        // Given
        PortfolioEntity portfolio = creaPortfolioEntity();
        AccountEntity account = createAccountEntity(portfolio);

        // Persist 3 transactions for this portfolio
        persistTransaction(portfolio.getId(), account, "AAPL", TransactionType.BUY, LocalDateTime.now());
        persistTransaction(portfolio.getId(), account, "MSFT", TransactionType.BUY, LocalDateTime.now());
        persistTransaction(portfolio.getId(), account, "GOOG", TransactionType.SELL, LocalDateTime.now());

        // When
        long count = repository.countByPortfolioId(new PortfolioId(portfolio.getId()));

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void countByAccountId_ReturnsCorrectTotal() {
        // Given
        PortfolioEntity portfolio = creaPortfolioEntity();
        AccountEntity accountA = createAccountEntity(portfolio);
        AccountEntity accountB = createAccountEntity(portfolio);

        // 2 transactions for Account A
        persistTransaction(portfolio.getId(), accountA, "AAPL", TransactionType.BUY, LocalDateTime.now());
        persistTransaction(portfolio.getId(), accountA, "MSFT", TransactionType.BUY, LocalDateTime.now());

        // 1 transaction for Account B
        persistTransaction(portfolio.getId(), accountB, "GOOG", TransactionType.SELL, LocalDateTime.now());

        // When
        long countA = repository.countByAccountId(new AccountId(accountA.getId()));
        long countB = repository.countByAccountId(new AccountId(accountB.getId()));

        // Then
        assertThat(countA).isEqualTo(2);
        assertThat(countB).isEqualTo(1);
    }

    @Test
    void count_ReturnsZero_WhenNoTransactionsExist() {
        // When
        long count = repository.countByPortfolioId(new PortfolioId(UUID.randomUUID()));

        // Then
        assertThat(count).isZero();
    }

    @Test
    void testMapPage_ThrowExceptionWhenPageableIsNull() throws Exception {
        // 1. Setup real mocks, not matchers
        JpaTransactionRepository mockJpaRepo = mock(JpaTransactionRepository.class);
        TransactionEntityMapper mockMapper = mock(TransactionEntityMapper.class);
        @SuppressWarnings("unchecked")
        Page<TransactionEntity> mockPage = mock(Page.class);

        // 2. Pass the mocks into the constructor
        TransactionQueryRepository repository = new TransactionQueryRepositoryImpl(
                mockJpaRepo,
                mockMapper,
                null);

        // 3. Reflection logic
        Method mapPage = repository.getClass().getDeclaredMethod("mapPage", Page.class, Pageable.class);
        mapPage.setAccessible(true);

        // 4. Execution & Assertion
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> {
            mapPage.invoke(repository, mockPage, null);
        });

        assertTrue(exception.getCause() instanceof NullPointerException);
    }

    // Helper: persist a transaction
    private TransactionEntity persistTransaction(
            UUID portfolioId,
            AccountEntity account,
            String symbol,
            TransactionType type,
            LocalDateTime transactionDate) {

        TransactionEntity entity = new TransactionEntity();
        entity.setId(UUID.randomUUID());
        entity.setAccount(account);
        entity.setPortfolioId(portfolioId);
        entity.setTransactionType(type);
        entity.setQuantity(new BigDecimal("10"));
        entity.setPriceAmount(new BigDecimal("150.00"));
        entity.setPriceCurrency("USD");
        entity.setTransactionDate(transactionDate.toInstant(ZoneOffset.UTC));
        entity.setFees(new ArrayList<>());
        entity.setAssetType("STOCK");
        entity.setPrimaryId(symbol);
        entity.setSecondaryIds(Map.of("ISIN", "US0378331005"));
        entity.setDisplayName(symbol + " Inc.");
        entity.setUnitOfTrade("shares");
        entity.setMetadata(Map.of("exchange", "NASDAQ"));
        entity.setIsDrip(false);

        return entityManager.persistAndFlush(entity);
    }

    private AccountEntity createAccountEntity(PortfolioEntity portfolio) {
        AccountEntity account = new AccountEntity();
        account.setId(UUID.randomUUID());
        account.setName("Test Account");
        account.setAccountType(AccountType.TFSA);
        account.setBaseCurrency("USD");
        account.setCashBalanceAmount(new BigDecimal("1000"));
        account.setCashBalanceCurrency("USD");
        account.setActive(true);
        account.setPortfolio(portfolio);
        account.setAssets(new ArrayList<>());
        account.setTransactions(new ArrayList<>());
        account.setCreateDate(testTime);
        account.setLastUpdated(testTime);
        // Use merge instead of persist
        return entityManager.getEntityManager().merge(account);
    }

    private PortfolioEntity creaPortfolioEntity() {
        PortfolioEntity entity = new PortfolioEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(testUserId);
        entity.setCurrencyPreference("USD");
        entity.setCreatedAt(testTime);
        entity.setUpdatedAt(testTime);
        entityManager.persist(entity); // persist immediately
        return entity;
    }

}
