package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.AccountJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.FeeJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.PortfolioJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.TransactionJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.valueobjects.FeeAggregationResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaTransactionRepositoryTest {
  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private JpaPortfolioRepository portfolioRepository;

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private JpaTransactionRepository jpaRepository;

  private final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  @BeforeEach
  void setUp() {
    // Seed the user that Supabase usually handles.
    // Flyway has already run by this point, so the table exists.
    jdbcTemplate.execute(String.format(
        "INSERT INTO users (id, email) VALUES ('%s', 'test@example.com') ON CONFLICT DO NOTHING",
        TEST_USER_ID));
  }

  @Test
  void shouldProperlyProjectAggregatedFees() {
    // Given
    UUID portfolioId = UUID.randomUUID();
    PortfolioJpaEntity portfolio = PortfolioJpaEntity.create(
        portfolioId,
        TEST_USER_ID, // Matches the seeded user
        "Portfolio name",
        "DESC",
        "CAD",
        false,
        null,
        null,
        Instant.now(),
        Instant.now());

    // Use persist and flush for cleaner lifecycle management in DataJpaTest
    entityManager.persist(portfolio);
    portfolioRepository.save(portfolio);

    UUID accountId = UUID.randomUUID();
    AccountJpaEntity account = AccountJpaEntity.create(
        accountId,
        portfolio,
        "Account name",
        "TFSA",
        "CAD",
        "ACB",
        "HEALTHY",
        "ACTIVE",
        BigDecimal.valueOf(10000),
        "CAD",
        null,
        Instant.now(),
        Instant.now());

    portfolio.replaceAccounts(List.of(account));
    entityManager.merge(portfolio);

    TransactionJpaEntity tx = createTestTransaction(accountId, portfolioId);
    tx.setAccountId(accountId);
    tx.setExecutionSymbol("AAPL");
    tx.setTransactionType("BUY");
    tx.setCashDeltaCurrency("USD");
    tx.setExcluded(false);
    tx.setCashDeltaAmount(new BigDecimal("-1000.00"));
    tx.setMetadataSource("MANUAL");

    FeeJpaEntity fee = FeeJpaEntity.create(
        tx, "BROKERAGE", null, "CAD", null, null, null, null,
        null, null, Instant.now());
    fee.setNativeAmount(new BigDecimal("10.50"));
    fee.setNativeCurrency("CAD");

    tx.setFees(List.of(fee));

    // When
    jpaRepository.save(tx);
    entityManager.flush();
    entityManager.clear();

    var results = jpaRepository.sumBuyFeesByAccountAndSymbol(List.of(accountId));

    // Then
    assertThat(results).hasSize(1);
    FeeAggregationResult result = results.get(0);

    assertThat(result.getAccountId()).isEqualTo(accountId);
    assertThat(result.getSymbol()).isEqualTo("AAPL");
    // Note: Check your query logic; if the interface expects 10.50, ensure types
    // match
    assertThat(result.getTotalFees()).isEqualByComparingTo("10.50");
  }

  private TransactionJpaEntity createTestTransaction(UUID accountId, UUID portfolioId) {
    return TransactionJpaEntity.create(UUID.randomUUID(), portfolioId, accountId, "BUY", "AAPL",
        null, BigDecimal.valueOf(1000), "USD", "STOCK", null, null, null, "USD", null, false, null,
        null, null, null, "SOME NOTES HERE", Instant.now(), null, null);
  }
}