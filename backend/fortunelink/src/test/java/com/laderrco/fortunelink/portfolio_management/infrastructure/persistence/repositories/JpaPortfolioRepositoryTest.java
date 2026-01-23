package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.JdbcDatabaseContainer.NoDriverFoundException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.PortfolioEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.AssetEntityMapperImpl;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.PortfolioEntityMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.PortfolioEntityMapperImpl;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.TransactionEntityMapperImpl;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // Tell Spring NOT to use H2
@Import({ JpaPortfolioRepository.class, PortfolioEntityMapperImpl.class, AssetEntityMapperImpl.class,
        TransactionEntityMapperImpl.class })
class JpaPortfolioRepositoryTest {

    @Autowired
    JpaPortfolioRepository repository;

    @Autowired
    private PortfolioEntityMapper portfolioMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.7");

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

    @Test
    void saveAndFindById_shouldPersistPortfolio() {
        // given
        UserId userId = new UserId(UUID.randomUUID());

        Portfolio portfolio = Portfolio.createNew(
                userId,
                ValidatedCurrency.USD,
                "Name",
                "Desc");

        PortfolioId portfolioId = portfolio.getPortfolioId();
        // when
        repository.save(portfolio);

        // then
        Optional<Portfolio> found = repository.findById(portfolioId);

        assertThat(found).isPresent();
        assertThat(found.get().getPortfolioId()).isEqualTo(portfolioId);
        assertThat(found.get().getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should map entities to domain objects correctly")
    void shouldMapToDomainList() {
        // Given
        UserId testUserId = UserId.randomId();

        // 1. Insert the user into the auth schema to satisfy the FK constraint
        jdbcTemplate.update("INSERT INTO auth.users (id, email) VALUES (?, ?)",
                testUserId.userId(), "test@example.com");

        // 2. Now the database will allow this save
        PortfolioEntity entity = createEntity("Main Portfolio", testUserId);
        repository.save(portfolioMapper.toDomain(entity));

        // When
        List<Portfolio> entities = repository.findAllByUserId(testUserId);

        // Then
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).getName()).isEqualTo("Main Portfolio");
    }

    @Test
    void findByUserId_shouldReturnPortfolio() throws NoDriverFoundException, SQLException {
        // 1. Create the ID
        UUID rawUserId = UUID.randomUUID();
        UserId userId = new UserId(rawUserId);

        // 2. INSERT the user into the auth schema so the FK is satisfied
        // We use the postgres container connection to do this manually
        try (var conn = postgres.createConnection("")) {
            var st = conn.createStatement();
            st.execute(String.format(
                    "INSERT INTO auth.users (id, email) VALUES ('%s', 'test@example.com')",
                    rawUserId));
        }

        // 3. Now you can safely save the portfolio
        PortfolioId portfolioId = new PortfolioId(UUID.randomUUID());
        Instant time = LocalDateTime.of(2025, 01, 01, 0, 0).toInstant(ZoneOffset.UTC);

        Portfolio portfolio = Portfolio.reconstitute(
                portfolioId,
                userId,
                new ArrayList<>(),
                "Name",
                ValidatedCurrency.USD,
                "Desc",
                false,
                null,
                null,
                time,
                time);

        repository.save(portfolio);

        // 4. Verify
        Optional<Portfolio> result = repository.findByUserId(userId);
        assertThat(result).isPresent();
    }

    @Test
    void delete_shouldRemovePortfolio() {
        PortfolioId portfolioId = new PortfolioId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        Instant time = LocalDateTime.of(2025, 01, 01, 0, 0).toInstant(ZoneOffset.UTC);
        Portfolio portfolio = Portfolio.reconstitute(
                portfolioId,
                userId,
                new ArrayList<>(),
                "Name",
                ValidatedCurrency.USD,
                "Desc",
                false,
                null,
                null,
                time,
                time);

        repository.save(portfolio);

        repository.delete(portfolioId);

        Optional<Portfolio> found = repository.findById(portfolioId);
        assertThat(found).isEmpty();
    }

    private PortfolioEntity createEntity(String name, UserId userId) {
        PortfolioEntity entity = new PortfolioEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId.userId());
        entity.setName(name);
        entity.setCurrencyPreference("USD");
        entity.setDescription("name");
        entity.setAccounts(List.of());
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        // Set other mandatory fields
        return entity;
    }
}