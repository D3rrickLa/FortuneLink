package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.PortfolioJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@code PortfolioJpaEntity}.
 * <p>
 * Only this interface and {@code PortfolioRepositoryImpl} interact with JPA
 * directly. The rest of
 * the application sees the domain-level {@code PortfolioRepository} interface
 * only.
 */
@Repository
public interface JpaPortfolioRepository extends JpaRepository<PortfolioJpaEntity, UUID> {

  Optional<PortfolioJpaEntity> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

  /**
   * Fetches the portfolio with its complete account graph (accounts → positions +
   * realized gains).
   * Used for both reads and writes so Hibernate tracks the managed instances for
   * dirty-checking on
   * save.
   */
  @EntityGraph(attributePaths = { "accounts", "accounts.positions", "accounts.realizedGains" })
  Optional<PortfolioJpaEntity> findWithAccountsByIdAndUserId(@Param("id") UUID id,
      @Param("userId") UUID userId);

  /**
   * Returns all non-deleted portfolios for a user. Soft-deleted rows
   * ({@code deleted = true}) are
   * intentionally excluded.
   */
  @EntityGraph(attributePaths = { "accounts", "accounts.positions", "accounts.realizedGains" })
  @Query("""
      SELECT p FROM PortfolioJpaEntity p
      WHERE p.userId = :userId
        AND p.deleted = false
      """)
  List<PortfolioJpaEntity> findAllActiveByUserId(@Param("userId") UUID userId);

  @Query("""
      SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
      FROM PortfolioJpaEntity p
      WHERE p.userId = :userId
        AND p.deleted = false
      """)
  boolean existsActiveByUserId(@Param("userId") UUID userId);

  @Query("""
      SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
      FROM PortfolioJpaEntity p
      WHERE p.id = :id
        AND p.userId = :userId
      """)
  boolean existsByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

  /**
   * Ownership check: does this portfolio contain this account? Used by
   * {@code PortfolioLoader.validateAccountOwnershipToPortfolio()}.
   */
  @Query("""
      SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
      FROM PortfolioJpaEntity p
      JOIN p.accounts a
      WHERE p.id = :portfolioId
        AND a.id = :accountId
      """)
  boolean existsByIdAndAccountId(@Param("portfolioId") UUID portfolioId,
      @Param("accountId") UUID accountId);

  /**
   * Three-way ownership check: portfolio belongs to user AND contains account.
   */
  @Query("""
      SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
      FROM PortfolioJpaEntity p
      JOIN p.accounts a
      WHERE p.id = :portfolioId
        AND p.userId = :userId
        AND a.id = :accountId
      """)
  boolean existsByIdAndUserIdAndAccountId(@Param("portfolioId") UUID portfolioId,
      @Param("userId") UUID userId, @Param("accountId") UUID accountId);

  /**
   * Count of active (non-deleted) portfolios per user. Used to enforce the
   * one-portfolio-per-user
   * limit.
   * <p>
   * IMPORTANT: must exclude soft-deleted rows. See {@code PortfolioRepository}
   * interface Javadoc
   * for the rationale.
   */
  @Query("""
      SELECT COUNT(p)
      FROM PortfolioJpaEntity p
      WHERE p.userId = :userId
        AND p.deleted = false
      """)
  Long countActiveByUserId(@Param("userId") UUID userId);

  /**
   * Loads the full aggregate graph by portfolio ID only. Ownership is
   * pre-validated by
   * PortfolioLoader before save is called — repeating the userId check here is
   * redundant and costs
   * a round-trip.
   */
  @EntityGraph(attributePaths = { "accounts", "accounts.positions", "accounts.realizedGains" })
  @Query("SELECT p FROM PortfolioJpaEntity p WHERE p.id = :id")
  Optional<PortfolioJpaEntity> findWithAccountsById(@Param("id") UUID id);

  @Modifying
  @Query("UPDATE AccountJpaEntity a SET a.healthStatus = 'STALE' WHERE a.id = :accountId")
  void markAccountStale(@Param("accountId") UUID accountId);

  @Query("SELECT DISTINCT p.userId FROM PortfolioJpaEntity p WHERE p.deleted = false")
  List<UUID> findAllActiveUserIds();
}