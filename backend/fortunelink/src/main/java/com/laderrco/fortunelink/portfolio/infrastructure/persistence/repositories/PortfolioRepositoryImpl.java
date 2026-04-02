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
  private final JpaPortfolioRepository jpaRepository;
  private final PortfolioDomainMapper mapper;

  /*
   * Warning about the save strategy. It loads the managed JPA entity before every
   * save so Hibernate's dirty-checking works correctly. That's one extra query
   * per write. For an MVP with low write volume this is fine. If you're ever
   * doing bulk imports you'll want a different path
   */
  @Override
  public Portfolio save(Portfolio domain) {
    Objects.requireNonNull(domain, "Portfolio cannot be null");

    UUID id = UUID.fromString(domain.getPortfolioId().toString());

    // Single SELECT with entity graph. Ownership was already validated by
    // PortfolioLoader before this call, no need to re-check userId here.
    Optional<PortfolioJpaEntity> existing = jpaRepository.findWithAccountsById(id);

    PortfolioJpaEntity entity = mapper.toEntity(domain, existing.orElse(null));
    return mapper.toDomain(jpaRepository.save(entity));
  }

  @Override
  public void delete(PortfolioId id) {
    Objects.requireNonNull(id, "PortfolioId cannot be null");
    jpaRepository.deleteById(UUID.fromString(id.toString()));
  }

  @Override
  public Optional<Portfolio> findByIdAndUserId(PortfolioId id, UserId userId) {
    return jpaRepository
        .findWithAccountsByIdAndUserId(
            UUID.fromString(id.toString()),
            UUID.fromString(userId.toString()))
        .map(mapper::toDomain);
  }

  @Override
  public List<Portfolio> findAllActiveByUserId(UserId userId) {
    // Spring Data query — see JpaPortfolioRepository for the @Query definition.
    return jpaRepository
        .findAllActiveByUserId(UUID.fromString(userId.toString()))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public Optional<Portfolio> findWithAccountsByIdAndUserId(PortfolioId id, UserId userId) {
    return jpaRepository
        .findWithAccountsByIdAndUserId(
            UUID.fromString(id.toString()),
            UUID.fromString(userId.toString()))
        .map(mapper::toDomain);
  }

  @Override
  public boolean existsActiveByUserId(UserId userId) {
    return jpaRepository.existsActiveByUserId(UUID.fromString(userId.toString()));
  }

  @Override
  public boolean existsByIdAndUserId(PortfolioId id, UserId userId) {
    return jpaRepository.existsByIdAndUserId(
        UUID.fromString(id.toString()),
        UUID.fromString(userId.toString()));
  }

  @Override
  public boolean existsByPortfolioIdAndAccountId(PortfolioId portfolioId, AccountId accountId) {
    return jpaRepository.existsByIdAndAccountId(
        UUID.fromString(portfolioId.toString()),
        UUID.fromString(accountId.toString()));
  }

  @Override
  public boolean existsByIdAndUserIdAndAccountId(PortfolioId portfolioId, UserId userId,
      AccountId accountId) {
    return jpaRepository.existsByIdAndUserIdAndAccountId(
        UUID.fromString(portfolioId.toString()),
        UUID.fromString(userId.toString()),
        UUID.fromString(accountId.toString()));
  }

  @Override
  public Long countByUserId(UserId userId) {
    return jpaRepository.countActiveByUserId(UUID.fromString(userId.toString()));
  }
}