package com.laderrco.fortunelink.portfolio.domain.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.util.List;
import java.util.Optional;

public interface PortfolioRepository {
  /**
   * Persists or updates a portfolio.
   *
   * @param portfolio The aggregate to save.
   * @return The saved portfolio instance.
   */
  Portfolio save(Portfolio portfolio);

  /**
   * Removes a portfolio by its ID.
   *
   * @param id The unique identifier of the portfolio to delete.
   */
  void delete(PortfolioId id);

  /**
   * Retrieves the full aggregate, ensuring both ID and ownership match. Use this when performing
   * mutations to maintain consistency.
   *
   * @param id     The portfolio identifier.
   * @param userId The owner identifier.
   * @return An Optional containing the portfolio if both conditions are met.
   */
  Optional<Portfolio> findByIdAndUserId(PortfolioId id, UserId userId);

  /**
   * Retrieves only active (non-deleted) portfolios for a user. Deleted portfolios are invisible to
   * the application layer and is filtered here.
   *
   * @param userId The user identifier.
   * @return A list of active portfolios.
   */
  List<Portfolio> findAllActiveByUserId(UserId userId);

  /**
   * Performs a lightweight ownership check without loading the full aggregate.
   *
   * @param id     The portfolio identifier.
   * @param userId The user identifier.
   * @return True if the portfolio belongs to the user, false otherwise.
   */
  boolean existsByIdAndUserId(PortfolioId id, UserId userId);

  /**
   * Ownership check if Portfolio has this account id in it or not. NOTE: this should be used after
   * checking if the Portfolio is apart of the user id via {@Link existsByIdAndUserId()}
   * @param id
   * @param accountId
   * @return True if the account belongs to the portfolio, false otherwise.
   */
  boolean existsByPortfolioIdAndAccountId(PortfolioId id, AccountId accountId);

  /**
   * Ownership check if account belongs to portfolio which belongs to user.
   * @param portfolioId
   * @param userId
   * @param accountId
   * @return
   */
  boolean existsByIdAndUserIdAndAccountId(PortfolioId portfolioId, UserId userId, AccountId accountId);

  /**
   * Counts ACTIVE (non-deleted) portfolios for a user.
   *
   * @param userId The user identifier.
   * @return The count of active portfolios.
   * @implNote Implementations MUST exclude soft-deleted portfolios (WHERE deleted = false). If
   * soft-deleted portfolios are counted, a user who deletes their portfolio to start anew, will be
   * permanently locked out of creating a new one.
   * @implNote JPA implementation example: @Query("SELECT COUNT(p) FROM Portfolio p WHERE p.userId =
   * :userId AND p.deleted = false") Long countActiveByUserId(@Param("userId") UserId userId);
   */
  Long countByUserId(UserId userId);
}
