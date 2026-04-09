package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioDeletionException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioLimitReachedException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.utils.ValidationUtils;
import com.laderrco.fortunelink.portfolio.application.validators.PortfolioLifecycleCommandValidator;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PortfolioLifecycleService {
  private static final String DEFAULT_NAME = "Default Account";

  private final PortfolioRepository portfolioRepository;
  private final PortfolioViewMapper portfolioViewMapper;

  private final PortfolioLifecycleCommandValidator validator;
  private final PortfolioLoader portfolioLoader;

  public PortfolioView createPortfolio(CreatePortfolioCommand command) {
    ValidationUtils.validate(command, validator::validate, "createPortfolio");

    if (portfolioRepository.existsActiveByUserId(command.userId())) {
      throw new PortfolioLimitReachedException("User already has an active portfolio");
    }

    Portfolio portfolio = Portfolio.createNew(command.userId(), command.name(),
        command.description(), command.currency());

    if (command.createDefaultAccount()) {
      portfolio.createAccount(DEFAULT_NAME, command.defaultAccountType(), command.currency(),
          command.defaultStrategy());
    }

    try {
      return portfolioViewMapper.toNewPortfolioView(portfolioRepository.save(portfolio));
    } catch (DataIntegrityViolationException e) {
      // This only happens if a race condition occurred between the check and the save
      throw new PortfolioLimitReachedException("A portfolio was recently created for this user.");
    }
  }

  public PortfolioView updatePortfolio(UpdatePortfolioCommand command) {
    ValidationUtils.validate(command, validator::validate, "updatePortfolio");
    Portfolio existing = portfolioLoader.loadUserPortfolio(command.portfolioId(), command.userId());
    existing.updateDetails(command.name(), command.description());
    if (command.currency() != null) {
      existing.updateDisplayCurrency(command.currency());
    }
    Portfolio saved = portfolioRepository.save(existing);
    return portfolioViewMapper.toNewPortfolioView(saved);
  }

  public void deletePortfolio(DeletePortfolioCommand command) {
    ValidationUtils.validate(command, validator::validate, "deletePortfolio");

    Portfolio portfolio = portfolioRepository.findByIdAndUserId(command.portfolioId(),
        command.userId()).orElseThrow(() -> new PortfolioNotFoundException(command.portfolioId()));

    if (command.softDelete()) {
      softDelete(portfolio, command);
    } else {
      hardDelete(portfolio, command);
    }
  }

  /**
   * Permanent removal from the database. Bypasses soft-delete audit trail.
   * Reserved for admin/test
   * use — no API path exposes this in production.
   * <p>
   * Domain invariants are still enforced: a portfolio with active accounts or
   * open positions cannot
   * be hard-deleted any more than soft-deleted. ON DELETE CASCADE in the schema
   * handles child rows,
   * but we validate business state here first so the error message is meaningful.
   */
  private void hardDelete(Portfolio portfolio, DeletePortfolioCommand command) {
    // Same invariant as soft delete, you cannot delete a live portfolio.
    // The DB cascade would handle orphaned rows, but silent data destruction
    // is worse than a rejected request.
    if (command.recursive()) {
      closeAllEligibleAccounts(portfolio);
    }

    long activeCount = portfolio.getAccounts().stream().filter(Account::isActive).count();

    if (activeCount > 0) {
      throw new PortfolioDeletionException(
          "Cannot hard delete portfolio with " + activeCount + " active account(s). "
              + "Close all accounts first, or use recursive=true.");
    }

    portfolioRepository.delete(command.portfolioId());
  }

  private void softDelete(Portfolio portfolio, DeletePortfolioCommand command) {
    if (command.recursive()) {
      closeAllEligibleAccounts(portfolio);
    }
    // Let domain exceptions (NotEmpty, AlreadyDeleted) bubble up naturally
    portfolio.markAsDeleted(command.userId());
    portfolioRepository.save(portfolio);
  }

  private void closeAllEligibleAccounts(Portfolio portfolio) {
    boolean hasNonEmptyAccounts = portfolio.getAccounts().stream().filter(Account::isActive)
        .anyMatch(acc -> acc.getPositionCount() > 0 || acc.getCashBalance().isPositive());

    if (hasNonEmptyAccounts) {
      throw new PortfolioDeletionException(
          "Recursive delete requires all accounts to have zero positions and zero cash balance.");
    }

    portfolio.getAccounts().stream().filter(Account::isActive)
        .forEach(acc -> portfolio.closeAccount(acc.getAccountId()));
  }
}
