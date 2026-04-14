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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SuppressWarnings("resource")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
class JpaTransactionRepositoryTest {
  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
      "postgres:17.9-alpine").withDatabaseName("testdb").withUsername("test").withPassword("test")
      // This runs BEFORE Flyway starts
      .withInitScript("init-auth.sql").withInitScript("seed_user_portfolio.sql");

  static {
    postgres.start(); // Manual start to ensure we can run commands before Flyway
    try {
      // Execute the SQL directly against the running container
      postgres.execInContainer("psql", "-U", "test", "-c", "CREATE SCHEMA IF NOT EXISTS auth;");
      postgres.execInContainer("psql", "-U", "test", "-c",
          "CREATE TABLE IF NOT EXISTS auth.users (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), email TEXT);");
      postgres.execInContainer("psql", "-U", "postgres", "-d", "testdb", "-c", "\\d portfolios");
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize dummy auth schema", e);
    }
  }

  private final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
  @Autowired
  private JpaPortfolioRepository portfolioRepository;
  @Autowired
  private TestEntityManager entityManager;
  @Autowired
  private JpaTransactionRepository jpaRepository; // Test the interface directly

  @AfterAll
  static void stopContainer() {
    if (postgres != null) {
      postgres.stop();
      postgres.close();
    }
  }

  /*
   * FeeAggregationResult interface projection, this is a runtime risk, not a
   * compile-time one. The JPQL query returns a projection onto an interface:
   * javaList<FeeAggregationResult>
   * sumBuyFeesByAccountAndSymbol(@Param("accountIds") List<UUID> accountIds);
   *
   *
   * Spring Data JPA projections onto interfaces work when the property names on
   * the interface match the aliases in the query exactly, case-sensitively. Your
   * query uses as accountId, as symbol, as totalFees, as currency , and your
   * interface has getAccountId(), getSymbol(), getTotalFees(), getCurrency().
   * That should work, but verify it with an integration test before you ship. If
   * it silently returns nulls you'll have a fee calculation bug that's hard to
   * diagnose.
   */
  @Test
  void shouldProperlyProjectAggregatedFees() {
    UUID portfolioId = UUID.randomUUID();
    PortfolioJpaEntity portfolio = PortfolioJpaEntity.create(portfolioId, TEST_USER_ID,
        "Portfolio name", "DESC", "CAD", false, null, null, Instant.now(), Instant.now());

    portfolioRepository.save(portfolio);

    UUID accountId = UUID.randomUUID();

    AccountJpaEntity account = AccountJpaEntity.create(accountId, portfolio, "Account name", "TFSA",
        "CAD", "ACB", "HEALTHY", "ACTIVE", BigDecimal.valueOf(10000), "CAD", null, Instant.now(),
        Instant.now());

    portfolio.replaceAccounts(List.of(account));
    portfolioRepository.save(portfolio);

    TransactionJpaEntity tx = createTestTransaction(accountId, portfolioId);
    tx.setAccountId(accountId);
    tx.setExecutionSymbol("AAPL");
    tx.setTransactionType("BUY");
    tx.setCashDeltaCurrency("USD");
    tx.setExcluded(false);
    tx.setCashDeltaAmount(new BigDecimal("-1000.00"));
    tx.setMetadataSource("MANUAL");

    FeeJpaEntity fee = FeeJpaEntity.createEmpty();
    fee.setNativeAmount(new BigDecimal("10.50"));
    fee.setTransaction(tx);
    fee.setFeeType("BROKERAGE");
    fee.setOccurredAt(Instant.now());
    fee.setNativeCurrency("CAD");
    tx.setFees(List.of(fee));

    jpaRepository.save(tx);
    entityManager.flush();
    entityManager.clear();

    var results = jpaRepository.sumBuyFeesByAccountAndSymbol(List.of(accountId));

    assertThat(results).hasSize(1);
    FeeAggregationResult result = results.get(0);

    // This is the critical check for the "Silent Null" issue
    assertThat(result.getAccountId()).isEqualTo(accountId);
    assertThat(result.getSymbol()).isEqualTo("AAPL");
    assertThat(result.getTotalFees()).isEqualByComparingTo("10.50");
    assertThat(result.getCurrency()).isEqualTo("USD");
    assertThat(results).isNotEmpty();
    assertThat(results.get(0).getTotalFees()).isNotNull();

  }

  private TransactionJpaEntity createTestTransaction(UUID accountId, UUID portfolioId) {
    return TransactionJpaEntity.create(UUID.randomUUID(), portfolioId, accountId, "BUY", "AAPL",
        null, BigDecimal.valueOf(1000), "USD", "STOCK", null, null, null, "USD", null, false, null,
        null, null, null, "SOME NOTES HERE", Instant.now(), null, null);
  }
}