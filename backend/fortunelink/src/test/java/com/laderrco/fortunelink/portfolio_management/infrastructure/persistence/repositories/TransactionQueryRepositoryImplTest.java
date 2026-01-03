package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories;

import static org.junit.Assert.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.TransactionQuery;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.TransactionQueryRepository;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.TransactionEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.AssetMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.PortfolioEntityMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.TransactionEntityMapper;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({ JpaPortfolioRepository.class, PortfolioEntityMapper.class, AssetMapper.class, TransactionEntityMapper.class })
public class TransactionQueryRepositoryImplTest {
    /*
     * We are NOT go t hrough the domain aggreagate or calling Portfolio methods. NO
     * test invariants here
     * we are inserting entitie sdirectly
     */

    @Autowired
    TransactionQueryRepository repository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void find_byPortfolioId_returnsTransactions() {
        UUID portfolioId = UUID.randomUUID();

        persistTx(portfolioId, null, "AAPL", TransactionType.BUY, now());
        persistTx(portfolioId, null, "MSFT", TransactionType.SELL, now());

        TransactionQuery query = new TransactionQuery(
                portfolioId,
                null,
                null,
                null,
                null,
                null);

        Page<Transaction> result = repository.find(query, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void find_paginatesCorrectly() {
        UUID portfolioId = UUID.randomUUID();

        for (int i = 0; i < 12; i++) {
            persistTx(portfolioId, null, "AAPL", TransactionType.BUY, now());
        }

        TransactionQuery query = new TransactionQuery(
                portfolioId, null, null, null, null, null);

        Page<Transaction> page = repository.find(query, PageRequest.of(0, 5));

        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isEqualTo(12);
    }

    @Test
    void find_filtersByDateRange() {
        UUID portfolioId = UUID.randomUUID();

        persistTx(portfolioId, null, "AAPL", TransactionType.BUY, now().minusDays(10));
        persistTx(portfolioId, null, "AAPL", TransactionType.BUY, now());

        TransactionQuery query = new TransactionQuery(
                portfolioId,
                null,
                null,
                now().minus(Duration.ofDays(1)),
                now().plus(Duration.ofDays(1)),
                null);

        Page<Transaction> result = repository.find(query, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void find_filtersByAssetSymbol() {
        UUID portfolioId = UUID.randomUUID();

        persistTransaction(portfolioId, null, "AAPL", TransactionType.BUY, now());
        persistTransaction(portfolioId, null, "MSFT", TransactionType.BUY, now());

        TransactionQuery query = new TransactionQuery(
                portfolioId,
                null,
                null,
                null,
                null,
                Set.of("AAPL"));

        Page<Transaction> result = repository.find(query, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)
                .getAssetIdentifier().getPrimaryId()).isEqualTo("AAPL");
    }

    private void persistTransaction(
            UUID portfolioId,
            UUID accountId,
            String symbol,
            TransactionType type,
            LocalDateTime timestamp) {

        TransactionEntity entity = new TransactionEntity();
        entity.setId(UUID.randomUUID());
        entity.setPortfolioId(portfolioId);
        entity.setaccoun(accountId);
        entity.setTransactionType(type.name());
        entity.setAssetSymbol(symbol);
        entity.setTimestamp(timestamp);

        entityManager.persist(entity);
    }
    private Instant now() {
        return Instant.now();
    }
}
