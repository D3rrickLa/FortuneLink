package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.AccountEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.PortfolioEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.TransactionEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.AssetMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.PortfolioEntityMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.TransactionEntityMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.queries.TransactionQuery;

import jakarta.transaction.Transactional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Transactional
@ActiveProfiles("test")
@Import({ 
    TransactionQueryRepositoryImpl.class, 
    TransactionEntityMapper.class,
    PortfolioEntityMapper.class, 
    AssetMapper.class, 
})
public class TransactionQueryRepositoryImplTest {
    /*
     * We are NOT going through the domain aggreagate or calling Portfolio methods. NO
     * test invariants here
     * we are inserting entitie sdirectly
     */

    @Autowired
    private TransactionQueryRepository repository;
    

    @Autowired
    private TestEntityManager entityManager;

    Instant testTime = Instant.now();

    @Test
    public void findByPortfolio_returnsTransactions() {
        PortfolioEntity portfolioEntity = creaPortfolioEntity();
        AccountEntity accountEntity = createAccountEntity(null);
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

    // Helper: persist a transaction
    // helper to persist a transaction
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
        entity.setTransactionDate(transactionDate.toInstant(ZoneOffset.UTC)); // LocalDateTime, not Instant
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
        account.setAccountType(AccountType.TFSA.toString());
        account.setBaseCurrency("USD");
        account.setCashBalanceAmount(new BigDecimal("1000"));
        account.setCashBalanceCurrency("USD");
        account.setActive(true);
        account.setPortfolio(portfolio);
        account.setAssets(new ArrayList<>());
        account.setTransactions(new ArrayList<>());
        account.setCreatedAt(testTime);
        account.setLastUpdated(testTime);
        // Use merge instead of persist
        return entityManager.getEntityManager().merge(account);
    }

    private PortfolioEntity creaPortfolioEntity() {
        PortfolioEntity entity = new PortfolioEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(UUID.randomUUID());
        entity.setCurrencyPreference("USD");
        entity.setCreatedAt(testTime);
        entity.setUpdatedAt(testTime);
        entityManager.persist(entity); // persist immediately
        return entity;
    }

}
