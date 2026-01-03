package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.AssetMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.PortfolioEntityMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers.TransactionEntityMapper;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({ JpaPortfolioRepository.class, PortfolioEntityMapper.class, AssetMapper.class, TransactionEntityMapper.class })
class JpaPortfolioRepositoryTest {

    @Autowired
    JpaPortfolioRepository repository;

    @Test
    void saveAndFindById_shouldPersistPortfolio() {
        // given
        UserId userId = new UserId(UUID.randomUUID());
        
        Portfolio portfolio = Portfolio.createNew(
            userId,
            ValidatedCurrency.USD);
            
        PortfolioId portfolioId = portfolio.getPortfolioId();
        // when
        Portfolio saved = repository.save(portfolio);

        // then
        Optional<Portfolio> found = repository.findById(portfolioId);

        assertThat(found).isPresent();
        assertThat(found.get().getPortfolioId()).isEqualTo(portfolioId);
        assertThat(found.get().getUserId()).isEqualTo(userId);
    }

    @Test
    void findByUserId_shouldReturnPortfolio() {
        UserId userId = new UserId(UUID.randomUUID());
        PortfolioId portfolioId = new PortfolioId(UUID.randomUUID());

        Instant time = LocalDateTime.of(2025, 01, 01, 0, 0).toInstant(ZoneOffset.UTC);
        Portfolio portfolio = Portfolio.reconstitute(
                portfolioId,
                userId,
                new ArrayList<>(),
                ValidatedCurrency.USD,
                time,
                time);

        repository.save(portfolio);

        Optional<Portfolio> result = repository.findByUserId(userId);

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(userId);
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
                ValidatedCurrency.USD,
                time,
                time);

        repository.save(portfolio);

        repository.delete(portfolioId);

        Optional<Portfolio> found = repository.findById(portfolioId);
        assertThat(found).isEmpty();
    }
}