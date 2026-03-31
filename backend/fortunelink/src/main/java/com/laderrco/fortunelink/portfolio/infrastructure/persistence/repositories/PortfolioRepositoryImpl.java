package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.PortfolioJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.mappers.PortfolioDomainMapper;

import lombok.RequiredArgsConstructor;

/**
 * Implements the domain {@code PortfolioRepository} port using JPA.
 * <p>
 * This class knows about both layers by design — that is its entire purpose.
 * All other classes in the application layer see only the interface.
 * <p>
 * Save strategy: load the managed JPA entity first (if it exists), then
 * pass it to the mapper for an in-place update so Hibernate's dirty-checking
 * works correctly and doesn't issue a DELETE + INSERT for every save.
 */
@Repository
@RequiredArgsConstructor
public class PortfolioRepositoryImpl implements PortfolioRepository {

  private final JpaPortfolioRepository jpaRepo;
  private final PortfolioDomainMapper mapper;

  // -------------------------------------------------------------------------
  // Write
  // -------------------------------------------------------------------------

  @Override
  public Portfolio save(Portfolio domain) {
    Objects.requireNonNull(domain, "Portfolio cannot be null");

    UUID id = UUID.fromString(domain.getPortfolioId().toString());

    // Load the managed entity to support in-place update.
    // findWithAccountsByIdAndUserId uses an @EntityGraph so positions and
    // gains are already in the session — no lazy-load surprises.
    Optional<PortfolioJpaEntity> existing = jpaRepo.findWithAccountsByIdAndUserId(
        id, UUID.fromString(domain.getUserId().toString()));

    PortfolioJpaEntity entity = mapper.toEntity(domain, existing.orElse(null));
    PortfolioJpaEntity saved = jpaRepo.save(entity);

    return mapper.toDomain(saved);
  }

  @Override
  public void delete(PortfolioId id) {
    Objects.requireNonNull(id, "PortfolioId cannot be null");
    jpaRepo.deleteById(UUID.fromString(id.toString()));
  }

  // -------------------------------------------------------------------------
  // Read
  // -------------------------------------------------------------------------

  @Override
  public Optional<Portfolio> findByIdAndUserId(PortfolioId id, UserId userId) {
    return jpaRepo
        .findWithAccountsByIdAndUserId(
            UUID.fromString(id.toString()),
            UUID.fromString(userId.toString()))
        .map(mapper::toDomain);
  }

  @Override
  public List<Portfolio> findAllActiveByUserId(UserId userId) {
    // Spring Data query — see JpaPortfolioRepository for the @Query definition.
    return jpaRepo
        .findAllActiveByUserId(UUID.fromString(userId.toString()))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public Optional<Portfolio> findWithAccountsByIdAndUserId(PortfolioId id, UserId userId) {
    return jpaRepo
        .findWithAccountsByIdAndUserId(
            UUID.fromString(id.toString()),
            UUID.fromString(userId.toString()))
        .map(mapper::toDomain);
  }

  // -------------------------------------------------------------------------
  // Existence checks (lightweight — no mapping needed)
  // -------------------------------------------------------------------------

  @Override
  public boolean existsActiveByUserId(UserId userId) {
    return jpaRepo.existsActiveByUserId(UUID.fromString(userId.toString()));
  }

  @Override
  public boolean existsByIdAndUserId(PortfolioId id, UserId userId) {
    return jpaRepo.existsByIdAndUserId(
        UUID.fromString(id.toString()),
        UUID.fromString(userId.toString()));
  }

  @Override
  public boolean existsByPortfolioIdAndAccountId(PortfolioId portfolioId, AccountId accountId) {
    return jpaRepo.existsByIdAndAccountId(
        UUID.fromString(portfolioId.toString()),
        UUID.fromString(accountId.toString()));
  }

  @Override
  public boolean existsByIdAndUserIdAndAccountId(PortfolioId portfolioId, UserId userId,
      AccountId accountId) {
    return jpaRepo.existsByIdAndUserIdAndAccountId(
        UUID.fromString(portfolioId.toString()),
        UUID.fromString(userId.toString()),
        UUID.fromString(accountId.toString()));
  }

  @Override
  public Long countByUserId(UserId userId) {
    return jpaRepo.countActiveByUserId(UUID.fromString(userId.toString()));
  }
}