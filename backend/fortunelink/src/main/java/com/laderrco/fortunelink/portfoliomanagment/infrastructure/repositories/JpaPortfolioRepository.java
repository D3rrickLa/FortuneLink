package com.laderrco.fortunelink.portfoliomanagment.infrastructure.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Portfolio;
import com.laderrco.fortunelink.portfoliomanagment.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.mapper.PortfolioMapper;


@Repository
public interface JpaPortfolioRepository extends JpaRepository<Portfolio, UUID>, PortfolioRepository {    
    private final SpringDataJpaPortfolioEntityRepository jpaRepository;
    private final PortfolioMapper portfolioMapper;

    public JpaPortfolioRepository(SpringDataJpaPortfolioEntityRepository jpaRepository,
                                  PortfolioMapper portfolioMapper) {
        this.jpaRepository = jpaRepository;
        this.portfolioMapper = portfolioMapper;
    }

    @Override
    public Optional<Portfolio> findById(UUID id) {
        return jpaRepository.findById(id)
                            .map(portfolioMapper::toDomain);
    }

    @Override
    public Portfolio save(Portfolio portfolio) {
        // Load existing entity if it exists to ensure JPA's @Version is handled correctly
        Optional<PortfolioEntity> existingEntityOpt = jpaRepository.findById(portfolio.getPortfolioId().value());
        PortfolioEntity entityToSave;

        if (existingEntityOpt.isPresent()) {
            // Update the existing entity's state from the domain object
            entityToSave = existingEntityOpt.get();
            // Crucial: Manually copy fields that could have changed
            // This is where the mapping from domain to entity state occurs.
            // Note: `id`, `userId` are typically immutable after creation
            entityToSave.setName(portfolio.getName());
            entityToSave.setDescription(portfolio.getDescription());
            entityToSave.setCashBalanceAmount(portfolio.getPortfolioCashBalance().amount());
            entityToSave.setCashBalanceCurrencyCode(portfolio.getPortfolioCashBalance().currency().getCurrencyCode());
            entityToSave.setCurrencyPreferenceCode(portfolio.getCurrencyPreference().getCurrencyCode());

            // Clear and re-add children to handle additions/removals
            entityToSave.getAssetHoldings().clear();
            portfolio.getAssetHoldings().forEach(ah -> entityToSave.addAssetHolding(portfolioMapper.assetHoldingMapper.toEntity(ah, entityToSave)));

            entityToSave.getLiabilities().clear();
            portfolio.getLiabilities().forEach(liab -> entityToSave.addLiability(portfolioMapper.liabilityMapper.toEntity(liab, entityToSave)));

            entityToSave.getTransactions().clear();
            portfolio.getTransactions().forEach(tx -> entityToSave.addTransaction(portfolioMapper.transactionMapper.toEntity(tx, entityToSave)));

            // JPA will increment the version automatically when saving the modified entity
        } else {
            // New portfolio, create a new entity from the domain object
            entityToSave = portfolioMapper.toEntity(portfolio);
        }

        PortfolioEntity savedEntity = jpaRepository.save(entityToSave);
        return portfolioMapper.toDomain(savedEntity);
    }
}